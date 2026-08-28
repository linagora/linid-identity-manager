# Certificates Configuration

This guide explains how TLS certificates and OIDC keys are managed in the **development,
demo, and E2E environments**.

All HTTPS-enabled services (Nginx UI, LinID API, LemonLDAP::NG, Apache Superset™) share a
**single server certificate** signed by a **local certificate authority (CA)**. Trusting
that CA once is enough to access every service without browser security warnings.

---

## 🔐 1️⃣ Local Certificate Authority

The CA is generated **once** in `docker/certs/` (`ca.crt` and `ca.key`) and reused by all
environments. It is created automatically by the certificate tasks and is **never
regenerated** as long as the files exist, so the trust you grant it in your browser or
operating system survives environment restarts.

```bash
task setup:certs        # dev + e2e
task setup:certs:demo   # demo
```

To start from scratch, delete `docker/certs/` and run the task again (you will need to
trust the new CA).

---

## 🌐 2️⃣ Shared Server Certificate

Each environment gets a single `server.crt`/`server.key` pair in
`docker/<env>/resources/`, signed by the local CA. Its Subject Alternative Names cover
every hostname used in the platform:

* `linid.localtest.me`
* `localhost`
* `ui`, `api`, `auth`, `lemon`, `superset` (internal Docker service names)

The same certificate is used by:

| Service        | Usage                                              |
| -------------- | -------------------------------------------------- |
| Nginx (UI)     | HTTPS on the frontend                              |
| LemonLDAP::NG  | HTTPS on the SSO portal (dev environment)          |
| LinID API      | HTTPS via `keystore.p12` (PKCS12, same key pair)   |
| Superset       | HTTPS on the analytics instance                    |

---

## ✅ 3️⃣ Trust the CA (one-time setup)

Trust `docker/certs/ca.crt` once and all services are accepted by your browser.

**Linux (system-wide):**

```bash
sudo cp docker/certs/ca.crt /usr/local/share/ca-certificates/linid-dev-ca.crt
sudo update-ca-certificates
```

**macOS:**

```bash
sudo security add-trusted-cert -d -r trustRoot \
  -k /Library/Keychains/System.keychain docker/certs/ca.crt
```

**Firefox** (uses its own trust store): *Settings → Privacy & Security → Certificates →
View Certificates → Authorities → Import*, select `docker/certs/ca.crt` and check
*"Trust this CA to identify websites"*.

**Chrome/Chromium on Linux** reads the system store after a restart; alternatively import
the CA under *Settings → Privacy and security → Security → Manage certificates*.

---

## 🔑 4️⃣ OIDC Keys for LemonLDAP::NG

These RSA keys **sign JWTs** for OIDC authentication. They are not TLS certificates and
are generated per environment alongside the server certificate:

* `oidc.key`: private key
* `oidc.pub`: public key

---

## 🗝️ 5️⃣ API Keystore and Truststore

Generated automatically by the certificate tasks:

* **Keystore** (`api/src/main/resources/keystore.p12`): PKCS12 archive built from the
  shared server certificate and key. The Spring Boot API uses it to serve HTTPS, so the
  API presents the same certificate as every other service.
* **Truststore** (`truststore.jks`): contains only the local CA certificate. Because
  every service certificate is signed by that CA, this single entry lets the API validate
  TLS connections to LemonLDAP::NG, Superset, and any future HTTPS service.

Passwords come from `docker/<env>/env/cert.env` (`SSL_KEY_PASSWORD`,
`SSL_TRUSTSTORE_PASSWORD`).

---

## 📝 Notes

* All generated files are **git-ignored**; never commit certificates, keys, or stores
* The CA and certificates are valid for **10 years (3650 days)**
* Server certificates are regenerated on each setup, but this is invisible to the
  browser as long as the CA stays the same
* These certificates are for **development, demo, and E2E only** — use certificates from
  a real authority in production, and externalize all passwords
