# Installation & Setup

## 🎯 Purpose

This guide explains how to **install and run LinId Identity Manager** in a development or demo environment.

You can use **Docker** for a quick setup or install the backend and frontend manually.

---

## 🐳 Using Docker (Recommended)

LinId provides **Docker setups per environment** (e.g., demo, integration). Each environment folder contains a README
with detailed instructions.

1. **Navigate to the environment folder**

```bash id="docker-env"
cd docker/<environment>
```

2. **Follow the environment README**

```bash id="docker-readme"
cat README.md
# or open it in your editor
```

> ✅ Each README contains the full steps to start the Docker environment, including backend, frontend, and LemonLDAP.

---

## ⚙️ Post-Installation Checks

* Backend health endpoint: `https://localhost:8443/actuator/health`
* Swagger UI (if enabled): `https://localhost:8443/swagger-ui.html`
* Frontend loads translations, theming, and plugins correctly

---

## 📝 Best Practices

* Use **Docker** for demos or quick setups
* Keep `.env` files secure and **do not commit secrets**
* Verify **network connectivity** between backend and LemonLDAP
* Test frontend after backend is running to ensure translations and theming are applied

---

## ➡️ Next Steps

* Configure **application settings**:
  👉 [configuration/application/config-files](../configuration/application/config-files.md)

* Configure **policies / roles**:
  👉 [advanced/policies](../advanced/policies.md)

* Customize **UI / themes / translations**:
  👉 [configuration/ui/theming](../configuration/ui/theming.md)
  👉 [configuration/ui/translations](../configuration/ui/translations.md)
