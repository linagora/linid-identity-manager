# Prerequisites

## 🎯 Purpose

This guide lists all **requirements and prerequisites** before installing or running **LinId Identity Manager**.

Ensuring these are met will allow a smooth setup and avoid common issues.

---

## 🧩 System Requirements

* **Operating System**: Linux or macOS (Windows for development is supported via Docker)
* **CPU / Memory**: Minimum 2 cores, 8GB RAM (12GB recommended)
* **Disk Space**: Minimum 20GB free

---

## 💾 Software Requirements

### 1️⃣ Database

* **PostgreSQL** (version 18 or later)
* Database user with **create/read/write privileges**
* Database accessible from the LinId backend

### 2️⃣ Java Runtime

* **Java 21** (LTS) or later
* Required for **backend Spring Boot application**

### 3️⃣ Node.js / Frontend

* **Node.js 22** or later (for development and local builds)
* **npm 9** or **pnpm 10**

> ⚠️ Required only if you plan to build or customize the frontend locally. Docker images include prebuilt frontend.

### 4️⃣ Docker (Optional for demo)

* **Docker 24+**
* **Docker Compose 2+**
* Used to run the demo environment quickly without installing dependencies manually

---

## 🔑 Environment Variables

Some configuration depends on environment variables:
* `DATABASE_HOST`, `DATABASE_PORT`, `DATABASE_NAME`, `DATABASE_USER`, `DATABASE_PASSWORD`
* `AUTH_ISSUER_URI` (URL of the OIDC issuer / LemonLDAP)
* `I18N_EXTERNAL_PATH` (optional, for backend/plugin translations)

> ⚠️ A `.env` file can be used for local development

---

## 🌐 Network & Access

* Ports required by LinId backend and frontend (default `8443` for backend) must be accessible
* LemonLDAP OIDC endpoint must be reachable by the backend and applications

---

## 📝 Best Practices

* Install and verify **PostgreSQL and Java** before running LinId
* Use **Docker** for an isolated, reproducible environment during development or demo
* Keep environment variables **secure and versioned** carefully
* Test network access to backend and LemonLDAP before configuring applications

---

## ➡️ Next Steps

* Proceed with **installation**:
  👉 [installation/setup](setup.md)

* Configure **environment variables**:
  👉 [configuration/application/config-files](../configuration/application/config-files.md)

* Verify **frontend build requirements** (if needed):
  👉 [configuration/ui/theming](../configuration/ui/theming.md)
