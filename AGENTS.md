# Unified Test MCP — AGENTS.md

## What this is

An MCP (Model Context Protocol) server that lets an AI agent test a running Vaadin Flow app in
a real browser, in terms of Vaadin components rather than DOM nodes: `Button "Save"`, a text
field by its label, a Grid's rows. The component knowledge comes from the Unified API for
Vaadin UI tests running on Playwright; this project owns the MCP tools on top of it.

## Promises

- **Vaadin components, not DOM.** The agent finds and acts on Button, TextField, Grid by what a user sees on them; never on shadow DOM, CSS selectors or an ARIA tree.
- **An agent drives the app the way a person does.** Every action is a real browser event that goes through the app's real listeners and server round-trip; nothing is set from behind.
- **Saving agent tokens.** What a tool returns is text whose size scales with the components on the screen, not with DOM nodes or pixels.

## Design docs

| File | Owns | Loaded |
|---|---|---|
| `README.md` | the pitch, how to run it | — |
| `AGENTS.md` (this) | promises, the module map, conventions, commands | every turn |
| `design/decisions.md` | why this and not that — `D_` entries, FAQ-shaped | lazy |
| `design/research.md` | what the Unified API, Dramafinder and Playwright actually do — `R_` entries with provenance | lazy |
| `design/ideas/` | ideas not yet acted on, one per file; deleted once implemented or rejected, their lasting parts moved into the files above | lazy |
| doc comments | what one symbol does and why it is shaped so | at the symbol |

Every fact lives in exactly one of these; the others link to it.

## Module map

- The MCP protocol and its transports are [TinyMCPServer](https://github.com/mvysny/tinymcpserver), a separate product this depends on as `com.github.mvysny.tinymcpserver:mcp-server`.
- The Vaadin component knowledge is the [Unified API](https://github.com/parttio/unified-testing-tool), depended on as `org.vaadin.addons:unified-test-playwright`. `unified-testing-tool/` is a gitignored clone of its repository, there to read its source; a change to it goes upstream as a PR.
- `unified-test-mcp` — the MCP server itself: `main()`, the tools, and opening a Unified API page outside JUnit.
- `test-app` — a one-view Vaadin Boot app the tools are tried and tested against.

## Conventions

- **Java 21**, the Unified API's own floor (`R_unified_playwright_packaging`); `--release 21`, not a toolchain, so any installed JDK 21+ builds it and none is downloaded.
- **Gradle, Kotlin DSL; every version in `gradle/libs.versions.toml`.** Vaadin stays on the line the Unified API is built against.
- **A sibling of `../swing-mcp`:** where a tool shape, a ref scheme or an error posture is undecided here, start from what swing-mcp does.

## Commands

- `./gradlew` — clean, build, all tests (the default tasks); needs a JDK 21 or newer.
- `./gradlew :test-app:test --tests "com.vaadin.unifiedtestmcp.testapp.MainViewTest"` — one class; append `.methodName` for one method.
- `./gradlew :test-app:run` — the test app in dev mode on http://localhost:8080.
- `./gradlew :unified-test-mcp:installDist` — the server's launcher, `unified-test-mcp/build/install/unified-test-mcp/bin/unified-test-mcp`, speaking MCP on stdio.
- `design/verify_design_tripwires.sh` — the doc layer.

## Skills this project follows

- **Component-oriented:** self-sufficient components that reach their data directly, no MVC layers; the `cop` skill has the rules.
- **Test app on Vaadin Boot:** a Vaadin Flow app started from a plain `main()` on embedded Jetty, no Spring; the `vaadin-boot-app` skill has the setup.

## Maintenance of this file

Loaded every turn; cap 34 KB. Over it, in this order:
delete what has no home — status, history, class lists, what the code already says; trim
each line to its fact plus one clause and send the explanation home — why →
`design/decisions.md`, how across symbols → `design/architecture.md`, how in one symbol →
its doc comment, what upstream does → `design/research.md`; only then a module's own
`AGENTS.md`, peripheral modules first, never the core. Never paraphrase a lazy entry into a
line here. `design/verify_design_tripwires.sh` checks the cap and the cites.
