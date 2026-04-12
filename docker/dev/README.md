# Environment: Development

This environment is intended for development.
All services are containerized with ports exposed for debugging and test access.

## Quick Start

Generate certificates and start everything with a single command:

```bash
task start:dev
```

## Services

| Service    | URL                    | Description                |
| ---------- | ---------------------- | -------------------------- |
| db         | (internal network)     | Default database           |
| auth       | https://localhost:8080 | LemonLDAP::NG portal       |
