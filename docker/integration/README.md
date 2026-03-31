# Environment: Integration

This environment is intended for end-to-end (E2E) testing.
All services are containerized with ports exposed for debugging and test access.

## Quick Start

```bash
# Clean old containers (optional)
docker container prune -f

# Build Docker images
docker build -f api/Dockerfile -t linid-identity-manager-api api/

# Start the stack
docker compose -f docker/integration/docker-compose.yml --env-file docker/integration/.env up
```

## Services

| Service         | URL                    | Description                 |
|-----------------| ---------------------- |-----------------------------|
| db              | (internal network)     | Default database            |
| api             | (internal network)     | LinID Identity Manager API  |
| e2e-test-runner | (internal network)     | Runner to execute e2e tests |
