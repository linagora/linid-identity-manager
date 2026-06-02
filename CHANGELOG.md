# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.5.0] - 2026-06-02

### Added

- You can now create, view, and delete accounts directly through the application.
- The account list has been enhanced with pagination, allowing you to browse results more easily.
- Filtering options have been added to help you quickly find specific accounts.
- Input validation has been improved to reduce errors when managing accounts.
- Add account suspension management feature.
- Add account deactivation management feature.

### Fixed

- Fixed an issue that could prevent initial database migrations from running correctly, which might have blocked setup or updates in some cases.

## [0.4.0] - 2026-04-14

### Added

#### Backend

- Introduced a modular database migration strategy with Flyway, allowing separate migration flows for the core system
  and custom client-specific extensions

### Changed

- Restructured the project into a monorepo architecture
- Set up the API with full Spring Boot ecosystem integration (security, Swagger/OpenAPI, JPA, etc.)
- Migrated the frontend repository into a dedicated ui/ folder with improved plugin management

## [0.3.0] - 2026-03-18

### Added

#### Backend

- Add label support in DynamicListPlugin
- Introduce plugin for dynamic list validation and routing
- Introduce plugin for static list validation
- HttpProviderPlugin now uses JsonParsingTaskPlugin
- Extract JSON parsing into a dedicated plugin

#### Frontend - Core

- Forward attributes in ZoneRenderer
- Add multiple design definitions from Quasar
- Add Nunjucks integration for module federation
- Add unique field validation
- Ensure single plugin instance per zone ("register once" mechanism)
- Add composable for dialog management

#### Frontend - Import Module

- Complete import data module:
  - Load multiple CSV files
  - Interact with rows before import
  - Display imported rows
  - Clear errors and imported rows

#### UI Component Catalog

- Add field for dynamic list management
- Add field for static list management

### Removed

#### Backend

- Remove PATCH support in HttpProviderPlugin

### Fixed

#### Backend

- Fix error handling in dynamic routing
- Remove unnecessary result check in delete response
- Skip validation for null or empty values in DynamicListValidationPlugin
- Skip validation for null or empty values in ListValidationPlugin

#### UI Component Catalog

- Fix active navigation tab prefix handling

### Build

#### Backend

- Migrate to Spring Boot 4
- Replace libraries with lighter alternatives

## [0.2.0] - 2026-02-11

### Added

#### Backend

- Support for **dynamic entity attribute validation**.
- Improved validation capabilities for configurable attributes.
- Improved task execution engine.
- Enhanced provider management logic.

#### Frontend – Core

- Modular frontend architecture based on **Module Federation**.
- Shared Axios HTTP client across modules.
- Shared Vue Router instance.
- Pinia store setup.
- Express-based mock backend with filtering and pagination.
- Docker-based development environment.
- Global design configuration.
- Theme import system.
- CSS override mechanism for custom theming.
- Dynamic route management.
- Internationalization (i18n) setup with dynamic language loading.

#### Frontend – User Module

- Complete **User Management module**:
  - Users list page
  - User details page
  - User creation page
  - User edition page

- User creation and edition forms.
- Date picker field support.
- Advanced search integration.
- Integration into main navigation.

#### UI Component Catalog

- Introduction of reusable UI components:
  - AdvancedSearchCard
  - GenericEntityTable
  - EntityDetailsCard
  - InformationCard
  - ButtonsCard
  - BlurLoader

- Enhanced BaseLayout with header and navigation menu.
- Simple field validation support.
- Improved slot forwarding and configurability.

#### Testing & Quality

- End-to-end testing improvements (`data-cy` attributes).
- CI now fails on unit test failures.
- General stability improvements.

---

### Fixed

#### Backend

- Handle null configuration in TaskEngine.
- Rename provider resolution method for clarity.
- Improved error timestamp handling.
- Return 404 for non-existing i18n files.
- Added missing dependencies.

#### Frontend

- Routing configuration fixes.
- Dependency corrections (Module Federation, Vue, Quasar).
- Fixed translation key issues.
- Fixed loading state handling.
- Removed obsolete configuration fields.
- Improved Docker build reliability.

## [0.1.0] - 2025-12-09

### Added

- API support for managing entities in the system.
- Various back-end plugins for enhanced functionality:
  - **Route Management Plugin** – Handles API routing.
  - **Validation Plugins** – Ensure data integrity, including a regex-based validator.
  - **Provider Plugins** – Connect to different data sources, currently supporting an external API.
  - **Task Plugins** – Enable shared context management for background tasks.
  - **Authorization Plugin** – Manage user permissions and access control.
- Initial front-end setup, including a **component catalog** for modular UI development.
- End-to-end tests implemented to validate the entire platform.
- Comprehensive documentation created, both **global** and **per project**.

[0.1.0]: https://github.com/linagora/linid-identity-manager/releases/tag/v0.1.0
[0.2.0]: https://github.com/linagora/linid-identity-manager/releases/tag/v0.2.0
[0.3.0]: https://github.com/linagora/linid-identity-manager/releases/tag/v0.3.0
[0.4.0]: https://github.com/linagora/linid-identity-manager/releases/tag/v0.4.0
[0.5.0]: https://github.com/linagora/linid-identity-manager/releases/tag/v0.5.0
