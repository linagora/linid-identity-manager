# Prerequisites

## 🎯 Purpose

This guide lists all **requirements and prerequisites** before installing or running **LinId Identity Manager**.

Ensuring these are met will allow a smooth setup and avoid common issues.

---

## 🧩 System Requirements

* **Operating System**: Linux or macOS (Windows for development is supported via Docker)
* **CPU / Memory**: Minimum 2 cores, 8GB RAM (12GB recommended)
* **Disk Space**: Minimum 20GB free

---

## 💾 Software Requirements

### 1️⃣ Database

* **PostgreSQL** (version 18 or later)
* Database user with **create/read/write privileges**
* Database accessible from the LinId backend

`sync-api` has its own `sync_api` database, with two accounts:

* An **administrator account** (`DATABASE_ADMIN_USER`), owner of the `sync_api` database, which runs the Flyway
  migrations of `sync-api` when it starts: they create the Spring Batch tables.
* A **runtime account** (`DATABASE_USER`), used by `sync-api` to read and write the Spring Batch tables and to use
  their sequences. It must not own the `sync_api` database, which would let it create tables in its `public` schema.

The grants of the runtime account are given by the administrator account, before the first start of `sync-api`:
default privileges only apply to the tables and sequences later created by the account that defines them.

```sql
\connect sync_api

GRANT USAGE ON SCHEMA public TO sync_api_user;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE
    ON TABLES
    TO sync_api_user;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT
    ON SEQUENCES
    TO sync_api_user;
```

### 2️⃣ Java Runtime

* **Java 21** (LTS) or later
* Required for **backend Spring Boot application**

### 3️⃣ Node.js / Frontend

* **Node.js 22** or later (for development and local builds)
* **npm 9** or **pnpm 10**

> ⚠️ Required only if you plan to build or customize the frontend locally. Docker images include prebuilt frontend.

### 4️⃣ Docker (Optional for demo)

* **Docker 24+**
* **Docker Compose 2+**
* Used to run the demo environment quickly without installing dependencies manually

---

## 🔑 Environment Variables

Some configuration depends on environment variables:
* `DATABASE_HOST`, `DATABASE_PORT`, `DATABASE_NAME`, `DATABASE_USER`, `DATABASE_PASSWORD`
* `AUTH_ISSUER_URI` (URL of the OIDC issuer / LemonLDAP)
* `AUTH_JWK_SET_URI` (URL of the JSON Web Key Set of the OIDC issuer / LemonLDAP)
* `AUTH_AUDIENCE` (expected `aud` claim of the access tokens, i.e. the audience declared for the API at the OIDC provider, distinct from the client ID)
* `AUTH_JWT_EXPECTED_TYPE` (optional, JOSE `typ` header required on access tokens, default `at+jwt`)
* `I18N_EXTERNAL_PATH` (optional, for backend/plugin translations)
* `sync-api`: `LINID_DATABASE_HOST`, `LINID_DATABASE_PORT`, `LINID_DATABASE_NAME`, `LINID_DATABASE_USER`,
  `LINID_DATABASE_PASSWORD` (its access to the LinId database) and `SYNC_TECHNICAL_ACCOUNT_ID` (its technical
  account), see [sync-api access to the LinId database](#-sync-api-access-to-the-linid-database)

> ⚠️ A `.env` file can be used for local development

---

## 🌐 Network & Access

* Ports required by LinId backend and frontend (default `8443` for backend) must be accessible
* LemonLDAP OIDC endpoint must be reachable by the backend and applications
* LemonLDAP must issue access tokens carrying a `typ` header whose value matches
  `AUTH_JWT_EXPECTED_TYPE` (default `at+jwt`, compared case-insensitively). Access tokens without a `typ` header are rejected with `401`.
* The OIDC provider must issue ID tokens with an audience different from the one of the access tokens
  (`AUTH_AUDIENCE`), otherwise an ID token with the expected `typ` would be accepted as a bearer token.
  LemonLDAP::NG copies the additional audiences (`oidcRPMetaDataOptionsAdditionalAudiences`) into its ID tokens
  as well, so with the shipped configuration the `typ` header is what keeps ID tokens out.

---

## 🔐 Certificates

LinId backend calls `sync-api` with mutual TLS, which requires:

* A **client certificate for LinId backend**, with the `clientAuth` extended key usage, packaged as a PKCS12 keystore
  (`SYNC_API_CLIENT_KEY_STORE`). LinId backend presents it to `sync-api` only.
* The **CA trusted by `sync-api`**: a truststore holding the CA that signed this client certificate
  (`SSL_CLIENT_TRUSTSTORE_PATH`). `sync-api` accepts any certificate chained to this CA whose subject is
  `SYNC_ALLOWED_CLIENT`, so the certificates this CA issues to the other components must not be CA certificates.
* A **certificate for `sync-api`** covering the hostname of `SYNC_API_URL`, trusted by the truststore of LinId backend
  (`SSL_TRUSTSTORE_PATH`).

---

## 🔄 sync-api access to the LinId database

`sync-api` reads the account data in the LinId database and writes the payloads and the computed rights to it. It
connects with its own PostgreSQL account (`LINID_DATABASE_USER`), limited to the tables and views it needs:

```sql
CREATE ROLE sync_api_linid_user WITH LOGIN PASSWORD '<password>';
```

Its grants are given once LinId backend has created its tables and views, since its migrations run when it starts:

```sql
GRANT USAGE ON SCHEMA public TO sync_api_linid_user;

GRANT SELECT
    ON accounts, applications, application_roles, account_computed_rights_view, account_payload_view
    TO sync_api_linid_user;

GRANT SELECT, INSERT, UPDATE
    ON account_payloads, computed_rights
    TO sync_api_linid_user;
```

* This account must not create nor alter any table: the migrations of the LinId database stay in LinId backend.
* Each source of the account payloads (`sync-data.job.sources`) needs the `SELECT` grant: the grants above cover
  `accounts` and `account_payload_view`, the sources of the example configuration.
* The audit tables of `account_payloads` and `computed_rights` are written by their triggers and need no grant.

`sync-api` also records its writes with a **technical account** of LinId as author. Create it as an account of LinId,
then set its `act_id` as `SYNC_TECHNICAL_ACCOUNT_ID`:

```sql
INSERT INTO accounts (external_id, email, lastname, firstname, created_by, updated_by)
VALUES ('sync-api', 'sync-api@<your-domain>', 'API', 'Sync', '<act_id of an administrator>', '<act_id of an administrator>')
RETURNING act_id;
```

---

## 📝 Best Practices

* Install and verify **PostgreSQL and Java** before running LinId
* Use **Docker** for an isolated, reproducible environment during development or demo
* Keep environment variables **secure and versioned** carefully
* Test network access to backend and LemonLDAP before configuring applications

---

## ➡️ Next Steps

* Proceed with **installation**:
  👉 [installation/setup](setup.md)

* Configure **environment variables**:
  👉 [configuration/application/config-files](../configuration/application/config-files.md)

* Verify **frontend build requirements** (if needed):
  👉 [configuration/ui/theming](../configuration/ui/theming.md)
