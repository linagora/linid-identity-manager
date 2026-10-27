# UI Server Environment Variables

## 🎯 Purpose

This page lists the environment variables of the **UI server**.

---

## 📍 Location

| Environment   | File                                                                              |
|---------------|-----------------------------------------------------------------------------------|
| Development   | `docker/dev/env/ui.env` (only `NODE_EXTRA_CA_CERTS`)                              |
| E2E           | `docker/e2e/env/ui.env`, plus `e2e.env` (CI) or `local.env` (local runs)          |
| Demo          | `docker/demo/.env`                                                                |

---

## ⚙️ Runtime variables

Read by the UI server when it starts.

| Variable                | Default                | Description                                              |
|-------------------------|------------------------|----------------------------------------------------------|
| `SERVER_CONFIG_PATH`    | `server-config.json`   | Path of the configuration file                           |
| `APP_ENV`               | `DEV`                  | Replaces `__APP_ENV__` in the configuration              |
| `NODE_EXTRA_CA_CERTS`   | —                      | Extra certificate authority trusted by Node.js           |

**`NODE_EXTRA_CA_CERTS`**: the local certificates (LemonLDAP::NG, API) are signed by `docker/certs/ca.crt`, which
Node.js does not trust by default. Without it, the login fails with `unable to verify the first certificate`.
Node.js reads it **only at startup**: it must be set by the launcher (Task or Docker Compose), not in the code.

---

## 🧩 Template variables

Used by `task setup:config` to generate `server-config.json`.

| Variable                        | Parameter                                                        | Example                                  | Description                                       |
|---------------------------------|------------------------------------------------------------------|------------------------------------------|---------------------------------------------------|
| `SERVER_CERT_PATH`              | `http.tls.cert`                                                  | `/etc/server/certs/server.crt`           | Path to the server certificate                    |
| `SERVER_KEY_PATH`               | `http.tls.key`                                                   | `/etc/server/certs/server.key`           | Path to the server key                            |
| `HTTP_TRUST_PROXY`              | `http.trustProxy`                                                | `false`                                  | Trusted reverse proxy, written as JSON            |
| `API_UPSTREAM`                  | `upstream` of `/backend`                                         | `https://api:8443`                       | URL of the API upstream                           |
| `LIN_PULSAR_UPSTREAM`           | `upstream` of `/lin-pulsar`                                      | `http://lin-pulsar`                      | URL of the LinPulsar upstream                     |
| `AUTH_UPSTREAM`                 | `upstream` of the LemonLDAP::NG routes, `discovery.issuer`       | `http://auth`                            | URL of the Auth upstream                          |
| `LEMONLDAP_REDIRECT_URL`        | `callbackUri`, `logoutUrl`, `postLogoutRedirectUri`              | `https://linid.localtest.me:9000`        | Public URL of the application                     |
| `LEMONLDAP_PORTAL_HOSTNAME`     | `oidc.oauth2.credentials.http.headers.host`                      | `linid.localtest.me`                     | `Host` of the LemonLDAP::NG portal                |
| `OIDC_CLIENT_ID`                | `oidc.oauth2.credentials.client.id`                              | `linid-im-client`                        | OIDC client ID                                    |
| `OIDC_CLIENT_SECRET`            | `oidc.oauth2.credentials.client.secret`                          | `linid-im-secret`                        | OIDC client secret                                |
| `OIDC_SCOPE`                    | `oidc.oauth2.scope`                                              | `"openid email profile roles"`           | OIDC scope                                        |
| `OIDC_LOGOUT_PATH`              | `oidc.logoutUrl`                                                 | `/auth/oauth2/logout`                    | Path of the logout endpoint                       |
| `OIDC_USERINFO_PATH`            | `rewritePrefix` of `/auth/me`                                    | `/oauth2/userinfo`                       | Path of the userinfo endpoint on `AUTH_UPSTREAM`  |
| `OIDC_PKCE`                     | `oidc.oauth2.pkce`                                               | `S256`                                   | PKCE method (`S256` or `plain`)                   |
| `OIDC_REFRESH_MARGIN_MILLISECONDS` | `oidc.refreshMarginMilliseconds`                              | `30000` (30 seconds)                     | Refresh margin for OIDC tokens in milliseconds    |
| `SESSION_SECRET`                | `session.secret`                                                 | 32 characters or more                    | Secret used to sign the session cookie            |
| `SESSION_COOKIE_NAME`           | `session.cookieName`                                             | `linid`                                  | Name of the session cookie                        |
| `SESSION_MAX_AGE_MILLISECONDS`  | `session.maxAgeMilliseconds`                                     | `28800000` (8 hours)                     | Lifetime of the session cookie from the login, ms |
| `SESSION_LRU_CACHE_MAX`         | `session.lruCache.max`                                           | `10000`                                  | Maximum number of sessions in memory              |
| `SESSION_LRU_CACHE_TTL`         | `session.lruCache.ttl`                                           | `28800000` (8 hours)                     | Lifetime of a session after its last change, ms   |
| `SESSION_LRU_CACHE_TTL_AUTOPURGE` | `session.lruCache.ttlAutopurge`                                | `true`                                   | Deletes each session as soon as it expires        |

`LEMONLDAP_REDIRECT_URL` and `LEMONLDAP_PORTAL_HOSTNAME` are in `e2e.env` / `local.env`, because their values differ
between CI and local runs. `OIDC_LOGOUT_PATH` is the path of the SSO as served by the UI server, under `/auth`.
`OIDC_PKCE` empty does not disable PKCE: the method is then chosen from the discovery of the provider.
`HTTP_TRUST_PROXY` is written as is in the JSON: quote the strings, e.g. `'"10.0.0.1"'` or
`'["10.0.0.1", "10.0.0.0/8"]'`.
`OIDC_CLIENT_SECRET` is in `e2e.env` / `local.env` (`dev.env` in development, `.env` in the demo), because it is also
written in the LemonLDAP::NG configuration (`oidcRPMetaDataOptionsClientSecret`). In development, `server-config.json`
is not generated: the secret is written in it and must match `dev.env`.
The demo only uses the variables of its `.env`, the other values are written in its template.
