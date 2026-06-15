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

- Java 21+
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

The `UserAuthenticationFilter` extracts the user email from the JWT token and creates a `UserPrincipal` in the security context.

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

### SSL

| Variable                  | Description                           | Required |
| ------------------------- |---------------------------------------|----------|
| `SSL_KEY_STORE`           | Path to keystore                      | Yes      |
| `SSL_KEY_PASSWORD`        | Password for private key and JKS file | Yes      |
| `SSL_TRUSTSTORE_PATH`     | Path to truststore                    | Yes      |
| `SSL_TRUSTSTORE_PASSWORD` | Truststore password                   | Yes      |

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

---

## Generate Certificates for HTTPS

```bash
keytool -genkey -alias myKeyAlias -keyalg RSA -keysize 2048 \
  -keystore src/main/resources/keystore.jks -validity 3650

keytool -importcert -noprompt -trustcacerts -alias lemonldap \
  -file selfsigned.crt -keystore src/main/resources/truststore.jks \
  -storepass changeit >/dev/null 2>&1
```

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
