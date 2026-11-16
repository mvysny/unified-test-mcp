# Beyond Buttons: more component types, faster failures

What the click / get_text / snapshot work left open.

`Q_typed_actions`: a ref resolves to a `UnifiedContainer` (any tag), enough for `click` and
`get_text`. A typed action — `set_value` on a TextField, `check` on a Checkbox — needs the type's
own wrapper: `ComponentTypes` gets a wrapper column (`vaadin-text-field` → `UnifiedTextField`),
and `Refs.resolve` finds the component through it, `find(UnifiedTextField.class).withAttribute(…)`.
For a type with a Dramafinder element (Button, TextField) that filter goes through Dramafinder's
query by reflection (`R_unified_no_enumeration`); check it finds the stamped element. The
`@UnifiedComponent` annotations on the wrappers carry the tag → wrapper table, if hand-listing
the column gets long.

`Q_get_text_name`: `get_text` matches `UnifiedButton.getText()`. A TextField's interesting
thing is its value, not its text — later that's `get_value`/`set_value`, or a per-type `read`
that returns what matters for that type.

`Q_timeout`: a ref that isn't in the latest snapshot, or whose component is gone, fails at once.
What still costs the full 10 s is an action on a component that is there but stays disabled or
hidden: `click` on the snapshot's `Button "Delete" [disabled]` waits for it to be enabled. Fail at
once on a component the snapshot showed disabled, or a `--timeout` option?

`Q_browser_download`: Playwright downloads every browser on its first start, 1.3 GB
(`R_playwright_thread_safety`). With `--browser-executable`, the server could set
`PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1` for itself — but the adapter calls `Playwright.create()`
without options, so the variable has to be in the process environment: the launcher script, or
an upstream option.
