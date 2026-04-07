# Plugins Overview

## 🎯 Purpose

This guide provides an **overview of plugin management** in LinId Identity Manager.

LinId supports **backend and frontend plugins**, each loaded and configured differently.

---

## 🧩 Plugin Types

There are **two main types of plugins**:

1. **Backend Plugins (JARs)**

    * Loaded from the path specified by the environment variable:

```bash id="plugin-loader-path"
export PLUGIN_LOADER_PATH=/path/to/plugins
```

* 🚧 **TODO**: Describe available backend plugin types, expected structure, and configuration

2. **Frontend Plugins**

    * Defined in two files:

| File           | Purpose                                                                                |
|----------------|----------------------------------------------------------------------------------------|
| `remotes.json` | Lists frontend plugins and their remote entry points                                   |
| `modules.json` | Lists configuration files for each frontend module to be placed in the `public` folder |

### Example: `remotes.json`

```json id="remotes-example"
[
  {
    "name": "catalogUI",
    "entry": "http://localhost:5001/mf-manifest.json"
  }
]
```

### Example: `modules.json`

```json id="modules-example"
{
  "modules": []
}
```

> 🚧 **TODO**: Explain how to configure each frontend plugin, link modules to public folder, and usage in the UI

---

## ⚙️ How Plugins Work

* Backend JARs are automatically loaded at startup by the backend plugin loader
* Frontend plugins are **federated modules**: the frontend downloads manifests and merges module configuration
  dynamically
* Both types of plugins can extend **functionality, UI, or integration**

> 🚧 **TODO**: Add step-by-step guide for installing and activating plugins

---

## 📝 Best Practices

* Keep plugins organized in dedicated folders
* Document plugin versions and dependencies
* Test plugins in **development environment** before production
* Maintain separate **backend and frontend plugin registries**

---

## ➡️ Next Steps

* Configure **application settings**:
  👉 [application/config-files](../application/config-files.md)

* Define **claims and policies**:
  👉 [advanced/policies](../../advanced/policies.md)

* Configure **UI / theming / translations**:
  👉 [ui/theming](../ui/theming.md)
  👉 [ui/translations](../ui/translations.md)
