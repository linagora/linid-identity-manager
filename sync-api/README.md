# LinID Synchronization API

## Description

The **LinID Synchronization API** (`sync-api`) is a REST API built with Spring Boot and Spring Batch. Its batch jobs
read the account data in the linid-api database, compute the rights of each account on each application, and write
them to the linid-api database. They are started by a cron and by endpoints called by linid-api, never when the
application starts.

---

## Prerequisites

- Java 25, also needed by the certificate tasks of the Taskfile (`task setup:dev`, `task setup:e2e`), which build the
  truststore with `keytool`
- Maven 3.9+ (or use the included `./mvnw` wrapper)
- PostgreSQL 18

---

## Build & Run

### Build

```bash
./mvnw clean package
```

The Docker image is built from the repository root:

```bash
task build:image:sync-api
```

### Run tests

```bash
./mvnw clean verify
```

### Run locally

From the repository root, `task setup:dev` generates the certificates and `task start:dev` starts the database. Then,
from `sync-api/`:

```bash
set -a; source ../docker/dev/env/sync-api.env; set +a
./mvnw spring-boot:run
```

In dev, `sync-api` listens on port 8444, since linid-api already uses the port 8443. Its health endpoint is
`https://localhost:8444/actuator/health`, reached with the client certificate of linid-api:

```bash
curl --cacert ../docker/certs/ca.crt \
  --cert ../docker/dev/resources/api-client.crt --key ../docker/dev/resources/api-client.key \
  https://localhost:8444/actuator/health
```

---

## Mutual TLS

`sync-api` only accepts the requests of linid-api, without any JWT:

- Tomcat rejects during the TLS handshake the clients without a certificate signed by a CA of
  `SSL_CLIENT_TRUSTSTORE_PATH`.
- Since the other components have certificates signed by the same CA, `ClientCertificateFilter` also answers `403`
  when the subject of the client certificate is not `SYNC_ALLOWED_CLIENT`.

---

## Databases

`sync-api` connects to two databases:

- Its own `sync_api` database (`DATABASE_*`), the default datasource, used by Spring Batch and by the cycle lock.
- The linid-api database (`LINID_DATABASE_*`), where it reads the account data and writes the payloads and the
  computed rights, with its own PostgreSQL account limited to these tables.

`LinidDataSourceConfig` declares the beans of the linid-api database with the `linid` qualifier, and they are only
injected with it: its `DataSource`, its `JdbcTemplate`, its `EntityManagerFactory` (`linidEntityManagerFactory`,
entities of `persistence.model`) and its `JpaTransactionManager` (`linidTransactionManager`), which also manages the
transactions of the `JdbcTemplate`. The Spring Data JPA repositories of `persistence.repository` are bound to them. A
`@Transactional` without qualifier uses the transaction manager of the `sync_api` database.

The `spring.jpa.*` properties do not apply to the linid-api `EntityManagerFactory`: its Hibernate properties go in the
`linid.jpa.properties` block of `application.yaml`, each key written between brackets and quotes, for example
`"[hibernate.jdbc.batch_size]"`, set by `SYNC_JDBC_BATCH_SIZE`: Hibernate sends the inserts and updates of a
transaction in groups of this size, one round trip per group. `"[org.hibernate.flushMode]"` is `COMMIT`: a query does
not flush the pending changes of the chunk, which its writer flushes at once. Otherwise, a processor reading a row of a
table already changed in the chunk would send each change alone, outside the groups. Hibernate never changes the schema
of the linid-api database, whose migrations stay in linid-api.

The snake_case naming of Spring Boot does not apply to it either: the entities of `persistence.model` name their tables
and columns explicitly, with `@Table(name = …)` and `@Column(name = …)`, like in linid-api. Without it, a field
`accountId` would be mapped to a column `accountid`.

The linid-api database is part of `/actuator/health`, which is `DOWN` when this database is unreachable, and
`sync-api` does not start without it: Hibernate connects to it at startup to detect the dialect.

The rows written in the linid-api database have the technical account of `sync-api` (`SYNC_TECHNICAL_ACCOUNT_ID`) as
author. Its instances share it: the `sync_instance_id` column of these rows identifies the instance that wrote them,
`SYNC_INSTANCE_ID`, the host name by default.

---

## Synchronization cycle

`syncJob`, in `SyncJobConfig`, is the global job: it runs `allAccountsPayloadStep`, then `allPairsComputeRightsStep`.
`allPairsComputeRightsStep` only runs when `allAccountsPayloadStep` succeeds: when the payload build fails, the job
stops and ends `FAILED`, since computing a new script with the previous payloads would store wrong rights.

`SyncScheduler` starts it at the time of the cron `SYNC_CRON` (`0 0 2 * * *` by default, every day at 2:00 UTC), with
`@Scheduled`, enabled by `SchedulingConfig`. The cron runs in UTC, whatever the time zone of the JVM, so that the
changes of the daylight saving time never skip nor repeat a cycle. The job runs synchronously, in the thread of the
scheduler. Its parameters are `scheduledAt`, the time of the cron in UTC (the start date rounded to the nearest
second, since the scheduler may call it slightly early), identifying, so that each time of the cron is one job
instance, and `instanceId`, the `SYNC_INSTANCE_ID` of the instance running it, non-identifying, logged by Spring Batch
with the parameters of the job.

`syncJob` is not restartable: Spring Batch refuses to start a job instance that already exists. An instance whose
clock is late, taking the cycle lock once the cycle of the same time ended on another instance, skips it and logs it,
instead of running it again. A failed cycle is neither resumed nor restarted: the next cycle is a new job instance,
which builds the payloads from the first account and computes all the pairs still `to_compute`, including the ones
left by the failed cycle.

A job that fails raises no exception to the code starting it: Spring Batch only sets the status of its execution.
`JobStatusListener`, registered as a listener of `syncJob` and `accountRecomputeJob`, raises the `jobNotCompleted` alert
when a job ends with another status than `COMPLETED`, with its parameters and one of its failures.

---

## Account payloads

`allAccountsPayloadStep`, in `PayloadStepConfig`, builds the payloads of the accounts, used as OPA input, from the
`sync-data` section of the file referenced by `EXTERNAL_CONFIGURATION` (see
`docs/configuration/application/config-files.md`). It is a Spring Batch chunk step: each chunk of
`SYNC_PAYLOAD_CHUNK_SIZE` accounts is read, processed and written in its own transaction of `linidTransactionManager`,
whose persistence context is closed at its end.

- **Read**: `allAccountSnapshotsReader`, an `AllAccountSnapshotsReader` extending `AbstractPagingItemReader`, reads
  every account of `account_computed_rights_view` once, except the technical account of `sync-api`, by pages of the
  chunk size, at the first read of each chunk. Each page starts after the last account of the previous one, not at an
  offset: an account deleted since the previous page does not shift the next ones. `AccountSnapshotLoader` then locks
  the accounts of the page (see [Several instances](#several-instances)), before reading the checksums of their current
  payloads in `account_payloads`, through its unique index on `act_id`, then each source once, with
  `SourceRepository`, which checks and quotes the source and column names, reads the PostgreSQL arrays as lists, and
  parses the `json` and `jsonb` columns. The reader keeps no state for a restart: each cycle reads every account from
  the first one.
- **Process**: `PayloadProcessor`, step-scoped to receive the job execution of its step, builds the payload with
  `PayloadService`, which renders the `mapping` with Jinjava, with the row of the account in each source, and parses
  it as a JSON object. The checksum of the payload is computed with `ChecksumService` on its compact JSON, with its
  keys sorted at every level and its arrays in their order, and compared with the checksum read by the reader: an
  unchanged payload is filtered out, without reading its row, so it adds no audit row and its rights are not computed
  again. For a changed payload, the current row of `account_payloads` is read and updated, or a new row is created
  when the reader gave no checksum. An account deleted since it was read is filtered out, since inserting its row
  would fail on its foreign key: an account without payload is no longer in the view, and an account with a payload no
  longer has its row, deleted with it.
- **Write**: a `JpaItemWriter` persists the new rows; the changed rows, managed by the persistence context of the
  chunk, are updated. It flushes at the end of each write, so that an error of an insert or an update is raised by
  the writer rather than at the commit of the chunk.

The `mapping` is checked before the step, rendered with an empty context and parsed as JSON: when it is invalid, the
step fails before reading any account. An account in error (several rows in a source, template error, rendered text
that is not a JSON object) raises a `PayloadException`, skipped by the step: it keeps its previous payload. When a
source cannot be read, the step fails: the current chunk is not written, and the chunks already written stay written.
`PayloadStepListener`, registered as a listener of the step, checks the `mapping` before it, logs the `payloadError`
alert of each account skipped, and logs at its end the number of accounts of each `PayloadOutcome`: payloads inserted,
updated and unchanged, accounts in error or deleted.

---

## Rights computation

`allPairsComputeRightsStep`, in `ComputeRightsStepConfig`, computes the rights of the account and application pairs
whose `to_compute` is true in `account_computed_rights_view`, and writes them in the `computed_rights` table. It is a
Spring Batch chunk step: each chunk of `SYNC_COMPUTE_CHUNK_SIZE` pairs is read, processed and written in its own
transaction of `linidTransactionManager`, whose persistence context is closed at its end. It is the second step of
`syncJob`. Its processor and its writer are beans, to be shared with the recomputations of an account and of an
application.

- **Read**: `allPairsReader`, an `IteratorItemReader`, reads the pairs to compute by windows of the chunk size, each
  one starting after the last pair of the previous one.
- **Process**: `ComputeRightsProcessor`, step-scoped, gives each pair to `RightsComputationService`, with the role
  names of its application, read with `ApplicationRoleRepository` at its first pair in the step and kept until its
  end. The service evaluates the policy of the application with `OpaService`, sending the payload of the account,
  checks the roles returned against the role names, and renders the claims template of the application with Jinjava,
  with the variables `payload`, `allow` and `roles`, as a JSON object. The processor then reads the current checksums
  of the pair, as a `PairChecksums` projection rather than the entity already held by the persistence context of the
  chunk: a pair whose payload, script or claims template checksum changed since it was read is filtered out, as well
  as a pair no longer in the view, its account or its application being deleted or no longer deployed, since inserting
  the row of a deleted account would fail on its foreign key. Otherwise, it gives the current row of the pair updated,
  or a new row when the pair was never computed, with the checksums read with the pair, the technical account, the
  instance and the job execution.
- **Write**: a `JpaItemWriter` persists the new rows; the changed rows, managed by the persistence context of the
  chunk, are updated. It flushes at the end of each write, so that an error of an insert or an update is raised by the
  writer rather than at the commit of the chunk.

A role missing from the roles of the application raises the `unknownRole` alert, and the rights are written anyway. A
pair in error (failed evaluation, template error, rendered text that is not a JSON object) raises a
`RightsComputationException`, skipped by the step: its row, if any, keeps its previous rights and its outdated
checksums, so it is computed again at the next cycle. When OPA cannot be reached, `OpaService` raises the
`opaUnreachable` alert and the step fails. `ComputeRightsStepListener`, registered as a listener of the step, locks
the accounts of each chunk before its pairs are computed (see [Several instances](#several-instances)), logs the
`pairError` alert of each pair skipped, with the message of its root cause and without its stack trace, since an error
of the claims template of an application puts all its pairs in error, and logs at its end the number of pairs written,
in error and discarded: the write count, the process skip count and the filter count of the step.

---

## Account rights recomputation

linid-api relays the recompute button of an account, and the status of the cycle, to two endpoints:

| Endpoint                               | Response                                                                                         |
| -------------------------------------- | ------------------------------------------------------------------------------------------------ |
| `POST /accounts/{id}/recompute-rights` | `202`, `{ "alreadyRunning": false }` when the job is started, `true` when one is already running |
| `GET /cycle`                           | `200`, `{ "running": true }` while the cycle lock of `syncJob` is held, by any instance          |

`accountRecomputeJob`, in `AccountRecomputeJobConfig`, recomputes the rights of one account without waiting for the
next cycle. Its parameters are `accountId` and `requestedAt`, both identifying, so that each request is a new job
instance; a `DefaultJobParametersValidator` rejects the parameters without them before any job execution is created.
It runs two steps, and stops at the first failed one:

- `accountPayloadStep`, built like `allAccountsPayloadStep`, with the same processor, writer and listener, on
  `accountSnapshotReader`: an `AccountSnapshotReader`, which reads the account in `account_computed_rights_view` at
  its first read, in the transaction of the chunk, and loads its snapshot with the same `AccountSnapshotLoader`, so
  that only its rows are read. An account without row in the view, since no application is deployed, gives no
  snapshot, and the technical account of `sync-api` gives none either, without any query.
- `accountComputeRightsStep`, built like `allPairsComputeRightsStep`, on `accountPairsReader`: only the pairs of the
  account whose payload, script or claims template checksum changed are computed, so that OPA is not called when nothing
  changed.

`RecomputeJobServiceImpl` starts the job in a task of the application task executor and answers at once: no `404`, since
linid-api checks that the account exists, and no end notification. Before, it takes the session lock of the
recomputation of the account (see [Several instances](#several-instances)): when it is already held, no job is started
and `alreadyRunning` is `true`. A recomputation holds this lock, and its connection, as soon as it waits for a thread:
`spring.task.execution.pool` limits an instance to 8 jobs running and 16 waiting. Beyond them, the executor rejects the
job, the lock is released and the endpoint answers `503`, which linid-api relays. The jobs running and the cycle share
the pool of the linid-api database, each chunk holding a connection during its OPA calls: `LINID_DATABASE_MAX_POOL_SIZE`
should grow with `core-size`, up to `core-size + 1` for all of them to run at once. Beyond the pool, a chunk waits for a
connection for 30 seconds at most, the default `connectionTimeout` of Hikari, then fails. When the instance stops, the
jobs running and waiting go on during `spring.lifecycle.timeout-per-shutdown-phase` (`30s` by default), before the
datasources close: the executor then drops the jobs still waiting, each one raising the `jobNotStarted` alert.
`spring.task.execution.shutdown.await-termination` must stay `false`: the executor is destroyed after the datasources,
so waiting there would only make the jobs fail. `CycleServiceImpl` reads the status of the cycle in `pg_locks`,
restricted to the `sync_api` database, since `pg_locks` lists the locks of every database of the server.

Jobs are started at the same time, so Spring Batch creates its executions with
`spring.batch.jdbc.isolation-level-for-create: repeatable_read`: with its default `SERIALIZABLE`, PostgreSQL makes part
of them fail with serialization errors, some after the request was answered. The session locks, not this level, prevent
two jobs of the same account, or two cycles, from running together.

---

## Several instances

Several `sync-api` instances can run at the same time, each one with its own `SYNC_INSTANCE_ID`. They coordinate with
PostgreSQL advisory locks, in their two-integer form `(namespace, key)`, each kind of lock having its own namespace in
`LockNamespaces`. Flyway takes an advisory lock too, in the one-key form, which never meets the two-key form.

- **Cycle lock** `(LockNamespaces.CYCLE, 0)`: before starting `syncJob`, `SyncScheduler` takes it with
  `pg_try_advisory_lock` in the `sync_api` database, through `SessionLockService`, on a dedicated connection outside the
  pool, opened by the `sessionLockDataSource` of `SchedulingConfig`, the datasource of the session locks, and kept
  during the whole cycle. When it is already held, by another instance or by the previous cycle still running, the
  instance skips the cycle and logs it with its `SYNC_INSTANCE_ID`. The lock is never released explicitly: closing the
  connection at the end of the cycle ends its session, and PostgreSQL releases the lock with it. A pooled connection
  would go back to the pool with the lock still held whenever its release failed, until Hikari retires it
  (`maxLifetime`, 30 minutes by default), and the other instances would skip their cycles meanwhile. An instance that
  crashes during the cycle does not block the next ones either. Only the cycle takes it, so that a recomputation never
  makes a cycle skip.
- **Account recompute lock** `(LockNamespaces.ACCOUNT_RECOMPUTE, accountId.hashCode())`: `RecomputeJobServiceImpl` takes
  it with `pg_try_advisory_lock` in the `sync_api` database, through `SessionLockService`, on a dedicated connection of
  `sessionLockDataSource`, before starting `accountRecomputeJob`, and closes the connection at the end of the job, even
  when it fails or cannot be launched. One recomputation of an account runs at a time, whatever the instance. Two
  accounts with the same hash code share the lock: while one is recomputed, a request for the other one starts no job
  and answers `alreadyRunning: true`. Like the cycle lock, it disappears with the instance that held it, unlike an
  execution of the Spring Batch tables, which stays `STARTED` after a crash.
- **Per-account lock** `(LockNamespaces.ACCOUNT, accountId.hashCode())`: the payload build and the computation of the
  rights take `pg_advisory_xact_lock` on the accounts of each chunk, in its transaction of the linid-api database, so
  that two processings, cycles or recomputations, never write the same account at the same time. `AccountSnapshotLoader`
  locks the accounts read before reading the checksums of their payloads and their sources, and
  `ComputeRightsStepListener` locks the accounts of the pairs of a chunk in `beforeChunk`, once they are read and
  before they are computed. The locks are released at the commit of the chunk: the processings of different accounts
  go on in parallel, and a processing of an account being written waits for the end of the other chunk.
  `AccountLockServiceImpl` takes them with one query per chunk: the keys are deduplicated and sorted in Java, and
  `unnest` reads them in the order of the array, so that two processings always take their common keys in the same
  order, without deadlock. A UUID is too large for a key: two accounts with the same hash code only wait for each
  other. It requires a transaction of the linid-api database (`Propagation.MANDATORY`): outside of it, the locks would
  be released at the end of their own statement. The wait for the locks is limited by `SYNC_LOCK_TIMEOUT`, set as the
  `lock_timeout` of the transaction: a transaction left open by a frozen or disconnected instance makes the chunk, and
  so the cycle, fail instead of blocking it without end. It must stay above the longest chunk, whose OPA calls are made
  under the locks.

The per-account lock guarantees that a slower processing never overwrites the data of a more recent one. The checksums
and the sources of the payloads are read under the lock, and the payloads are written in the same transaction, so the
later processing reads the fresher data. The computation of the rights reads again the checksums of each pair under
the lock, after its OPA call, and discards a result computed with outdated data.

The locks constrain the deployment:

- The cycle lock needs a stable session on the `sync_api` database: no PgBouncer in transaction pooling mode in front
  of it, since it does not support the session advisory locks, and no `idle_session_timeout` for `DATABASE_USER`, since
  the connection of the lock stays idle during the cycle. A session lost during the cycle releases the lock: another
  instance could start a cycle, the per-account locks still protecting the writes. The session locks hold connections
  outside the pool: an instance opens one connection to the `sync_api` database more than its pool during a cycle, and
  one more per recomputation running or waiting, 24 at most.
- A chunk of the payload build holds up to `SYNC_PAYLOAD_CHUNK_SIZE` per-account locks, in the lock table shared by the
  sessions of the linid-api PostgreSQL server and sized by `max_locks_per_transaction` and `max_connections`.
- An instance killed while a chunk waits for a per-account lock (`kill -9`, OOM killer, `SIGKILL` at the end of the
  grace period) leaves a backend on the linid-api server that keeps the locks already taken by the chunk until its
  wait ends, up to `SYNC_LOCK_TIMEOUT`: PostgreSQL only notices the lost client when it next talks to it. Setting
  `client_connection_check_interval` for `LINID_DATABASE_USER` (PostgreSQL 14 or later, e.g.
  `ALTER ROLE sync_api_user SET client_connection_check_interval = '10s'`) makes the backend check the client during
  the wait and release the locks within this interval. It does not detect a frozen host or a network cut, which are
  left to `SYNC_LOCK_TIMEOUT`.
- The advisory lock functions need no grant: the PostgreSQL accounts of `sync-api` use them without any privilege.

---

## Alerts

`AlertService` logs the anomalies detected by the checks, with their context and their cause: the blocking ones,
after which the step or the job cannot proceed, at the `ERROR` level, and the non-blocking ones, after which the step
goes on with a fallback, at the `WARN` level.

| Method             | Level   | Anomaly                                                                  |
| ------------------ | ------- | ------------------------------------------------------------------------ |
| `invalidMapping`   | `ERROR` | Invalid `mapping` template, the payload build is aborted                 |
| `sourceUnreadable` | `ERROR` | Source that cannot be read, the payload build is aborted                 |
| `opaUnreachable`   | `ERROR` | OPA cannot be reached, the authorization check is aborted                |
| `jobNotStarted`    | `ERROR` | Job that cannot be started, left to the next cycle                       |
| `jobNotCompleted`  | `ERROR` | Job ended with another status than `COMPLETED`, left to the next cycle   |
| `payloadError`     | `WARN`  | Account in error during the payload build, it keeps its previous payload |
| `unknownRole`      | `WARN`  | Role returned by OPA and missing from the roles of the application       |
| `pairError`        | `WARN`  | Pair in error, nothing is written for it                                 |

---

## Database

The Spring Batch tables of the `sync_api` database are created by the Flyway migrations of
`src/main/resources/db/migration`, run with `DATABASE_ADMIN_USER` when `sync-api` starts. `DATABASE_USER` only reads
and writes them.

`V1__create_spring_batch_tables.sql` is the unchanged `schema-postgresql.sql` script of Spring Batch 6.0.5. When a
Spring Batch upgrade changes the schema, its official migration script
(`org/springframework/batch/core/migration/<version>/migration-postgresql.sql` in `spring-batch-core`) is added
unchanged as a new Flyway version.

---

## OPA

`OpaService` evaluates the policy of an application for an account, by sending the account payload unchanged as the
OPA input. The policies are deployed on OPA by linid-api: `sync-api` only evaluates them. It raises:

- `OpaUnreachableException` when the OPA server cannot be reached, or when a gateway in front of it answers `502`,
  `503` or `504`, after logging the `opaUnreachable` alert: the rights computation stops the cycle at this first
  exception, so that the alert is logged once;
- `OpaEvaluationException` when OPA returns an error or no result, such as when the policy is missing: only the pair
  of the account and the application is in error.

---

## Environment Variables

| Variable                         | Description                                                                      | Default                        |
| -------------------------------- | -------------------------------------------------------------------------------- | ------------------------------ |
| `DATABASE_HOST`                  | PostgreSQL host of the `sync_api` database                                       | —                              |
| `DATABASE_PORT`                  | PostgreSQL port                                                                  | —                              |
| `DATABASE_NAME`                  | Database name                                                                    | —                              |
| `DATABASE_USER`                  | Database user                                                                    | —                              |
| `DATABASE_PASSWORD`              | Database password                                                                | —                              |
| `DATABASE_ADMIN_USER`            | Database administrator user, running the Flyway migrations                       | —                              |
| `DATABASE_ADMIN_PASSWORD`        | Database administrator password                                                  | —                              |
| `LINID_DATABASE_HOST`            | PostgreSQL host of the linid-api database                                        | —                              |
| `LINID_DATABASE_PORT`            | PostgreSQL port of the linid-api database                                        | —                              |
| `LINID_DATABASE_NAME`            | Name of the linid-api database                                                   | —                              |
| `LINID_DATABASE_USER`            | User of `sync-api` in the linid-api database                                     | —                              |
| `LINID_DATABASE_PASSWORD`        | Password of this user                                                            | —                              |
| `LINID_DATABASE_MAX_POOL_SIZE`   | Maximum number of connections to the linid-api database                          | `5`                            |
| `SYNC_TECHNICAL_ACCOUNT_ID`      | `act_id` of the technical account shared by the `sync-api` instances             | —                              |
| `SYNC_INSTANCE_ID`               | Identifier of the instance, written in its rows, its logs and its job parameters | the host name                  |
| `SSL_KEY_STORE`                  | Path to the keystore of the server certificate                                   | —                              |
| `SSL_KEY_STORE_TYPE`             | Keystore type (`PKCS12` or `JKS`)                                                | `PKCS12`                       |
| `SSL_KEY_PASSWORD`               | Password for the private key and the keystore                                    | —                              |
| `SSL_TRUSTSTORE_PATH`            | Path to the truststore used for the outgoing TLS connections                     | —                              |
| `SSL_TRUSTSTORE_PASSWORD`        | Truststore password                                                              | —                              |
| `SSL_CLIENT_TRUSTSTORE_PATH`     | Path to the truststore validating the client certificates                        | —                              |
| `SSL_CLIENT_TRUSTSTORE_PASSWORD` | Password of the client truststore                                                | `SSL_TRUSTSTORE_PASSWORD`      |
| `SYNC_ALLOWED_CLIENT`            | Subject of the only accepted client certificate, in the RFC 2253 format          | —                              |
| `SYNC_CRON`                      | Schedule of the global job, as a Spring cron expression, in UTC                  | `0 0 2 * * *`                  |
| `SYNC_PAYLOAD_CHUNK_SIZE`        | Number of accounts per chunk of the payload build, in one transaction            | `500`                          |
| `SYNC_COMPUTE_CHUNK_SIZE`        | Number of account and application pairs computed per chunk                       | `100`                          |
| `SYNC_LOCK_TIMEOUT`              | Maximum wait for the per-account locks of a chunk, as a duration                 | `30m`                          |
| `SYNC_JDBC_BATCH_SIZE`           | Number of inserts or updates sent together to the linid-api database             | `50`                           |
| `OPA_SERVER_URL`                 | Base URL of the OPA server                                                       | `http://opa:8181`              |
| `OPA_EVALUATION_PATH`            | Path of the result of an application policy, `{code}` being the application code | `/v1/data/authz/{code}/result` |
| `OPA_CONNECT_TIMEOUT`            | Maximum time to establish the connection to the OPA server, as a duration        | `5s`                           |
| `OPA_READ_TIMEOUT`               | Maximum time to wait for the response of the OPA server, as a duration           | `10s`                          |
| `LOGGING_LEVEL`                  | Root logging level                                                               | `INFO`                         |
| `EXTERNAL_CONFIGURATION`         | Path to the external YAML configuration file, holding the `sync-data` section    | —                              |
| `SERVER_PORT`                    | HTTPS port                                                                       | `8443`                         |
