# Unified Test MCP

An MCP (Model Context Protocol) server that lets an AI agent test a running Vaadin Flow app in
a real browser, in terms of Vaadin components rather than DOM nodes: `Button "Save"`, a text
field by its label, a Grid's rows. The component knowledge comes from the Unified API for
Vaadin UI tests running on Playwright; this project owns the MCP tools on top of it.

## Status

A prototype with two tools, Buttons only:

- `click(id)` — clicks the Button with that id; returns once the server has handled the click.
- `get_text(id)` — the Button's caption.

What's next is in [design/ideas/](design/ideas/).

## Building

A JDK 21 or newer. `./gradlew` builds everything and runs the tests.

- `test-app` — a one-button Vaadin app to test against: `./gradlew :test-app:run`, then
  http://localhost:8080.
- `unified-test-mcp` — the MCP server: `./gradlew :unified-test-mcp:installDist`, then register
  it with Claude Code:
  ```bash
  claude mcp add vaadin-test -- "$PWD/unified-test-mcp/build/install/unified-test-mcp/bin/unified-test-mcp"
  ```
  Options: `--url <app url>` (default `http://localhost:8080`), `--browser-executable <path>`
  to use an installed Chromium instead of Playwright's own download, `--headed` to watch the
  browser. The browser opens the app on the first tool call, so start the app first, or at
  least before the first call.

Try it: start `test-app`, start `claude` in this folder, and ask it to click the button `clicker`
twice and tell you its caption.
