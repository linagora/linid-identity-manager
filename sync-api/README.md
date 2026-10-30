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

- Its own `sync_api` database (`DATABASE_*`), the default datasource, used by Spring Batch.
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
transaction in groups of this size, one round trip per group. Hibernate never changes the schema of the linid-api
database, whose migrations stay in linid-api.

The snake_case naming of Spring Boot does not apply to it either: the entities of `persistence.model` name their tables
and columns explicitly, with `@Table(name = …)` and `@Column(name = …)`, like in linid-api. Without it, a field `actId`
would be mapped to a column `actid`.

The linid-api database is part of `/actuator/health`, which is `DOWN` when this database is unreachable, and
`sync-api` does not start without it: Hibernate connects to it at startup to detect the dialect.

The rows written in the linid-api database have the technical account of `sync-api` (`SYNC_TECHNICAL_ACCOUNT_ID`) as
author. Its instances share it: the `sync_instance_id` column of these rows identifies the instance that wrote them,
`SYNC_INSTANCE_ID`, the host name by default.

---

## Account payloads

`buildPayloadsStep`, in `PayloadStepConfig`, builds the payloads of the accounts, used as OPA input, from the
`sync-data` section of the file referenced by `EXTERNAL_CONFIGURATION` (see
`docs/configuration/application/config-files.md`). It is a Spring Batch chunk step: each chunk of
`SYNC_PAYLOAD_CHUNK_SIZE` accounts is read, processed and written in its own transaction of `linidTransactionManager`,
whose persistence context is closed at its end.

- **Read**: `payloadSourcesReader`, a `PayloadSourcesReader` extending `AbstractPagingItemReader`, reads every
  account of `account_computed_rights_view` once, with the checksum of its current payload, except the technical
  account of `sync-api`, by pages of the chunk size, at the first read of each chunk. Each page starts after the last
  account of the previous one, not at an offset: an account deleted since the previous page does not shift the next
  ones. For each page, each source is read once with `SourceRepository`, which checks and quotes the source and
  column names, reads the PostgreSQL arrays as lists, and parses the `json` and `jsonb` columns. The last account read
  is saved with the step at each commit, so that a restarted step resumes after it.
- **Process**: `PayloadProcessor` builds the payload with `PayloadService`, which renders the `mapping` with Jinjava,
  with the row of the account in each source, and parses it as a JSON object. The checksum of the payload is computed
  with `ChecksumService` on its compact JSON, with its keys sorted at every level and its arrays in their order, and
  compared with the checksum read with the page: an unchanged payload is filtered out, without reading its row, so it
  adds no audit row and its rights are not computed again. For a changed payload, the current row of
  `account_payloads` is read and updated, or a new row is created when the page gave no checksum. An account deleted
  since its page was read is filtered out, since inserting its row would fail on its foreign key: an account without
  payload is no longer in the view, and an account with a payload no longer has its row, deleted with it.
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

`computeRightsStep`, in `ComputeRightsStepConfig`, computes the rights of the account and application pairs whose
`to_compute` is true in `account_computed_rights_view`, and writes them in the `computed_rights` table. It is a Spring
Batch chunk step: each chunk of `SYNC_COMPUTE_CHUNK_SIZE` pairs is read, processed and written in its own transaction
of `linidTransactionManager`, whose persistence context is closed at its end. It is not part of the global job yet.
Its processor and its writer are beans, to be shared with the recomputations of an account and of an application.

- **Read**: `cyclePairsReader`, an `IteratorItemReader`, reads the pairs to compute by windows of the chunk size, each
  one starting after the last pair of the previous one.
- **Process**: `ComputeRightsProcessor`, step-scoped, gives each pair to `RightsComputationService`, with the role
  names of its application, read with `ApplicationRoleRepository` at its first pair in the step and kept until its
  end. The service evaluates the policy of the application with `OpaService`, sending the payload of the account,
  checks the roles returned against the role names, and renders the claims template of the application with Jinjava,
  with the variables `payload`, `allow` and `roles`, as a JSON object. The processor then reads the current checksums
  of the pair, as a `PairChecksums` projection rather than the entity already held by the persistence context of the
  chunk: a pair whose payload, script or claims template checksum changed since it was read, or no longer in the view,
  is filtered out. Otherwise, it gives the current row of the pair updated, or a new row when the pair was never
  computed, with the checksums read with the pair, the technical account, the instance and the job execution.
- **Write**: a `JpaItemWriter` persists the new rows; the changed rows, managed by the persistence context of the
  chunk, are updated. It flushes at the end of each write, so that an error of an insert or an update is raised by the
  writer rather than at the commit of the chunk.

A role missing from the roles of the application raises the `unknownRole` alert, and the rights are written anyway. A
pair in error (failed evaluation, template error, rendered text that is not a JSON object) raises a
`RightsComputationException`, skipped by the step: its row, if any, keeps its previous rights and its outdated
checksums, so it is computed again at the next cycle. When OPA cannot be reached, `OpaService` raises the
`opaUnreachable` alert and the step fails. `ComputeRightsStepListener`, registered as a listener of the step, logs the
`pairError` alert of each pair skipped, and logs at its end the number of pairs written, in error and discarded: the
write count, the process skip count and the filter count of the step.

---

## Alerts

`AlertService` logs the anomalies detected by the checks, with their context and their cause: the blocking ones,
after which the step cannot proceed, at the `ERROR` level, and the non-blocking ones, after which the step goes on
with a fallback, at the `WARN` level.

| Method             | Level   | Anomaly                                                                  |
| ------------------ | ------- | ------------------------------------------------------------------------ |
| `invalidMapping`   | `ERROR` | Invalid `mapping` template, the payload build is aborted                 |
| `sourceUnreadable` | `ERROR` | Source that cannot be read, the payload build is aborted                 |
| `opaUnreachable`   | `ERROR` | OPA cannot be reached, the authorization check is aborted                |
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
| `SYNC_INSTANCE_ID`               | Identifier of the instance, written with the rows it writes                      | the host name                  |
| `SSL_KEY_STORE`                  | Path to the keystore of the server certificate                                   | —                              |
| `SSL_KEY_STORE_TYPE`             | Keystore type (`PKCS12` or `JKS`)                                                | `PKCS12`                       |
| `SSL_KEY_PASSWORD`               | Password for the private key and the keystore                                    | —                              |
| `SSL_TRUSTSTORE_PATH`            | Path to the truststore used for the outgoing TLS connections                     | —                              |
| `SSL_TRUSTSTORE_PASSWORD`        | Truststore password                                                              | —                              |
| `SSL_CLIENT_TRUSTSTORE_PATH`     | Path to the truststore validating the client certificates                        | —                              |
| `SSL_CLIENT_TRUSTSTORE_PASSWORD` | Password of the client truststore                                                | `SSL_TRUSTSTORE_PASSWORD`      |
| `SYNC_ALLOWED_CLIENT`            | Subject of the only accepted client certificate, in the RFC 2253 format          | —                              |
| `SYNC_CRON`                      | Schedule of the global job, as a Spring cron expression                          | `0 0 2 * * *`                  |
| `SYNC_PAYLOAD_CHUNK_SIZE`        | Number of accounts per chunk of the payload build, in one transaction            | `500`                          |
| `SYNC_COMPUTE_CHUNK_SIZE`        | Number of account and application pairs computed per chunk                       | `100`                          |
| `SYNC_JDBC_BATCH_SIZE`           | Number of inserts or updates sent together to the linid-api database             | `50`                           |
| `OPA_SERVER_URL`                 | Base URL of the OPA server                                                       | `http://opa:8181`              |
| `OPA_EVALUATION_PATH`            | Path of the result of an application policy, `{code}` being the application code | `/v1/data/authz/{code}/result` |
| `OPA_CONNECT_TIMEOUT`            | Maximum time to establish the connection to the OPA server, as a duration        | `5s`                           |
| `OPA_READ_TIMEOUT`               | Maximum time to wait for the response of the OPA server, as a duration           | `10s`                          |
| `LOGGING_LEVEL`                  | Root logging level                                                               | `INFO`                         |
| `EXTERNAL_CONFIGURATION`         | Path to the external YAML configuration file, holding the `sync-data` section    | —                              |
| `SERVER_PORT`                    | HTTPS port                                                                       | `8443`                         |
