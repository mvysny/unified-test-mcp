# First tools: `click(id)` and `get_text(id)` on a one-Button app

The smallest thing that proves the stack end to end: an MCP server boots the Unified API's
Playwright backend outside JUnit, opens a running Vaadin app, clicks a Button found by its id
and reads its text back. The snapshot is on hold (`snapshot.md` beside this file); the agent is
told the id, which is enough for a prototype.

## The tools

Against `test-app`'s `MainView`: one Button, id `clicker`, caption "Click me" → "Clicked 1 times".


```
click(id: string)      → "Clicked Button#clicker"
get_text(id: string)   → "Clicked 1 times"
```

- `click` → `page.find(UnifiedButton.class).withId(id).single().click()`. The Unified API
  waits until the button is visible and enabled, clicks, then waits for the server round-trip,
  so a `get_text` right after it sees the new caption.
- `get_text` → `page.find(UnifiedButton.class).withId(id).single().getText()`: the DOM
  `textContent`, whitespace collapsed (`R_unified_no_enumeration`).
- No match / several matches → an `MCPErrorResponseException` naming the id and what to do,
  as swing-mcp does; the Unified API waits up to its timeout before failing.

`Q_lookup_type`: every Unified API lookup starts from a component type; there is no "find by
id, whatever it is" (`R_unified_no_enumeration`). For the prototype both tools assume Button.
Next steps, in the order I'd take them:
1. an optional `type` argument (`"Button"`, `"TextField"`, …) mapped to its wrapper class;
2. without `type`, ask the page for the element's tag (`document.getElementById(id).localName`
   through the escape hatch) and map tag → wrapper (`vaadin-button` → `UnifiedButton`); the
   `@UnifiedComponent` annotations on the wrappers already carry that table.

`Q_get_text_name`: `get_text` matches `UnifiedButton.getText()`. A TextField's interesting
thing is its value, not its text — later that's `get_value`, or a per-type `read` that returns
what matters for that type. Fine for now.

## Booting the backend outside JUnit

The only entry point is the JUnit extension, and the session class is package-private
(`R_unified_junit_bound`). `Q_bootstrap`, two ways:

- **A. Fake the JUnit context.** Call `new PlaywrightAdapter().open(ctx)` (or the one
  `ServiceLoader` finds) with our own `AdapterContext`: `configuration(key)` reads our settings
  (`vaadin.testing.base-url` = the app's URL), `timeout()` is ours, and `extensionContext()`
  returns a `java.lang.reflect.Proxy` that answers `getRoot()` with itself and `getStore(GLOBAL)`
  with a map-backed `Store` we close at shutdown (that's where the `Playwright` + `Browser`
  live). Wrap the session: `new UnifiedPageImpl(new TestRun(TestingTool.PLAYWRIGHT, session,
  timeout))`. No upstream change, but it leans on an `internal` package and on which
  `ExtensionContext` methods the adapter happens to call today.
- **B. Upstream PR.** A public, JUnit-free factory, e.g.
  `UnifiedPages.open(TestingTool, Map<String,String> settings)` returning an `AutoCloseable`
  `UnifiedPage`, with `AdapterContext.extensionContext()` becoming optional. Clean, and useful
  beyond us (a REPL, a main()-driven smoke test), but waits on the maintainers.

I'd do A for the spike, in one class, and open an issue/PR for B with the spike as the use
case; when B lands, A is deleted.

## Process and transport

`Q_process_model`: the server runs **outside** the app, as its own JVM, pointed at a URL —
unlike swing-mcp, which runs inside the Swing app. The app needs no change, and Playwright
needs its own process anyway. (Inside the app, as a `-javaagent`, would later allow the
Browserless backend too — no browser at all. Not now.)

`Q_transport`: stdio, spawned by Claude Code (`Main` already serves stdio, no tools yet);
`--url http://localhost:8080` names the app. The browser lives as long as the MCP session; it opens the URL lazily on the first tool call
(so a not-yet-running app doesn't fail the server's start). HTTP transport later, if a
long-lived browser across Claude restarts turns out useful.

`Q_navigation`: with only `--url` the browser sits on the start page. A `navigate(path)` tool
is the obvious third tool once there's a second view; `page.navigate(String path)` exists.

`Q_browser`: Playwright's own Chromium download by default (allowed for a project's own
Playwright setup); `--browser-executable /snap/bin/chromium` passes through to
`vaadin.testing.playwright.executable`. `--headed` for watching it work.

## The system test

`Q_system_test_home`: boot `test-app` in the test JVM (Vaadin Boot on a free port), open the
tools against it, call `get_text`, `click`, `get_text`, assert `"Click me"` → `"Clicked 1 times"`
— by calling the tool handlers directly at first, TinyMCPServer's client later. It lives in
`unified-test-mcp`'s tests with `testImplementation(project(":test-app"))`; to check that Vaadin
Boot finds the app's routes and frontend from another module's test classpath.

## To verify in the spike

- Dramafinder's `ButtonElement` query has `withId` and finds `vaadin-button#clicker`
  (reflection, so only a run proves it).
- The proxy `ExtensionContext` is enough for `PlaywrightAdapter.open` — no other method is hit.
- Excluding `vaadin-dev` breaks nothing on the adapter's side.
