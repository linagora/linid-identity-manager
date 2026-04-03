# Environment: Integration

This environment is intended for end-to-end (E2E) testing.
All services are containerized with ports exposed for debugging and test access.

## Quick Start

First generate the necessary certificates and keys:

```bash
# Generate self-signed certificates for Nginx
openssl req -x509 -newkey rsa:2048 -keyout docker/integration/resources/selfsigned.key -out docker/integration/resources/selfsigned.crt -days 3650 -nodes
```

or with the provided Taskfile:

```bash
task setup:certs
```

Then build the Docker images and start the stack:

```bash
# Clean old containers (optional)
docker container prune -f

# Build Docker images
docker build -f api/Dockerfile -t linid-identity-manager-api api/
docker build -f ui/Dockerfile -t linid-identity-manager-ui ui/
docker build -f linid-im-front-community-plugins/docker/catalog-ui.Dockerfile -t catalog-ui linid-im-front-community-plugins/

# Start the stack
docker compose -f docker/integration/docker-compose.yml --env-file docker/integration/.env up
```

## Services

| Service         | URL                | Description                 |
| --------------- | ------------------ | --------------------------- |
| db              | (internal network) | Default database            |
| api             | (internal network) | LinID Identity Manager API  |
| ui              | (internal network) | LinID Identity Manager UI   |
| catalog-ui      | (internal network) | UI components library       |
| e2e-test-runner | (internal network) | Runner to execute e2e tests |
