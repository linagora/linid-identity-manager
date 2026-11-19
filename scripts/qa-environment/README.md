# QA environment

A throw-away LinID stack built from this working tree, for Twake QA or a developer: the services of `docker/e2e`
(PostgreSQL, OPA, LemonLDAP::NG as OIDC provider, lin-pulsar, api, sync-api, ui), with the e2e environment's own env
files and seed, under the compose project `linid-qa`. Needs docker (with compose and BuildKit) and openssl on the host;
Maven, pnpm, keytool and Java run in containers. Superset is not part of it.

```bash
scripts/qa-environment/build.sh   # api, sync-api, ui images (linid-qa-*); once, and when the code changes
scripts/qa-environment/start.sh   # fresh certificates, configuration and data, waits for health
scripts/qa-environment/stop.sh    # deletes the containers, the volumes, the generated files
```

Maven and pnpm caches are BuildKit caches: a rebuild is fast. Image builds use the host network (`QA_BUILD_NETWORK`):
the default BuildKit network stalled on the development machine.

## Reaching it

| What | Address (variable, default) |
| --- | --- |
| Application + OIDC portal (`/auth`) | `https://linid.localtest.me:19320` (`QA_UI_PORT`) |
| API | `https://127.0.0.1:19321` (`QA_API_PORT`), health `/actuator/health` |
| sync-api (mutual TLS) | `https://127.0.0.1:19322` (`QA_SYNC_PORT`) |
| LemonLDAP::NG, http, debugging only | `http://127.0.0.1:19323` (`QA_AUTH_PORT`) |

Accounts: `admin`, `user1`, `user2`, `noaccount`, all with the password `password`. Everything is published on
127.0.0.1 only; the certificates come from a CA generated at each start, so the browser must ignore HTTPS errors.

## Host names

The OIDC login embeds the public address (issuer, redirect URIs, Secure cookies), and the browser, the UI server and the
API all use it. It is `linid.localtest.me:QA_UI_PORT`, rendered into the templates of `docker/e2e/resources` by
`start.sh` (the checked-in templates are untouched; the UI server's port is the only edit of the rendered output, 443
to `QA_UI_PORT`):

- developer's browser: `localtest.me` resolves to 127.0.0.1 on the public DNS, the UI is published there;
- container browser of Twake QA, on the network `linid-qa_default`: the `ui` service carries `linid.localtest.me` as a
  network alias, so it resolves anyway; to be explicit, `--host-resolver-rules=MAP linid.localtest.me:19320 ui:19320`;
  with ignore-https-errors;
- the API and the UI server reach the issuer through the same alias.

For Twake QA: `app_url: https://linid.localtest.me:19320`, `browser_args`
`--host-resolver-rules=MAP linid.localtest.me:19320 ui:19320`, network `linid-qa_default`.

## Limits

The ports are variables, not free: if `QA_UI_PORT` changes, the browser rule changes with it. Data is the e2e seed
(dialog accounts, three applications, groups...). Superset (dashboards) is absent. `stop.sh` leaves `git status` clean.
