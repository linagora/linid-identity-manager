# Certificates Configuration

This guide explains how to generate **self-signed certificates** for Nginx, **OIDC keys for LemonLDAP::NG**, and the *
*keystore for the LinId API**.

---

## 🔐 1️⃣ Generate Self-Signed Certificates for Nginx

These certificates are used for **HTTPS in the Docker demo environment**.

```bash id="nginx-cert"
# for demo env
openssl req -x509 -newkey rsa:2048 \
  -keyout docker/demo/resources/selfsigned.key \
  -out docker/demo/resources/selfsigned.crt \
  -days 3650 -nodes
```

> The certificates are valid for **10 years (3650 days)** and placed in the `docker/demo/resources/` folder.

---

## 🔑 2️⃣ Generate OIDC Keys for LemonLDAP::NG

These keys are used to **sign JWTs** for OIDC authentication.

```bash id="oidc-keys"
# for demo env
openssl genpkey -algorithm RSA -out docker/demo/resources/oidc.key -pkeyopt rsa_keygen_bits:2048
openssl pkey -in docker/demo/resources/oidc.key -pubout -out docker/demo/resources/oidc.pub
```

* `oidc.key`: private key
* `oidc.pub`: public key

> Place them in `docker/demo/resources/` and configure LemonLDAP to use them.

---

## 🔒 3️⃣ Generate TLS Certificate for Apache Superset™

This certificate is used to **enable HTTPS** on the Apache Superset™ demo instance.

```bash
# for demo env
openssl req -x509 -newkey rsa:2048 \
  -keyout superset.key \
  -out superset.crt \
  -days 3650 -nodes \
  -subj "/CN=linid.localtest.me/OU=Test/O=Test/L=Test/ST=Test/C=FR" \
  -addext "subjectAltName=DNS:linid.localtest.me,DNS:ui,DNS:localhost,DNS:superset"
```

* `superset.key`: TLS private key
* `superset.crt`: TLS certificate

> Place both files in the Superset configuration directory and mount them into the container. The certificate includes Subject Alternative Names (SANs) for `linid.localtest.me`, `ui`, `localhost`, and `superset` to support local demo deployments.

---

## 🗝️ 4️⃣ Create a Keystore for the API

LinId API uses a **Java Keystore (JKS)** to serve HTTPS.

```bash id="keystore-api"
keytool -genkey -alias myKeyAlias \
  -keyalg RSA \
  -keysize 2048 \
  -keystore api/src/main/resources/keystore.jks \
  -validity 3650
```

* `myKeyAlias`: key alias (can be any name)
* `keystore.jks`: Java keystore file used by the Spring Boot backend
* Valid for 10 years

> ⚠️ Make sure to configure `application.yml` with the keystore path and password.

---

## 🧾 5️⃣ Generate Truststore (Automation)

This section covers the generation of the **truststore for SSL validation** and an alternative **API keystore setup** using automated scripts.

---

### 🔐 Create Truststore for LemonLDAP::NG Certificate

The truststore is used to **trust the self-signed certificate** generated earlier.

```bash id="truststore-gen"
keytool -importcert -noprompt -trustcacerts \
  -alias lemonldap \
  -file docker/demo/resources/selfsigned.crt \
  -keystore docker/demo/resources/truststore.jks \
  -storepass $SSL_TRUSTSTORE_PASSWORD >/dev/null 2>&1
```

---

### 🔐 Add Apache Superset™ Certificate to the Truststore

The truststore is used to **trust the self-signed Apache Superset™ certificate** generated earlier.

```bash id="superset-truststore-gen"
keytool -importcert -noprompt -trustcacerts \
  -alias superset \
  -file superset.crt \
  -keystore docker/demo/resources/truststore.jks \
  -storepass $SSL_TRUSTSTORE_PASSWORD >/dev/null 2>&1
```

> This command adds the Apache Superset™ certificate to the shared demo truststore, allowing Java applications to establish TLS connections without certificate validation errors.

---

## 📝 Notes

* The **truststore** is required for SSL trust chain validation in internal services
* The **keystore** is used by the Spring Boot API to serve HTTPS
* Both artifacts are generated with **10-year validity (3650 days)**
* Avoid committing generated `.jks` files into version control
* Ensure passwords are externalized in production environments

---


## 📝 Best Practices

* Keep **private keys secure** and do not commit them to the repository
* Regenerate certificates/keys periodically in production environments
* For demo purposes, self-signed certificates are sufficient
* Document passwords and aliases for the keystore securely
