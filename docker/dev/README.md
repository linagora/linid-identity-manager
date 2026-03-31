# Environment: Development

This environment is intended for development.
All services are containerized with ports exposed for debugging and test access.

## Quick Start

```bash
# Clean old containers (optional)
docker container prune -f

# Start the stack
docker compose -f docker/dev/docker-compose.yml --env-file docker/dev/.env up
```

## Services

| Service        | URL                    | Description      |
|----------------| ---------------------- |------------------|
| db             | (internal network)     | Default database |
