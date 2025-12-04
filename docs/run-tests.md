### Run E2E (integration) tests

To execute the full end-to-end tests, follow these steps:

1. **Prepare the test runner submodule**
   Make sure you have cloned and updated the `e2e-test-runner` submodule within your project.
   Also, ensure that **Node.js** is installed on your machine.

```bash
git submodule update --init --recursive
```

2. **Install dependencies**
   Navigate to the `e2e` directory and run:

```bash
cd e2e
npm install
npm install -g dotenv-cli
```

3. **Start the full application stack**
   Launch your application along with all required dependencies.

You can use Docker Compose to start these services.

```bash
# Build all docker
docker build -f docker/Dockerfile -t gese-vue .
docker build test/e2e -t e2e-test-runner
docker build docker/api-mock -t gese-api

# Start application stack
docker compose -f docker/integration/docker-compose.yml --env-file docker/integration/.env up
```

4. **Run integration tests**
   From the `e2e` directory, run the following command to launch the test runner with the proper environment variables:

To run on environment `integration`:

```bash
# In e2e folder:
dotenv -e ../../docker/integration/.env -- npm run start
# Or start with cypress ui
dotenv -e ../../docker/integration/.env -- npm run start:ui

```

To run on environment `integration`:

```bash
docker run --rm \
  --env-file ./docker/integration/.env \
  --network test-network \
  -v "$(pwd)/tests/features":/app/features \
  e2e-test-runner
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
