# Application Configuration

## 🎯 Purpose

This guide explains how to configure **LinId Identity Manager**.

LinId uses **environment variables** for primary configuration and allows optional **YAML overrides** via an external
file.

---

## 🧩 Environment Variables

All critical settings are configured via a `.env` file or environment variables.

### Example `.env` structure:

```text id="env-example"
################################################################################
#                             LINID-IM-API CONFIGURATION                       #
################################################################################

# PostgreSQL database host
DATABASE_HOST=db

# PostgreSQL database port
DATABASE_PORT=5432

# Name of the database
DATABASE_NAME=linid-db

# Database admin username
DATABASE_ADMIN_USER=admin

# Database admin password
DATABASE_ADMIN_PASSWORD=password

# Database username
DATABASE_USER=linid_user

# Database password
DATABASE_PASSWORD=password

# Enable or disable Swagger UI
SWAGGER_ENABLED=true

# Logging level (DEBUG, INFO, WARN, ERROR)
LOGGING_LEVEL=INFO

# OIDC issuer URI; Spring Security uses this to discover configuration and validate JWTs
AUTH_ISSUER_URI=https://linid.localtest.me:9000/auth

# JWK Set URI; direct HTTP access to LemonLDAP for fetching public keys
AUTH_JWK_SET_URI=https://linid.localtest.me:9000/auth/oauth2/jwks

# Path to SSL/TLS key store
SSL_KEY_STORE=classpath:keystore.jks

# Password for the individual SSL key within the key store
SSL_KEY_PASSWORD=password

# Path to SSL/TLS trust store
SSL_TRUSTSTORE_PATH=file:/etc/ssl/truststore.jks

# Password for accessing the SSL/TLS trust store
SSL_TRUSTSTORE_PASSWORD=changeit

################################################################################
#                             LEMONLDAP CONFIGURATION                          #
################################################################################

# Hostname for the LemonLDAP::NG portal, used in OIDC configuration
LEMONLDAP_PORTAL_HOSTNAME=linid.localtest.me

# Port for the LemonLDAP::NG portal
LEMONLDAP_PORTAL_PORT=8080

# Database configuration for LemonLDAP::NG
LEMONLDAP_DATABASE_ADMIN_USER=admin

# Database admin password for LemonLDAP::NG
LEMONLDAP_DATABASE_ADMIN_PASSWORD=password

# Database username for LemonLDAP::NG
LEMONLDAP_DATABASE_USER=linid_user

# Database password for LemonLDAP::NG
LEMONLDAP_DATABASE_PASSWORD=password

# Database name for LemonLDAP::NG
LEMONLDAP_DATABASE_NAME=linid-db

# Domain of the LemonLDAP portal
LEMONLDAP_SSO_DOMAIN=linid.localtest.me

# Redirect URL after LemonLDAP authentication
LEMONLDAP_REDIRECT_URL=https://linid.localtest.me:9000/callback

# Full URL of the LemonLDAP portal
LEMONLDAP_PORTAL_URL=https://linid.localtest.me:9000/auth

################################################################################
#                             COMMON CONFIGURATION (LOCAL)                     #
################################################################################

CONFIG_DIR=local
```

> ⚠️ Do **not commit secrets** to version control.

---

## ⚙️ Overriding YAML Configuration

LinId ships with a default `application.yml` in the classpath.

You can **override any configuration** by pointing to an external YAML:

```bash
export EXTERNAL_CONFIGURATION=/path/to/my-config.yml
```

> This is optional and allows custom setups without modifying the packaged YAML.

---

## 🏷️ Key Configuration Domains

### 1. Database

Configure PostgreSQL connection, admin user, and migration paths via environment variables.

### 2. Logging & Swagger

Control logging levels and enable/disable Swagger UI.

### 3. SSL / HTTPS

Configure keystore path and passwords for secure connections.

### 4. OAuth2 / OIDC

Specify `AUTH_ISSUER_URI`. Note: authentication is handled entirely by **pre-configured LemonLDAP**.

### 5. Plugins & i18n

* External plugins path can be configured
* External translations path and merge order can be configured

---

## 📝 Best Practices

* Store `.env` files securely
* Version control **only non-sensitive config templates**
* Use `EXTERNAL_CONFIGURATION` for environment-specific overrides
* Document all custom claims, roles, and policies in LinId
* Test configuration in a dev/staging environment before production

---

## ➡️ Next Steps

* Define **claims per application**:
  👉 [authentication/claims](../authentication/claims.md)

* Configure **application roles and policies**:
  👉 [advanced/policies](../../advanced/policies.md)

* Customize **UI / themes / i18n**:
  👉 [ui/theming](../ui/theming.md)
