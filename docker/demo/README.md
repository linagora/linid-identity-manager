# Environment: Demo

This environment is intended for a user demo.
All services are containerized with ports exposed for debugging and demo access.

## Quick Start

```bash
# Init submodules
git submodule update --init --recursive

# Build Docker images
docker build -t mock-api docker/mock-api/
docker build -f linid-im-api/docker/Dockerfile -t linid-im-api linid-im-api/
docker build -f linid-im-front/docker/Dockerfile -t linid-im-front linid-im-front/
docker build -f linid-im-front-community-plugins/docker/catalog-ui.Dockerfile -t catalog-ui linid-im-front-community-plugins/
docker build -f linid-im-front-community-plugins/docker/module-users.Dockerfile -t module-users linid-im-front-community-plugins/

# Init plugins (download required JAR files)
rm -rf docker/plugins
source docker/demo/.env && sh docker/scripts/init-plugins.sh

# Clean old containers (optional)
docker container prune -f

# Start the stack
docker compose -f docker/demo/docker-compose.yml --env-file docker/demo/.env up
```

## Services

| Service       | URL                    | Description                    |
| ------------- | ---------------------- | ------------------------------ |
| mock-api      | (internal network)     | Mock API for testing           |
| linid-im-api  | (internal network)     | LinID Identity Manager API     |
| linid-im-front| http://localhost:9000  | LinID Identity Manager Frontend|
| catalog-ui    | (internal network)     | Module catalog                 |
| module-users  | (internal network)     | Users module                   |

> **Note:** Most services are only accessible through the Docker network. The frontend
> (`linid-im-front`) acts as a reverse proxy to the API and modules.

## Mock API

The `mock-api` service is an Express.js server that simulates an external API for testing purposes.
It stores data **in memory**, which means:

- Data is reset when the container restarts
- Each test run should start with a fresh container to ensure consistent state
