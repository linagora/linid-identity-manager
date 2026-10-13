# UI Server (Backend For Frontend)

## 🎯 Purpose

The **UI server** (`ui/server`, [Fastify](https://fastify.dev/)) serves the SPA, proxies the backend services and
handles the **OIDC authentication** with the SSO (LemonLDAP::NG in the provided environments).

It follows the **Backend For Frontend (BFF)** pattern: the tokens stay on the server, the browser only has a session
cookie.

👉 See also: [`server-config.json`](../configuration/ui/server-config.md) ·
[environment variables](../configuration/ui/server-environment.md)

---

## 🏗️ How it works

```text
Browser ── cookie ──► UI server ── OAuth 2 / OIDC ─────► SSO (LemonLDAP::NG)
                          │
                          └── /backend + Bearer token ─► LinID API
```

---

## 🔗 Routes

| Route              | Behavior                                                                                                                                     |
|--------------------|----------------------------------------------------------------------------------------------------------------------------------------------|
| `/auth/login`      | Sets the `returnTo` (`?returnTo=/path`), `state` and PKCE verifier cookies, redirects to the SSO                                             |
| `/auth/callback`   | Checks the `state`, exchanges the code for the tokens, regenerates the session ID, stores the tokens in the session, redirects to `returnTo` |
| `/auth/me`         | Proxy to the SSO userinfo endpoint (`proxies`, `withAccessToken`): the user claims, or `401`                                                 |
| `/auth/logout`     | Destroys the session, clears the cookie and redirects to the SSO logout                                                                      |
| `/auth/logged-out` | Where the SSO sends the user back after the logout: redirects to `/auth/login`                                                               |

Any other `/auth/*` path is proxied to LemonLDAP::NG.

---

## 🔄 Login

1. The SPA calls `/auth/me`. Without a session: `401`, so it redirects to `/auth/login`.
2. `/auth/login` sets the `returnTo`, `state` and PKCE verifier cookies, then redirects to the SSO. No session is
   created before the login succeeds.
3. The user logs in. The SSO redirects to `/auth/callback?code=…&state=…`.
4. The server checks the `state`, exchanges the code for the tokens (server to server), regenerates the session ID
   (against session fixation) and stores the tokens in the new session.
5. The server redirects to `returnTo` if it is a path of the application (starting with `/`, not `//`), or to `/`.

---

## 🔁 API calls

For `/backend` and `/auth/me` (`withAccessToken`), the UI server:

1. renews the access token if it expires soon (`oidc.refreshMarginMilliseconds`);
2. adds `Authorization: Bearer <access token>` and removes the cookie;
3. forwards the request.

If the SSO does not send a new refresh token or ID token on renewal (allowed by OAuth 2 and OIDC), the previous ones
are kept. If the renewal fails, the session is destroyed and the server answers `401`: the SPA redirects to the login.

---

## 🍪 Session

* Stored in the **server memory** (`@fastify/session`), no database.
* Stored in a `LRUCache`, configured with `session.lruCache`.
* The cookie only holds the **session ID**, signed with `session.secret`.
* The cookie is `HttpOnly`, `Secure` and `SameSite=Lax` by default.
* The session holds the tokens (access, refresh, ID). **They are never sent to the browser.**
* During the login, there is no session yet.

---

## 🔐 Security

* **PKCE** and `state` protect the login.
* The session ID is regenerated after the login.
* A failed login returns `400` `{"message":"Authentication failed"}`. The cause is only logged.
* `SameSite=Lax` blocks the cookie on cross-site `POST`, `PUT`, `PATCH` and `DELETE`.

---
