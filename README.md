# Unified Test MCP

An MCP (Model Context Protocol) server that lets an AI agent test a running Vaadin Flow app in
a real browser, in terms of Vaadin components rather than DOM nodes: `Button "Save"`, a text
field by its label, a Grid's rows. The component knowledge comes from the Unified API for
Vaadin UI tests running on Playwright; this project owns the MCP tools on top of it.

## Status

A prototype with four tools:

- `snapshot()` — the Vaadin components on the page, one line each, each with a `ref` the other
  tools take:

  ```
  URL: http://localhost:8080/form
  Title: Form
  - TextField "Name" [ref=1, id=name, value="Alice"]
  - Button "Save" [ref=4] actions: click
  ```
- `navigate(url)` — opens that URL, a full page load; says so when the app has no view there.
  Returns the new snapshot.
- `click(ref)` — clicks the component; returns the snapshot once the server has handled the
  click.
- `get_text(ref)` — a Button's or a Span's text.
- `get_value(ref)` — a field's value, uncut; a ComboBox's item by the label the user sees;
  whether a Checkbox is ticked.

Set ids on the components your tests touch: the snapshot shows them, so what an agent finds
carries straight into a Java test.

What's next is in [design/ideas/](design/ideas/).

## Running

The server is one jar, `unified-test-mcp-<version>.jar`, from the GitHub releases page; it
needs Java 21 or newer. Register it with Claude Code:

```bash
claude mcp add vaadin-test -- java -jar /path/to/unified-test-mcp-<version>.jar
```

Options, after the jar: `--browser-executable <path>` to use an installed Chromium instead of
Playwright's own download, `--headed` to watch the browser. The browser starts on an empty page
and goes where the agent navigates, so tell the agent the app's URL; the app needs to be up only
by then.

## Building

A JDK 21 or newer. `./gradlew` builds everything and runs the tests; the server's jar lands in
`unified-test-mcp/build/libs/unified-test-mcp-<version>.jar`.

`test-app` is a Vaadin app to test against: `./gradlew :test-app:run`, then
http://localhost:8080. The start view has the button `clicker`; the view at `second` has the
button `back`, which returns to the start view; the view at `form` has a few fields and buttons.

Try it: start `test-app`, register the jar you built as above, start `claude`, and ask it to
open http://localhost:8080, click the button `clicker` twice and tell you its text, or to go to
`form` and save.
