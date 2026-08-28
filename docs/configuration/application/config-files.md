# Application Configuration

## 🎯 Purpose

This guide explains how to configure **LinId Identity Manager**.

LinId uses **service-specific environment files** for its configuration. Each service has its own `.env` file containing the environment variables required by that service.

The demo environment uses the following configuration files:

* `api.env`: LinId API configuration
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

# Port exposed by the LemonLDAP::NG portal
LEMONLDAP_PORTAL_PORT=8080

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

# Basic Auth encoding: "linid-im-client:linid-im-secret" → Base64
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

# OIDC issuer URI; Spring Security uses this to discover configuration and validate JWTs
AUTH_ISSUER_URI=https://linid.localtest.me:9000/auth

# JWK Set URI; direct HTTP access to LemonLDAP::NG for fetching public keys
AUTH_JWK_SET_URI=https://linid.localtest.me:9000/auth/oauth2/jwks
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

The API communicates with OPA using:

```text
OPA_SERVER_URL=http://opa:8181
```

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
