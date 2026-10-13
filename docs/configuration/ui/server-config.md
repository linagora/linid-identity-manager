# UI Server Configuration (`server-config.json`)

## 🎯 Purpose

`server-config.json` configures the **UI server** (`ui/server`).

---

## 📍 Location

| Environment   | File                                                                                         |
|---------------|----------------------------------------------------------------------------------------------|
| Development   | `ui/server/server-config.json`                                                               |
| E2E, demo     | Generated from `docker/<env>/resources/server-config.template.json` by `task setup:config`   |

* The path can be changed with `SERVER_CONFIG_PATH`.
* The file is read at startup: **restart the server** after a change.
* `__APP_ENV__` is replaced by the `APP_ENV` environment variable.

---

## 📄 Example

```json
{
  "environment": "__APP_ENV__",
  "http": { "port": 3000 },
  "oidc": {
    "oauth2": {
      "credentials": {
        "client": { "id": "linid-im-client" },
        "options": { "authorizationMethod": "body" }
      },
      "discovery": { "issuer": "https://localhost:8080" },
      "scope": ["openid", "email", "profile", "roles"],
      "callbackUri": "https://localhost:9000/auth/callback",
      "pkce": "S256"
    },
    "logoutUrl": "https://localhost:8080/oauth2/logout",
    "postLogoutRedirectUri": "https://localhost:9000/auth/logged-out"
  },
  "session": {
    "secret": "dev-only-session-secret-do-not-use-in-production",
    "cookieName": "linid",
    "maxAgeMilliseconds": 28800000,
    "lruCache": { "max": 10000, "ttl": 28800000, "ttlAutopurge": true },
    "secure": false
  },
  "proxies": [
    { "prefix": "/backend", "upstream": "https://localhost:8443", "withAccessToken": true },
    {
      "prefix": "/auth/me",
      "upstream": "https://localhost:8080",
      "rewritePrefix": "/oauth2/userinfo",
      "withAccessToken": true
    },
    { "prefix": "/auth", "upstream": "https://localhost:8080" }
  ],
  "staticDir": "../dist/spa",
  "configJsonPath": "../dist/spa/config.json",
  "cache": {
    "html": "no-cache, no-store, must-revalidate",
    "assets": "public, max-age=604800, immutable"
  }
}
```

---

## ⚙️ Parameters

### General

| Parameter          | Description                                                 |
|--------------------|-------------------------------------------------------------|
| `environment`      | Environment name, injected in the SPA `/config.json`        |
| `http.port`        | Listening port                                              |
| `http.tls`         | Optional. `cert` and `key` paths (PEM) to listen in HTTPS   |
| `staticDir`        | Directory of the built SPA                                  |
| `configJsonPath`   | SPA configuration served at `/config.json`                  |
| `cache.html`       | `Cache-Control` of `index.html` and `/config.json`          |
| `cache.assets`     | `Cache-Control` of the files under `/assets/`               |

### `oidc`

| Parameter                   | Default   | Description                                                                       |
|-----------------------------|-----------|-----------------------------------------------------------------------------------|
| `oauth2`                    | —         | Options of `@fastify/oauth2`, see below                                           |
| `logoutUrl`                 | —         | URL of the logout endpoint of the SSO (`end_session_endpoint`)                    |
| `postLogoutRedirectUri`     | —         | Where the SSO sends the user back after the logout: `{publicUrl}/auth/logged-out` |
| `refreshMarginMilliseconds` | `30000`   | Renews the access token this many milliseconds before it expires                  |

### `oidc.oauth2`

Options of `@fastify/oauth2` (type `FastifyOAuth2Options`), passed as is to the plugin, except `name` set by the server.
All the options are described in the [documentation of `@fastify/oauth2`](https://github.com/fastify/fastify-oauth2#readme).
The main ones:

| Parameter                                   | Description                                                                                        |
|---------------------------------------------|----------------------------------------------------------------------------------------------------|
| `credentials.client.id`                     | Client identifier declared in the SSO                                                              |
| `discovery.issuer`                          | URL of the SSO: the endpoints are read from `{issuer}/.well-known/openid-configuration` at startup |
| `credentials.options.authorizationMethod`   | `body` for a public client (no secret) or `client_secret_post`, `header` for `client_secret_basic` |
| `scope`                                     | Requested scopes: array, or string separated by spaces (`openid` is required)                      |
| `callbackUri`                               | `{publicUrl}/auth/callback`, declared as redirect URI in the SSO                                   |
| `pkce`                                      | `S256`                                                                                             |
| `cookie`                                    | Options of the `state`, PKCE verifier and `returnTo` cookies: `httpOnly`, `sameSite`, `secure`     |

`{publicUrl}` is the public URL of the application, without trailing slash (e.g. `https://linid.localtest.me:9000`).

The discovery runs once, when the server starts: the server does not start if the SSO cannot be reached. It only gives
the authorization and token endpoints, `logoutUrl` and the userinfo path of `/auth/me` are still configured.

When the SSO is served by the UI server itself (e.g. LemonLDAP::NG under `/auth` in e2e and demo), its public URL cannot
be reached while the server starts. The discovery then uses its internal URL, with the `Host` of its portal, sent with
every request to the SSO:

```json
"credentials": {
  "client": { "id": "linid-im-client" },
  "options": { "authorizationMethod": "body" },
  "http": { "headers": { "host": "linid.localtest.me" } }
},
"discovery": { "issuer": "http://auth" }
```

Without discovery, the endpoints can be given with `credentials.auth` (`tokenHost`, `tokenPath`, `authorizePath`), see
the [documentation of `@fastify/oauth2`](https://github.com/fastify/fastify-oauth2#readme).

### `session`

| Parameter            | Default | Description                                                  |
|----------------------|---------|--------------------------------------------------------------|
| `secret`             | —       | Signs the session cookie. **At least 32 characters**         |
| `cookieName`         | —       | Name of the session cookie                                   |
| `maxAgeMilliseconds` | —       | Lifetime of the session cookie, in milliseconds              |
| `lruCache`           | —       | Options of `lru-cache`, see below                            |
| `secure`             | `true`  | Cookie sent over HTTPS only. Set `false` only for plain HTTP |
| `sameSite`           | `lax`   | `lax` (recommended), `strict` or `none`                      |

### `session.lruCache`

The sessions are stored in server memory in a `LRUCache`, created with these options as is (see the documentation of
`lru-cache`). For example:

| Parameter        | Description                                                                                     |
|------------------|-------------------------------------------------------------------------------------------------|
| `max`            | Maximum number of sessions, the least recently used are deleted beyond                          |
| `ttl`            | Lifetime of a session without request, in milliseconds: usually equal to `maxAgeMilliseconds`   |
| `ttlAutopurge`   | `true` deletes each session as soon as it expires, otherwise only when it is read or evicted    |

### `proxies`

| Parameter           | Default   | Description                                               |
|---------------------|-----------|-----------------------------------------------------------|
| `prefix`            | —         | Public URL prefix                                         |
| `upstream`          | —         | Base URL of the proxied service                           |
| `keepPrefix`        | `false`   | Keeps the prefix in the upstream path                     |
| `rewritePrefix`     | —         | Replaces the prefix with this path in the upstream path   |
| `withAccessToken`   | `false`   | Adds the user's access token, `401` if not logged in      |
