## Run E2E (integration) tests

To execute the full end-to-end tests, follow these steps:

### 1. Prepare the test runner submodule

Make sure you have cloned and updated the `e2e-test-runner` submodule within your project.
Also, ensure that **Node.js** is installed on your machine.

```bash
git submodule update --init --recursive
```

### 2. Install dependencies

Navigate to the `tests/e2e` directory and run:

```bash
cd tests/e2e
npm install
npm install -g dotenv-cli
```

### 3. Build docker images

Build all the docker images of the application stack.

```bash
docker build -f linid-im-api/docker/Dockerfile -t linid-im-api linid-im-api/
docker build -f linid-im-front/docker/Dockerfile -t linid-im-front linid-im-front/
docker build -f linid-im-front-community-plugins/docker/catalog-ui.Dockerfile -t catalog-ui linid-im-front-community-plugins/
docker build -f linid-im-front-community-plugins/docker/module-users.Dockerfile -t module-users linid-im-front-community-plugins/
```

### 4. Download API plugins

Download the required plugins (HTTP Provider Plugin) from Maven Central:

```bash
./docker/scripts/init-plugins.sh
```

This will download the `hpp` (HTTP Provider Plugin) JAR to `docker/plugins/`.

### 5. Run integration tests locally

Launch your application along with all required dependencies using Docker Compose:

```bash
docker compose -f docker/integration/docker-compose.yml --env-file docker/integration/local.env up
```

Then run the tests using Taskfile commands (recommended):

```bash
# Run all E2E tests
task e2e

# Run tests with Cypress UI (interactive mode)
task e2e:ui

# Run a specific test file
task e2e:spec -- ../features/front/Home.feature

# Lint Gherkin feature files
task e2e:lint

# Manually reset mock-api data (done automatically by other tasks)
task e2e:reset-mock-api
```

> **Important:** The `mock-api` container uses in-memory data storage. Test data is automatically reset before each
> test run. If you need to reset manually, use `task e2e:reset-mock-api`.

> **Note:** Taskfile requires the `task` CLI. Install it with one of these methods:
> - **Snap (Ubuntu/Debian):** `sudo snap install task --classic`
> - **Script:** `sh -c "$(curl --location https://taskfile.dev/install.sh)" -- -d -b ~/.local/bin`
> - **Go:** `go install github.com/go-task/task/v3/cmd/task@latest`
>
> See [taskfile.dev/installation](https://taskfile.dev/installation/) for more options.

Alternatively, you can run the tests manually from the `tests/e2e` directory:

```bash
cd tests/e2e
dotenv -e ../../docker/integration/local.env -- npm run start
# Or with Cypress UI
dotenv -e ../../docker/integration/local.env -- npm run start:ui
```

### 6. Run integration tests on CI

Launch the application stack:

```bash
docker compose -f docker/integration/docker-compose.yml --env-file docker/integration/.env up -d
```

Run the tests using Docker:

```bash
docker run --rm \
  --env-file ./docker/integration/.env \
  --env TZ=Europe/Paris \
  --network test-network \
  -v "$(pwd)/tests/features":/app/features \
  vincentmoittie/e2e-test-runner:latest
```

## Gherkin feature files

All Gherkin scenarios used for E2E tests are located in:

```
tests/features/
├── api/                    # API tests
│   ├── Health.feature      # Health check endpoint
│   ├── I18N.feature        # Internationalization endpoints
│   ├── Metadata.feature    # Metadata endpoints
│   └── users/              # Users module API tests
│       ├── ModuleUsers.feature
│       └── ModuleUsersSandbox.feature
└── front/                  # Frontend tests
    ├── Home.feature        # Homepage validation
    └── users/              # Users module frontend tests
        ├── ModuleUsers.feature
        └── ModuleUsersSandbox.feature
```

Each feature file defines business scenarios using the Gherkin syntax (`Given`, `When`, `Then`) and is linked to
step definitions in the runner documentation.

To easily read and write tests in your IDE, install the **Cucumber.js** plugin (IntelliJ) or **Cucumber** extension (VS Code).

## Runner documentation

The full documentation for the test runner, including all available step definitions, is available at:
[https://github.com/Zorin95670/e2e-test-runner](https://github.com/Zorin95670/e2e-test-runner)

You can find all usable steps and detailed usage examples on this page.
