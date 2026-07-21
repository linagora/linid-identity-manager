# UI Translations

## 🎯 Purpose

This guide explains how to configure **translations and language support** in LinId Identity Manager.

LinId supports **backend and frontend translations**, which are merged to provide a consistent user experience.

---

## 🧩 Translation Types

There are **two main types of translations**:

1. **Backend / Plugin Translations**
2. **Frontend Translations**

---

### 1️⃣ Backend / Plugin Translations

* Used by the **backend and plugins**
* You must indicate the location of translation files via the environment variable:

```bash id="i18n-backend"
export I18N_EXTERNAL_PATH=/path/to/translations
```

* LinId will load all translation files from this path
* Backend services and plugins can then **serve translations to the frontend**

---

### 2️⃣ Frontend Translations

* Defined in the **resources folder** of the frontend:

```
resources/i18n.json
resources/i18n/fr-FR.json
resources/i18n/en-US.json
```

#### i18n.json

```json id="i18n-config"
{
  "languages": [
    "fr-FR",
    "en-US"
  ],
  "locale": "fr-FR"
}
```

* `languages`: list of supported languages
* `locale`: default language

#### Language Files (`fr-FR.json`, `en-US.json`)

* Contain all frontend text strings
* Are merged with backend translations when the frontend loads
* Ensure a **consistent UI for both backend and frontend messages**

> ⚠️ The frontend downloads backend translations and merges them with its own internal translations automatically.

---

## ⚙️ How It Works

1. Backend translations are **loaded from the path specified in `I18N_EXTERNAL_PATH`**
2. Frontend translations are **read from `i18n/*.json`**
3. Frontend merges the two sets of translations:
    * Plugin/backend translations
    * Frontend-specific translations
4. The resulting language bundle is used by the UI dynamically

---

## 🌐 Language Switcher

The header profile menu includes a **language switcher** that lets users change the application language at runtime.

### How the active language is resolved

At startup, the active language is resolved by priority:

1. The **user preference** stored on the server
2. The language **persisted locally** (browser storage) from a previous selection
3. The **default locale** from the configuration

When the user selects a language, the choice is applied immediately to the whole UI and persisted (locally and as a server-side user preference).

### Required resources per language

For every language listed in `languages`, the frontend resources must provide:

* the locale messages file (`i18n/{locale}.json`)
* the translation key `application.language.title` (label of the selector)
* the translation key `application.languages.{locale}` (display name of the language, written in its own language — must be identical across all locale files)
* a flag image served at `icons/{locale}.svg`

If any of these is missing, the language renders with a broken flag image or a raw translation key.

---

## 📝 Best Practices

* Keep backend and plugin translations **modular and organized**
* Always include frontend defaults in `i18n.json`
* Test translation merging by switching the locale in the UI
* Document all translation keys for auditing and maintainability

---

## ➡️ Next Steps

* Configure **UI theming**:
  👉 [theming](theming.md)

* Configure **application settings**:
  👉 [../application/config-files](../application/config-files.md)
