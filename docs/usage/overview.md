# Usage Overview

## 🎯 Purpose

This section explains how to use **LinId Identity Manager** to manage users, applications, and access rights within your
organization.

It provides a functional overview of the main workflows.

---

## 🧭 Typical Workflow

Using LinId generally follows this flow:

1. Define your organizational structure (OU)
2. Assign users to Organizational Units
3. Register applications
4. Define application roles
5. Configure authentication (OIDC)
6. Define access policies (OPA)
7. Let LinId automatically assign permissions

---

## 👤 Managing Users

Users are the core entities of the platform.

### What you can do:

* Edit users
* Assign users to Organizational Units
* View user attributes and identity data

### Why it matters:

Users are the entry point for all access control decisions.

---

## 🏢 Managing Organizational Units

Organizational Units (OU) allow you to structure your organization.

### What you can do:

* Create hierarchical structures
* Group users by team, department, or role
* Apply consistent policies across groups

### Why it matters:

OU simplify large-scale access management.

---

## 📦 Managing Applications

Applications represent the systems connected to LinId.

### What you can do:

* Register new applications
* Configure authentication (OIDC)
* Define required claims
* Manage the roles exposed by each application
* Write **access rules** (OPA/Rego) that decide, per application, which roles a user is granted and whether access is
  allowed

### Why it matters:

Each application has its own access model and requirements. Access rules let you express that model declaratively: LinId
compiles the active rules of an application into a single OPA policy and deploys it automatically.

> See: [docs/advanced/policies.md](../advanced/policies.md) to learn how to write OPA/Rego rules.

---

## 🏷️ Managing Roles

Roles define permissions within an application.

### What you can do:

* Create roles per application
* Define permission scopes
* Map roles to policies

### Why it matters:

Roles are the bridge between users and permissions.

---

## 🔐 Configuring Authentication

LinId uses OIDC to authenticate users.

### What you can do:

* Configure identity providers (e.g., LemonLDAP)
* Set up certificates
* Define claims mapping

### Why it matters:

Authentication ensures secure and standardized access.

---

## 🧠 Defining Access Policies (OPA)

LinId uses policy-based access control with OPA.

### What you can do:

* Write policies to assign roles dynamically
* Use user attributes and context
* Automate access decisions

### Why it matters:

Policies eliminate manual role assignment and reduce errors.

---

## 🔄 How Permissions Are Assigned

Permissions are not always manually assigned.

Instead:

1. A user logs in
2. Authentication is handled via OIDC
3. Claims are generated
4. OPA evaluates policies
5. Roles are dynamically assigned
6. The application receives the final permissions

---

## 🎨 Customizing the User Interface

LinId allows UI customization.

### What you can do:

* Apply themes
* Customize branding
* Manage translations (i18n)

---

## ⚠️ Best Practices

* Use Organizational Units to structure users early
* Avoid hardcoding permissions
* Prefer policy-based access (OPA)
* Keep application configurations isolated
* Document your claims and roles clearly

---

## ➡️ Next Steps

* Learn how to install LinId:
  👉 [installation/setup](../installation/setup.md)

* Configure authentication:
  👉 [authentication/oidc](../configuration/authentication/oidc.md)

* Explore configuration options:
  👉 [configuration/overview](../configuration/overview.md)
