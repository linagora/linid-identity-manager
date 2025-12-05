# FAQ

## 1. What is Linid Identity Manager and what can I use it for?

Linid Identity Manager is an identity management application that can be fully customized through modules.
It allows organizations to manage users, groups, and other entities according to their own structure and workflows.

---

## 2. How do I configure the application for my organization?

You can configure the system by defining the data sources for your entities (users, groups, etc.).
You can also personalize each module so it matches your organization’s processes and way of working.

---

## 3. What is the purpose of the modular system and how do modules work?

Each module works like a small application (micro-frontend) that integrates into the main platform.
If needed, you can also create extensions that add extra features to existing modules.

---

## 4. Can I enable or disable specific modules?

Yes. Simply edit the `modules.json` file to define which modules are active in your instance.

---

## 5. How do I connect the application to my data sources?

Data sources are configured in the back-end configuration file.
There, you can choose which back-end plugins to enable and provide their related configuration.

---

## 6. What types of data sources are supported?

For now, only HTTP-based data sources are supported.
In the future, support will be added for databases, LDAP, and additional connectors.

---

## 7. How can I validate or control the data coming from my sources?

Back-end configuration allows you to define validation rules for each entity attribute.
You can specify validation behavior depending on the action (create, update, delete).

---

## 8. How can I customize the interface or design to match my company branding?

You can update the global configuration to define your organization’s color theme, override CSS, and customize translations throughout the application.

---

## 9. How do I run the demo locally using Docker?

All necessary commands are listed in the main README.
Simply follow the steps to initialize submodules, build the images, and start the demo environment.

---

## 10. Where can I find additional documentation?

Each project repository contains its own documentation.
You can find all related repositories listed in the main README of this project.

---

## 11. How do module dependencies work?

Currently, module dependencies are documented in each module’s README.
A more automated dependency-handling system will be introduced in the future.

---

## 12. Can I develop my own custom modules or plugins?

Yes. The `linid-im-front-corelib` repository provides extensive documentation to help you build your own plugins, which you can place in `linid-im-front-community-plugins`.
