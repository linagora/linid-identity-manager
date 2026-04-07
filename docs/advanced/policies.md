# Policies Configuration (OPA)

## 🎯 Purpose

LinId uses **OPA scripts internally** to control how roles and permissions are assigned to users per application.

> Users do **not** need to install or manage OPA themselves.
> All policy management happens inside **LinId UI**, which updates the internal OPA server automatically.

---

## 🧩 Core Concepts

* **Policy**: A script that defines how roles are assigned to users based on attributes, organizational units, or other
  conditions.
* **Active Policy per Application**: Each application has **one active OPA script** that governs role assignments.
* **Script Editor**: LinId provides a built-in editor to write, edit, and test OPA scripts in **Rego syntax**.

---

## ⚙️ Managing Policies in LinId

> 🚧 TODO: Document how to create, edit, test, and activate a policy per application in the LinId UI
> Steps to include:
>
> * Selecting an application
> * Accessing the Policies / OPA tab
> * Writing or editing a Rego script
> * Testing the script with sample users
> * Activating the script and updating the internal OPA server

---

## 🧪 Expected Script Output

> 🚧 TODO: Define what the OPA script should return for LinId
>
> * Roles format
> * Claims mapping (if applicable)
> * JSON structure or other conventions
> * Error handling / defaults

---

## 🔄 Policy Lifecycle

1. **Draft**: Admin writes or edits a script
2. **Test**: Validate the script against test users
3. **Activate**: LinId deploys the policy to the internal OPA engine
4. **Update**: Modify and retest when rules change

---

## 📝 Best Practices

* Keep scripts **focused per application**
* Test scripts with sample users before activation
* Document scripts and intended behavior for auditing
* Avoid overly complex logic — use multiple rules if needed
* Maintain version history for rollback if needed
