# Synchronization API (sync-api)

## 🎯 Purpose

`sync-api` computes the rights of each account on each application: it evaluates the OPA policy of the application
with the payload of the account, then renders the claims template of the application. The results are written in the
`computed_rights` table of the LinId database.

👉 See also: [sync-api README](../../sync-api/README.md) ·
[`sync-data` configuration](../configuration/application/config-files.md#-account-payloads--sync-data) ·
[advanced/policies](policies.md)

---

## 🏗️ How it works

```text
linid-api ── mTLS ──► sync-api ── payload ──► OPA
                         │
                         ├──► LinId database: accounts, payloads, computed rights
                         └──► sync_api database: Spring Batch jobs, locks
```

---

## 🔄 Synchronization cycle

Every day at 2:00 UTC by default (`SYNC_CRON`), the cycle runs two steps:

1. **Payloads**: builds the payload of each account, by chunks of `SYNC_PAYLOAD_CHUNK_SIZE` accounts. Only the changed
   payloads are written.
2. **Rights**: computes the new account and application pairs, and those whose payload, policy or claims template
   changed, by chunks of `SYNC_COMPUTE_CHUNK_SIZE` pairs.

* Each chunk is written in its own transaction.
* The rights are not computed when the payload build fails.
* A failed cycle is not restarted: the next cycle computes the pairs left to compute.

---

## 🔁 Recomputation

linid-api relays the recompute buttons and the status of the cycle to `sync-api`:

| Endpoint                                   | Behavior                                                        |
| ------------------------------------------ | --------------------------------------------------------------- |
| `POST /accounts/{id}/recompute-rights`     | Rebuilds the payload of the account, then computes its rights   |
| `POST /applications/{id}/recompute-rights` | Rebuilds all the payloads, then computes the application rights |
| `GET /cycle`                               | `{ "running": true }` while a cycle runs, on any instance       |

* The endpoint answers at once `202` `{ "alreadyRunning": false }`, and the job runs in the background.
* Only one recomputation per account or application runs at a time: a second request answers
  `{ "alreadyRunning": true }`.
* As in the cycle, only the pairs whose payload, policy or claims template changed are computed.
* An instance runs `core-size` recomputations and queues `queue-capacity` more at most (`spring.task.execution.pool.*`
  of `application.yaml`, 8 and 16 by default): beyond that, it answers `503`.

---

## 🔒 Several instances

Several instances of `sync-api` can run at the same time, on the same databases. PostgreSQL locks keep them from doing
the same work twice, or from writing the same account at the same time:

| Situation                                                        | What happens                                                                                                                                                                                      |
| ---------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Two instances reach the time of the cycle                        | Only one runs it, the other skips it                                                                                                                                                              |
| A recomputation of the account or application is already running | The new request starts nothing and answers `alreadyRunning: true`                                                                                                                                 |
| Two processings (cycle or recomputations) reach the same account | The second waits until the first has written the account, and fails if this wait exceeds `SYNC_LOCK_TIMEOUT`                                                                                      |
| A process of an instance is killed                               | Its locks are released at once, or within `client_connection_check_interval` if it was waiting for a lock                                                                                         |
| The host or the network of an instance is lost                   | Its locks are released at `SYNC_LOCK_TIMEOUT` if it was waiting for a lock, otherwise they stay held until the TCP keepalive of PostgreSQL detects it, 2 h 11 by default (see `tcp_keepalives_*`) |

---

## ⚙️ Deployment constraints

### `sync_api` database

The cycle lock lives in a PostgreSQL session, open and idle during the whole cycle. If this session is closed, the
lock is released and another instance could start the same cycle.

| Setting                                                           | Value                                                                         | Why                                                                                      |
| ----------------------------------------------------------------- | ----------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------- |
| PgBouncer                                                         | No transaction pooling                                                        | It does not keep the session of the lock                                                 |
| `idle_session_timeout` of `DATABASE_USER`                         | Not set                                                                       | It would close the idle session of the lock                                              |
| `max_connections`                                                 | Pool + `core-size` + `queue-capacity` + 1 per instance (pool + 25 by default) | The cycle and each recomputation, running or waiting, hold a connection outside the pool |
| `tcp_keepalives_idle` / `_interval` / `_count` of `DATABASE_USER` | `60` / `10` / `6` (recommended)                                               | A lost host or network keeps the locks about 2 min instead of 2 h 11                     |

### LinId database

| Setting                                                                 | Value                                                      | Why                                                                                    |
| ----------------------------------------------------------------------- | ---------------------------------------------------------- | -------------------------------------------------------------------------------------- |
| `LINID_DATABASE_MAX_POOL_SIZE`                                          | `core-size` + 1, i.e. `9` (default `5`)                    | One connection for the cycle and for each running recomputation                        |
| `SYNC_LOCK_TIMEOUT`                                                     | Above the longest chunk (default `30m`)                    | A chunk keeps its account locks during its OPA calls                                   |
| `max_locks_per_transaction`                                             | Room for `SYNC_PAYLOAD_CHUNK_SIZE` locks per running chunk | A payload chunk locks each of its accounts                                             |
| `client_connection_check_interval` of `LINID_DATABASE_USER`             | `10s` (recommended)                                        | The locks of a killed instance are released within 10 s instead of `SYNC_LOCK_TIMEOUT` |
| `tcp_keepalives_idle` / `_interval` / `_count` of `LINID_DATABASE_USER` | `60` / `10` / `6` (recommended)                            | A lost host or network keeps the locks about 2 min instead of 2 h 11                   |

---

## 🚨 Alerts

`sync-api` logs each anomaly as an alert: `ERROR` when the processing stops, `WARN` when it goes on.

| Alert              | Level   | Anomaly                                                                  |
| ------------------ | ------- | ------------------------------------------------------------------------ |
| `invalidMapping`   | `ERROR` | Invalid `mapping` template, the payload build is aborted                 |
| `sourceUnreadable` | `ERROR` | Source that cannot be read, the payload build is aborted                 |
| `opaUnreachable`   | `ERROR` | OPA cannot be reached, the rights computation is aborted                 |
| `jobNotStarted`    | `ERROR` | Job that cannot be started, left to the next cycle                       |
| `jobNotCompleted`  | `ERROR` | Job ended with another status than `COMPLETED`, left to the next cycle   |
| `payloadError`     | `WARN`  | Account in error during the payload build, it keeps its previous payload |
| `unknownRole`      | `WARN`  | Role returned by OPA and missing from the roles of the application       |
| `pairError`        | `WARN`  | Pair in error, it keeps its previous rights until it is computed again   |
