# Introduction

## 🎯 What is LinId Identity Manager?

**LinId Identity Manager** is an Identity and Access Management (IAM) platform designed to help organizations manage
users, permissions, and application access in a centralized and scalable way.

It allows you to define **who can access what**, across multiple applications, using structured configuration and
policy-based logic.

---

## 🧩 Core Concepts

LinId is built around a few key concepts:

### 👤 Users

Users represent individual identities within your organization.

They can:

* Be created and managed centrally
* Be assigned to organizational units
* Receive permissions dynamically

---

### 🏢 Organizational Units (OU)

Organizational Units are used to structure users logically.

They allow you to:

* Group users by department, team, or business unit
* Apply consistent access rules across groups

---

### 📦 Applications

Applications represent the systems connected to LinId.

For each application, you can:

* Define roles
* Configure authentication (OIDC)
* Specify required claims
* Control access rules

---

### 🏷️ Roles

Roles define what a user can do within an application.

They are:

* Application-specific
* Used to group permissions
* Assigned dynamically via policies

---

### 🔐 Authentication

LinId uses **OIDC (OpenID Connect)** for authentication.

It integrates with identity providers such as **LemonLDAP** and supports:

* Secure login flows
* Certificate-based configuration
* Custom claims mapping

---

### 🧠 Policy Engine (OPA)

LinId leverages **Open Policy Agent (OPA)** to dynamically generate permissions.

This allows you to:

* Define access rules using policies
* Automatically assign roles based on context
* Avoid hardcoded permission logic

---

## 🚀 What You Can Do with LinId

With LinId, you can:

* Centralize identity management across applications
* Automate user access provisioning
* Enforce consistent security policies
* Adapt access control dynamically using policies
* Integrate authentication with external providers

---

## 🏗️ How It Fits Together

At a high level:

1. Users are created and assigned to Organizational Units
2. Applications define roles and required claims
3. OPA policies determine which roles users receive
4. Authentication is handled via OIDC
5. Applications receive user identity and permissions through claims

---

## 🧪 Current Status

⚠️ LinId is currently under active development.

Some features may not be fully implemented or documented yet.

---

## ➡️ Next Steps

* To quickly try the application:
  👉 [demo](demo.md)

* To install LinId in your environment:
  👉 [installation/installation](../installation/installation.md)

* To understand how to use the platform:
  👉 [usage/overview](../usage/overview.md)
