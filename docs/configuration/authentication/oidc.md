# OIDC Configuration

## 🎯 Purpose

This guide explains how **OIDC authentication is integrated with LinId Identity Manager**.

LinId does **not handle authentication directly**.
Authentication is fully managed by a pre-configured **LemonLDAP** instance provided with LinId.

---

## 🧩 Architecture Overview

LinId operates alongside:

* **LemonLDAP** (pre-configured Identity Provider)
* **LDAP directory** (user source)
* **Applications** (OIDC clients)

### Key Principle

👉 All authentication flows go through **LemonLDAP**
👉 LinId is responsible for **identity configuration and claims logic**

---

## 🔄 Authentication Flow

1. A user accesses an application
2. The application redirects the user to **LemonLDAP**
3. The user authenticates via LemonLDAP
4. LemonLDAP retrieves user data from LDAP
5. LinId provides:
    * roles definition
    * claims configuration
    * access logic (OPA policies)
6. LemonLDAP generates the OIDC token
7. The application receives the user identity and claims

---

## 📦 Pre-configured Identity Provider

LinId provides a **ready-to-use LemonLDAP configuration**.

This means:

* No manual OIDC setup is required
* No client configuration needed in most cases
* Authentication flows are already operational

> 🚧 TODO: Document how to start the provided LemonLDAP environment

---

## 🔗 Role of LinId

LinId is responsible for:

* Synchronizing data with LDAP
* Managing Organizational Units
* Defining application roles
* Configuring claims structure
* Providing access logic via OPA

---

## 🔐 Role of LemonLDAP

LemonLDAP is responsible for:

* User authentication
* Session management
* OIDC token generation
* Injecting claims into tokens

---

## ⚙️ What You Need to Configure

In most cases, you only need to configure **LinId**.

### Required configuration:

* Applications
* Roles
* Claims definitions
* OPA policies

👉 See:

* [application/config-files](../application/config-files.md)
* [claims](claims.md)

---

## 🏷️ Claims Integration

LinId defines:

* What claims should exist
* How they are computed

LemonLDAP:

* Injects these claims into OIDC tokens

---

## 🔄 Data Synchronization

LinId synchronizes with LDAP to:

* Retrieve user data
* Maintain organizational structure
* Provide context for policy evaluation

> 🚧 TODO: Document synchronization process

---

## 🧪 Example Flow

1. User logs in via LemonLDAP
2. LDAP provides user attributes
3. LinId defines roles and claims
4. OPA evaluates policies
5. LemonLDAP generates token with claims
6. Application receives identity and permissions

---

## ⚠️ Common Misconceptions

### ❌ "I need to configure OIDC manually"

➡️ No — a pre-configured LemonLDAP is provided

### ❌ "LinId handles authentication"

➡️ No — authentication is handled by LemonLDAP

### ❌ "Applications connect to LinId for login"

➡️ No — applications connect to LemonLDAP

---

## 🧠 Best Practices

* Treat LemonLDAP as a managed component
* Focus configuration efforts on LinId
* Clearly define roles and claims
* Keep policies simple and maintainable
* Validate the full authentication flow early

---

## ➡️ Next Steps

* Configure claims:
  👉 [claims](claims.md)

* Configure application settings:
  👉 [../application/config-files](../application/config-files.md)
