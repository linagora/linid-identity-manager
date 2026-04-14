# Environment: E2E

This environment is intended for end-to-end (E2E) testing.
All services are containerized with ports exposed for debugging and test access.

## Quick Start

Generate certificates, LemonLDAP::NG required configuration file, Frontend OIDC configuration file, build images and start everything with the following commands:

```bash
task setup:e2e
task start:e2e
```

## Services

| Service         | URL                             | Description                 |
| --------------- | ------------------------------- | --------------------------- |
| db              | (internal network)              | Default database            |
| api             | (internal network)              | LinID Identity Manager API  |
| ui              | https://linid.localtest.me:9000 | LinID Identity Manager UI   |
| catalog-ui      | (internal network)              | UI components library       |
| auth            | (internal network)              | LemonLDAP::NG portal        |
| e2e-test-runner | (internal network)              | Runner to execute e2e tests |

## Notes

- Inside lmConf-1.template.json we have set `oidcRPMetaDataOptionsAllowPasswordGrant` to `1` to allow password grant for testing purposes, especially for API tests (cf tests/features/api/\*.feature). This should not be used in production environments.
