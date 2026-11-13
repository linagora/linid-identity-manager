# LinID Synchronization API

## Description

The **LinID Synchronization API** (`sync-api`) computes the rights of each account on each application. It is a Spring
Boot and Spring Batch application, part of the **LinID** monorepo.

Its batch jobs read the account data in the linid-api database, evaluate the OPA policies of the applications, and write
the computed rights to that database. They are started in two ways, never when the application starts:

- The **synchronization cycle** processes all the accounts, every day at 2:00 UTC by default (`SYNC_CRON`).
- A **recomputation** processes one account or one application on demand, requested by linid-api.

`sync-api` also has its own `sync_api` database, used by Spring Batch. Several instances can run at the same time.

👉 Databases, jobs, locks, alerts and deployment constraints are described in
[docs/advanced/sync-api.md](../docs/advanced/sync-api.md).

---

## Prerequisites

- Java 25, also needed by `task setup:dev` and `task setup:e2e`, which build the truststore with `keytool`
- Maven 3.9+ (or the included `./mvnw` wrapper)
- PostgreSQL 18

---

## Build & Run

```bash
./mvnw clean package         # Build
./mvnw clean verify          # Run the tests
task build:image:sync-api    # Build the Docker image, from the repository root
```

To run it locally, `task setup:dev` generates the certificates and `task start:dev` starts the database, from the
repository root. Then, from `sync-api/`:

```bash
set -a; source ../docker/dev/env/sync-api.env; set +a
./mvnw spring-boot:run
```

In dev, it listens on port 8444, since linid-api already uses 8443. Reach it with the client certificate of linid-api:

```bash
curl --cacert ../docker/certs/ca.crt \
  --cert ../docker/dev/resources/api-client.crt --key ../docker/dev/resources/api-client.key \
  https://localhost:8444/actuator/health
```

---

## Security

`sync-api` only accepts the requests of linid-api, with mutual TLS and without any JWT. Tomcat rejects during the TLS
handshake the clients without a certificate signed by a CA of `SSL_CLIENT_TRUSTSTORE_PATH`, and
`ClientCertificateFilter` answers `403` when the certificate subject is not `SYNC_ALLOWED_CLIENT`.

---

## Endpoints

| Endpoint                                   | Description                                        |
| ------------------------------------------ | -------------------------------------------------- |
| `POST /accounts/{id}/recompute-rights`     | Starts the recomputation of an account             |
| `POST /applications/{id}/recompute-rights` | Starts the recomputation of an application         |
| `GET /cycle`                               | Tells whether the synchronization cycle is running |

---

## API Documentation

When `SWAGGER_ENABLED=true`, Swagger is available in dev at `https://localhost:8444/swagger-ui/index.html`, and the
OpenAPI document at `https://localhost:8444/v3/api-docs`.

Both require the client certificate of linid-api: use the `curl` command above, or import
`docker/dev/resources/api-client-keystore.p12` in a browser, with the `SSL_KEY_PASSWORD` of `docker/dev/env/cert.env`.
With this certificate, `Try it out` really starts the recomputations.

---

## Environment Variables

### `sync_api` database

| Variable                  | Description                                                | Default |
| ------------------------- | ---------------------------------------------------------- | ------- |
| `DATABASE_HOST`           | PostgreSQL host of the `sync_api` database                 | —       |
| `DATABASE_PORT`           | PostgreSQL port                                            | —       |
| `DATABASE_NAME`           | Database name                                              | —       |
| `DATABASE_USER`           | Database user                                              | —       |
| `DATABASE_PASSWORD`       | Database password                                          | —       |
| `DATABASE_ADMIN_USER`     | Database administrator user, running the Flyway migrations | —       |
| `DATABASE_ADMIN_PASSWORD` | Database administrator password                            | —       |

### linid-api database

| Variable                       | Description                                             | Default |
| ------------------------------ | ------------------------------------------------------- | ------- |
| `LINID_DATABASE_HOST`          | PostgreSQL host of the linid-api database               | —       |
| `LINID_DATABASE_PORT`          | PostgreSQL port of the linid-api database               | —       |
| `LINID_DATABASE_NAME`          | Name of the linid-api database                          | —       |
| `LINID_DATABASE_USER`          | User of `sync-api` in the linid-api database            | —       |
| `LINID_DATABASE_PASSWORD`      | Password of this user                                   | —       |
| `LINID_DATABASE_MAX_POOL_SIZE` | Maximum number of connections to the linid-api database | `5`     |

### SSL

| Variable                         | Description                                                             | Default                   |
| -------------------------------- | ----------------------------------------------------------------------- | ------------------------- |
| `SSL_KEY_STORE`                  | Path to the keystore of the server certificate                          | —                         |
| `SSL_KEY_STORE_TYPE`             | Keystore type (`PKCS12` or `JKS`)                                       | `PKCS12`                  |
| `SSL_KEY_PASSWORD`               | Password for the private key and the keystore                           | —                         |
| `SSL_TRUSTSTORE_PATH`            | Path to the truststore used for the outgoing TLS connections            | —                         |
| `SSL_TRUSTSTORE_PASSWORD`        | Truststore password                                                     | —                         |
| `SSL_CLIENT_TRUSTSTORE_PATH`     | Path to the truststore validating the client certificates               | —                         |
| `SSL_CLIENT_TRUSTSTORE_PASSWORD` | Password of the client truststore                                       | `SSL_TRUSTSTORE_PASSWORD` |
| `SYNC_ALLOWED_CLIENT`            | Subject of the only accepted client certificate, in the RFC 2253 format | —                         |

### Synchronization

| Variable                    | Description                                                                      | Default       |
| --------------------------- | -------------------------------------------------------------------------------- | ------------- |
| `SYNC_TECHNICAL_ACCOUNT_ID` | `act_id` of the technical account shared by the `sync-api` instances             | —             |
| `SYNC_INSTANCE_ID`          | Identifier of the instance, written in its rows, its logs and its job parameters | the host name |
| `SYNC_CRON`                 | Schedule of the global job, as a Spring cron expression, in UTC                  | `0 0 2 * * *` |
| `SYNC_PAYLOAD_CHUNK_SIZE`   | Number of accounts per chunk of the payload build, in one transaction            | `500`         |
| `SYNC_COMPUTE_CHUNK_SIZE`   | Number of account and application pairs computed per chunk                       | `100`         |
| `SYNC_LOCK_TIMEOUT`         | Maximum wait for the per-account locks of a chunk, as a duration                 | `30m`         |
| `SYNC_JDBC_BATCH_SIZE`      | Number of inserts or updates sent together to the linid-api database             | `50`          |

### OPA

| Variable              | Description                                                                      | Default                        |
| --------------------- | -------------------------------------------------------------------------------- | ------------------------------ |
| `OPA_SERVER_URL`      | Base URL of the OPA server                                                       | `http://opa:8181`              |
| `OPA_EVALUATION_PATH` | Path of the result of an application policy, `{code}` being the application code | `/v1/data/authz/{code}/result` |
| `OPA_CONNECT_TIMEOUT` | Maximum time to establish the connection to the OPA server, as a duration        | `5s`                           |
| `OPA_READ_TIMEOUT`    | Maximum time to wait for the response of the OPA server, as a duration           | `10s`                          |

### Application

| Variable                 | Description                                                                   | Default |
| ------------------------ | ----------------------------------------------------------------------------- | ------- |
| `SWAGGER_ENABLED`        | Enable Swagger UI and API docs                                                | —       |
| `LOGGING_LEVEL`          | Root logging level                                                            | `INFO`  |
| `EXTERNAL_CONFIGURATION` | Path to the external YAML configuration file, holding the `sync-data` section | —       |
| `SERVER_PORT`            | HTTPS port                                                                    | `8443`  |

---

## Database Migrations

The Spring Batch tables are created by Flyway from `src/main/resources/db/migration/`: when a Spring Batch upgrade
changes their schema, add its official script
(`org/springframework/batch/core/migration/<version>/migration-postgresql.sql` of `spring-batch-core`) as a new version.
