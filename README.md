# Unified Test MCP

An MCP (Model Context Protocol) server that lets an AI agent test a running Vaadin Flow app in
a real browser, in terms of Vaadin components rather than DOM nodes: `Button "Save"`, a text
field by its label, a Grid's rows. The component knowledge comes from the Unified API for
Vaadin UI tests running on Playwright; this project owns the MCP tools on top of it.

## Status

Project structure only: the server starts but offers no tools yet. The first prototype (click
a Button by its id, read its text) is sketched in
[design/ideas/first-tools.md](design/ideas/first-tools.md).

## Building

A JDK 21 or newer. `./gradlew` builds everything and runs the tests.

- `test-app` — a one-button Vaadin app to test against: `./gradlew :test-app:run`, then
  http://localhost:8080.
- `unified-test-mcp` — the MCP server: `./gradlew :unified-test-mcp:installDist`, then register
  it with Claude Code:
  ```bash
  claude mcp add vaadin-test -- "$PWD/unified-test-mcp/build/install/unified-test-mcp/bin/unified-test-mcp"
  ```
