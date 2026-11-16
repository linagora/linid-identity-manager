# Environment: Demo

This environment is intended for demo.
All services are containerized with ports exposed for debugging and test access.

> ⚠️ Not for production: the LemonLDAP::NG configuration (`resources/lmConf-1.template.json`), the `.env` file and the
> seed scripts contain hard-coded, public secrets and test accounts. Replace them before any real deployment, see
> [OIDC configuration](../../docs/configuration/authentication/oidc.md).

## Quick Start

Generate certificates, LemonLDAP::NG required configuration file, Frontend OIDC configuration file, build images and start everything with the following commands:

```bash
task setup:demo
task start:demo
```

## Services

| Service    | URL                             | Description                |
| ---------- | ------------------------------- | -------------------------- |
| db         | (internal network)              | Default database           |
| api        | (internal network)              | LinID Identity Manager API |
| ui         | https://linid.localtest.me:9000 | LinID Identity Manager UI  |
| lin-pulsar | (internal network)              | UI components library      |
| auth       | (internal network)              | LemonLDAP::NG portal       |
