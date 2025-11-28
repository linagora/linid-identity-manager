# Docker Environments

This directory contains Docker configurations for two distinct environments:

- **Integration**: Used for end-to-end (E2E) testing
- **Demo**: A production-like environment for showcasing the product

## Prerequisites

- Docker and Docker Compose installed
- Git with submodules initialized
- Java 21 and Maven (for building API plugins)

### Initialize Submodules

```bash
git submodule update --init --recursive
```

## Building Images

Before running any environment, you need to build the Docker images for each component.

### Build API Image

```bash
cd linid-im-api
docker build -f docker/Dockerfile -t linid-im-api .
```

### Build API Community Plugins

```bash
cd linid-im-api-community-plugins
mvn clean package -DskipTests

# Copy JAR files to resources/plugins
cp target/*.jar ../docker/integration/resources/plugins/
cp target/*.jar ../docker/demo/resources/plugins/
```

### Build Frontend Image

```bash
cd linid-im-front
docker build -f docker/Dockerfile -t linid-im-front .
```

### Build Catalog UI Image

```bash
cd linid-im-front-community-plugins
docker build -f docker/catalog-ui.Dockerfile -t catalog-ui .
```

---

## Integration Environment

Used for E2E testing with exposed ports for debugging and test access.

### Run Integration Environment

```bash
cd docker/integration
docker compose up -d
```

### Access Points (Integration)

| Service        | URL                     |
|----------------|-------------------------|
| Frontend       | http://localhost:8080   |
| API            | http://localhost:8081   |
| Catalog UI     | http://localhost:5001   |

### Stop Integration Environment

```bash
cd docker/integration
docker compose down
```

### View Logs

```bash
cd docker/integration
docker compose logs -f
```

---

## Demo Environment

Production-like environment for showcasing the product. Uses Nginx as reverse proxy.

### Run Demo Environment

```bash
cd docker/demo
docker compose up -d
```

### Access Points (Demo)

| Service        | URL                          |
|----------------|------------------------------|
| Frontend       | http://localhost/            |
| API            | http://localhost/api/        |
| Catalog UI     | http://localhost/catalog/    |

### Stop Demo Environment

```bash
cd docker/demo
docker compose down
```

### View Logs

```bash
cd docker/demo
docker compose logs -f
```

---

## Configuration

### Environment Variables

Both environments use `.env` files for configuration:

| Variable             | Description                          | Default               |
|----------------------|--------------------------------------|-----------------------|
| `CONFIGURATION_PATH` | Path to configuration files          | `/app/configuration`  |
| `PLUGIN_LOADER_PATH` | Path to plugin JAR files             | `/app/plugins`        |
| `I18N_EXTERNAL_PATH` | Path to external i18n files          | `/app/resources/i18n` |

### Plugins

API plugins are JAR files compiled from `linid-im-api-community-plugins` and placed in:
- `docker/integration/resources/plugins/`
- `docker/demo/resources/plugins/`

### Custom i18n

Add or modify translation files in `resources/i18n/`:
- `en.json` - English translations
- `fr.json` - French translations

---

## Troubleshooting

### Images not found

Ensure all images are built before running:

```bash
docker images | grep -E "linid-im-api|linid-im-front|catalog-ui"
```

### Submodules not initialized

```bash
git submodule update --init --recursive
```

### Plugins not loaded

Check that JAR files are present:

```bash
ls -la docker/integration/resources/plugins/
ls -la docker/demo/resources/plugins/
```

### Container logs

```bash
docker logs linid-im-api
docker logs linid-im-front
docker logs catalog-ui
```

### Clean restart

```bash
cd docker/integration  # or docker/demo
docker compose down -v
docker compose up -d --force-recreate
```
