# Architecture

How the pieces compose — what no single symbol can say and what would be expensive to overturn:
wiring and dependency direction, the lifecycle / threading / data-flow story, the flows a newcomer
needs, where to start reading. **Normative: the code conforms.** Change this file first, then the
code. Not here: why (`decisions.md` — cite the `D_`), what upstream does (`research.md` — cite the
`R_`), one symbol's behaviour (its doc comment), the module map (`AGENTS.md`).

---

## Wiring

- The server is a process of its own, pointed at the app's URL; the app under test needs no change (`D_separate_process`).
- Dependencies point down: `Main` → `UnifiedTestMCP` (the handler) → one class per tool (`ClickTool`, …) → `UnifiedBrowser` → the Unified API's `PlaywrightAdapter`. The tools see a `UnifiedPage` and the Unified API's wrappers, never Playwright.
- `Main` constructs one `UnifiedBrowser` and one `MCPHandler` over it and serves the handler on stdio; the system test serves the same handler over HTTP instead. The transport is the seam.
- `UnifiedBrowser` opens its page through `JUnitFreeContext`, which stands in for the JUnit extension that normally opens a Unified API page (`D_junit_free_context`).

## Threading

- `UnifiedBrowser` owns one thread, `unified-browser`. Every touch of the page — opening it, every tool's lookup and action, closing it — is a task on that thread; the caller blocks for the result (`R_playwright_thread_safety`).
- Tool calls arrive on whatever thread the transport uses: stdio's read loop, or HTTP's pool. They queue on the browser thread, so two calls never interleave inside the browser.

## Flows

**A tool call** (`click`, say):

1. The transport hands `tools/call` to `MCPHandler`, which calls the tool's class, a TinyMCPServer `ToolFunction`.
2. `ClickTool` submits `page -> button(page, id).click()` through `ToolCall` to `UnifiedBrowser.call`.
3. On the browser thread, the first call ever opens the page: `PlaywrightAdapter.open` launches Playwright and the browser, then the page navigates to the app's start path.
4. The Unified API finds the Button, waits until it is visible and enabled, clicks, and waits for the server round-trip.
5. A failure — a lookup that timed out, an app that isn't up — comes back as a tool error carrying the Unified API's own message, which the model reads.

**Shutdown**: the client closes stdin → `runStdio` returns → `Main` closes `UnifiedBrowser`, which on its thread closes the tab, then `JUnitFreeContext`'s store closes the browser and Playwright.

## Where to start reading

`UnifiedTestMCP` — the tools registered; then one tool, `ClickTool`, a few lines over the Unified API; then `UnifiedBrowser` for the one thread and the lazy open.
