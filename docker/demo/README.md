# Environment: Demo

This environment is intended for demo.
All services are containerized with ports exposed for debugging and test access.

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
| catalog-ui | (internal network)              | UI components library      |
| auth       | (internal network)              | LemonLDAP::NG portal       |
