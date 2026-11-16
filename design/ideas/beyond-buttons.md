# Beyond Buttons: more component types, navigation, faster failures

What the click / get_text spike left open.

`Q_lookup_type`: every Unified API lookup starts from a component type; there is no "find by
id, whatever it is" (`R_unified_no_enumeration`). The tools assume Button. Next steps, in the
order I'd take them:
1. an optional `type` argument (`"Button"`, `"TextField"`, …) mapped to its wrapper class;
2. without `type`, ask the page for the element's tag (`document.getElementById(id).localName`
   through the escape hatch) and map tag → wrapper (`vaadin-button` → `UnifiedButton`); the
   `@UnifiedComponent` annotations on the wrappers already carry that table.

`Q_get_text_name`: `get_text` matches `UnifiedButton.getText()`. A TextField's interesting
thing is its value, not its text — later that's `get_value`/`set_value`, or a per-type `read`
that returns what matters for that type.

`Q_navigation`: with only `--url` the browser sits on the start page. A `navigate(path)` tool is
the obvious next tool once there's a second view; `UnifiedPage.navigate(String path)` exists.

`Q_timeout`: a missing id costs the full 10 s lookup timeout before the tool fails — slow for an
agent probing, and for `ButtonToolsTest.unknownIdIsToolError`. A `--timeout` option, or a
shorter timeout for reads than for actions (a read of something not there yet is rarer than an
action on something still loading)?

`Q_browser_download`: Playwright downloads every browser on its first start, 1.3 GB
(`R_playwright_thread_safety`). With `--browser-executable`, the server could set
`PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1` for itself — but the adapter calls `Playwright.create()`
without options, so the variable has to be in the process environment: the launcher script, or
an upstream option.

`Q_upstream_entry_point`: open an issue on parttio/unified-testing-tool asking for a JUnit-free
way to open a page (`D_junit_free_context`), with this server as the use case.
