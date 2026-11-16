# Environment: Development

This environment is intended for development.
All services are containerized with ports exposed for debugging and test access.

> ⚠️ Not for production: the LemonLDAP::NG configuration (`resources/lmConf-1.template.json`), the `env/*.env` files and
> the seed scripts contain hard-coded, public secrets and test accounts. Replace them before any real deployment, see
> [OIDC configuration](../../docs/configuration/authentication/oidc.md).

## Quick Start

### Setup

Generate certificates, LemonLDAP::NG required configuration file and start everything with the following commands:

```bash
task setup:dev
task start:dev
```

`task setup:dev` is only needed once: it creates the local CA in `docker/certs/` and the shared
`server.crt` used by every HTTPS service, including the Quasar dev server on port 9000. Trust
`docker/certs/ca.crt` once in your browser to stop the certificate warnings
(see [certificates.md](../../docs/configuration/certificates.md)).

## Services

| Service | URL                    | Description          |
| ------- | ---------------------- | -------------------- |
| db      | (internal network)     | Default database     |
| auth    | https://localhost:8080 | LemonLDAP::NG portal |
