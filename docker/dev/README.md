# Environment: Development

This environment is intended for development.
All services are containerized with ports exposed for debugging and test access.

## Quick Start

### Setup

Generate certificates, LemonLDAP::NG required configuration file and start everything with the following commands:

```bash
task setup:dev
task start:dev
```

## Services

| Service | URL                    | Description          |
| ------- | ---------------------- | -------------------- |
| db      | (internal network)     | Default database     |
| auth    | https://localhost:8080 | LemonLDAP::NG portal |
