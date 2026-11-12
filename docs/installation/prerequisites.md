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

* An **administrator account** (`DATABASE_ADMIN_USER`), owner of the database, which runs the migrations of `sync-api`
* A **runtime account** (`DATABASE_USER`), which must not own it, with the grants of
  [`init_db.sql`](../../docker/init-db/init_db.sql), run as `DATABASE_ADMIN_USER` before the first start of `sync-api`

> ⚠️ Check the [deployment constraints](../advanced/sync-api.md#%EF%B8%8F-deployment-constraints) of `sync-api` (PgBouncer,
> `idle_session_timeout`, `max_connections`).

### 2️⃣ Java Runtime

* **Java 25** (LTS) or later
* Required for **backend Spring Boot application**

### 3️⃣ Node.js / Frontend

* **Node.js 22.22** or later (for development and local builds)
* **pnpm 11**

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
* `LINID_DATABASE_*` and `SYNC_TECHNICAL_ACCOUNT_ID` for `sync-api`, see
  [sync-api access to the LinId database](#-sync-api-access-to-the-linid-database)

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

* A **client certificate** for LinId backend (`clientAuth`), in a PKCS12 keystore (`SYNC_API_CLIENT_KEY_STORE`)
* A **truststore** for `sync-api`, holding the CA of this certificate (`SSL_CLIENT_TRUSTSTORE_PATH`): the other
  certificates issued by this CA must not be CA certificates
* A **certificate for `sync-api`**, covering the hostname of `SYNC_API_URL` and trusted by LinId backend
  (`SSL_TRUSTSTORE_PATH`)

---

## 🔄 sync-api access to the LinId database

`sync-api` connects to the LinId database with its own account (`LINID_DATABASE_USER`), which must not create nor alter
any table:

* Created like `sync_api_linid_user` in [`init_db.sql`](../../docker/init-db/init_db.sql)
* Granted with [`init_sync_api_linid_user.sql`](../../docker/init-db/scripts/init_sync_api_linid_user.sql), once LinId
  backend has created its tables
* With the `SELECT` grant on each source of the account payloads (`sync-data.job.sources`)

Its writes have a **technical account** of LinId as author: create it, then set the returned `act_id` as
`SYNC_TECHNICAL_ACCOUNT_ID`:

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
