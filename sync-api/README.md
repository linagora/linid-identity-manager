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

## Environment Variables

| Variable                         | Description                                                             | Default                   |
| -------------------------------- | ----------------------------------------------------------------------- | ------------------------- |
| `DATABASE_HOST`                  | PostgreSQL host of the `sync_api` database                              | —                         |
| `DATABASE_PORT`                  | PostgreSQL port                                                         | —                         |
| `DATABASE_NAME`                  | Database name                                                           | —                         |
| `DATABASE_USER`                  | Database user                                                           | —                         |
| `DATABASE_PASSWORD`              | Database password                                                       | —                         |
| `DATABASE_ADMIN_USER`            | Database administrator user, running the Flyway migrations              | —                         |
| `DATABASE_ADMIN_PASSWORD`        | Database administrator password                                         | —                         |
| `SSL_KEY_STORE`                  | Path to the keystore of the server certificate                          | —                         |
| `SSL_KEY_STORE_TYPE`             | Keystore type (`PKCS12` or `JKS`)                                       | `PKCS12`                  |
| `SSL_KEY_PASSWORD`               | Password for the private key and the keystore                           | —                         |
| `SSL_TRUSTSTORE_PATH`            | Path to the truststore used for the outgoing TLS connections            | —                         |
| `SSL_TRUSTSTORE_PASSWORD`        | Truststore password                                                     | —                         |
| `SSL_CLIENT_TRUSTSTORE_PATH`     | Path to the truststore validating the client certificates               | —                         |
| `SSL_CLIENT_TRUSTSTORE_PASSWORD` | Password of the client truststore                                       | `SSL_TRUSTSTORE_PASSWORD` |
| `SYNC_ALLOWED_CLIENT`            | Subject of the only accepted client certificate, in the RFC 2253 format | —                         |
| `SYNC_CRON`                      | Schedule of the global job, as a Spring cron expression                 | `0 0 2 * * *`             |
| `LOGGING_LEVEL`                  | Root logging level                                                      | `INFO`                    |
| `EXTERNAL_CONFIGURATION`         | Path to an external YAML configuration file, loaded when present        | —                         |
| `SERVER_PORT`                    | HTTPS port                                                              | `8443`                    |
