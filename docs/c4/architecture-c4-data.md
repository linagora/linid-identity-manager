# LinID Identity Manager — C4 Architecture Data

> Raw architecture information extracted from the repository (Taskfile, docker-compose, pom.xml, package.json, application.yaml, nginx configs, Module Federation configs, LemonLDAP config, OPA/Rego templates, Superset config). To be used later to generate PlantUML C4 diagrams.

---

## 1. System Context (Level 1)

### System

| Field | Value |
|---|---|
| **Name** | LinID Identity Manager |
| **Purpose** | A platform that centralizes management of user identities, organizational structures, application-level permissions, OIDC authentication configuration, and policy-based authorization (PBAC) to automate access rights across an organization's applications. |

### External actors (human roles)

| Actor | Description |
|---|---|
| **Administrator** | Manages user accounts (create, suspend, deactivate, reactivate), organizational units, applications, application roles, and OPA authorization rules. Triggers OPA policy deployment and configures Superset dashboards. |
| **End user** | Authenticates through LemonLDAP::NG, views the application UI, manages their own user preferences, and consumes embedded Superset dashboards. |

### External systems

| System | Description | Protocol |
|---|---|---|
| **LemonLDAP::NG** | Identity provider (IdP) and SSO portal; authenticates users and issues OIDC JWT tokens (ID token + access token) consumed by the API. Also stores its user directory in PostgreSQL. | OIDC (OAuth2 Authorization Code + PKCE), JWT (RS256), HTTPS |
| **Open Policy Agent (OPA)** | Policy decision point; receives Rego policies compiled by the API and evaluates access decisions (allow/deny + roles) per application. | REST (HTTP) |
| **Apache Superset** | Embedded BI dashboard engine; the API authenticates as a service account, generates guest tokens with Row Level Security (RLS) rules, and the frontend embeds dashboards via the Superset Embedded SDK. | REST (HTTPS), Embedded SDK |
| **PostgreSQL** | Relational database hosting four logical databases: `linid` (LinID data), `lemon` (LemonLDAP user directory & sessions), `superset` (Superset metadata), `datalake` (analytical data for dashboards). | JDBC (PostgreSQL wire protocol) |

### External systems (future / target architecture)

| System | Description | Protocol |
|---|---|---|
| **Apache Flink** | Synchronization brick that reads reference identity data from external referentials, computes per account/application profiles (claims + entitlements) by evaluating OPA decisions, stores them in the `account_application_profiles` table, syncs referential data into the LinID database, and syncs claims & entitlements to the LDAP directory. | REST/HTTPS, REST/HTTP, JDBC, LDAP |
| **LDAP Directory** | Stores claims & entitlements per user, written by the Flink sync brick and read by LemonLDAP::NG to build per-application JWT tokens. | LDAP |
| **External Referential** | Master data source (e.g. HR system) providing reference identity data (accounts, organizational units) that the Flink sync brick reads to update the LinID database. | REST/HTTPS |

---

## 2. Containers (Level 2)

Containers are derived from the `../../docker/e2e/docker-compose.yml` (the most complete environment) and confirmed against `../../docker/demo` and `../../docker/dev`.

### 2.1 PostgreSQL (`postgres`)

| Field | Value |
|---|---|
| **Name** | PostgreSQL Database |
| **Technology** | PostgreSQL 18.4 (Alpine image `postgres:18.4-alpine`) |
| **Responsibility** | Central relational data store hosting four isolated databases. |
| **Exposed ports** | `5432:5432` |
| **Inbound connections** | API (JDBC), LemonLDAP::NG (DBI/PostgreSQL), Superset (SQLAlchemy/psycopg2) |
| **Outbound connections** | None |

#### 2.1.1 Database `linid` (`db_linid`)

| Field | Value |
|---|---|
| **Responsibility** | LinID business data + Flyway core migrations (on `public` schema; optional client migrations on a dedicated schema). Also stores the `account_application_profiles` table (future, written by the Flink sync brick). |
| **Inbound connections** | API (JDBC), Flink sync brick (JDBC, future) |
| **Created by** | `../../docker/init-db/init_db.sql` |

#### 2.1.2 Database `lemon` (`db_lemon`)

| Field | Value |
|---|---|
| **Responsibility** | LemonLDAP::NG user directory (table `lemonldap_accounts`) & sessions. |
| **Inbound connections** | LemonLDAP::NG (DBI/PostgreSQL) |
| **Created by** | `../../docker/init-db/init_db.sql` |

#### 2.1.3 Database `superset` (`db_superset`)

| Field | Value |
|---|---|
| **Responsibility** | Superset metadata: dashboards, charts, users, roles, datasets. |
| **Inbound connections** | Apache Superset (SQLAlchemy/psycopg2) |
| **Created by** | `../../docker/init-db/init_db.sql` |

#### 2.1.4 Database `datalake` (`db_datalake`)

| Field | Value |
|---|---|
| **Responsibility** | Analytical data consumed by Superset dashboards. |
| **Inbound connections** | Apache Superset (SQLAlchemy/psycopg2) |
| **Created by** | `../../docker/init-db/init_db.sql` |

### 2.2 LinID API (`api`)

| Field | Value |
|---|---|
| **Name** | LinID Identity Manager API |
| **Technology** | Java 25, Spring Boot 4.1.0, Spring Data JPA, Spring Security (OAuth2 Resource Server), Flyway, MapStruct, Springdoc OpenAPI. Built with Maven. Image: `linid-identity-manager-api`. |
| **Responsibility** | Core backend REST API managing accounts, organizational units, applications, application roles, application rules (OPA Rego fragments), user preferences, i18n, and Superset guest token generation. Compiles Rego policies from rules and deploys them to OPA on a schedule. |
| **Exposed ports** | `8443:8443` (HTTPS, TLS with keystore.jks) |
| **Data stores it owns** | `linid` database (via Flyway core migrations on `public` schema; optional client migrations on a dedicated schema) |
| **Inbound connections** | UI (via nginx reverse proxy `/backend/`, HTTPS) |
| **Outbound connections** | PostgreSQL `linid` DB (JDBC), OPA server (REST/HTTP), LemonLDAP::NG (OIDC JWK Set URI, HTTPS for JWT validation), Apache Superset (REST/HTTPS for guest tokens + dashboard config) |

### 2.3 LinID UI (`ui`)

| Field | Value |
|---|---|
| **Name** | LinID Identity Manager UI (frontend host) |
| **Technology** | Vue 3.5.39, Quasar 2.21.1, Vue Router 5.1.0, Pinia 3.0.4, Module Federation (host, `@module-federation/vite` 1.16.13), oidc-client-ts 3.5.0, Axios. Served by Nginx (stable-alpine). Image: `linid-identity-manager-ui`. |
| **Responsibility** | Module Federation host application; serves the SPA, handles OIDC authentication flow (via oidc-client-ts), loads remote modules at runtime from `remotes.json`, proxies API/backend, auth, and catalog-ui requests through Nginx reverse proxy. |
| **Exposed ports** | `9000:443` (HTTPS, TLS with self-signed cert) |
| **Data stores it owns** | None |
| **Inbound connections** | End user / Administrator (HTTPS browser) |
| **Outbound connections** | API (via Nginx `/backend/` proxy, HTTPS), LemonLDAP::NG (via Nginx `/auth/` proxy, HTTP), catalog-ui (via Nginx `/catalog-ui/` proxy, HTTP) |

### 2.4 Catalog UI (`catalog-ui`)

| Field | Value |
|---|---|
| **Name** | Catalog UI (Module Federation remote) |
| **Technology** | Vue 3.5.39, Quasar 2.21.1, Module Federation (remote `catalogUI`, `@module-federation/vite`), `@superset-ui/embedded-sdk`. Built with Nx + Vite. Served by Nginx (stable-alpine). Image: `catalog-ui`. |
| **Responsibility** | Module Federation remote exposing shared UI components (layouts, tables, trees, forms, fields, cards, dialogs, navigation, Superset widget card) consumed by the host app and other remotes. |
| **Exposed ports** | `80` (internal, no host port mapping) |
| **Data stores it owns** | None |
| **Inbound connections** | UI (via Nginx `/catalog-ui/` proxy, HTTP) |
| **Outbound connections** | None (static assets only) |

### 2.5 LemonLDAP::NG (`auth`)

| Field | Value |
|---|---|
| **Name** | LemonLDAP::NG (authentication / IdP) |
| **Technology** | LemonLDAP::NG 2.23 (image `lemonldapng/lemonldap-ng:2.23`) |
| **Responsibility** | OIDC Identity Provider and SSO portal; authenticates users against its PostgreSQL user directory, issues JWT tokens (RS256 signed with configured OIDC keys), and serves the OIDC discovery/JWKS endpoints. Configured as RP `linid-im-client` with PKCE and bypass consent. |
| **Exposed ports** | `8080:80` (e2e/demo) or `8080:443` (dev) |
| **Data stores it owns** | `lemon` database (user directory table `lemonldap_accounts` & sessions) |
| **Inbound connections** | UI (via Nginx `/auth/` proxy, HTTP), API (OIDC JWK Set URI, HTTPS) |
| **Outbound connections** | PostgreSQL `lemon` DB (DBI/PostgreSQL) |

### 2.6 Open Policy Agent (`opa`)

| Field | Value |
|---|---|
| **Name** | Open Policy Agent |
| **Technology** | OPA 1.18.1 (image `openpolicyagent/opa:1.18.1`) |
| **Responsibility** | Policy decision point; stores Rego policies pushed by the API (one per application, keyed by application code) and evaluates access decisions returning `{allow, roles}`. |
| **Exposed ports** | `8181:8181` |
| **Data stores it owns** | None (in-memory policy store) |
| **Inbound connections** | API (REST/HTTP for policy PUT at `/v1/policies/{id}`) |
| **Outbound connections** | None |

### 2.7 Apache Superset (`superset`)

| Field | Value |
|---|---|
| **Name** | Apache Superset |
| **Technology** | Apache Superset 6.1.0 (image `apache/superset:6.1.0`), Gunicorn (HTTPS), psycopg2, Authlib, flask-cors. Custom config: `superset_config.py`. |
| **Responsibility** | Embedded BI dashboard engine; stores dashboard metadata in the `superset` database, reads analytical data from the `datalake` database, and serves embedded dashboards via guest tokens with RLS. |
| **Exposed ports** | `8088:8443` (HTTPS) |
| **Data stores it owns** | `superset` database (metadata: dashboards, charts, users, roles, datasets) |
| **Inbound connections** | API (REST/HTTPS for login, CSRF, guest token, embedded config) |
| **Outbound connections** | PostgreSQL `superset` DB (SQLAlchemy/psycopg2), PostgreSQL `datalake` DB (SQLAlchemy/psycopg2 for dashboard data) |

### 2.8 Apache Flink sync brick (`flink`) — future

| Field | Value |
|---|---|
| **Name** | Apache Flink sync brick |
| **Technology** | Apache Flink (future) |
| **Responsibility** | Reads reference identity data from external referentials, computes per account/application profiles (claims + entitlements) by evaluating OPA decisions, stores them in the `account_application_profiles` table, syncs referential data into the LinID database, and syncs claims & entitlements to the LDAP directory linked to the user. |
| **Exposed ports** | TBD |
| **Data stores it owns** | None |
| **Inbound connections** | None |
| **Outbound connections** | External Referential (REST/HTTPS), OPA (REST/HTTP), PostgreSQL `linid` DB (JDBC), LDAP Directory (LDAP) |

### 2.9 LDAP Directory (`ldap`) — future

| Field | Value |
|---|---|
| **Name** | LDAP Directory |
| **Technology** | LDAP server (future) |
| **Responsibility** | Stores claims & entitlements per user, written by the Flink sync brick and read by LemonLDAP::NG to build per-application JWT tokens. |
| **Exposed ports** | `389` (STARTTLS) or `636` (LDAPS) — to be confirmed |
| **Data stores it owns** | None (LDAP entries) |
| **Inbound connections** | Flink sync brick (LDAP, write), LemonLDAP::NG (LDAP, read) |
| **Outbound connections** | None |

### 2.10 External Referential (`referentiel`) — future

| Field | Value |
|---|---|
| **Name** | External Referential |
| **Technology** | External system (e.g. HR system) |
| **Responsibility** | Master data source providing reference identity data (accounts, organizational units) that the Flink sync brick reads to update the LinID database. |
| **Exposed ports** | N/A (external) |
| **Data stores it owns** | N/A |
| **Inbound connections** | Flink sync brick (REST/HTTPS, read) |
| **Outbound connections** | None |

---

## 3. Components (Level 3)

### 3.1 Backend API (Spring Boot) — `io.github.linagora.linid.im.api`

#### 3.1.1 Configuration (`config` package)

| Component | Type | Responsibility | Key dependencies | External dependency |
|---|---|---|---|---|
| `SecurityConfig` | Security Config | Configures two Spring Security filter chains: one for public endpoints (health, actuator, i18n, swagger) and one for secured endpoints (JWT via OAuth2 Resource Server + `UserAuthenticationFilter`). Configures JWT `typ` header validator (`at+jwt` for LemonLDAP::NG) and OpenAPI bearer security scheme. | `AccountService`, `UserAuthenticationFilter` | Spring Security OAuth2 Resource Server (JWT) |
| `OpaConfig` | Config | Creates the `RestClient` bean targeting the OPA server URL. | None | OPA server (REST) |
| `SchedulingConfig` | Config | Enables Spring's `@EnableScheduling` for the OPA deployment scheduler. | None | None |
| `FlywayConfig` | Config | Defines two Flyway beans: `flywayCore` (core migrations on `public` schema) and `flywayClient` (client-specific migrations on a dedicated schema, conditional on config). | None | PostgreSQL (JDBC/Flyway) |
| `SslConfig` | Config | Sets JVM system properties for the SSL truststore used for HTTPS calls to external services (LemonLDAP, Superset). | None | None |

#### 3.1.2 Controllers (`controller` package)

| Component | Type | Responsibility | Key dependencies | External dependency |
|---|---|---|---|---|
| `AccountController` | Controller (REST `/accounts`) | CRUD for user accounts; status transitions (suspend, deactivate, reactivate, activate, schedule-activation). | `AccountService`, `AccountMapper`, `OrganizationalUnitService`, `PagedResponseStatusResolver` | None |
| `ApplicationController` | Controller (REST `/applications`) | CRUD for applications; triggers OPA policy deployment (`POST /{id}/deploy`); exports Rego script (`GET /{id}/script`). | `ApplicationService`, `ApplicationMapper`, `OpaApplicationDeployerService` | OPA (indirectly) |
| `ApplicationRoleController` | Controller (REST `/applications/{id}/roles`) | CRUD for application roles. | `ApplicationRoleService`, `ApplicationRoleMapper` | None |
| `ApplicationRuleController` | Controller (REST `/applications/{id}/rules`) | CRUD for application rules (Rego fragments); triggers policy regeneration on rule changes. | `ApplicationRuleService`, `ApplicationService`, `ApplicationRuleMapper` | OPA (indirectly via policy regeneration) |
| `GroupController` | Controller (REST `/groups`) | CRUD for groups (hierarchical, with parent/OU/application references). | `GroupService`, `GroupMapper` | None |
| `OrganizationalUnitController` | Controller (REST `/organizational-units`) | CRUD for organizational units; suspend/reactivate; account attach/detach/update-relation; list accounts of an OU. | `OrganizationalUnitService`, `OrganizationalUnitMapper`, `OrganizationalUnitAccountMapper` | None |
| `SupersetController` | Controller (REST `/superset`) | Generates Superset guest tokens for embedded dashboards; resolves dashboard ID from slug. | `SupersetService` | Apache Superset (REST, via service) |
| `UserPreferenceController` | Controller (REST `/user-preferences`) | Key/value preference store scoped to the authenticated user (upsert, delete, list all). | `UserPreferenceService`, `UserPreferenceMapper` | None |
| `I18nController` | Controller (REST `/i18n`) | Returns available languages and translation files (public, no auth). | `I18nService` (from corelib) | None |
| `PagedResponseStatusResolver` | Helper | Resolves HTTP 200 vs 206 status for paginated responses. | None | None |

#### 3.1.3 Filters & Exception Handling (`controller/filter`, `controller/handler`)

| Component | Type | Responsibility | Key dependencies | External dependency |
|---|---|---|---|---|
| `UserAuthenticationFilter` | Security Filter | Runs after the JWT Bearer filter; extracts the email claim from the JWT, resolves the corresponding LinID account, and sets a `UserPrincipal` in the security context. Rejects with 401 when a token is presented but no account matches. | `AccountService` | LemonLDAP::NG (JWT, indirectly) |
| `CopyrightFilter` | Servlet Filter | Adds a configurable `X-Copyright` header to all HTTP responses (modes: `default`, `custom`, `none`). | None | None |
| `GlobalExceptionHandler` | Controller Advice (`@ControllerAdvice`) | Intercepts `ApiException` and validation exceptions (`MethodArgumentNotValidException`, `HandlerMethodValidationException`), translates messages via i18n, and returns standardized error response bodies. | `I18nService` | None |

#### 3.1.4 Services (`service` package)

| Component | Type | Responsibility | Key dependencies | External dependency |
|---|---|---|---|---|
| `AccountServiceImpl` | Service | CRUD and lifecycle management (suspend, deactivate, reactivate, activate, schedule-activation) for accounts; email/externalId lookup; paginated search. | `AccountRepository`, `AccountStatusRepository`, `AccountDistinctViewRepository`, `AccountOrganizationalUnitViewRepository`, validators | None |
| `ApplicationServiceImpl` | Service | CRUD for applications; `regeneratePolicy()` compiles active rules into a Rego script via `OpaService` and persists it with a checksum. | `ApplicationRepository`, `ApplicationViewRepository`, `ApplicationRuleRepository`, `OpaService`, `ChecksumService`, `SystemApplicationValidator` | OPA (indirectly) |
| `ApplicationRoleServiceImpl` | Service | CRUD for application roles; system-reserved application role immutability. | `ApplicationRoleRepository`, `ApplicationRoleViewRepository` | None |
| `ApplicationRuleServiceImpl` | Service | CRUD for application rules (Rego fragments); checksum computation; unique-code enforcement. | `ApplicationRuleRepository`, `ApplicationRuleViewRepository` | None |
| `GroupServiceImpl` | Service | CRUD for groups; parent-group cycle detection; unique-code enforcement. | `GroupRepository`, `GroupViewRepository`, `GroupAncestorRepository` | None |
| `OrganizationalUnitServiceImpl` | Service | CRUD and lifecycle (suspend, reactivate) for organizational units; account attach/detach/update-relation; paginated search. | `OrganizationalUnitRepository`, `OrganizationalUnitViewRepository`, `OrganizationalUnitDistinctViewRepository`, `OrganizationalUnitAccountRepository`, `OrganizationalUnitAccountViewRepository`, validators | None |
| `UserPreferenceServiceImpl` | Service | Key/value preference store scoped to the authenticated user. | `UserPreferenceRepository` | None |
| `OpaServiceImpl` | Service / Adapter | Generates a Rego policy from active rules using a Jinja template (`opa-policy.rego.j2`) and publishes it to the OPA server via REST (`PUT /v1/policies/{id}`). | `JinjaService`, `RestClient` (OPA) | OPA server (REST) |
| `OpaApplicationDeployerServiceImpl` | Service | Deploys a single application policy to OPA within its own transaction (`REQUIRES_NEW`); updates `deployedAt`. | `ApplicationRepository`, `OpaService`, `ApplicationService` | OPA server (REST, via `OpaService`) |
| `OpaDeploymentScheduler` | Scheduler | Periodically (default every 5 min) deploys all applications with a generated script but no `deployedAt` to OPA. | `ApplicationRepository`, `OpaApplicationDeployerService` | OPA server (REST, via deployer) |
| `SupersetServiceImpl` | Service / Adapter | Authenticates to Superset (login + CSRF + session), generates guest tokens with RLS rules built from LinID account/OU data, and resolves dashboard IDs from slugs. | `AccountDistinctViewRepository`, `OrganizationalUnitDistinctViewRepository`, `RestClient` (Superset), `SupersetCacheService` | Apache Superset (REST/HTTPS) |
| `SupersetCacheServiceImpl` | Service | Caches the Superset access token (Caffeine, `@Cacheable`); evicts on 401/403. Isolated to avoid AOP self-invocation. | None | None |
| `JinjaServiceImpl` | Service | Renders Jinja/Jinjava templates (used for OPA policy generation). | None | None |
| `ChecksumServiceImpl` | Service | Computes SHA-256 checksums of generated OPA scripts to detect changes. | None | None |

#### 3.1.5 Validation (`service/validation` package)

| Component | Type | Responsibility | Key dependencies |
|---|---|---|---|
| `AccountCreationValidator` | Validator | Validates account creation business rules (validity start present and not in past). | `AccountRepository` |
| `AccountActivationValidator` | Validator | Validates account activation rules (not already activated, validity exists & not in future, activationAt after validity start). | `AccountStatusRepository` |
| `AccountDeactivationValidator` | Validator | Validates account deactivation rules (deactivation timestamp not in past, not before validity start). | `AccountStatusRepository` |
| `AccountReactivationValidator` | Validator | Validates account reactivation rules (active suspension or expired validity exists; new validity end not in past). | `AccountStatusRepository` |
| `AccountSuspensionValidator` | Validator | Validates account suspension rules (period coherent, start/end not in past, within validity period). | `AccountStatusRepository` |
| `AccountValidityValidator` | Validator | Validates account validity period rules (not already activated, validity start strictly in future, coherent with existing end). | `AccountStatusRepository` |
| `OrganizationalUnitSuspensionValidator` | Validator | Validates OU suspension rules (period coherent, start/end not in past). | `OrganizationalUnitStatusRepository` |
| `OrganizationalUnitReactivationValidator` | Validator | Validates OU reactivation rules (active suspension to lift). | `OrganizationalUnitStatusRepository` |
| `PeriodValidator` | Validator | Reusable temporal-period checks: `ensureCoherent` (start ≤ end), `ensureStartNotInPast`, `ensureEndNotInPast`. | None |
| `SystemApplicationValidator` | Validator | Protects the system-reserved application (`LINID` code) from mutation and its roles from CRUD. | None |

#### 3.1.6 Persistence (`persistence` package)

| Component | Type | Responsibility | Key dependencies |
|---|---|---|---|
| `AccountRepository` | Repository (JPA) | CRUD for `Account` entities + custom queries (e.g., `findByEmail`). | PostgreSQL |
| `AccountStatusRepository` | Repository (JPA) | CRUD for `AccountStatus` entities. | PostgreSQL |
| `AccountDistinctViewRepository` | Repository (JPA) | Read-only queries on `AccountDistinctView` (database view). | PostgreSQL |
| `AccountOrganizationalUnitViewRepository` | Repository (JPA) | Read-only queries on `AccountOrganizationalUnitView` (database view). | PostgreSQL |
| `ApplicationRepository` | Repository (JPA) | CRUD for `Application` entities + `findByCode`, `findByDeployedAtIsNullAndScriptIsNotNull`, `updatePolicy`. | PostgreSQL |
| `ApplicationViewRepository` | Repository (JPA) | Read-only queries on `ApplicationView`. | PostgreSQL |
| `ApplicationRoleRepository` | Repository (JPA) | CRUD for `ApplicationRole` entities. | PostgreSQL |
| `ApplicationRoleViewRepository` | Repository (JPA) | Read-only queries on `ApplicationRoleView`. | PostgreSQL |
| `ApplicationRuleRepository` | Repository (JPA) | CRUD for `ApplicationRule` entities + `findByApplicationIdAndDisabledFalseOrderByPriorityAsc`. | PostgreSQL |
| `ApplicationRuleViewRepository` | Repository (JPA) | Read-only queries on `ApplicationRuleView`. | PostgreSQL |
| `GroupRepository` | Repository (JPA) | CRUD for `Group` entities. | PostgreSQL |
| `GroupViewRepository` | Repository (JPA) | Read-only queries on `GroupView`. | PostgreSQL |
| `GroupAncestorRepository` | Repository (JPA) | Read-only queries on `GroupAncestor` (closure table view, `String` ID). | PostgreSQL |
| `OrganizationalUnitRepository` | Repository (JPA) | CRUD for `OrganizationalUnit` entities. | PostgreSQL |
| `OrganizationalUnitViewRepository` | Repository (JPA) | Read-only queries on `OrganizationalUnitView`. | PostgreSQL |
| `OrganizationalUnitDistinctViewRepository` | Repository (JPA) | Read-only queries on `OrganizationalUnitDistinctView`. | PostgreSQL |
| `OrganizationalUnitRelationRepository` | Repository (JPA) | CRUD for `OrganizationalUnitRelation` (OU hierarchy). | PostgreSQL |
| `OrganizationalUnitStatusRepository` | Repository (JPA) | CRUD for `OrganizationalUnitStatus` entities. | PostgreSQL |
| `OrganizationalUnitAccountRepository` | Repository (JPA) | CRUD for `OrganizationalUnitAccount` (OU membership). | PostgreSQL |
| `OrganizationalUnitAccountViewRepository` | Repository (JPA) | Read-only queries on `OrganizationalUnitAccountView`. | PostgreSQL |
| `UserPreferenceRepository` | Repository (JPA) | CRUD for `UserPreference` entities. | PostgreSQL |

#### 3.1.7 i18n (`i18n` package)

| Component | Type | Responsibility | Key dependencies |
|---|---|---|---|
| `I18nServiceImpl` | Service (implements `CommandLineRunner`) | Merges translations from multiple loaders (plugin, external, internal) in configured order at startup; provides translation lookup with placeholder substitution. | `I18nSourceLoader` list |
| `ClasspathI18nLoader` | Loader | Loads translations from classpath resources (`classpath*:i18n/*.json`). | None |
| `ExternalPathI18nLoader` | Loader | Loads translations from an external filesystem path (`i18n.external.path`). | None |
| `PluginI18nLoader` | Loader | Loads translations from plugin JARs in `plugin.loader.path`. | None |
| `I18nMergeCollector` | Collector | Custom collector that deep-merges translation maps by language. | None |

#### 3.1.8 Models / Mappers (`model` package)

| Component | Type | Responsibility |
|---|---|---|
| `*DTO` / `*Record` / `*ViewDTO` | DTO / Record | Request/response data transfer objects for accounts, applications, roles, rules, groups, OUs, user preferences, Superset tokens, periods. |
| `*Mapper` (MapStruct) | Mapper | Entity-to-DTO conversion (AccountMapper, AccountOrganizationalUnitMapper, AccountStatusMapper, ApplicationMapper, ApplicationRoleMapper, ApplicationRuleMapper, GroupMapper, OrganizationalUnitMapper, OrganizationalUnitAccountMapper, OrganizationalUnitRelationMapper, OrganizationalUnitStatusMapper, CommonMapper, UserPreferenceMapper). |
| `UserPrincipal` | Principal | Custom security principal carrying the LinID account ID and email. |

### 3.2 Frontend Host App (`ui`)

The host app is a Quasar/Vue 3 SPA configured as a Module Federation **host** (`name: 'linid-identity-manager-ui'`). It has no static remotes in `quasar.config.ts`; instead, remotes are loaded at runtime from `/config.json` (consolidated configuration). Module Federation setup is delegated to `@linagora/linid-im-front-corelib`'s `linidModuleFederation.init`.

#### Boot files (initialization sequence, 10 files)

| Boot file | Responsibility |
|---|---|
| `dayjs.ts` | Extends Day.js with `utc` and `customParseFormat` plugins; registers singleton in corelib and as Vue global property. |
| `pinia.ts` | Creates Pinia instance, registers in corelib; pre-registers `catalogUI/ConfirmationDialog` and `catalogUI/FormDialog` as base-layout dialog plugins in the zone store. |
| `nunjucks.ts` | Configures Nunjucks templating environment (autoescape off); registers in corelib. |
| `config.ts` | Fetches runtime configuration from `/config.json` (environment, i18n, remotes, modules, oidc, theme, design files, lifecycle fields). |
| `axios.ts` | Creates Axios instance (baseURL `/backend`, 30s timeout); adds Bearer token request interceptor and 401 redirect response interceptor; registers in corelib. |
| `oidc.ts` | Initializes `oidc-client-ts` `UserManager` via `AuthService.init()`, retrieves the user, and populates the corelib user store + preferences. |
| `i18n.ts` | Loads translations from the API (`/i18n/{lang}.json`) and static files (`/i18n/{lang}/{file}.json`), merges them, creates Vue I18n, and loads the matching Quasar language pack. |
| `module-lifecycle.ts` | Delegates Module Federation setup to corelib `linidModuleFederation.init`: registers remotes, module config files, extra zones, and host-local components; executes all lifecycle phases (SETUP, CONFIGURE, INITIALIZE, READY, POST_INIT). |
| `theme.ts` | Applies theme variables from `config.json` as `--q-{key}` CSS custom properties on `document.documentElement`. |
| `ui-design.ts` | Fetches design JSON files listed in `config.json`, merges them, and registers the UI design configuration in corelib. |

#### Pinia stores (from `@linagora/linid-im-front-corelib`)

| Store | Responsibility |
|---|---|
| `useLinidUserStore` | Holds the authenticated user's claims (profile). |
| `useLinidUserPreference` | Persists user preferences (e.g. selected language). |
| `useLinidUiStore` | Global UI state: available languages, current locale, main navigation menu items. |
| `useLinidZoneStore` | Zone-based plugin registry: maps zone keys to federated components/plugins. |

#### Frontend services (`../../ui/src/services`)

| Service | Responsibility |
|---|---|
| `AuthService.ts` | Wraps `oidc-client-ts` `UserManager`: init, login, logout, getUser, getAccessToken, handleCallback, handleLogoutCallback, handleSilentRenewCallback, clearUser. Handles token expiry, silent renew errors, and remote sign-out events. |
| `AccountService.ts` | REST client for account lifecycle endpoints: `suspendAccount`, `deactivateAccount`, `reactivateAccount`, `setAccountValidity` (PUT `/accounts/{id}/status/*`). |
| `ApplicationService.ts` | REST client for application script export: `exportApplicationScript` (GET `/applications/{id}/script` as Blob). |
| `OrganizationalUnitService.ts` | REST client for OU endpoints: `getOrganizationalUnitById`, `suspendOrganizationalUnit`, `reactivateOrganizationalUnit` (GET/PUT `/organizational-units/{id}/*`). |

#### Composables (`../../ui/src/composables`)

| Composable | Responsibility |
|---|---|
| `useAccountMapper` | Converts `AccountDTO` to `AccountStatus` and form values to API records; flattens status into form values for dialog pre-fill. |
| `useAccountLifecycleUi` | Projects `AccountStatus` ref into deterministic lifecycle UI state (badges, banners, info text, dropdown items) across 13 lifecycle cases. |
| `useLifecycleDialogs` | Centralizes lifecycle dialog plumbing: `openFormDialog` and `openConfirmationDialog` emit UI events for `catalogUI/FormDialog` and `catalogUI/ConfirmationDialog`. |
| `useOrganizationalUnitMapper` | Converts `OrganizationalUnitDTO` to `OrganizationalUnitStatus` and form values to API records. |
| `useOrganizationalUnitLifecycleUi` | Projects `OrganizationalUnitStatus` ref into deterministic lifecycle UI state across 3 cases. |

#### Local components (host-registered, non-federated)

| Component | Type | Responsibility |
|---|---|---|
| `Homepage` | Page | Renders intro/open-source/license/branding HTML from i18n. |
| `AuthenticationCallbackPage` | Page | Processes OIDC login callback; populates user store; resolves redirect URL. |
| `AuthenticationLogoutPage` | Page | Triggers RP-initiated logout (`signoutRedirect`). |
| `AuthenticationLoggedOutPage` | Page | Processes post-logout callback; re-initiates login. |
| `AuthenticationSilentRenewPage` | Page | Silent token renewal callback in hidden iframe. |
| `AccountLifecyclePanel` | Panel | Orchestrates account lifecycle UI: banners, info text, dialogs; calls `AccountService`; emits `account-status-updated`. |
| `OrganizationalUnitLifecyclePanel` | Panel | Orchestrates OU lifecycle UI: suspended banner, info text, dialogs; calls `OrganizationalUnitService`; emits `organizational-unit-status-updated`. |
| `ExportApplicationScriptBtn` | Button | Downloads application OPA Rego script as `.rego` file. |
| `OrganizationalUnitCreateChildBtn` | Button | Navigates to `/organizational-units/new?parent={id}` to create child OU. |
| `OrganizationalUnitParentField` | Field | Resolves parent OU from route query; displays name; emits parent id. |
| `AccountSuspendedBanner` | Banner | Yellow warning banner for suspended accounts. |
| `AccountDeactivatedBanner` | Banner | Red banner for deactivated accounts. |
| `AccountDeactivatedWarningBanner` | Banner | Light-red warning banner for accounts within 15 days of deactivation. |
| `OrganizationalUnitSuspendedBanner` | Banner | Yellow warning banner for suspended OUs. |
| `AccountNotActivatedInfoText` | Info Text | Info text for accounts that have never been activated. |
| `AccountSuspendedInfoText` | Info Text | Info text showing upcoming suspension start date. |
| `AccountDeactivatedInfoText` | Info Text | Info text showing upcoming deactivation date. |
| `OrganizationalUnitSuspendedInfoText` | Info Text | Info text showing upcoming OU suspension start date. |

#### Module config files (dynamic routes, `../../ui/public/modules/`)

9 JSON files that drive dynamic route registration via `linidModuleFederation.init`:

| Config file | Base path | Federated page | Entity |
|---|---|---|---|
| `AccountsPage.json` | `/accounts` | `catalogUI/GenericTablePage` | accounts |
| `AccountCreationPage.json` | `/accounts/new` | `catalogUI/GenericCreationPage` | accounts |
| `AccountDetailsPage.json` | `/accounts/:id` | `catalogUI/GenericDetailsPage` | accounts |
| `ApplicationsPage.json` | `/applications` | `catalogUI/GenericTablePage` | applications |
| `ApplicationCreationPage.json` | `/applications/new` | `catalogUI/GenericCreationPage` | applications |
| `ApplicationDetailsPage.json` | `/applications/:id` | `catalogUI/GenericDetailsPage` | applications |
| `OrganizationalUnitPage.json` | `/organizational-units` | `catalogUI/GenericTablePage` | organizational-units |
| `OrganizationalUnitCreationPage.json` | `/organizational-units/new` | `catalogUI/GenericCreationPage` | organizational-units |
| `OrganizationalUnitDetailsPage.json` | `/organizational-units/:id` | `catalogUI/GenericDetailsPage` | organizational-units |

#### Module Federation remotes loaded at runtime

| Remote name | Entry | Responsibility |
|---|---|---|
| `catalogUI` | `mf-manifest.json` (URL from `config.json`) | Shared UI component library: `BaseLayout`, `GenericTablePage`, `GenericDetailsPage`, `GenericCreationPage`, `GenericEditionPage`, `EntityProfilePanel`, `GenericEditableTableCard`, `GenericSortableListCard`, `SupersetWidgetCard`, `FormDialog`, `ConfirmationDialog`, `NavigationMenu`, `HeaderProfile`, `StatusBadge`, `LinidSmartFilter`, ~40 federated modules. |

#### Available community plugin remotes (from `../../linid-im-front-community-plugins/apps`)

These are built as Module Federation remotes but are **not registered** in `config.json`:

| Remote name | App | Responsibility |
|---|---|---|
| `moduleUsers` | `module-users` | User management pages: `HomePage`, `NewUserPage`, `EditUserPage`, `UserDetailsPage` + routes, i18n, lifecycle. |
| `moduleImport` | `module-import` | Data import feature: `ImportPage`, `ImportButton` + routes, i18n, lifecycle. Uses papaparse for CSV parsing. |

---

## 4. Relationships summary

```
Administrator -> LinID UI : Manages identities, applications, rules, dashboards [HTTPS]
End user -> LinID UI : Authenticates, views UI, manages preferences, views dashboards [HTTPS]
Administrator -> LinID API : Manages accounts, OUs, applications, roles, rules, deploys policies [HTTPS/REST]
End user -> LinID API : Reads/writes user preferences, requests Superset guest tokens [HTTPS/REST]
LinID UI -> LinID API : Proxies API calls via /backend/ [HTTPS/REST]
LinID UI -> LemonLDAP::NG : OIDC login flow (authorization code + PKCE), token renewal [OIDC/HTTPS]
LinID UI -> Catalog UI : Loads Module Federation remote components via /catalog-ui/ [HTTP]
LinID API -> PostgreSQL : Reads/writes LinID business data (linid DB) [JDBC]
LinID API -> LemonLDAP::NG : Fetches OIDC JWKS for JWT validation [OIDC/HTTPS]
LinID API -> OPA : Publishes Rego policies (PUT /v1/policies/{id}) [REST/HTTP]
LinID API -> Apache Superset : Login, CSRF, guest token generation, dashboard config [REST/HTTPS]
LemonLDAP::NG -> PostgreSQL : Reads user directory (lemon DB) [DBI/PostgreSQL]
Apache Superset -> PostgreSQL : Reads/writes metadata (superset DB) [SQLAlchemy/psycopg2]
Apache Superset -> PostgreSQL : Reads analytical data (datalake DB) [SQLAlchemy/psycopg2]
LinID API -> OPA : Scheduled deployment of pending policies (every 5 min) [REST/HTTP]
LinID UI -> LinID API : Fetches i18n translations [HTTPS/REST]

' --- Future / target architecture ---
Apache Flink -> External Referential : Reads source reference data (accounts, organizational units) [REST/HTTPS]
Apache Flink -> OPA : Evaluates decisions to compute entitlements [REST/HTTP]
Apache Flink -> PostgreSQL : Syncs referential data & writes computed profiles (account_application_profiles, linid DB) [JDBC]
Apache Flink -> LDAP Directory : Syncs claims & entitlements per user [LDAP]
LemonLDAP::NG -> LDAP Directory : Reads claims & entitlements to build per-application JWT [LDAP]
```

---

## 5. Open questions / assumptions (clarified)

1. **OPA decision consumption** — *Clarified (target architecture)*: The API only publishes Rego policies to OPA; it does not evaluate decisions at runtime. A future **Apache Flink synchronization brick** will compute per account/application profiles by evaluating OPA decisions, then store the result (claims + entitlements) in the `account_application_profiles` table (see schema below). After computation, the sync brick will save claims & entitlements into the **LDAP directory** linked to the user, so LemonLDAP::NG can reuse them. LemonLDAP will expose two pieces of information coming from the sync brick: **claims** and **entitlements**. A **LemonLDAP script** will convert these into a JWT token returned to the user based on the requested application. Additionally, the Flink sync brick reads reference identity data (accounts, organizational units) from one or more **external referential systems** (e.g. an HR system) and synchronizes it into the LinID database. This flow is not yet implemented in the repository.

   ```sql
   CREATE TABLE IF NOT EXISTS account_application_profiles (
     aap_id              UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
     act_id              UUID REFERENCES accounts (act_id) ON DELETE CASCADE,
     app_id              UUID REFERENCES applications (app_id) ON DELETE CASCADE,
     claims              JSONB        NOT NULL,
     entitlements        JSONB        NOT NULL,
     account_checksum    VARCHAR(64)  NOT NULL,
     opa_script_checksum VARCHAR(64)  NOT NULL,
     changed_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
     created_by          VARCHAR(128) NOT NULL,
     updated_by          VARCHAR(128) NOT NULL,
     insert_date         TIMESTAMPTZ  NOT NULL DEFAULT now(),
     update_date         TIMESTAMPTZ  NOT NULL DEFAULT now()
   );
   ```
   - `claims`: JSONB payload containing the JWT claims evaluated for this account in the context of this application, based on the application JWT template.
   - `entitlements`: JSONB payload containing the rights and permissions computed by the OPA script for this account and application.
   - `account_checksum` / `opa_script_checksum`: detect whether the profile needs recomputation due to account or policy changes.
   - `changed_at`: updated only when the computation result differs from the previous one.

2. **Module Federation remotes in production** — *Clarified*: Only `catalogUI` is used today. `moduleUsers` will likely disappear, and `moduleImport` will be incorporated into `catalog-ui`. The architecture intentionally leaves the door open for other remotes (external plugins) to be registered via `remotes.json`.

3. **`module-oidc` app** — *Clarified*: The `../../linid-im-front-community-plugins/apps/module-oidc` directory is to be deleted and must not be taken into account.

4. **Client Flyway migrations**: The `flywayClient` bean is conditional on `spring.flyway.client.location` being set. In the e2e environment, this points to `filesystem:docker/dev/resources/db-client`. It contains client-specific schema extensions loaded after the core migrations on a dedicated schema.

5. **LemonLDAP::NG as OPA consumer** — *Clarified*: Still in development. In the target architecture, LemonLDAP will read claims & entitlements from the LDAP directory (populated by the Flink sync brick) and a LemonLDAP script will convert them into per-application JWT tokens. The current `lmConf-1.template.json` maps `roles` as an exported variable/OIDC claim but the full flow is not yet wired.

6. **Superset RLS configuration** — *Clarified*: The `superset.yaml` config file (e2e) defines a single dashboard (`USER_LOG_DASHBOARD`) with RLS on the `ACCOUNT` entity's `externalId` attribute as a demo example. In the future, company administrators will be able to add additional dashboards.

7. **External configuration file** — *Clarified*: The optional external configuration file (`EXTERNAL_CONFIGURATION=/app/extra-configuration.yaml`) is a convenience for administrators to override or extend the default application properties (which come from `.env` files) with more specific settings without modifying the main configuration.

8. **`corelib` dependency**: The API depends on `linid-im-api-corelib` 0.13.0 (a published library) and the UI depends on `@linagora/linid-im-front-corelib`. These libraries provide shared interfaces (`I18nService`, `ApiException`, `I18nMessage`, stores, etc.) but their source is not in this repository. Component listings for the backend may be incomplete if corelib defines additional components.