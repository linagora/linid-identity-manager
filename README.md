# LinId Identity Manager

## 🚀 Overview

**LinId Identity Manager** is a platform designed to manage user identities, access rights, and application permissions
within an organization.

It provides a centralized way to:

* Manage user accounts and organizational structures
* Define and control application-level permissions
* Configure authentication and identity flows
* Dynamically generate access rights using policy-based logic

## 🎯 Purpose

LinId helps organizations:

* Centralize identity and access management (IAM)
* Automate permission assignment
* Standardize authentication and authorization across applications
* Reduce complexity in multi-application environments

## 🧩 Key Features

* 👤 **User Management**

    * Create and manage user accounts
    * Assign users to Organizational Units (OU)
    * Manage account status (Active, Inactive, Suspended)

* 🏢 **Organizational Structure**

    * Create and manage Organizational Units
    * Structure users logically within the organization

* 📦 **Application Management**

    * Register applications
    * Define application-specific roles
    * Configure authentication per application

* 🏷️ **Organizational Roles**

    * Create and manage organizational roles
    * Assign roles to users

* 🔐 **Authentication (OIDC)**

    * OIDC-based authentication
    * Integration with LemonLDAP
    * Certificate-based configuration

* 🧠 **Policy-Based Authorization (PBAC)**

    * Use OPA (Open Policy Agent) scripts
    * Dynamically generate user permissions per application

* 🏷️ **Claims Management**

    * Define and map claims required by applications
    * Customize identity payloads per application

* 🎨 **UI Customization**

    * Theme configuration
    * Externalized translations (i18n)

## 👥 Target Audience

* Enterprises managing internal access control
* SaaS platforms requiring multi-tenant identity management
* DevOps / IAM / Security teams

## 🧪 Project Status

🚧  This project is currently **under active development**.

Features and APIs may evolve.

---

## ⚡ Quick Start

> 🚧 Minimal setup to run LinId Identity Manager in under 5 minutes

LinId provides a **Taskfile** to simplify running the demo environment.
All you need is **Docker** and **Task** installed on your machine.

---

### 1️⃣ Prerequisites

* **Docker 24+** and **Docker Compose 2+**
* **Task** ([https://taskfile.dev/#/installation](https://taskfile.dev/#/installation))

> ⚠️ Make sure Docker is running before executing any tasks.

---

### 2️⃣ Install LinId Repository

```bash
git clone https://github.com/linagora/linid-identity-manager.git
cd linid-identity-manager
git submodule update --init --recursive
```

---

### 3️⃣ Run the Demo

1. Use the provided **Taskfile** to start the demo:

```bash
task setup:demo
task start:demo
```

2. The task will automatically:

* Start the backend, frontend, and database containers
* Configure LemonLDAP demo environment
* Apply initial configuration for quick testing

👉 Access the demo: https://linid.localtest.me:9000

---

### 4️⃣ Access the Application

* **Backend API**: `https://localhost:8443/swagger-ui/index.html`
* **Frontend UI**: `https://linid.localtest.me:9000`
* **LemonLDAP portal**: `http://localhost:8080` (pre-configured demo)

---

### 📝 Notes

* `task start:demo` is intended for **quick testing / development only**
* For custom environments, use the **Docker folders** with their README (`docker/<environment>/README.md`)
* No manual configuration required for the demo

---

## 📚 Documentation

Full documentation is available in the [/docs](docs/index.md) directory.

It includes:

* Getting started guides
* Installation instructions
* Configuration references
* Advanced topics

---

## 🛠️ Installation

LinId provides Docker-based environments for running the application.

> See: [docs/installation](docs/installation/prerequisites.md)

---

## 🔐 Authentication

LinId supports **OIDC-based authentication**.

### Supported Providers

* LemonLDAP

### Certificates

Authentication requires certificate configuration.

> See: [docs/configuration/certificates](docs/configuration/certificates.md)

---

## ⚙️ Configuration

LinId is highly configurable and supports multiple configuration domains:

### 📄 Application Configuration

* YAML-based configuration files
* Environment-based overrides

> See: [docs/configuration/application/](docs/configuration/application/config-files.md)

---

### 🔐 Authentication Configuration

* OIDC setup
* Provider integration
* Certificate management
* Claims mapping

> See: [docs/configuration/authentication/](docs/configuration/authentication/oidc.md)

---

### 🔌 Plugins

LinId is designed to support a plugin system.

> 🚧 Plugin system is not fully documented yet

Planned documentation:

* Plugin architecture
* Installing plugins
* Plugin configuration

---

### 🎨 UI Configuration

* Theme customization
* Branding
* Externalized translations (i18n)

> See: [docs/configuration/ui/](docs/configuration/ui/theming.md)

---

## 🧱 Tech Stack

* **Frontend**: Quasar / Vue / Module Federation
* **Backend**: Java / Spring Boot / Spring Plugin
* **Database**: PostgreSQL
* **Directory / IAM**: LemonLDAP
* **Policy Engine**: OPA (Open Policy Agent)

---

## 🤝 Contributing

Contributions are welcome.

Please refer to the [CONTRIBUTING](CONTRIBUTING.md) file for guidelines.

---

## 📜 License

This project is licensed under the:

**GNU Affero General Public License (AGPL)**

---

## 🆘 Support

For questions, issues, or feature requests:

👉 Use **GitHub Issues**

