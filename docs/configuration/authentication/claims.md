# Claims Configuration

## 🎯 Purpose

Claims are pieces of information about a user that applications receive via OIDC tokens.

In **LinId Identity Manager**:

* Each application defines **one claims template**, which produces all its claims
* Claims are **computed from the payload of each account**, built from the data of the LinId database
* LinId uses **Jinja templates** to generate claims dynamically

> LinId does not handle authentication — it only manages **claims definitions**.

---

## 🧩 Core Concepts

### 👤 User Data

`sync-api` builds a **payload** for each account from the data of the LinId database:

* The `external_id` and `email` of the account
* Other data of the account, such as its Organizational Units (OU) and groups

The content of the payload is set by the `mapping` of `sync-data`
👉 [application/config-files](../application/config-files.md#-account-payloads--sync-data)

### 🏷️ Claims

A claim represents:

* A key-value pair that applications need
* Example: `{"role": "admin"}`, `{"department": "finance"}`

---

### 📦 Application-Specific Claims

The claims template of each application defines:

* Which claims the application **receives**
* How each claim is **computed from the payload of the account** and the roles returned by OPA

> This ensures applications only receive relevant information.

---

### 🧠 Claims Generation with Jinja

LinId uses **Jinja templates** to generate claim values dynamically.

The claims template of an application is rendered for each account with these variables:

| Variable  | Content                                                                    |
| --------- | -------------------------------------------------------------------------- |
| `payload` | Payload of the account, built from the `mapping` of `sync-data`            |
| `allow`   | `allow` returned by OPA, `true` or `false`                                 |
| `roles`   | Roles returned by OPA, a sorted list of role names, empty when it has none |

Example: the claims template of the `LINID` system application, which gives the claims LinId needs in its tokens.
The API identifies the user by the `email` claim.

```jinja id="claims-example"
{
  "sub": {{ payload.external_id | tojson }},
  "email": {{ payload.email | tojson }},
  "roles": {{ roles | tojson }}
}
```

* Every value is inserted with the `tojson` filter, without quotes: a string is correctly escaped, and a list stays a
  JSON array
* Loops / conditionals allowed for complex logic
* Output is rendered as a **JSON object for claims**

The computation of the claims and its errors are described in
👉 [application/config-files](../application/config-files.md#-claims-template)

---

## ⚙️ How to Configure Claims in LinId

1. Set the **Claims generation template** of an application: in the **Claims management** section when creating it,
   or by editing the application from its details page
2. Save the application
3. Check the template in the **Claims** card of the details page

When the template changes, `sync-api` computes the claims of every account again at its next cycle, for an application
that is deployed. The **Recompute rights** button of the application starts this computation at once.

> 🚧 TODO: Add screenshots

---

## 🔄 Example Workflow

1. Account `alice@example.com` exists in LinId
2. The account belongs to OU `Finance`
3. `sync-api` builds the payload of the account, with its `external_id`, its `email` and its OUs
4. OPA evaluates the policy of application `PayrollApp` with this payload, and returns the roles of the account
5. `sync-api` renders the claims template of `PayrollApp` with the payload and the roles
6. The rendered claims are stored in the `computed_rights` table of the LinId database

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
