# 🚀 Linid Identity Manager

Welcome to **Linid Identity Manager**, your portal for managing identities and entities within your information system.

**Linid Identity Manager** allows you to:

- Manage users, groups, and other organizational features.
- Easily customize entity attributes to fit your company’s needs.

**Linid Identity Manager** is built around a **modular system**, which means you can add or remove modules to fit your
organization. Each module provides a specific feature, making the application flexible and adaptable.

The application also includes **system plugins**, which handle the connection to different data sources. These plugins:

- Allow **retrieving and storing data** from multiple types of sources (currently only an external API is supported).
- Ensure that the data is **validated and consistent** before it is saved or updated in the system.
- Provide a foundation for future data sources, such as databases, LDAP, or other APIs.

In addition, we plan to provide **modules** that let you manage users, groups, and other organizational features
directly from the application. Each module is independent, so you can choose only the features your company needs.

---

## 🧪 Running the Demo Locally

You can quickly test **Linid Identity Manager** using the **Docker demo** provided.

### Step 1: Install docker

Make sure Docker is installed on your machine. You can follow the official installation guide here:  
https://docs.docker.com/get-docker/

### Step 2: Initialize submodules

```bash
git submodule update --init --recursive
```

### Step 3: Start the demo

```bash
./run
```

Once the demo are running, access the application at:
[http://localhost:9000](http://localhost:9000)

---

## 🧩 Modules

**What is a Module?**

In **Linid Identity Manager**, a **module** is a self-contained feature that adds a specific functionality to the
application. Modules allow you to adapt the system to your organization’s needs, enabling you to pick only the features
that are relevant for your business.

Each module can also be **customized for your organization**:

- 🗂️ **Data configuration** – Decide which data fields are available and how they are handled.
- 🖥 **Display settings** – Adjust how information is presented in the interface.
- 🎨 **Design customization** – Change the look and feel to match your company’s style or branding.

Modules can have **hierarchies and dependencies**. For example, you could have a main **User Management Module** that
handles creating, editing, and assigning users. Then, an additional module could extend it by adding extra interfaces or
specialized tools for user management. This extension module would depend on the main User Management Module in order to
function correctly (for example : company management).

---

### 📦 Currently Available

At the moment, no modules are included in the demo.

---

### 🛠️ Planned Modules

The following modules are planned for future releases:

- **User Management Module** – Manage users: create, edit, and assign them to groups.
- **Group Management Module** – Manage user groups, including group hierarchies and permissions.
- **Buildings Management Module** – Manage company buildings and their attributes.
- **Roles & Functions Module** – Manage user roles, functions, and positions within the organization.

> Each module is designed to be independent whenever possible, but some modules can extend others to add extra features
> or interfaces.
> All modules can be configured for your organization in terms of data, display, and design.

---

## 📚 Additional Documentation

You can find more detailed information in the `docs/` folder:

- **[Configuration Guide](docs/configuration.md)** – How to configure **Linid Identity Manager** and connect to
  different data sources.
- **[Modules Overview](docs/modules.md)** – List and description of available and planned front-end modules.
- **[FAQ](docs/faq.md)** – Answers to common questions and troubleshooting tips.

> Check these files for all the specifics you need to get the most out of **Linid Identity Manager**.

## 📄 License

**Linid Identity Manager** is **open source** and licensed under
the [GNU Affero General Public License (AGPL)](https://www.gnu.org/licenses/agpl-3.0.html).

Maintained by **[Linagora](https://linagora.com/)**

<a href="https://linagora.com/">
  <img src="https://linagora.com/themes/custom/linagora/images/header-logo-white.svg" alt="Linagora Logo" width="200"/>
</a>

---

## 🙌 Contributors & Related Projects

**Linid Identity Manager** is composed of multiple projects, each responsible for a specific part of the system:

- **[linid-im-api](https://github.com/linagora/linid-im-api)** – The main back-end API for managing data.
- **[linid-im-api-community-plugins](https://github.com/linagora/linid-im-api-community-plugins)** –
  Community-contributed plugins for the back-end API.
- **[linid-im-api-corelib](https://github.com/linagora/linid-im-api-corelib)** – Core libraries and utilities for the
  back-end api and back-end plugins.
- **[linid-im-front](https://github.com/linagora/linid-im-front)** – Main front-end application.
- **[linid-im-front-community-plugins](https://github.com/linagora/linid-im-front-community-plugins)** –
  Community-contributed modules for the front-end.
- **[linid-im-front-corelib](https://github.com/linagora/linid-im-front-corelib)** – Core libraries and utilities for
  the front-end and modules.

---

## 🔖 Submodule Tags

The project relies on several Git submodules.
Each submodule is pinned to a specific **tag** to ensure consistency and compatibility across the whole system.

Here are the submodules and the tags currently in use:

| Submodule                            | Tag Used |
| ------------------------------------ | -------- |
| **linid-im-api**                     | `v0.1.3` |
| **linid-im-front**                   | `v0.0.6` |
| **linid-im-front-community-plugins** | `v0.0.1` |

> You can check or update submodule tags using:
> `git submodule foreach "git describe --tags"`
