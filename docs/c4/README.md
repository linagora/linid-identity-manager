# Architecture diagrams (C4)

[C4 model](https://c4model.com/) diagrams of LinID Identity Manager, written with
[C4-PlantUML](https://github.com/plantuml-stdlib/C4-PlantUML) (pinned to `v2.13.0`).

| File                       | Level | Content                                                                         |
|----------------------------|-------|---------------------------------------------------------------------------------|
| `C1-system-context.puml`   | C1    | LinID, its users and the external systems (LinOpenFactory, SSO, OPA, Superset…) |
| `C2-containers.puml`       | C2    | SPA, UI server (BFF), API, Sync API, OPA, Superset, databases, LinPulsar        |
| `C3-api.puml`              | C3    | Components of the LinID API                                                     |
| `C3-ui.puml`               | C3    | Components of the LinID UI (SPA)                                                |
| `C3-bff.puml`              | C3    | Components of the UI server (BFF)                                               |
| `network-flows.puml`       | —     | Network flows (ports of the e2e environment)                                    |
| `network-flows-table.puml` | —     | Same flows as a table: keep it in sync with `network-flows.puml`                |

Elements and flows of the target architecture are drawn with dashed lines (`future`), optional ones with dotted
lines (`optional`).

## Rendering

The diagrams include C4-PlantUML from GitHub: rendering needs network access.

```bash
plantuml -tsvg docs/c4/*.puml
```

IDE plugins (IntelliJ PlantUML Integration, VS Code PlantUML) also render them.
