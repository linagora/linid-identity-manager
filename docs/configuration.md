# Configuration Guide

This document explains the different configuration layers available in **LINID Identity Management**.
Both the **back-end** and the **front-end** can be adapted to match the functional, business, and design needs of each client.

---

## 1. Back-End Configuration

The back-end is fully modular and highly configurable.
It allows you to define **how the system works internally**, including entities, validation rules, and data sources.

### 🔧 1.1. Entities & Attributes

You can configure:

- **Business entities** (User, Group, Organization, Role, etc.)
- Their **attributes** (types, format, default values…)
- **Relations** between entities

Each entity is defined using a flexible schema, so the data model can be adjusted for each organization.

### 🧪 1.2. Validation Rules

Each attribute can include validation options:

- Basic types (string, number, boolean…)
- Regex validation
- Predefined choices or lists
- Multi-field constraints
- Advanced validation using plugins

These rules ensure consistent and high-quality data.

### 🔌 1.3. Datasources & Plugin System

The back-end uses a **plugin system** to manage data sources. This allows you to:

- Connect **external data sources** (LDAP, SQL, REST API, files…)
- Define how data is **fetched, synchronized, or enriched**
- Control **priority**, **mapping**, and **conflict resolution**

Plugins can also introduce custom business logic specific to each deployment.

For more details on available **back-end plugins**, visit:
**[https://github.com/linagora/linid-im-api-community-plugins](https://github.com/linagora/linid-im-api-community-plugins)**
_This repository contains all community plugins used to extend data sources and back-end logic._

For the full specification of the **back-end configuration file**, see:
**[https://github.com/linagora/linid-im-api-corelib/blob/main/docs/plugins/plugin-configuration.md](https://github.com/linagora/linid-im-api-corelib/blob/main/docs/plugins/plugin-configuration.md)**
_This guide explains how to define entities, attributes, validations, and plugin behavior._

---

## 2. Front-End Configuration

The front-end is also configurable and follows the modular architecture of the platform.

### 🎛️ 2.1. Enabled Modules

You can choose which modules should be available in the UI, based on your needs:

- Dashboard
- User management
- Audit / logs
- Workflows
- Custom modules (via front-end plugins)

Each organization can enable only the modules it requires.

### 🧩 2.2. Module Configuration

Every module can be configured to match your usage:

- What data it uses
- What components it displays
- Permissions & visibility rules
- Business logic
- Layout & interactions

To learn how to configure **front-end modules**, refer to:
**[https://github.com/linagora/linid-im-front?tab=readme-ov-file#-documentation](https://github.com/linagora/linid-im-front?tab=readme-ov-file#-documentation)**
_This documentation covers module declaration, customization options, and front-end plugin integration._

### 🎨 2.3. Design & Theming

The front-end design can be customized:

- Colors and theme
- Logos and branding
- Layout and components
- Client-specific design overrides

---

## 3. Summary

| Layer         | What you can configure                               | Example                                                     |
| ------------- | ---------------------------------------------------- | ----------------------------------------------------------- |
| **Back-end**  | Entities, attributes, validation, datasources, logic | Add an “Employee ID” field with custom validation           |
| **Plugins**   | Datasources and business workflows                   | Synchronize with an external LDAP server                    |
| **Front-end** | Enabled modules, permissions, display, behavior      | Enable the “User Management” module for administrators only |
| **Design**    | Themes, colors, branding                             | Apply the client’s color palette and logo                   |
