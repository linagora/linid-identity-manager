# LinID Identity Manager

Monorepo tying together the LinID IM API, host UI, E2E tests, Docker environments, and docs.
Two submodules (`linid-im-front-community-plugins`, `linid-ai-core`) live inside the repo;
the E2E test runner (`tests/e2e`) is also a git submodule.

## Package layout

| Directory | Language | Package manager | Key versions |
|---|---|---|---|
| `api/` | Java 25 | Maven (`./mvnw`) | Spring Boot 4.1, PostgreSQL 42.7.13, Flyway |
| `ui/` | TypeScript 6 / Vue 3.5 | pnpm **only** (≥10.32.1) | Quasar 2.21, Pinia 3.0, vue-i18n 11.4, Module Federation 2.7 |
| `tests/features/` | Gherkin | — | Linted with `gherkin-lint` (installed in `tests/e2e/`) |
| `linid-im-front-community-plugins/` | TypeScript 6 | pnpm (Nx 23 monorepo) | Module Federation remotes |
| `linid-ai-core/` | Python / OpenCode | — | AI review tooling; **read-only** from this repo |

## Quick dev commands

```bash
# Start Docker infra (DB + LemonLDAP)
task start:dev

# API (from api/)
./mvnw spring-boot:run               # run locally
./mvnw test                            # unit tests
./mvnw -ntp checkstyle:check           # style check only
./mvnw -ntp org.pitest:pitest-maven:mutationCoverage  # mutation tests
./mvnw spotless:apply                  # auto-apply license header + formatting

# UI (from ui/)
pnpm install                           # first time (postinstall runs quasar prepare)
pnpm dev                               # dev server (https://localhost:9000)
pnpm validate                          # type-check → lint → format:check (must pass in this order)
pnpm test                              # unit tests (vitest, happy-dom)
pnpm test:ci                           # CI-mode tests with coverage

# Run a single UI test file
pnpm vitest run tests/unit/path/to/test.spec.js

# E2E
task setup:e2e && task start:e2e       # start E2E Docker environment
task start:test                        # run all E2E tests (Cypress)
task start:test SPEC=features/api/Accounts.feature  # run a single feature
task lint:test                         # lint Gherkin feature files
```

## Build / CI pipeline order

The CI workflow (`pull-request.yml`) reveals the required order:

1. **UI**: `pnpm build` → `pnpm validate` → `pnpm test:ci`
2. **API**: `mvn checkstyle:check` + `mvn javadoc:javadoc` (parallel) → `mvn test` → `mvn mutationCoverage`
3. **E2E**: `gherkin-lint` → Docker-based test suite (depends on API unit tests passing)

When validating locally, follow the same order: build first, then lint/typecheck, then tests.

## Commit conventions

- **Conventional Commits**: `<type>(<scope>): <summary>` — types: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `security`, `deprecated`, `chore`
- **Branch naming**: `feature/*`, `bugfix/*`, `improvement/*`, `release/*`, `hotfix/*` — lowercase, dashes/underscores/dots only. Only `release/*` and `hotfix/*` can merge to `main`.
- **All commits must be GPG signed** (`git commit -S`).
- CI enforces commitlint against `origin/main..HEAD`.

## Submodules

```bash
git submodule update --init --recursive   # first checkout
```

- `linid-ai-core` has `update = none` in `.gitmodules` — it is pinned to a specific commit.
- `tests/e2e` points to `zorin95670/e2e-test-runner`.

## Key conventions

### API (Java)

- License header (AGPL-3.0) enforced by **Spotless** (`mvn spotless:apply`) and **Checkstyle** (runs on `verify`, fails on violations).
- Checkstyle config: `api/checkstyle/checkstyle.xml` (Sun Style Guide + project customizations).
- Mandatory Javadoc on types, methods, fields (`-Xdoclint:all,-missing` — missing Javadoc is tolerated but everything else must be correct).
- `package-info.java` required in every package.
- Annotation processors: Lombok ↔ MapStruct (via `lombok-mapstruct-binding`), `spring-query-swagger-processor`.
- The `linid-api-java-conventions` skill documents the Record → Mapper → Service → Repository → View chain.
- Flyway migrations in `api/src/main/resources/db/migration/`; the `linid-db-migrations` skill documents naming conventions (3-letter id prefixes, `f_audit_*`, `tg_*`, `idx_*`, `ck_*`, mandatory `COMMENT ON`).

### UI (Vue/TypeScript)

- **pnpm only** — the `packageManager` field in `package.json` enforces this.
- Copyright header enforced by `eslint-plugin-headers` (header content in `ui/COPYRIGHT`).
- ESLint flat config (`eslint.config.js`): `script-setup` required, `consistent-type-imports`, single quotes, curly braces mandatory, `no-explicit-any` is a warning.
- JSDoc required on classes, interfaces, types, enums, function expressions (not arrow functions — see `jsdoc/require-jsdoc` config).
- Prettier: single quotes, trailing commas ES5, `singleAttributePerLine`, `jsdocPrintWidth: 120`.
- `tsconfig.json` extends `.quasar/tsconfig.json` (Quasar-generated base).
- Module Federation: host shares `vue`, `vue-router`, `quasar`, `pinia`, `@linagora/linid-im-front-corelib` as singletons.
- The `linid-front-conventions` skill documents the full SFC layout, naming, `data-cy`, `uiNamespace`/`i18nScope`/`useUiDesign` patterns.

### Configuration-driven UI

- `ui/public/modules/*.json` — module declarations.
- `ui/public/design/*.json` — design namespaces.
- `ui/public/i18n/{en-US,fr-FR}/*.json` — translations (must stay in parity; French has specific typography rules).
- The `linid-host-ui-config` skill documents the consistency requirements.

### Docker / environments

- Three environments: `demo` (all-in-Docker quickstart), `dev` (Docker DB + LemonLDAP, API/UI run locally), `e2e` (all-in-Docker for CI).
- Certificates are **generated** by Taskfile tasks, never committed (`.gitignore` covers `*.crt`, `*.key`, `*.pub`, `*.jks`, `*.p12`).
- `task setup:certs:dev` generates `api/src/main/resources/keystore.p12` and `truststore.jks` for local dev.

### E2E tests

- Gherkin features in `tests/features/api/` and `tests/features/front/`.
- The `linid-e2e-features` skill documents the index header, numbering by operation, `Scenario Outline`, context variables, and data isolation conventions.

## Architecture notes

- **API package root**: `io.github.linagora.linid.im.api` — sub-packages: `config`, `controller`, `i18n`, `model` (DTOs/Records), `persistence` (entities + repositories + views), `service`.
- **Views are read-only SQL projections** mapped to `@Entity` classes with `@Immutable` — `AbstractViewEntity` base class. View repositories extend `JpaViewRepository` (from `linid-im-api-corelib`).
- **`extraParameters`**: JSONB columns on entities, default to `{}` in the database, exposed as `Map<String, Object>` in DTOs.
- **`spring-query-filter`** (v4.3.0): annotation-driven filtering on view repositories (`@QFilter` on controller parameters generates JPA specifications).
- **UI uses `@linagora/linid-im-front-corelib`** (v0.0.96) for shared services, stores, composables, and the design system. The host loads Module Federation remotes from the community-plugins monorepo at runtime.
- **UI boot files** (loaded by Quasar in order): `dayjs → pinia → nunjucks → config → axios → oidc → i18n → module-lifecycle → theme → ui-design`.

## Known pitfalls

- **Never run `npm install` in `ui/`** — only `pnpm install`. The `packageManager` field + `engines.pnpm` in `package.json` enforce this.
- **Build before validate** in CI: the Quasar build step generates `.quasar/tsconfig.json` and type-check needs it.
- **API `mvn verify`** runs Checkstyle, Javadoc, Spotless, and Pitest all together — it's slow. Use targeted goals during development.
- **The `linid-im-front-community-plugins` submodule** is an Nx monorepo with its own `pnpm install` and `pnpm validate`. Its commit scopes (**per-module**) differ from this repo (no scopes = global).
- **`linid-ai-core` is not product code** — its AGENTS.md, skills, and review-rules.md are for AI code review agents only.
- **Do not trust the working directory**: scripts and skills from `linid-ai-core` use absolute paths (`LINID_AI_CORE` env var). When reviewing, the code lives in a worktree, not the main checkout.
