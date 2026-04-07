# UI Theming

## 🎯 Purpose

This guide explains how to **customize the look and feel** of LinId Identity Manager using **design tokens, themes, and
CSS overrides**.

LinId uses **Quasar** as the frontend framework. The UI can be themed globally or per namespace.

---

## 🧩 Theming Overview

There are **three main files** to control theming:

| File              | Purpose                                                           |
|-------------------|-------------------------------------------------------------------|
| `design.json`     | Override Quasar component attributes globally or per UI namespace |
| `theme.json`      | Define colors used throughout the application                     |
| `theme-style.css` | Apply global CSS overrides                                        |

---

## 1️⃣ Design Configuration (`design.json`)

* Defines **component-level defaults**
* Can be applied **globally** or for a **specific namespace / layout**
* Examples of attributes:

    * `dense`, `outline`, `color`, `noCaps`, `align`, `inlineLabel`, etc.

### Example

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

> ⚠️ You can override any Quasar component property globally or in a specific layout/namespace.

---

## 2️⃣ Theme Colors (`theme.json`)

* Defines **primary, secondary, accent, dark, positive, negative, info, warning colors**
* Used by Quasar components that reference these color names

### Example

```json id="theme-example"
{
  "primary": "#1976d2",
  "secondary": "#26a69a",
  "accent": "#9c27b0",
  "dark": "#1d1d1d",
  "positive": "#21ba45",
  "negative": "#c10015",
  "info": "#31ccec",
  "warning": "#f2c037"
}
```

> ⚠️ Colors defined here are referenced in `design.json` via the `color` property.

---

## 3️⃣ CSS Overrides (`theme-style.css`)

* Allows **global CSS customizations**
* Can be used for advanced styling not covered by `design.json`
* Typical use cases:

    * Custom fonts
    * Margins/paddings adjustments
    * Custom animations or transitions

---

## ⚙️ How to Apply Themes

1. Place your `design.json`, `theme.json`, and `theme-style.css` in the **resources folder** of your deployment.
2. LinId UI will **load them automatically** at startup.
3. Changes take effect after **refreshing the application**.

> ⚠️ Namespace-specific overrides in `design.json` take precedence over global defaults.

---

## 📝 Best Practices

* Keep `theme.json` **simple and consistent**
* Use `design.json` for **component behavior adjustments**, not just colors
* Use `theme-style.css` **sparingly** for advanced tweaks
* Test changes in **different layouts and screen sizes**
* Document customizations for future maintenance

---

## ➡️ Next Steps

* Define **claims per application**:
  👉 [authentication/claims](../authentication/claims.md)

* Configure **application settings**:
  👉 [application/config-files](../application/config-files.md)
