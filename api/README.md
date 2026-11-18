# LinID Identity Manager API

## Description

The **LINID Identity Manager API** provides management of user accounts, applications, roles, and organizational units within a company.
It is part of the **LinID** monorepo and serves as the central service for access control and authorization management for internal applications.

This API allows you to:

- Manage **user accounts**: create, update, activate, suspend, or delete accounts.
- Handle **applications**: register applications, manage their roles, and configure authentication types.
- Organize **organizational units (OUs)**: structure users and applications by departments or teams.
- Enforce **roles and permissions** within applications.

---

## Prerequisites

- Java 25+
- Maven 3.8+ (or use the included `./mvnw` wrapper)
- PostgreSQL 15+

---

## Build & Run

### Build

```bash
./mvnw clean package
```

### Run tests

```bash
./mvnw clean verify
```

### Run locally

```bash
./mvnw spring-boot:run
```

> All required environment variables must be set before running. See [Environment Variables](#environment-variables).

### Run the JAR

```bash
java -jar target/linid-identity-manager-api-0.1.0.jar
```

---

## API Documentation

When Swagger is enabled (`SWAGGER_ENABLED=true`), the full API documentation is available at:

```
https://localhost:8443/swagger-ui/index.html
```

---

## Security

The API uses **OAuth2 JWT** as a resource server (Spring Security). All endpoints except health, actuator, and Swagger are secured.

Authentication is configured via:

- `AUTH_ISSUER_URI`: OIDC issuer URI
- `AUTH_JWK_SET_URI`: JSON Web Key Set URI
- `AUTH_AUDIENCE`: expected `aud` claim of the access tokens, i.e. the audience declared for the API at the OIDC provider (not the client ID)
- `AUTH_JWT_EXPECTED_TYPE`: JOSE `typ` header required on access tokens (default `at+jwt`, compared case-insensitively); only change it if the OIDC provider issues another type. Leave the variable unset to use the default: an empty value is not the default and rejects every token

The `UserAuthenticationFilter` resolves the account whose `external_id` matches the `sub` claim of the JWT token and creates a `UserPrincipal` in the security context.
The `email` claim is never used to resolve the account: some OIDC providers let users change their own email address, whereas only the OIDC provider sets the subject.
The `external_id` is set when the account is created and cannot be changed afterwards through the API: `PUT /accounts/{id}` ignores it, since changing it would bind the account to another identity. A database administrator can still change it with SQL: see [Changing the External Identifier of an Account](../docs/configuration/authentication/oidc.md#-changing-the-external-identifier-of-an-account).
The subject is only as stable as the attribute the OIDC provider derives it from: see [Before a Real Deployment](../docs/configuration/authentication/oidc.md#-before-a-real-deployment).
When the token is valid but no account matches its `sub` claim, the API responds `401 Unauthorized` with a `WWW-Authenticate: Bearer` header, like any rejected bearer token, and logs a warning with the subject.

---

## Environment Variables

### Database

| Variable                  | Description                          | Required |
| ------------------------- | ------------------------------------ | -------- |
| `DATABASE_HOST`           | PostgreSQL host                      | Yes      |
| `DATABASE_PORT`           | PostgreSQL port                      | Yes      |
| `DATABASE_NAME`           | Database name                        | Yes      |
| `DATABASE_USER`           | Application database user            | Yes      |
| `DATABASE_PASSWORD`       | Application database password        | Yes      |
| `DATABASE_ADMIN_USER`     | Admin user for Flyway migrations     | Yes      |
| `DATABASE_ADMIN_PASSWORD` | Admin password for Flyway migrations | Yes      |

### Authentication

| Variable           | Description     | Required |
| ------------------ | --------------- | -------- |
| `AUTH_ISSUER_URI`  | OIDC issuer URI | Yes      |
| `AUTH_JWK_SET_URI` | JWK Set URI     | Yes      |
| `AUTH_AUDIENCE`    | Expected `aud`  | Yes      |
| `AUTH_JWT_EXPECTED_TYPE` | Required `typ` header (default `at+jwt`) | No |

### SSL

| Variable                  | Description                               | Required               |
| ------------------------- |-------------------------------------------|------------------------|
| `SSL_KEY_STORE`           | Path to keystore                          | Yes                    |
| `SSL_KEY_STORE_TYPE`      | Keystore type (`PKCS12` or `JKS`)         | No (default `PKCS12`)  |
| `SSL_KEY_PASSWORD`        | Password for private key and keystore     | Yes                    |
| `SSL_TRUSTSTORE_PATH`     | Path to truststore                        | Yes                    |
| `SSL_TRUSTSTORE_PASSWORD` | Truststore password                       | Yes                    |

### Sync API

The API calls `sync-api` with mutual TLS, presenting a client certificate to `sync-api` only. When `sync-api` does not
answer within the timeouts, it is considered unreachable and the API answers `503`.

| Variable                       | Description                                                           | Required   |
| ------------------------------ | --------------------------------------------------------------------- | ---------- |
| `SYNC_API_URL`                 | Base URL of `sync-api`                                                | Yes        |
| `SYNC_API_CLIENT_KEY_STORE`    | Path to the PKCS12 keystore of the client certificate                 | Yes        |
| `SYNC_API_CLIENT_KEY_PASSWORD` | Password for the client keystore and its key                          | Yes        |
| `SYNC_API_CONNECT_TIMEOUT`     | Maximum time to establish the connection to `sync-api`, as a duration | No (`5s`)  |
| `SYNC_API_READ_TIMEOUT`        | Maximum time to wait for the response of `sync-api`, as a duration    | No (`10s`) |

### Application

| Variable                         | Description                                  | Default                    |
| -------------------------------- | -------------------------------------------- | -------------------------- |
| `SWAGGER_ENABLED`                | Enable Swagger UI and API docs               | —                          |
| `LOGGING_LEVEL`                  | Root logging level                           | `INFO`                     |
| `I18N_EXTERNAL_PATH`             | Path to external i18n files                  | —                          |
| `I18N_MERGE_ORDER`               | i18n merge priority                          | `plugin,external,internal` |
| `PLUGIN_LOADER_PATH`             | Path to plugin JARs                          | —                          |
| `COPYRIGHT_MODE`                 | Copyright mode (`default`, `custom`, `none`) | `default`                  |
| `COPYRIGHT_CUSTOM`               | Custom copyright text                        | —                          |
| `EXTERNAL_CONFIGURATION`         | Path to external YAML config                 | —                          |
| `CLIENT_DATABASE_SCHEMA`         | Client Flyway schema                         | `public`                   |
| `CLIENT_DATABASE_MIGRATION_PATH` | Client Flyway migration path                 | —                          |

### OpenBao

The secrets can be read at startup from the KV engine of [OpenBao](https://openbao.org), or of HashiCorp Vault whose
API it shares, with Spring Cloud Vault. Each key of a secret is a property: a key named like a variable used as a
`${...}` placeholder in `application.yaml` (e.g. `DATABASE_PASSWORD`) replaces this variable, which can then be left
unset. Other keys are ignored, as are the `VAULT_*` variables and `EXTERNAL_CONFIGURATION`, read before
OpenBao. A variable that is set takes precedence over a key of the same name.

Two secrets are read: `<VAULT_KV_BACKEND>/<VAULT_APPLICATION_NAME>`, then `<VAULT_KV_BACKEND>/<VAULT_DEFAULT_CONTEXT>`
(`linid`), shared with `sync-api`. Any other `spring.cloud.vault.*` property can be set with its environment variable,
e.g. `SPRING_CLOUD_VAULT_APP_ROLE_ROLE_ID` and `SPRING_CLOUD_VAULT_APP_ROLE_SECRET_ID` for the `APPROLE` authentication,
or `SPRING_CLOUD_VAULT_SSL_TRUST_STORE` and `SPRING_CLOUD_VAULT_SSL_TRUST_STORE_PASSWORD` for an OpenBao certificate
signed by a private CA.

When OpenBao cannot be reached or refuses the access, the startup stops. A missing secret is ignored.

| Variable                 | Description                                                    | Default                |
| ------------------------ | -------------------------------------------------------------- | ---------------------- |
| `VAULT_ENABLED`          | Read the secrets from OpenBao                                  | `false`                |
| `VAULT_URI`              | URL of OpenBao                                                 | `https://openbao:8200` |
| `VAULT_AUTHENTICATION`   | Authentication method (`TOKEN`, `APPROLE`, `KUBERNETES`, ...)  | `TOKEN`                |
| `VAULT_TOKEN`            | Token, for the `TOKEN` authentication                          | —                      |
| `VAULT_APPLICATION_NAME` | Name of the secret of this application                         | `linid-api`            |
| `VAULT_KV_BACKEND`       | Mount path of the KV engine                                    | `secret`               |
| `VAULT_DEFAULT_CONTEXT`  | Name of the secret shared by both applications                 | `linid`                |

---

## Generate Certificates for HTTPS

The keystore and truststore are generated from the repository root, together with all
other development certificates:

```bash
task setup:certs:dev
```

This produces `src/main/resources/keystore.p12` (server certificate signed by the local
development CA) and `src/main/resources/truststore.jks` (trusting that CA).
See [docs/configuration/certificates](../docs/configuration/certificates.md) for details.

Set the keystore and truststore passwords in the corresponding environment variables.

---

## Database Migrations

The API uses **Flyway** for database migrations. Migration files are located in:

```
src/main/resources/db/migration/
```

Current migrations:

- `V1__init.sql` — pgcrypto extension, `update_timestamp()` trigger function
- `V2__create_table_accounts.sql` — `accounts` table with indexes, triggers, and comments
