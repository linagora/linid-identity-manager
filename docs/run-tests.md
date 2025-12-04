### Run E2E (integration) tests

To execute the full end-to-end tests, follow these steps:

1. **Prepare the test runner submodule**

Make sure you have cloned and updated the `e2e-test-runner` submodule within your project.
Also, ensure that **Node.js** is installed on your machine.

```bash
git submodule update --init --recursive
```

2. **Install dependencies**

Navigate to the `tests/e2e` directory and run:

```bash
cd tests/e2e
npm install
npm install -g dotenv-cli
```

3. **Build docker images**

Build all the docker images of the application stack.

```bash
docker build -f linid-im-api/docker/Dockerfile -t linid-im-api linid-im-api/
docker build -f linid-im-front/docker/Dockerfile -t linid-im-front linid-im-front/
docker build -f linid-im-front-community-plugins/docker/catalog-ui.Dockerfile -t catalog-ui linid-im-front-community-plugins/
```

4. **Run integration tests locally**

Launch your application along with all required dependencies.

You can use Docker Compose to start these services.

```bash
docker compose -f docker/integration/docker-compose.yml --env-file docker/integration/local.env up
```

From the `e2e` directory, run the following command to launch the test runner with the proper environment variables:

To run on environment `integration`:

```bash
# In e2e folder:
dotenv -e ../../docker/integration/local.env -- npm run start
# Or start with cypress ui
dotenv -e ../../docker/integration/local.env -- npm run start:ui

```

5. **Run integration tests on CI**

Launch your application along with all required dependencies.

You can use Docker Compose to start these services.

```bash
docker compose -f docker/integration/docker-compose.yml --env-file docker/integration/.env up
```

To run on environment `integration`:

```bash
docker run --rm \
  --env-file ./docker/integration/.env \
  --env TZ=Europe/Paris \
  --network test-network \
  -v "$(pwd)/tests/features":/app/features \
  vincentmoittie/e2e-test-runner:latest
```

This command sets the environment variables from your `.env` file and starts the test runner.

### Gherkin feature files

All Gherkin scenarios used for E2E tests are located in:

```bash
tests/features
```

Each feature file defines business scenarios using the Gherkin syntax (`Given`, `When`, `Then`) and is linked to
step definitions in the runner documentation.

⚠️ On IntelliJ, to easily read and write tests, please install the **Cucumber.js** plugin!

**Runner Documentation:**

The full documentation for the test runner, including all available step definitions, is available at:
[https://github.com/Zorin95670/e2e-test-runner](https://github.com/Zorin95670/e2e-test-runner)

You can find all usable steps and detailed usage examples on this page.
