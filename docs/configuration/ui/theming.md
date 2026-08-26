# UI Theming

## 🎯 Purpose

This guide explains how to **customize the look and feel** of LinId Identity Manager using **design tokens, themes, and
CSS overrides**.

LinId uses **Quasar** as the frontend framework. The UI can be themed globally or per namespace.

---

## 🧩 Theming Overview

| Configuration                       | Purpose                                                             |
|-------------------------------------|---------------------------------------------------------------------|
| `themeVariables` (in `config.json`) | Define colors used throughout the application                       |
| `designFiles` (in `config.json`)    | List of design JSON files that override Quasar component attributes |
| `theme-style.css` (separate file)   | Apply global CSS overrides (served from the resources folder)       |

---

## 1️⃣ Design Configuration (in `config.json`)

* Specifies a list of **design JSON files** to be loaded from the resources folder
* Each design file defines **component-level defaults** globally or for a **specific namespace / layout**
* Examples of attributes:
    * `dense`, `outline`, `color`, `noCaps`, `align`, `inlineLabel`, etc.

### Example in `config.json`

```json id="designfiles-example"
{
  "designFiles": [
    "design/default.json",
    "design/base-layout.json",
    "design/accounts.json",
    "design/organizational-units.json"
  ]
}
```

### Example design file (e.g., `design/default.json`)

```json id="design-example"
{
  "default": {
    "q-btn": {
      "dense": true,
      "outline": true,
      "color": "primary",
      "noCaps": true
    },
    "q-tabs": {
      "dense": false,
      "align": "left",
      "noCaps": true,
      "inlineLabel": true,
      "activeBgColor": "primary"
    }
  },
  "base-layout": {
    "header": {
      "q-img": {
        "src": "/toolbarApplicationLogo.svg"
      }
    }
  }
}
```

> ⚠️ Design files must be placed in the `/design` subfolder of your resources directory.
> You can override any Quasar component property globally or in a specific layout/namespace.

---

## 2️⃣ Theme Colors (in `config.json`)

* Defines **primary, secondary, accent, dark, positive, negative, info, warning colors** under the `themeVariables` key
* Used by Quasar components that reference these color names

### Example in `config.json`

```json id="theme-example"
{
  "themeVariables": {
    "primary": "#1976d2",
    "secondary": "#26a69a",
    "accent": "#9c27b0",
    "dark": "#1d1d1d",
    "positive": "#21ba45",
    "negative": "#c10015",
    "info": "#31ccec",
    "warning": "#f2c037"
  }
}
```

> ⚠️ Colors defined here are referenced in design files via the `color` property.

---

## 3️⃣ CSS Overrides (`theme-style.css`)

* Allows **global CSS customizations**
* Can be used for advanced styling not covered by design files
* Typical use cases:
    * Custom fonts
    * Margins/paddings adjustments
    * Custom animations or transitions

---

## ⚙️ How to Apply Themes

**Update `config.json`** with your theme colors under `themeVariables` and list your design files under
   `designFiles`:

> ⚠️ Namespace-specific overrides in design files take precedence over global defaults.

```json
{
  "themeVariables": {
    "primary": "#1976d2",
    "secondary": "#26a69a"
    // ... other colors
  },
  "designFiles": [
    "design/default.json",
    "design/base-layout.json",
    "design/custom.json"
  ]
}
```

---

## 📝 Best Practices

* Keep `themeVariables` in `config.json` **simple and consistent**
* Create separate design JSON files **for different namespaces/pages** (e.g., `design/accounts.json`,
  `design/applications.json`)
* Use design files for **component behavior adjustments**, not just colors
* Use `theme-style.css` **sparingly** for advanced tweaks
* Test changes in **different layouts and screen sizes**
* Document customizations for future maintenance
* Ensure all design files referenced in `config.json` exist in the `/design` resources folder

---

## ➡️ Next Steps

* Define **claims per application**:
  👉 [authentication/claims](../authentication/claims.md)

* Configure **application settings**:
  👉 [application/config-files](../application/config-files.md)
