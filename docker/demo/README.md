# Environment: Demo

This environment is intended for showcasing the product to customers.  
All services communicate through an internal Docker network.

```bash
# init
git submodule update --init --recursive

# build
docker build -f linid-im-api/docker/Dockerfile -t linid-im-api linid-im-api/
docker build -f linid-im-front/docker/Dockerfile -t linid-im-front linid-im-front/
docker build -f linid-im-front-community-plugins/docker/catalog-ui.Dockerfile -t catalog-ui linid-im-front-community-plugins/

# clean
docker compose -f docker/demo/docker-compose.yml down -v

# run
docker compose -f docker/demo/docker-compose.yml --env-file docker/demo/.env up -d
```
