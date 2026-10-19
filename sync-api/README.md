# LinID Synchronization API

## Description

The **LinID Synchronization API** (`sync-api`) is a REST API built with Spring Boot and Spring Batch. Its batch jobs
read the account data in the linid-api database, compute the rights of each account on each application, and write
them to the linid-api database. They are started by a cron and by endpoints called by linid-api, never when the
application starts.

---

## Prerequisites

- Java 25
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
`https://localhost:8444/actuator/health`.

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

## Environment Variables

| Variable                  | Description                                                                 | Default       |
| ------------------------- | --------------------------------------------------------------------------- | ------------- |
| `DATABASE_HOST`           | PostgreSQL host of the `sync_api` database                                  | —             |
| `DATABASE_PORT`           | PostgreSQL port                                                             | —             |
| `DATABASE_NAME`           | Database name                                                               | —             |
| `DATABASE_USER`           | Database user                                                               | —             |
| `DATABASE_PASSWORD`       | Database password                                                           | —             |
| `SSL_KEY_STORE`           | Path to the keystore of the server certificate                              | —             |
| `SSL_KEY_STORE_TYPE`      | Keystore type (`PKCS12` or `JKS`)                                           | `PKCS12`      |
| `SSL_KEY_PASSWORD`        | Password for the private key and the keystore                               | —             |
| `SSL_TRUSTSTORE_PATH`     | Path to the truststore used for the outgoing TLS connections                | —             |
| `SSL_TRUSTSTORE_PASSWORD` | Truststore password                                                         | —             |
| `SYNC_CRON`               | Schedule of the global job, as a Spring cron expression                     | `0 0 2 * * *` |
| `LOGGING_LEVEL`           | Root logging level                                                          | `INFO`        |
| `EXTERNAL_CONFIGURATION`  | Path to an external YAML configuration file, loaded when present            | —             |
| `SERVER_PORT`             | HTTPS port                                                                  | `8443`        |
