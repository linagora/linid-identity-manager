# LinID Identity Manager API

## Description

The **LINID Identity Manager API** provides management of user accounts, applications, roles, and organizational units within a company.
It is part of the **LinID** monorepo and serves as the central service for access control and authorization management for internal applications.

This API allows you to:

- Manage **user accounts**: create, update, activate, suspend, or delete accounts.
- Handle **applications**: register applications, manage their roles, and configure authentication types.
- Organize **organizational units (OUs)**: structure users and applications by departments or teams.
- Enforce **roles and permissions** within applications.

---

## Base URL

The API is typically available at:

[https://localhost:8443/](https://localhost:8443/)

Adjust the host and port according to your deployment configuration.

---

## Build & Run

### Prerequisites

- Java 21+
- Maven 3.8+

### Run locally

```bash
mvn spring-boot:run
```

### Build artifact

```bash
mvn clean package
```

Then run the JAR:

```bash
java -jar target/linid-identity-manager-api-0.1.0.jar
```

### Generate certificate for HTTPS

To generate certificate, run this command at the root of your project folder:

```bash
keytool -genkey -alias myKeyAlias -keyalg RSA -keysize 2048 -keystore src/main/resources/keystore.jks -validity 3650

keytool -importcert -noprompt -trustcacerts -alias lemonldap -file selfsigned.crt -keystore src/main/resources/truststore.jks -storepass changeit >/dev/null 2>&1
```

You will be asked to set a password for the key store and another for the key itself. Make sure to use the same
passwords in your .env file when building unless it will use the default "password" password.
