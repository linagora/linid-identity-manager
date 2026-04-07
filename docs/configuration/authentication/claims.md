# Claims Configuration

## 🎯 Purpose

Claims are pieces of information about a user that applications receive via OIDC tokens.

In **LinId Identity Manager**:

* Applications define **which claims they require**
* Claims are **computed from LDAP user data**
* LinId uses **Jinja templates** to generate claims dynamically

> LinId does not handle authentication — it only manages **claims definitions**.

---

## 🧩 Core Concepts

### 👤 User Data

LinId synchronizes user accounts from LDAP:

* Username, email, and attributes
* Organizational Units (OU) membership

### 🏷️ Claims

A claim represents:

* A key-value pair that applications need
* Example: `{"role": "admin"}`, `{"department": "finance"}`

---

### 📦 Application-Specific Claims

Each application can define:

* Which claims it **requires**
* How each claim is **computed from user data**
* Optional defaults

> This ensures applications only receive relevant information.

---

### 🧠 Claims Generation with Jinja

LinId uses **Jinja templates** to generate claim values dynamically.

Example template:

```jinja id="claims-example"
{
  "username": "{{ user.username }}",
  "email": "{{ user.email }}",
  "department": "{{ user.name }}"
}
```

* `user` → Internal user information
* Loops / conditionals allowed for complex logic
* Output is rendered as a **JSON object for claims**

---

## ⚙️ How to Configure Claims in LinId

1. Select an **application** in LinId
2. Open the **Claims tab**
3. Define each claim using a **Jinja template**
4. Test the template against a user preview
5. Save configuration

> 🚧 TODO: Add screenshots and step-by-step UI instructions

---

## 🔄 Example Workflow

1. User `alice@example.com` exists in LinID
2. User belongs to OU `Finance`
3. Application `PayrollApp` requires claims:

    * username
    * email
    * department
    * roles
4. LinId generates claims using Jinja templates
5. LemonLDAP injects claims into OIDC token
6. Application receives token with all required claims

---

## 📝 Best Practices

* Only include **necessary claims** per application
* Keep Jinja templates **simple and maintainable**
* Use OU and roles to **dynamically control claims content**
* Document each claim template for **auditing and transparency**
* Test claims generation before integrating with production applications

---

## ➡️ Next Steps

* Configure application roles:
  👉 [application/config-files](../application/config-files.md)

* Integrate claims with policies (OPA):
  👉 [advanced/policies](../advanced/policies.md)

* Customize UI for claims preview (optional):
  👉 [ui/theming](../ui/theming.md)
