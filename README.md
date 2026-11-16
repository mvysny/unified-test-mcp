# Unified Test MCP

An MCP (Model Context Protocol) server that lets an AI agent test a running Vaadin Flow app in
a real browser, in terms of Vaadin components rather than DOM nodes: `Button "Save"`, a text
field by its label, a Grid's rows. The component knowledge comes from the Unified API for
Vaadin UI tests running on Playwright; this project owns the MCP tools on top of it.

## Status

A prototype with three tools; the component ones know Buttons only:

- `navigate(path)` — opens the view at that path relative to the app's URL, `""` being the
  start view; a full page load.
- `click(id)` — clicks the Button with that id; returns once the server has handled the click.
- `get_text(id)` — the Button's caption.

What's next is in [design/ideas/](design/ideas/).

## Running

The server is one jar, `unified-test-mcp-<version>.jar`, from the GitHub releases page; it
needs Java 21 or newer. Register it with Claude Code:

```bash
claude mcp add vaadin-test -- java -jar /path/to/unified-test-mcp-<version>.jar
```

Options, after the jar: `--url <app url>` (default `http://localhost:8080`),
`--browser-executable <path>` to use an installed Chromium instead of Playwright's own
download, `--headed` to watch the browser. The browser opens the app on the first tool call, so
start the app first, or at least before the first call.

## Building

A JDK 21 or newer. `./gradlew` builds everything and runs the tests; the server's jar lands in
`unified-test-mcp/build/libs/unified-test-mcp-<version>.jar`.

`test-app` is a two-view Vaadin app to test against: `./gradlew :test-app:run`, then
http://localhost:8080. The start view has the button `clicker`; the view at `second` has the
button `back`, which returns to the start view.

Try it: start `test-app`, register the jar you built as above, start `claude`, and ask it to
click the button `clicker` twice and tell you its caption, or to navigate to `second` and click
`back`.
