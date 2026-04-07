# Configuration Overview

## 🎯 Purpose

This section provides an overview of how to configure **LinId Identity Manager**.

LinId is designed to be highly flexible and modular. Its configuration is organized into several domains, each
addressing a specific aspect of the platform.

---

## 🧩 Configuration Domains

LinId configuration is divided into four main areas:

1. **Authentication**
2. **Application Configuration**
3. **Plugins**
4. **User Interface (UI)**

Each domain can be configured independently.

---

## 🔐 Authentication Configuration

This domain covers everything related to user authentication and identity management.

### Includes:

* OIDC configuration
* Identity provider integration (e.g., LemonLDAP)
* Certificate management
* Claims definition and mapping

### Why it matters:

Authentication is the foundation of secure access to your applications.

👉 See:

* [authentication/oidc](authentication/oidc.md)
* [certificates](certificates.md)
* [authentication/claims](authentication/claims.md)

---

## 📄 Application Configuration

This domain defines how LinId behaves at runtime.

### Includes:

* YAML configuration files
* Environment variables
* Global application settings

### Why it matters:

It allows you to control system behavior without modifying code.

👉 See:

* [application/config-files](application/config-files.md)

---

## 🔌 Plugin Configuration

LinId supports a plugin-based architecture to extend functionality.

### Includes:

* Plugin installation
* Plugin lifecycle
* Plugin-specific configuration

### Why it matters:

Plugins allow you to adapt LinId to your specific needs.

> 🚧 TODO: Plugin system documentation is not fully available yet

👉 See:

* [plugins/plugins-overview](plugins/plugins-overview.md)

---

## 🎨 User Interface Configuration

This domain allows customization of the application’s look and feel.

### Includes:

* Theme configuration
* Branding (logos, colors)
* Translations (i18n)

### Why it matters:

It enables you to adapt LinId to your organization’s identity.

👉 See:

* [ui/theming](ui/theming.md)
* [ui/translations](ui/translations.md)

---

## 🧠 Configuration Strategy

To configure LinId effectively:

1. Start with **authentication**
2. Define your **applications and roles**
3. Configure **policies (OPA)**
4. Customize the **UI**
5. Extend with **plugins** if needed

---

## ⚙️ Configuration Methods

LinId supports multiple configuration methods:

* Static configuration (YAML files)
* Environment variables
* Dynamic configuration (via UI or APIs - depending on features)

> 🚧 TODO: Clarify dynamic configuration capabilities

---

## ⚠️ Best Practices

* Keep configurations modular and isolated
* Use environment variables for sensitive data
* Version your configuration files
* Validate OIDC and certificate setup early
* Document your claims and policies

---

## ➡️ Next Steps

* Configure authentication:
  👉 [authentication/oidc](authentication/oidc.md)

* Configure application settings:
  👉 [application/config-files](application/config-files.md)

* Customize the UI:
  👉 [ui/theming](ui/theming.md)
