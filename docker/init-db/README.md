# Database Initialization

This directory contains the PostgreSQL initialization scripts used by the Docker environment.

The initialization process creates and configures the databases required by the different applications, then populates them with development and test data.

## Directory structure

```text
docker/init-db/
├── README.md
├── init_db.sql
├── init_data.sh
└── scripts/
    ├── init_linid.sql
    ├── init_lemon.sql
    └── init_datalake.sql
```

## Database architecture

The initialization creates four dedicated PostgreSQL databases:

| Database   | Purpose                       | Application user |
| ---------- | ----------------------------- | ---------------- |
| `linid`    | LinID application data        | `linid_user`     |
| `lemon`    | LemonLDAP::NG data            | `lemon_user`     |
| `superset` | Apache Superset metadata      | `superset_user`  |
| `datalake` | Analytical and dashboard data | `datalake_user`  |

The databases are logically isolated from each other.

In particular, Superset metadata and business/analytical data are stored separately:

```text
                         PostgreSQL
                             │
          ┌──────────────────┼──────────────────┐
          │                  │                  │
       linid               lemon             superset
          │                  │                  │
       LinID          LemonLDAP::NG       Superset metadata
                                             │
                                             │ reads
                                             ▼
                                          datalake
                                             │
                                      Authentication data
```

Superset therefore uses the `superset` database for its own metadata while querying the `datalake` database for dashboard data.

## Initialization flow

The PostgreSQL container first executes `init_db.sql`.

This script creates:

1. The application databases.
2. Dedicated PostgreSQL roles.
3. Database ownership.
4. Required PostgreSQL extensions.
5. Schema and table permissions.

Once the LinID application has created its schema, `init_data.sh` waits for the `accounts` table to become available.

The data initialization then runs:

```text
init_data.sh
    │
    ├── Wait for LinID schema
    │
    ├── init_linid.sql
    │
    ├── init_lemon.sql
    │
    └── init_datalake.sql
```

This ensures that the LinID seed data is inserted only after the application's database migrations have completed.

## `init_db.sql`

This script is responsible for PostgreSQL infrastructure initialization.

### Databases

The following databases are created:

```sql
CREATE DATABASE linid;
CREATE DATABASE lemon;
CREATE DATABASE superset;
CREATE DATABASE datalake;
```

### PostgreSQL roles

A dedicated login role is created for each application:

```text
linid_user
lemon_user
superset_user
datalake_user
```

Each database is owned by its corresponding application role.

This provides logical separation between applications and avoids using the PostgreSQL superuser from application containers.

### PostgreSQL extensions

The `pgcrypto` extension is enabled in the databases that require UUID generation and cryptographic functions.

It provides functions such as:

```sql
gen_random_uuid()
digest(...)
```

which are used by the initialization scripts.

### Permissions

The application users receive access to the `public` schema and CRUD permissions on existing tables.

Default privileges are also configured so that newly created tables and sequences receive the appropriate permissions automatically.

For example, `linid_user` receives:

```text
SELECT
INSERT
UPDATE
DELETE
```

on tables and sequence access required by the application.

The same principle is applied to the other application databases.

## `init_data.sh`

`init_data.sh` orchestrates the population of the databases.

The script first exports:

```text
PGPASSWORD=$POSTGRES_PASSWORD
```

so that `psql` can authenticate against PostgreSQL without interactive password prompts.

### Waiting for the LinID schema

The script waits until the LinID application's `accounts` table exists:

```sql
SELECT to_regclass('public.accounts');
```

This is important because the LinID schema is created by the application itself, typically through its database migration mechanism.

The initialization script therefore does not attempt to create the LinID schema.

It only waits for the schema to become available before inserting development data.

### Initialization order

Once the schema is available, the script executes:

```text
scripts/init_linid.sql
scripts/init_lemon.sql
scripts/init_datalake.sql
```

against their respective databases.

## `scripts/init_linid.sql`

This script populates the LinID database with development and test data.

### Test accounts

The script creates:

```text
admin
user1
user2
user3
user4
user5
user6
user7
user8
user9
user10
```

The accounts are created with generated UUIDs and development email addresses.

The `account_status` table is also populated for the regular test users.

The `admin` account intentionally does not receive an initial status entry. This allows specific application behavior, such as testing a missing account-status resource.

### Lifecycle/dialog test accounts

Additional accounts are created to exercise account lifecycle and UI dialog scenarios:

```text
dialog-d1
dialog-d2
dialog-d3
dialog-d4
dialog-d5
dialog-d6
dialog-d7
dialog-d8
dialog-d9
```

Each account is configured with a specific state designed to trigger a particular lifecycle operation or UI dialog.

Examples include:

```text
Activation
Suspension
Deactivation
Reactivation
Scheduled activation
Scheduled suspension
Scheduled deactivation
Modification of a scheduled deactivation
Modification of a suspension
```

The dates are generally calculated relative to `now()` so that the test cases remain valid when the environment is recreated.

### Organizational Unit hierarchy

The script also creates a development Organizational Unit tree:

```text
root
├── Company A
│   ├── Division A1
│   │   ├── Dept A1-1
│   │   │   └── Team Alpha
│   │   └── Dept A1-2
│   │       └── Team Beta
│   └── Division A2
│       └── Dept A2-1
│           └── Team Gamma
│
└── Company B
    └── Division B1
        └── Dept B1-1
            └── Team Delta
```

The hierarchy is intentionally more complex than a simple tree.

Some teams have multiple parents in order to exercise DAG-related behavior:

```text
Dept A1-2 ──┐
            ├── Team Gamma
Dept A2-1 ──┘

Dept A2-1 ──┐
            ├── Team Delta
Dept B1-1 ──┘
```

### User-to-OU assignments

Test users are associated with different organizational units.

For example:

```text
admin  -> root
user1  -> Company A
user2  -> Company B
user3  -> Division A1
user4  -> Division A2
user5  -> Division B1
```

Additional users are inserted into `Team Beta`, while lifecycle and dialog test accounts are associated with `Team Alpha`.

This provides realistic data for testing organizational-unit-based authorization and filtering.

### Suspended organizational units

Two additional organizational units are created to test suspension scenarios:

```text
SuspendedOuNoEnd
SuspendedOuWithEnd
```

The first has an open-ended suspension:

```text
start = now() - 5 days
end   = infinity
```

The second has a finite suspension:

```text
start = now() - 5 days
end   = now() + 30 days
```

The relative dates ensure that these test cases remain valid regardless of when the database is initialized.

## `scripts/init_lemon.sql`

This script creates a minimal LemonLDAP::NG development account table:

```text
lemonldap_accounts
```

The table contains:

| Column     | Description                       |
| ---------- | --------------------------------- |
| `username` | User identifier                   |
| `password` | Development password              |
| `cn`       | Common name                       |
| `mail`     | Email address                     |
| `roles`    | Comma-separated development roles |

The script creates the following test accounts:

```text
admin
user1
user2
```

All credentials in this script are intended exclusively for development/test environments.

They must not be reused in production.

## `scripts/init_datalake.sql`

This script initializes the analytical database consumed by Apache Superset.

### Authentication events

The main table is:

```text
authentication_events
```

It stores authentication events with fields including:

```text
id
event_date
rls_id
authentication_source
authentication_method
client_id
target_application
ip_address
success
session_id
extra_parameters
insert_date
```

The table is designed to provide data for authentication dashboards.

### Indexes

Indexes are created for the main dashboard query dimensions:

```text
event_date
rls_id
success
```

This improves filtering and aggregation performance for common dashboard queries.

### Demo data

The script generates one year of development authentication data.

The generated data includes:

* Multiple users.
* Multiple authentication methods.
* Multiple applications.
* Successful and failed authentications.
* Randomized IP addresses.
* Browser and operating system information.
* Random event timestamps.
* Session identifiers.

The generated applications include:

```text
linid-ui
grafana
superset
gitlab
```

Authentication methods include:

```text
OIDC
SAML
```

The data is generated dynamically using PostgreSQL functions such as:

```sql
generate_series()
random()
gen_random_uuid()
md5()
jsonb_build_object()
```

This makes it possible to recreate a reasonably realistic dataset without storing a large static SQL dump.

## Row Level Security test data

The `rls_id` column in `authentication_events` is intended to support dashboard-level Row Level Security.

For example:

```text
admin
user1
user2
...
user10
dialog-d1
...
dialog-d9
```

This allows Superset dashboards to associate authentication events with the identity represented by a guest token.

The dashboard configuration can therefore use the `rls_id` value when applying Superset RLS rules.

## Development vs production

These scripts are designed primarily for development, integration testing, demonstrations, and automated environments.

They contain intentionally simplified credentials such as:

```text
password
```

and development email addresses.

**Do not use these credentials in production.**

Production deployments should:

* Generate strong random passwords.
* Retrieve credentials from the project's secret-management system.
* Avoid committing secrets to source control.
* Restrict database permissions to the minimum required by each application.
* Avoid exposing PostgreSQL directly to untrusted networks.
* Review the permissions granted to application roles.
* Review and restrict Superset datasource access.

## Reinitializing the environment

The exact reset procedure depends on the Docker Compose configuration.

For a complete development reset, the PostgreSQL volume can be removed so that PostgreSQL initialization scripts are executed again:

```bash
docker compose down -v
docker compose up -d
```

**Warning:** removing the volume permanently deletes the PostgreSQL data stored in the Docker volume.

After PostgreSQL starts, the initialization sequence recreates the databases and development data.

## Adding new seed data

Application-specific seed data should be added to the corresponding script:

```text
scripts/init_linid.sql
scripts/init_lemon.sql
scripts/init_datalake.sql
```

Database-level configuration such as:

* databases;
* PostgreSQL roles;
* ownership;
* extensions;
* default privileges;

belongs in:

```text
init_db.sql
```

The distinction keeps PostgreSQL infrastructure configuration separate from application test data.

## Responsibilities

| File                | Responsibility                                      |
| ------------------- | --------------------------------------------------- |
| `init_db.sql`       | Create databases, users, permissions and extensions |
| `init_data.sh`      | Orchestrate data initialization                     |
| `init_linid.sql`    | Seed LinID development/test data                    |
| `init_lemon.sql`    | Seed LemonLDAP::NG development/test data            |
| `init_datalake.sql` | Create and populate analytical authentication data  |
| `README.md`         | Document the database initialization process        |
