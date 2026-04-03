# Environment: Development

This environment is intended for development.
All services are containerized with ports exposed for debugging and test access.

## Quick Start

First generate the necessary certificates and keys:

```bash
# Generate self-signed certificates for Nginx of LemonLDAP::NG
openssl req -x509 -newkey rsa:2048 -keyout docker/dev/resources/llng/ssl/selfsigned.key -out docker/dev/resources/llng/ssl/selfsigned.crt -days 3650 -nodes

# Generate OIDC keys for LemonLDAP::NG
openssl genpkey -algorithm RSA -out docker/dev/resources/llng/conf/oidc.key -pkeyopt rsa_keygen_bits:2048 && openssl pkey -in docker/dev/resources/llng/conf/oidc.key -pubout -out docker/dev/resources/llng/conf/oidc.pub
```

or with the provided Taskfile:

```bash
task setup:certs
```

Then build the Docker image and start the stack:

```bash
# Clean old containers (optional)
docker container prune -f

# Build Docker images
docker build -f docker/llng/Dockerfile -t lemonldap-ng docker/llng/

# Start the stack
docker compose -f docker/dev/docker-compose.yml --env-file docker/dev/.env up
```

## Services

| Service | URL                    | Description          |
| ------- | ---------------------- | -------------------- |
| db      | (internal network)     | Default database     |
| auth    | https://localhost:8080 | LemonLDAP::NG portal |
