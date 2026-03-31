# CONTRIBUTING

## Git conventions

## 🌿 Branch Naming Convention

All branches must follow one of the predefined naming patterns listed below:

| Type        | Pattern                           | Example                        |
| ----------- | --------------------------------- |--------------------------------|
| Main        | `main` or `dev`                   | `main`                         |
| Feature     | `feature/<short-description>`     | `feature/kafka_integration`    |
| Bugfix      | `bugfix/<short-description>`      | `bugfix/null_pointer_handling` |
| Improvement | `improvement/<short-description>` | `improvement/api_docs_format`  |
| Library     | `library/<library-name>`          | `library/jinjava_upgrade`      |
| Hotfix      | `hotfix/<short-description>`      | `hotfix/fix_offset_issue`      |

---

### ✅ Rules

* Use only **lowercase letters**, **numbers**, **dashes (`-`)**, **underscores (`_`)**, and **dots (`.`)**.
* Branch names must be **descriptive** and **concise**.
* Avoid generic names like `fix`, `update`, or `new-feature`.

### 💬 Commit message format

We follow the [Conventional Commits](https://www.conventionalcommits.org) specification:

```
<type>(<scope>): <short summary>
```

**Types**:

- `feat`: A new feature
- `fix`: A bug fix
- `docs`: Documentation only changes
- `style`: Code style changes (formatting, missing semicolons, etc.)
- `refactor`: Code change that neither fixes a bug nor adds a feature
- `perf`: A code change that improves performance
- `test`: Adding or correcting tests
- `security`: A change that addresses a security vulnerability or improves security
- `deprecated`: Marking features, APIs, or components as obsolete (scheduled for removal)
- `chore`: Maintenance, build, dependencies, etc

**Examples**:

```
feat: add kafka producer support
fix(api): correct null pointer in message handler
docs(contributing): add commit format rules
```

We use a **Maven plugin** to automate releases.
Tags and commit messages must follow [Conventional Commits](https://www.conventionalcommits.org/) to correctly trigger
version updates.

For more details on how tags and commits affect the release process, see:
👉 [https://github.com/Zorin95670/semantic-version](https://github.com/Zorin95670/semantic-version)

### 🔏 Commit signing (GPG)

All commits **must be signed** using [GPG](https://docs.gitlab.com/ee/user/project/repository/gpg_signed_commits/):

```bash
git commit -S -m "feat(core): add Kafka support"
```

Your commits will be rejected if not signed.

💡 If you're unsure how to set up commit signing, refer to GitLab's documentation.

## 📚 Functional Documentation and Diagrams

### 📁 `docs` Directory

All functional documentation (business specifications, use cases, etc.) is stored in the following directory:

```bash
/docs
```

Please keep this folder organized and up to date.

### 🖊️ Diagrams with Mermaid

We use [Mermaid](https://mermaid.js.org/) to create diagrams (such as flows, sequences, and architecture visuals).

Diagram source files should be written in Markdown or `.mmd` format and stored inside the `docs` directory.

### ⚙️ Install Mermaid CLI

To generate diagram images from Mermaid files, install the CLI tool:

```bash
npm install -g @mermaid-js/mermaid-cli
```

### 🖼️ Generate PNG Diagrams

Example command to generate a PNG from a Mermaid file:

```bash
mmdc -i docs/X.mmd -o docs/X.svg
```

💡 _Any modification to a Mermaid diagram **must include a manual regeneration** of its corresponding PNG image._

Make sure the updated image file is included in the same commit as the diagram source changes.

## 🧼 Code Style and Checkstyle

To ensure a clean and consistent codebase, we use **Checkstyle** with a strict configuration based on the [Sun Style Guide](http://java.sun.com/j2se/javadoc/writingdoccomments/index.html), along with a few customizations and suppression rules.

### 🔍 Automatic Checks

Checkstyle runs automatically during the Maven `verify` phase:

```bash
mvn verify
```

If your code violates any style rule, the build will fail and the violations will be printed in the console.

### 📜 Ruleset

The main Checkstyle configuration is located at:

```
checkstyle/checkstyle.xml
```

It includes:

* Enforced Sun Java Style conventions
* Mandatory Javadoc for types, methods, and fields
* Naming conventions for classes, methods, variables, and constants
* Rules for whitespace, indentation, and brace placement
* Bans on wildcard and unused imports
* Limits on line length, method size, and parameter count
* Detection of common pitfalls (e.g., magic numbers, empty blocks)
* Required `package-info.java` for each package

You can explore and customize these rules in the configuration file.

### 📄 Required License Header

All source files must start with a license header. This is automatically enforced by [Spotless](https://github.com/diffplug/spotless).

To automatically format and apply the header:

```bash
mvn spotless:apply
```

The header content is defined in:

```
checkstyle/java.header
```

### 💡 IDE Integration

Most IDEs support Checkstyle through plugins.

#### IntelliJ IDEA

1. Install the **Checkstyle** plugin.
2. Go to **Preferences → Tools → Checkstyle**.
3. Add a new configuration pointing to `checkstyle/checkstyle.xml`.
4. Under **Properties**, define the values listed above.
5. Enable Checkstyle for the project.

#### Eclipse

1. Install the **Checkstyle** plugin from the marketplace.
2. Import the `checkstyle.xml` file as a new configuration.
3. Set the required properties in the configuration UI.
4. Enable Checkstyle for the project.

#### VS Code

1. Install the **Checkstyle for Java** extension from the marketplace.
2. Open VS Code settings (File → Preferences → Settings).
3. Search for "checkstyle" and find the Java Checkstyle settings.
4. Set the following configurations:
   - Under "Java > Checkstyle: Configuration": add a new entry with the path `${workspaceFolder}/checkstyle/checkstyle.xml`
5. Alternatively, create a `.vscode/settings.json` file in your project with:
   ```json
   {
     "java.checkstyle.configuration": "${workspaceFolder}/checkstyle/checkstyle.xml"
   }
   ```

### 🚧 Failing Builds

To manually check for style violations before pushing your code:

```bash
mvn checkstyle:check
```

Any violations will cause the build to fail. Make sure to fix them or run `mvn spotless:apply` to automatically apply formatting when possible.

## 🧪 Run Unit Tests

This project uses **JUnit 5**, to run unit tests :

```bash
mvn test
```

## 🛠️ How to release

Development releases are automatically managed
using [Semantic Release](https://github.com/Zorin95670/semantic-version).
When a merge is performed into the `main` branch:

* Semantic Release calculates the next version based on commit messages.
* The pom.xml is updated with this version.
* The changelog is generated.
* The version bump and changelog are committed.
* A new Git tag is created.

⚠️ No manual intervention is required during this process.
