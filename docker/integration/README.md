# Environment: Integration

This environment is intended for end-to-end (E2E) testing.  
All services are containerized with ports exposed for debugging and test access.

```bash
# init
git submodule update --init --recursive

# build
docker build -f linid-im-api/docker/Dockerfile -t linid-im-api linid-im-api/
docker build -f linid-im-front/docker/Dockerfile -t linid-im-front linid-im-front/
docker build -f linid-im-front-community-plugins/docker/catalog-ui.Dockerfile -t catalog-ui linid-im-front-community-plugins/

# clean
docker compose -f docker/integration/docker-compose.yml down -v

# run
docker compose -f docker/integration/docker-compose.yml --env-file docker/integration/.env up -d
```
