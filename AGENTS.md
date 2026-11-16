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
| `RELEASING.md` | how a maintainer cuts a release | — |
| `AGENTS.md` (this) | promises, the module map, conventions, commands | every turn |
| `design/architecture.md` | how the pieces compose — wiring, the browser thread, a tool call's flow; normative | lazy |
| `design/decisions.md` | why this and not that — `D_` entries, FAQ-shaped | lazy |
| `design/research.md` | what the Unified API, Dramafinder and Playwright actually do — `R_` entries with provenance | lazy |
| `design/ideas/` | ideas not yet acted on, one per file; deleted once implemented or rejected, their lasting parts moved into the files above | lazy |
| doc comments | what one symbol does and why it is shaped so | at the symbol |

Every fact lives in exactly one of these; the others link to it.

## Invariants

- **Every Playwright call runs on `UnifiedBrowser`'s thread**, through `UnifiedBrowser.call`; Playwright for Java is not thread-safe, and a call from a transport thread corrupts the session. The threading is in `design/architecture.md`.
- **Every tool reaches the browser through `ToolCall`.** A lookup that times out throws `AssertionError`, which TinyMCPServer does not catch; `ToolCall` turns it into a tool error the model reads.
- **The tools use the Unified API, not Playwright**; its escape hatch only for what no wrapper offers. See `D_unified_api_backend`.

## Module map

- The MCP protocol and its transports are [TinyMCPServer](https://github.com/mvysny/tinymcpserver), a separate product this depends on as `com.github.mvysny.tinymcpserver:mcp-server`.
- The Vaadin component knowledge is the [Unified API](https://github.com/parttio/unified-testing-tool), depended on as `org.vaadin.addons:unified-test-playwright`. `unified-testing-tool/` is a gitignored clone of its repository, there to read its source; a change to it goes upstream as a PR.
- `unified-test-mcp` — the MCP server itself: `main()`, the tools, and opening a Unified API page outside JUnit.
- `test-app` — a two-view Vaadin Boot app the tools are tried and tested against.

## Conventions

- **Java 21**, the Unified API's own floor (`R_unified_playwright_packaging`); `--release 21`, not a toolchain, so any installed JDK 21+ builds it and none is downloaded.
- **Gradle, Kotlin DSL; every version in `gradle/libs.versions.toml`.** Vaadin stays on the line the Unified API is built against.
- **Tests: JUnit; a tool is tested end to end** — `test-app` booted in the test JVM, a real browser, the tool called over MCP's HTTP transport (`ToolsTest`). No mocks of the Unified API or Playwright.
- **Diagnostics go to `java.util.logging`**, never `System.out`: on stdio, stdout is the protocol.
- **One class per tool**, `<Name>Tool implements ToolFunction` with its `DESCRIPTOR`, registered in `UnifiedTestMCP`; no base class — shared steps are plain functions (`ToolCall`, `ComponentLookup`), per the `cop` skill.
- **A tool failure is an `MCPErrorResponseException`** whose message says what was looked for; the Unified API's own message does that already.
- **Released as one jar on GitHub Releases, never Maven Central**, every dependency and every OS's Playwright driver inside; steps in `RELEASING.md`, why in `D_single_jar_release`.
- **A sibling of `../swing-mcp`:** where a tool shape, a ref scheme or an error posture is undecided here, start from what swing-mcp does.
- **Designing a tool, read swing-mcp's nearest one first** — its description and input schema in `swing-mcp/src/main/java/com/vaadin/swingmcp/tools/SwingTools.java`, its errors in `mcp/tools/Swing*Tool.java`: polished wording and tool-design tips. Inspiration, not a port: keep what fits a browser and the Unified API.

## Commands

- `./gradlew` — clean, build, all tests (the default tasks); needs a JDK 21 or newer.
- `./gradlew :unified-test-mcp:test --tests "com.vaadin.unifiedtestmcp.ToolsTest"` — one class; append `.methodName` for one method. The first run downloads Playwright's browsers (`R_playwright_thread_safety`).
- `./gradlew :test-app:run` — the test app in dev mode on http://localhost:8080.
- `./gradlew :unified-test-mcp:shadowJar` — the server's jar, `unified-test-mcp/build/libs/unified-test-mcp-<version>.jar`; `java -jar` it to speak MCP on stdio.
- `design/verify_design_tripwires.sh` — the doc layer.
- CI: GitHub Actions, `.github/workflows/gradle.yml` — `./gradlew` on JDK 21 and 25, and the tripwires in a job of their own.

## Skills this project follows

- **Component-oriented:** self-sufficient components that reach their data directly, no MVC layers; the `cop` skill has the rules.
- **Javadoc carries the contract, not the narrative:** an example over an algorithm, no history, no referrer lists; the `writing-javadoc` skill has the rules.
- **Test app on Vaadin Boot:** a Vaadin Flow app started from a plain `main()` on embedded Jetty, no Spring; the `vaadin-boot-app` skill has the setup.

## Maintenance of this file

Loaded every turn; cap 34 KB. Over it, in this order:
delete what has no home — status, history, class lists, what the code already says; trim
each line to its fact plus one clause and send the explanation home — why →
`design/decisions.md`, how across symbols → `design/architecture.md`, how in one symbol →
its doc comment, what upstream does → `design/research.md`; only then a module's own
`AGENTS.md`, peripheral modules first, never the core. Never paraphrase a lazy entry into a
line here. `design/verify_design_tripwires.sh` checks the cap and the cites.
