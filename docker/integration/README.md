# Environment: Integration

This environment is intended for end-to-end (E2E) testing.
All services are containerized with ports exposed for debugging and test access.

## Quick Start

```bash
# Init submodules
git submodule update --init --recursive

# Build Docker images
docker build -f linid-im-api/docker/Dockerfile -t linid-im-api linid-im-api/
docker build -f linid-im-front/docker/Dockerfile -t linid-im-front linid-im-front/
docker build -f linid-im-front-community-plugins/docker/catalog-ui.Dockerfile -t catalog-ui linid-im-front-community-plugins/
docker build -f linid-im-front-community-plugins/docker/module-users.Dockerfile -t module-users linid-im-front-community-plugins/

# Init plugins (download required JAR files)
rm -rf docker/integration/resources/plugins
sh docker/integration/scripts/init-plugins.sh

# Clean old containers (optional)
docker container prune -f

# Start the stack
docker compose -f docker/integration/docker-compose.yml --env-file docker/integration/local.env up
```

## Services

| Service       | URL                    | Description                    |
| ------------- | ---------------------- | ------------------------------ |
| mock-api      | http://localhost:3000  | Mock API for testing           |
| linid-im-api  | http://localhost:8080  | LinID Identity Manager API     |
| linid-im-front| http://localhost:9000  | LinID Identity Manager Frontend|
| catalog-ui    | http://localhost:5001  | Module catalog                 |
| module-users  | http://localhost:5002  | Users module                   |

## Mock API

The `mock-api` service is an Express.js server that simulates an external API for testing purposes.
It stores data **in memory**, which means:

- Data is reset when the container restarts
- Each test run should start with a fresh container to ensure consistent state
- Use `task e2e:reset-mock-api` to reset data between test runs

## Running E2E Tests

See [docs/run-tests.md](../../docs/run-tests.md) for detailed instructions on running E2E tests.
