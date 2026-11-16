# Architecture

How the pieces compose — what no single symbol can say and what would be expensive to overturn:
wiring and dependency direction, the lifecycle / threading / data-flow story, the flows a newcomer
needs, where to start reading. **Normative: the code conforms.** Change this file first, then the
code. Not here: why (`decisions.md` — cite the `D_`), what upstream does (`research.md` — cite the
`R_`), one symbol's behaviour (its doc comment), the module map (`AGENTS.md`).

---

## Wiring

- The server is a process of its own, pointed at the app's URL; the app under test needs no change (`D_separate_process`).
- Dependencies point down: `Main` → `UnifiedTestMCP` (the handler) → one class per tool (`ClickTool`, …) → `Snapshot`, `Refs`, `ComponentTypes` → `UnifiedBrowser` → the Unified API's `PlaywrightAdapter`. The tools see a `UnifiedPage` and the Unified API's wrappers; only `Snapshot` reaches Playwright, through the escape hatch (`D_snapshot_from_javascript`).
- `Main` constructs one `MCPHandler` and serves it on stdio; the system test serves the same handler over HTTP instead. The transport is the seam.
- **One `UnifiedBrowser` per MCP session** (`D_browser_per_session`): the handler creates it when a session starts, as the session's attribute, and closes it when the session closes. Beside it, the session's `Refs`: the refs its latest snapshot showed (`D_refs_not_ids`). A tool finds both through `ToolCall`, so the tools hold no state.
- **The per-type knowledge is one table**, `ComponentTypes`, keyed by tag: the Flow class a line shows, which property names it, the tools made for it.
- `UnifiedBrowser` opens its page through `JUnitFreeContext`, which stands in for the JUnit extension that normally opens a Unified API page (`D_junit_free_context`).

## Threading

- Each `UnifiedBrowser` owns one thread, `unified-browser`. Every touch of the page — opening it, every tool's lookup and action, closing it — is a task on that thread; the caller blocks for the result (`R_playwright_thread_safety`). A session's `Refs` is touched on that thread only.
- Tool calls arrive on whatever thread the transport uses: stdio's read loop, or HTTP's pool. TinyMCPServer runs one session's calls one at a time, and they queue on that session's browser thread anyway; two sessions' calls run side by side, each in its own browser.

## Flows

**A snapshot**: `Snapshot.take` runs one script in the page that walks the light DOM from `<body>`, stamps each component it shows with `data-mcp-ref` (a stamp already there stays, a new one takes the next number from `Refs`), and returns the tree; Java renders a line per component through `ComponentTypes` and replaces the session's `Refs` with the refs it showed.

**A tool call** (`click(ref)`, say):

1. The transport hands `tools/call` to `MCPHandler`, which calls the tool's class, a TinyMCPServer `ToolFunction`.
2. `ClickTool` hands `(page, refs) -> …` to `ToolCall`, which submits it to the calling session's `UnifiedBrowser.call`.
3. On the browser thread, the session's first call opens the page: `PlaywrightAdapter.open` launches Playwright and the browser, on an empty page; only `navigate` sends it anywhere.
4. `Refs.resolve` turns the ref into a Unified API wrapper: by the component's id if the snapshot saw it unique, else by its stamp. A ref not in the latest snapshot, or a component gone from the page, fails right away with what to do next.
5. The Unified API waits until the component is visible and enabled, clicks, and waits for the server round-trip; then a snapshot, which the tool returns (`D_action_returns_snapshot`).
6. A failure — a lookup that timed out, an app that isn't up — comes back as a tool error carrying the Unified API's own message, which the model reads.

**A session's end**: on stdio, the client closes stdin, and `runStdio` closes every session before it returns; over HTTP, the client's DELETE, a 30-minute idle eviction, or the server's `stop()`. Each runs `onSessionClosed` → the session's `UnifiedBrowser.close()`, which on its thread closes the tab, then `JUnitFreeContext`'s store closes the browser and Playwright.

## Where to start reading

`UnifiedTestMCP` — the tools registered and a browser per session; then one tool, `ClickTool`, a few lines over the Unified API; then `Snapshot` and `Refs`, how a page becomes lines and a ref becomes a component again; then `UnifiedBrowser` for the one thread and the lazy open.
