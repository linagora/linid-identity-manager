# Apache Superset Docker Setup

This directory contains the Docker configuration used to deploy and initialize **Apache Superset 6.1.0** for dashboarding and embedded dashboards.

The setup provides:

* A PostgreSQL database for Superset metadata.
* HTTPS access to Superset using a custom certificate.
* Automatic Superset database initialization.
* Automatic creation of the Superset administrator.
* Automatic configuration of the `Guest` role.
* Automatic creation of a service account used to generate guest tokens.
* Automatic import of the dashboard assets from `dashboard_export.zip`.
* Automatic activation of dashboard embedding.
* Configuration through environment variables.
* CORS and Content Security Policy configuration for the application embedding Superset.

## Directory structure

```text
docker/superset/
├── Dockerfile
├── README.md
├── dashboard_export.zip
├── init_data_superset.sh
├── init_guest_role.py
├── init_superset.sh
└── superset_config.py
```

### `Dockerfile`

Builds the custom Superset image on top of:

```text
apache/superset:6.1.0
```

The image additionally installs:

* `flask-cors`
* `psycopg2-binary`
* `Authlib`
* `gettext-base`
* `zip`

It also copies the Superset configuration and initialization scripts into the image.

Superset is exposed internally over HTTPS on port `8443`.

The image includes a health check against:

```text
https://localhost:8443/health
```

### `superset_config.py`

Contains the runtime Superset configuration.

The Superset metadata database connection is built from environment variables:

```text
DATABASE_USER
DATABASE_PASSWORD
DATABASE_HOST
DATABASE_PORT
DATABASE_NAME
```

The Superset secret key is provided through:

```text
SUPERSET_SECRET_KEY
```

Embedded dashboards are enabled with:

```python
FEATURE_FLAGS = {"EMBEDDED_SUPERSET": True}
```

CORS is restricted to the application URL:

```text
APPLICATION_URL
```

The same URL is allowed as a CSP `frame-ancestor`, allowing the application to embed Superset dashboards.

The `Guest` role is explicitly configured as the role used for guest-token-based dashboard access:

```python
GUEST_ROLE_NAME = "Guest"
```

## Superset initialization

`init_superset.sh` is the container entrypoint.

It performs the following operations:

1. Waits briefly for the container environment to be ready.
2. Installs the Python dependencies required by the setup.
3. Creates the Superset administrator.
4. Runs the Superset database migrations.
5. Initializes Superset.
6. Creates and configures the custom `Guest` role.
7. Creates the `GuestTokenIssuer` service account.
8. Starts Superset using Gunicorn.

### Administrator

The administrator is created using:

```text
SUPERSET_ADMIN_USER
SUPERSET_ADMIN_EMAIL
SUPERSET_ADMIN_PASSWORD
```

If the user already exists, the command is allowed to fail so that container startup remains idempotent.

### Guest token issuer

A dedicated service account is created using:

```text
SUPERSET_BROKER_USERNAME
SUPERSET_BROKER_EMAIL
SUPERSET_BROKER_PASSWORD
```

The account receives the `GuestTokenIssuer` role.

This role is responsible for generating guest tokens for embedded dashboards.

## Guest roles

`init_guest_role.py` creates two roles.

### `Guest`

The `Guest` role is used by users accessing embedded dashboards.

It receives permissions required to access dashboards and their underlying data, including:

```text
can_read Dashboard
can_read Chart
can_read Dataset
can_list Dataset
can_get Dataset
can_read Database
can_list Database
can_get Database
can_read CurrentUserRestApi
can_explore Superset
can_explore_json Superset
can_time_range Api
all_datasource_access
```

The `all_datasource_access` permission gives the guest role access to all configured datasources.

If Row Level Security (RLS) is introduced later, this permission should be reviewed carefully and replaced or constrained as appropriate.

### `GuestTokenIssuer`

The `GuestTokenIssuer` role is used by the service account responsible for issuing guest tokens.

It receives:

```text
can_grant_guest_token SecurityRestApi
can_read SecurityRestApi
can_read Dashboard
can_get_embedded Dashboard
can_read EmbeddedDashboard
```

This keeps the token-issuing account separate from the dashboard guest role.

## Dashboard import

`dashboard_export.zip` contains the Superset assets that are imported automatically when the initialization process runs.

The import is performed by:

```text
init_data_superset.sh
```

The script waits until the Superset health endpoint reports that Superset is ready:

```text
$SUPERSET_URL/health
```

It then authenticates against:

```text
/api/v1/security/login
```

and obtains:

* an access token;
* a CSRF token;
* the associated Superset session cookie.

The dashboard archive is subsequently imported through:

```text
/api/v1/dashboard/import/
```

The database password required by the imported datasource is supplied dynamically through:

```text
DATALAKE_DATABASE_PASSWORD
```

The password is therefore not required to be stored directly inside the exported dashboard archive.

## Dashboard embedding

After importing the dashboard, the script searches for the dashboard using:

```text
DASHBOARD_SLUG
```

It retrieves the dashboard ID and enables embedding through:

```text
/api/v1/dashboard/{dashboard_id}/embedded
```

The configured application URL is passed as an allowed embedding domain:

```text
APPLICATION_URL
```

A successful operation returns the embedding UUID.

This UUID can then be used by the application when creating the embedded dashboard configuration.

## Environment variables

The following environment variables are expected by the setup.

### Superset metadata database

```text
DATABASE_HOST
DATABASE_PORT
DATABASE_NAME
DATABASE_USER
DATABASE_PASSWORD
```

These variables configure the PostgreSQL database used internally by Superset.

### Superset security

```text
SUPERSET_SECRET_KEY
```

Secret key used by Superset for session and application security.

It must be persistent across container restarts.

### Superset administrator

```text
SUPERSET_ADMIN_USER
SUPERSET_ADMIN_EMAIL
SUPERSET_ADMIN_PASSWORD
```

### Guest token issuer

```text
SUPERSET_BROKER_USERNAME
SUPERSET_BROKER_EMAIL
SUPERSET_BROKER_PASSWORD
```

### Application integration

```text
APPLICATION_URL
SUPERSET_URL
DASHBOARD_SLUG
```

`APPLICATION_URL` identifies the application that is allowed to embed Superset.

`SUPERSET_URL` identifies the Superset HTTP(S) endpoint used by the initialization script.

`DASHBOARD_SLUG` identifies the dashboard that must be configured for embedding.

### Datalake database

```text
DATALAKE_DATABASE_PASSWORD
```

Password injected into the Superset dashboard import for the datasource contained in `dashboard_export.zip`.

## Initialization flow

The overall initialization sequence is:

```text
Docker container starts
        │
        ▼
init_superset.sh
        │
        ├── Install Python dependencies
        │
        ├── Create Superset administrator
        │
        ├── superset db upgrade
        │
        ├── superset init
        │
        ├── Configure Guest role
        │
        ├── Create GuestTokenIssuer user
        │
        └── Start Gunicorn
                    │
                    ▼
             Superset HTTPS
                    │
                    ▼
             init_data_superset.sh
                    │
                    ├── Wait for /health
                    ├── Authenticate
                    ├── Retrieve CSRF token
                    ├── Import dashboard_export.zip
                    ├── Find dashboard by slug
                    └── Enable dashboard embedding
```

## HTTPS

Superset is started with Gunicorn using:

```text
/app/certs/superset.crt
/app/certs/superset.key
```

and listens on:

```text
0.0.0.0:8443
```

The certificate and private key are expected to be provided to the container through the Docker configuration.

The Docker health check uses HTTPS:

```text
https://localhost:8443/health
```

The initialization script uses `curl -k`, which allows connections to certificates that are not trusted by the container.

This is particularly useful when using an internal or self-signed certificate.

## Rebuilding the image

After modifying one of the files in this directory, rebuild the Superset image:

```bash
docker compose build superset
```

Then recreate the container:

```bash
docker compose up -d --force-recreate superset
```

If the initialization logic is provided by a separate container, recreate that container as well.

## Re-running dashboard initialization

The dashboard import is performed by `init_data_superset.sh`.

If the initialization container is defined as a Docker Compose service, it can be rerun with:

```bash
docker compose run --rm init-superset
```

or, depending on the Compose configuration:

```bash
docker compose up --force-recreate init-superset
```

The script is designed to authenticate against the running Superset instance before importing the dashboard archive.

## Dashboard archive

`dashboard_export.zip` is the exported Superset dashboard package.

It should contain the Superset assets required to recreate the dashboard, such as:

```text
charts/
dashboards/
databases/
datasets/
metadata.yaml
```

The archive is imported automatically by `init_data_superset.sh`.

Database credentials should not be hard-coded into the exported archive. The import script supplies the datasource password using:

```text
DATALAKE_DATABASE_PASSWORD
```

## Security considerations

### Secrets

Passwords and cryptographic secrets must be provided through environment variables or the project's secret-management mechanism.

Do not commit actual values for:

```text
SUPERSET_SECRET_KEY
SUPERSET_ADMIN_PASSWORD
SUPERSET_BROKER_PASSWORD
DATABASE_PASSWORD
DATALAKE_DATABASE_PASSWORD
```

to source control.

### Guest datasource access

The current `Guest` role includes:

```text
all_datasource_access
```

This is intentionally broad and should be reviewed if Superset is exposed to users who must only access a subset of datasets.

For a production deployment with multiple security domains, prefer explicit datasource permissions and/or Superset Row Level Security (RLS).

### CORS

CORS is restricted to:

```text
APPLICATION_URL
```

rather than allowing arbitrary origins.

### Content Security Policy

The CSP configuration allows the application URL to embed Superset:

```text
frame-ancestors 'self' APPLICATION_URL
```

If the frontend URL changes, `APPLICATION_URL` must be updated accordingly.

## Files summary

| File                    | Purpose                                             |
| ----------------------- | --------------------------------------------------- |
| `Dockerfile`            | Builds the custom Superset image                    |
| `superset_config.py`    | Superset runtime configuration                      |
| `init_superset.sh`      | Initializes Superset and starts Gunicorn            |
| `init_guest_role.py`    | Creates/configures the guest and token issuer roles |
| `init_data_superset.sh` | Imports the dashboard and enables embedding         |
| `dashboard_export.zip`  | Exported Superset dashboard and datasource assets   |
| `README.md`             | Documentation for this setup                        |
