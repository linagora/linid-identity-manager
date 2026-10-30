# Application Configuration

## 🎯 Purpose

This guide explains how to configure **LinId Identity Manager**.

LinId uses **service-specific environment files** for its configuration. Each service has its own `.env` file containing the environment variables required by that service.

The demo environment uses the following configuration files:

* `api.env`: LinId API configuration
* `sync-api.env`: LinId Sync API configuration (dev and E2E environments only)
* `cert.env`: SSL/TLS configuration
* `db.env`: PostgreSQL configuration
* `lemon.env`: LemonLDAP::NG configuration
* `demo.env`: Demo environment configuration
* `superset.env`: Apache Superset™ configuration

> ⚠️ Do **not commit secrets** to version control.

---

## 🧩 Environment Variables

### LinId API — `api.env`

```text
################################################################################
#                             LINID-IM-API CONFIGURATION                       #
################################################################################

# PostgreSQL database host
DATABASE_HOST=db

# PostgreSQL database port
DATABASE_PORT=5432

# PostgreSQL database administrator username
DATABASE_ADMIN_USER=admin

# PostgreSQL database administrator password
DATABASE_ADMIN_PASSWORD=password

# Name of the database
DATABASE_NAME=linid

# Database username
DATABASE_USER=linid_user

# Database password
DATABASE_PASSWORD=password

# Enable or disable Swagger UI
SWAGGER_ENABLED=true

# Logging level (DEBUG, INFO, WARN, ERROR)
LOGGING_LEVEL=INFO

# OIDC issuer URI; must match the "iss" claim in JWTs
AUTH_ISSUER_URI=https://ui/auth

# JWK Set URI; direct HTTPS access to LemonLDAP::NG for fetching public keys
AUTH_JWK_SET_URI=https://ui/auth/oauth2/jwks

# Expected "aud" claim of the access tokens; must match the API audience declared in LemonLDAP
# (oidcRPMetaDataOptionsAdditionalAudiences), distinct from the OIDC client ID
AUTH_AUDIENCE=linid-im-api

# JOSE "typ" header required on access tokens (default: at+jwt)
# Only needed if the OIDC provider issues another type
# Leave the variable unset to use the default: an empty value is not the default and rejects every token
AUTH_JWT_EXPECTED_TYPE=at+jwt

# Path to SSL/TLS key store
SSL_KEY_STORE=classpath:keystore.p12

# Password for the individual SSL key within the key store
SSL_KEY_PASSWORD=password

# Path to SSL/TLS trust store
SSL_TRUSTSTORE_PATH=file:/etc/ssl/truststore.jks

# Password for accessing the SSL/TLS trust store
SSL_TRUSTSTORE_PASSWORD=password

# Path to client-specific Flyway migrations
CLIENT_DATABASE_MIGRATION_PATH=filesystem:docker/dev/resources/db-client

# Schema name for client-specific Flyway migrations
# CLIENT_DATABASE_SCHEMA=public

# Path to an external configuration file
EXTERNAL_CONFIGURATION=/app/extra-configuration.yaml

# Base URL of the OPA server used by the API to publish application policies
OPA_SERVER_URL=http://opa:8181

# Base URL of the Apache Superset server
SUPERSET_URL=https://superset:8443

# Superset username used by the API to authenticate
SUPERSET_USERNAME=superset

# Superset password used by the API to authenticate
SUPERSET_PASSWORD=password

# Path to the Superset Row Level Security (RLS) configuration file
SUPERSET_RLS_CONFIG=/app/superset.yaml

# Caffeine cache spec for the Superset access token (single entry, renewed
# before Superset's JWT_ACCESS_TOKEN_EXPIRES to avoid using an expired token)
# SUPERSET_CACHE_OPTION=maximumSize=1,expireAfterWrite=55m,recordStats

# Base URL of sync-api, called by the API with mutual TLS
SYNC_API_URL=https://sync-api:8443

# Path to the PKCS12 key store holding the client certificate presented by the API to sync-api only
SYNC_API_CLIENT_KEY_STORE=file:/etc/ssl/api-client-keystore.p12

# Password of the client key store and of its private key
SYNC_API_CLIENT_KEY_PASSWORD=password

# Root directory of the avatar images, shared with the front-end web server that serves them
AVATAR_LOCATION=/app/avatars

# Only allowed avatar file extension
AVATAR_EXTENSION=png

# Maximum allowed avatar file size, in MB
AVATAR_MAX_SIZE=10

# Compression quality of the re-encoded JPEG avatars, only used when AVATAR_EXTENSION is a JPEG one
AVATAR_JPEG_QUALITY=0.9

# Maximum width times height of the uploaded avatar images, checked before decoding
AVATAR_MAX_PIXELS=16000000
```

---

### 🔄 LinId Sync API — `sync-api.env`

Used by the dev and E2E environments only: the demo does not run `sync-api`. `sync-api` only accepts the requests
presenting the client certificate of the API (mutual TLS).

```text
################################################################################
#                             LINID-SYNC-API CONFIGURATION                     #
################################################################################

# PostgreSQL database host
DATABASE_HOST=db

# PostgreSQL database port
DATABASE_PORT=5432

# PostgreSQL database administrator username
DATABASE_ADMIN_USER=admin

# PostgreSQL database administrator password
DATABASE_ADMIN_PASSWORD=password

# Name of the database
DATABASE_NAME=sync_api

# Database username
DATABASE_USER=sync_api_user

# Database password
DATABASE_PASSWORD=password

# PostgreSQL host of the linid-api database
LINID_DATABASE_HOST=db

# PostgreSQL port of the linid-api database
LINID_DATABASE_PORT=5432

# Name of the linid-api database
LINID_DATABASE_NAME=linid

# Username of sync-api in the linid-api database, limited to the tables it reads and writes
LINID_DATABASE_USER=sync_api_linid_user

# Password of sync-api in the linid-api database
LINID_DATABASE_PASSWORD=password

# Maximum number of connections to the linid-api database (default: 5)
# LINID_DATABASE_MAX_POOL_SIZE=5

# act_id of the technical account of sync-api, shared by its instances and recorded as the author of their writes
SYNC_TECHNICAL_ACCOUNT_ID=00000000-0000-4000-8000-00000000a012

# Identifier of the instance, written with the rows it writes in the linid-api database (default: the host name)
# SYNC_INSTANCE_ID=sync-api-1

# Path to SSL/TLS key store
SSL_KEY_STORE=file:/etc/ssl/keystore.p12

# Type of the SSL/TLS key store (PKCS12 or JKS)
SSL_KEY_STORE_TYPE=PKCS12

# Password for the individual SSL key within the key store
SSL_KEY_PASSWORD=password

# Path to SSL/TLS trust store
SSL_TRUSTSTORE_PATH=file:/etc/ssl/truststore.jks

# Password for accessing the SSL/TLS trust store
SSL_TRUSTSTORE_PASSWORD=password

# Path to the trust store holding the CA that signed the client certificate of linid-api, used to validate the
# client certificates
SSL_CLIENT_TRUSTSTORE_PATH=file:/etc/ssl/truststore.jks

# Password for accessing the client trust store (default: SSL_TRUSTSTORE_PASSWORD)
# SSL_CLIENT_TRUSTSTORE_PASSWORD=password

# Subject of the only accepted client certificate, the one of linid-api, in the RFC 2253 format
SYNC_ALLOWED_CLIENT=CN=linid-api

# Schedule of the global job, as a Spring cron expression (second, minute, hour, day of month, month, day of week)
SYNC_CRON="0 0 2 * * *"

# Number of accounts read, built and written per chunk of the payload build, in one transaction (default: 500)
# SYNC_PAYLOAD_CHUNK_SIZE=500

# Number of account and application pairs read, computed and written per chunk by the computation of the rights
# (default: 100)
# SYNC_COMPUTE_CHUNK_SIZE=100

# Number of inserts or updates sent together to the linid-api database, within the transaction of a chunk
# (default: 50)
# SYNC_JDBC_BATCH_SIZE=50

# Base URL of the OPA server used by sync-api to evaluate the application policies
OPA_SERVER_URL=http://opa:8181

# Path of the result of an application policy on the OPA server, {code} being the application code
OPA_EVALUATION_PATH=/v1/data/authz/{code}/result

# Maximum time to establish the connection to the OPA server, as a duration (e.g. 5s, 500ms)
OPA_CONNECT_TIMEOUT=5s

# Maximum time to wait for the response of the OPA server, as a duration (e.g. 10s, 500ms)
OPA_READ_TIMEOUT=10s

# Path to the external configuration file, holding the sources and the template of the account payloads
EXTERNAL_CONFIGURATION=/app/sync-config.yaml

# Logging level (DEBUG, INFO, WARN, ERROR)
LOGGING_LEVEL=INFO
```

---

### 🔒 SSL/TLS — `cert.env`

```text
################################################################################
#                             SSL CERTIFICATE CONFIGURATION                    #
################################################################################

# Password used to access the SSL/TLS trust store
SSL_TRUSTSTORE_PASSWORD=password

# Password used to access the private key stored in the SSL/TLS key store
SSL_KEY_PASSWORD=password
```

---

### 🗄️ PostgreSQL — `db.env`

```text
################################################################################
#                              POSTGRES CONFIGURATION                          #
################################################################################

# PostgreSQL database administrator username
POSTGRES_USER=admin

# PostgreSQL database administrator password
POSTGRES_PASSWORD=password

# Default PostgreSQL database created during container initialization
POSTGRES_DB=postgres

# PostgreSQL database host
DATABASE_HOST=db

# PostgreSQL database port
DATABASE_PORT=5432

# Database name used by LinId
LINID_DATABASE_NAME=linid

# Database name used by LemonLDAP::NG
LEMONLDAP_DATABASE_NAME=lemon

# Database name used by Apache Superset for its metadata
SUPERSET_DATABASE_NAME=superset

# Database name used as the analytical DataLake for dashboard data
DATALAKE_DATABASE_NAME=datalake
```

---

### 🔐 LemonLDAP::NG — `lemon.env`

```text
################################################################################
#                             LEMONLDAP CONFIGURATION                          #
################################################################################

# PostgreSQL administrator username used to initialize the LemonLDAP::NG database
LEMONLDAP_DATABASE_ADMIN_USER=admin

# PostgreSQL administrator password used to initialize the LemonLDAP::NG database
LEMONLDAP_DATABASE_ADMIN_PASSWORD=password

# Name of the database used by LemonLDAP::NG
LEMONLDAP_DATABASE_NAME=lemon

# PostgreSQL username used by LemonLDAP::NG to connect to its database
LEMONLDAP_DATABASE_USER=lemon_user

# PostgreSQL password used by LemonLDAP::NG to connect to its database
LEMONLDAP_DATABASE_PASSWORD=password

# Shared secret used by the /checkstate endpoint
LEMONLDAP_CHECKSTATE_SECRET=checkstate-secret
```

---

### 📊 Apache Superset™ — `superset.env`

```text
################################################################################
#                              SUPERSET CONFIGURATION                          #
################################################################################

# PostgreSQL database host used by Apache Superset
DATABASE_HOST=db

# PostgreSQL database port
DATABASE_PORT=5432

# Name of the PostgreSQL database used by Apache Superset for its metadata
DATABASE_NAME=superset

# PostgreSQL username used by Apache Superset
DATABASE_USER=superset_user

# PostgreSQL password used by Apache Superset
DATABASE_PASSWORD=password

# Path to the Apache Superset configuration file
SUPERSET_CONFIG_PATH=/app/pythonpath/superset_config.py

# SQLAlchemy connection URI for the Apache Superset metadata database
SUPERSET_DATABASE_URI=postgresql+psycopg2://superset_user:password@db:5432/superset

# Secret key used by Apache Superset for application and session security
SUPERSET_SECRET_KEY=super-secret-key

# Apache Superset administrator username
SUPERSET_ADMIN_USER=superset

# Apache Superset administrator email address
SUPERSET_ADMIN_EMAIL=admin@example.com

# Apache Superset administrator password
SUPERSET_ADMIN_PASSWORD=password

# Username of the service account used to generate Superset guest tokens
SUPERSET_BROKER_USERNAME=broker

# Email address of the Superset guest token issuer service account
SUPERSET_BROKER_EMAIL=broker@example.com

# Password of the Superset guest token issuer service account
SUPERSET_BROKER_PASSWORD=password

# Internal URL used by other services to communicate with Apache Superset
SUPERSET_URL=https://superset:8443

# Slug of the dashboard to import and configure for embedding
DASHBOARD_SLUG=USER_LOG_DASHBOARD

# Name of the PostgreSQL database containing analytical DataLake data
DATALAKE_DATABASE_NAME=datalake

# PostgreSQL username used by Superset to access the DataLake database
DATALAKE_DATABASE_USER=datalake_user

# PostgreSQL password used by Superset to access the DataLake database
DATALAKE_DATABASE_PASSWORD=password

# Public URL of the application allowed to embed Superset dashboards
APPLICATION_URL=https://localhost:9000
```

---

### 🧪 Demo — `demo.env`

```text
################################################################################
#                             DEMO CONFIGURATION                              #
################################################################################

# Name of environment used
ENV=demo

# Directory containing the LemonLDAP::NG configuration
CONFIG_DIR=local

# Public hostname used by the LemonLDAP::NG portal
PORTAL_HOSTNAME=linid.localtest.me

# Single Sign-On domain managed by LemonLDAP::NG
SSODOMAIN=linid.localtest.me

# Target environment
ENVIRONMENT=LOCAL_E2E

# Path to Cucumber feature files used for E2E tests
CYPRESS_FEATURES_PATH=/app/features

# Base URL of the Frontend to be tested in E2E scenarios
E2E_FRONT_URL=https://linid.localtest.me:9000

# Base URL of the API to be tested in E2E scenarios
E2E_API_URL=https://localhost:8443

# Base URL of the Auth server to be tested in E2E scenarios
E2E_AUTH_URL=https://linid.localtest.me:9000/auth

# Basic Auth encoding: "linid-im-client:linid-im-secret" → Base64, must match OIDC_CLIENT_SECRET
E2E_AUTH_TOKEN=Basic bGluaWQtaW0tY2xpZW50OmxpbmlkLWltLXNlY3JldA==

# PostgreSQL database host
E2E_DATABASE_HOST=localhost

# PostgreSQL database port
E2E_DATABASE_PORT=5432

# PostgreSQL database administrator username
E2E_DATABASE_ADMIN_USER=admin

# PostgreSQL database administrator password
E2E_DATABASE_ADMIN_PASSWORD=password

# Name of the database
E2E_DATABASE_NAME=linid

# Username of sync-api in the database, the LINID_DATABASE_USER of sync-api.env
E2E_DATABASE_SYNC_API_USER=sync_api_linid_user

# Password of sync-api in the database, the LINID_DATABASE_PASSWORD of sync-api.env
E2E_DATABASE_SYNC_API_PASSWORD=password

# Timezone for the application
TZ=Europe/Paris

# Hostname for the LemonLDAP::NG portal, used in OIDC configuration
LEMONLDAP_PORTAL_HOSTNAME=linid.localtest.me

# Domain of the LemonLDAP portal
LEMONLDAP_SSO_DOMAIN=linid.localtest.me

# Redirect URL after LemonLDAP authentication
LEMONLDAP_REDIRECT_URL=https://linid.localtest.me:9000

# Full URL of the LemonLDAP portal
LEMONLDAP_PORTAL_URL=https://linid.localtest.me:9000/auth

# PostgreSQL administrator username used to initialize the LemonLDAP::NG database
LEMONLDAP_DATABASE_ADMIN_USER=admin

# PostgreSQL administrator password used to initialize the LemonLDAP::NG database
LEMONLDAP_DATABASE_ADMIN_PASSWORD=password

# Name of the database used by LemonLDAP::NG
LEMONLDAP_DATABASE_NAME=lemon

# PostgreSQL username used by LemonLDAP::NG
LEMONLDAP_DATABASE_USER=lemon_user

# PostgreSQL password used by LemonLDAP::NG
LEMONLDAP_DATABASE_PASSWORD=password

# Secret of the OIDC client of the UI server, shared by LemonLDAP::NG and the UI server
OIDC_CLIENT_SECRET=linid-im-secret

# OIDC issuer URI; Spring Security uses this to discover configuration and validate JWTs
AUTH_ISSUER_URI=https://linid.localtest.me:9000/auth

# JWK Set URI; direct HTTP access to LemonLDAP::NG for fetching public keys
AUTH_JWK_SET_URI=https://linid.localtest.me:9000/auth/oauth2/jwks

# Expected "aud" claim of the access tokens; must match the API audience declared in LemonLDAP
# (oidcRPMetaDataOptionsAdditionalAudiences), distinct from the OIDC client ID
AUTH_AUDIENCE=linid-im-api

# JOSE "typ" header required on access tokens (default: at+jwt)
# Only needed if the OIDC provider issues another type
# Leave the variable unset to use the default: an empty value is not the default and rejects every token
AUTH_JWT_EXPECTED_TYPE=at+jwt
```

---

## ⚙️ Overriding YAML Configuration

LinId ships with a default `application.yml` in the classpath.

You can **override any configuration** by pointing to an external YAML file:

```bash
export EXTERNAL_CONFIGURATION=/path/to/my-config.yml
```

For the demo environment, the API uses:

```text
EXTERNAL_CONFIGURATION=/app/extra-configuration.yaml
```

> This is optional and allows custom setups without modifying the packaged YAML.

---

## 🧾 Account Payloads — `sync-data`

`sync-api` builds the payload of each account, the `input` document evaluated by the OPA policies of the
applications (see [advanced/policies](../../advanced/policies.md)), from the data of the LinId database. The data read
and the structure of the payload are set in the `sync-data` section of the file referenced by `EXTERNAL_CONFIGURATION`,
which `sync-api` requires to start (`docker/dev/resources/sync-config.yaml` in the development environment,
`docker/e2e/resources/sync/sync-config.yaml` in the E2E environment):

| Key                                                | Default  | Description                                                        |
| -------------------------------------------------- | -------- | ------------------------------------------------------------------ |
| `sync-data.job.sources`                            | —        | Sources read to build the payloads, by name                        |
| `sync-data.job.sources.<source>.account-id-column` | `act_id` | Column of the source holding the `act_id` of the account           |
| `sync-data.job.mapping`                            | —        | JSON template of the payload, whose values are Jinjava expressions |

```yaml
sync-data:
  job:
    sources:
      accounts:
        account-id-column: act_id
      account_payload_view:
        account-id-column: act_id
    mapping: |
      {
        "external_id": {{ accounts.external_id | tojson }},
        "email": {{ accounts.email | tojson }},
        "organizational_units": {{ account_payload_view.organizational_units | tojson }},
        "groups": {{ account_payload_view.groups | tojson }}
      }
```

Payload built for an account:

```json
{
  "external_id": "jdoe",
  "email": "jdoe@example.com",
  "organizational_units": ["Paris", "R&D"],
  "groups": ["DEV", "RND"]
}
```

### Sources

* A source is any table or view of the LinId database: a table, one of its views, or a view created for the payload.
  The `accounts` table is not required.
* The PostgreSQL account of `sync-api` (`LINID_DATABASE_USER`) needs the `SELECT` grant on each source.
* Every source has the column set in `account-id-column`, of type `uuid`, and at most one row per account. Each chunk
  of accounts filters on this column, so it must rely on an index of the underlying tables.
* A multi-valued attribute is a PostgreSQL array column, read as a list. It must be sorted and without duplicates,
  so that the checksum of the payload is stable. The `account_payload_view` view gives, for each account, the sorted
  names of its organizational units (`organizational_units`) and codes of its groups (`groups`), empty arrays when it
  has none.
* A `json` or `jsonb` column is read as JSON: `{{ accounts.extra_parameters | tojson }}` inserts the whole object,
  and `{{ accounts.extra_parameters.department | tojson }}` one of its fields, `null` when it is absent. A timestamp
  column is inserted as a number of milliseconds since the epoch.
* The source and column names must match `[a-z_][a-z0-9_]*`: they are checked and quoted before being used in the SQL
  queries.

### Template

* The template is rendered once per account with Jinjava, then parsed as a JSON object.
* Each source is a variable of the template, named after the source, holding the columns of the row of the account.
  The variable is empty when the account has no row in the source.
* Every value is inserted with the `tojson` filter, without quotes: a list stays a JSON array, a string is correctly
  escaped, and an empty variable gives `null`.
* Every `mapping` provides the `external_id` and `email` keys: the claims template of the LINID system application
  reads them in the payload to build its `sub` and `email` claims.

### Errors

`sync-api` logs an alert for each error (see `sync-api/README.md`):

* **Invalid mapping**: the template is checked before any source is read, rendered with an empty context and parsed.
  A Jinjava syntax error or a value inserted without `tojson` stops the payload build: nothing is written, and the
  rights are not computed.
* **Unreadable source**: an unknown source, an invalid name, a missing account id column or a database error stops
  the payload build. The chunks already written stay written, and the other accounts keep their previous payload.
* **Account in error**: an account with several rows in a source, or whose data gives a template error or an invalid
  JSON, keeps its previous payload, and the payloads of the other accounts are built.

### Chunk size

The payloads are built by chunks of accounts: for each chunk, each source is read once, with the `act_id` of the
accounts of the chunk, then the payloads are built and written in one transaction.

```text
SYNC_PAYLOAD_CHUNK_SIZE=500
```

* Reading the sources account by account would send one query per source and per account, and reading all the
  accounts at once would hold every row in memory and every write in one transaction. A larger chunk sends fewer
  queries, but holds more rows in memory and a longer transaction.
* It is set in `sync-api.env` (`sync.payload.chunk-size`), not in the `sync-data` section: the `sync-data` section
  describes the data, while the chunk size tunes the processing of `sync-api`.

### Writing

The payload of each account is written in the `account_payloads` table of the LinId database only when its checksum
changed. The checksum is computed on its compact JSON, with its keys sorted at every level: the order of the keys in
the template does not change it. The arrays keep their order. An account deleted since the start of the payload
build is left out.

```text
SYNC_JDBC_BATCH_SIZE=50
SYNC_INSTANCE_ID=sync-api-1
```

* `SYNC_JDBC_BATCH_SIZE` (default `50`) is the number of `INSERT` or `UPDATE` statements sent together to the LinId
  database when a chunk is written, just before its commit, one round trip per group: a chunk of 500 changed payloads
  takes 10 round trips instead of 500, and a chunk without change sends nothing. It does not change the chunk size, the
  number of accounts per transaction.
* `SYNC_INSTANCE_ID` (default: the host name) is the identifier of the instance, written in the `sync_instance_id`
  column of the rows it writes.

---

## 🏷️ Key Configuration Domains

### 1. Database

Database configuration is distributed between `db.env`, `api.env`, `lemon.env`, and `superset.env`.

### 2. Logging & Swagger

The API logging level and Swagger UI can be configured through `api.env`.

### 3. SSL / HTTPS

SSL/TLS configuration is defined through `api.env` and `cert.env`.

### 4. OAuth2 / OIDC

OIDC configuration is provided through `api.env` and `local.env`.

Authentication is handled entirely by the **pre-configured LemonLDAP::NG** instance.

### 5. Apache Superset™

Apache Superset™ configuration is isolated in `superset.env` and includes:

* Metadata database configuration
* Administrator credentials
* Guest token broker configuration
* DataLake connection
* Dashboard configuration
* Embedding configuration

### 6. Open Policy Agent (OPA)

The API and sync-api both communicate with OPA, through the `OPA_SERVER_URL` variable of `api.env` and of
`sync-api.env`:

```text
OPA_SERVER_URL=http://opa:8181
```

### 7. Avatars

Avatar images uploaded through `POST /avatars/{entity}/{id}` are stored on the filesystem as
`{AVATAR_LOCATION}/{entity}/{id}.{AVATAR_EXTENSION}` and served by the front-end web server, which mounts the same
directory under `/avatars`. In this repository, that shared directory is `docker/avatars`, mounted in the `api`
container as `/app/avatars` and in the `ui` container as `/usr/share/nginx/html/avatars`.

In dev mode the two sides run natively: the API resolves `AVATAR_LOCATION` from the directory it is started in,
`api/`, hence the `../docker/avatars` value of `docker/dev/env/api.env`, and the Quasar dev server serves that same
directory under `/avatars`.

```text
AVATAR_LOCATION=/app/avatars
AVATAR_EXTENSION=png
AVATAR_MAX_SIZE=10
AVATAR_JPEG_QUALITY=0.9
AVATAR_MAX_PIXELS=16000000
```

`AVATAR_MAX_SIZE` feeds the Spring multipart limits (`spring.servlet.multipart`), so a larger upload is rejected
before it reaches the API: the file part is capped at that size, the request size left unbounded by default
(`SERVER_MAX_REQUEST_SIZE`, `-1` when unset) so a file of exactly the maximum size is not rejected for its
multipart overhead.

The uploaded files come from the users, so the API validates their content, not only their name: the real type is
detected from the bytes and checked against `AVATAR_EXTENSION`, the dimensions are bounded by `AVATAR_MAX_PIXELS`
before decoding, and the image is decoded and re-encoded from its pixels only, dropping metadata and anything
appended to the image data. The front-end web server also serves the
files without content sniffing (`X-Content-Type-Options: nosniff`) and fully sandboxed
(`Content-Security-Policy: default-src 'none'; sandbox`): an image carrying a script, such as an SVG, cannot run
anything when its URL is opened. Keep `AVATAR_EXTENSION` on an image format that cannot embed code, and mirror
those headers in any other web server put in front of the avatars.

---

## 📝 Best Practices

* Store `.env` files securely.
* Version control **only non-sensitive configuration templates**.
* Never use production credentials in development or demo environments.
* Use service-specific environment files rather than a single global `.env`.
* Use `EXTERNAL_CONFIGURATION` for application-specific YAML overrides.
* Document custom claims, roles, dashboards, and policies.
* Test configuration in a development/staging environment before production.

---

## ➡️ Next Steps

* Define **claims per application**:
  👉 [authentication/claims](../authentication/claims.md)

* Configure **application roles and policies**:
  👉 [advanced/policies](../../advanced/policies.md)

* Customize **UI / themes / i18n**:
  👉 [ui/theming](../ui/theming.md)
