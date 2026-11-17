# Beyond Buttons: more component types, faster failures

What the click / get_text / snapshot work left open.

`Q_typed_actions`: a typed action — `set_value` on a TextField, `check` on a Checkbox — takes the
type's own wrapper, `refs.resolve(page, ref, UnifiedTextField.class)`, as `get_value` does for a
ComboBox. Which wrapper fits is the tag's: `ComponentTypes` gets a wrapper column then, or reads the
`@UnifiedComponent` annotations on the wrappers if hand-listing it gets long.

`Q_timeout`: a ref that isn't in the latest snapshot, or whose component is gone, fails at once.
What still costs the full 10 s is an action on a component that is there but stays disabled or
hidden: `click` on the snapshot's `Button "Delete" [disabled]` waits for it to be enabled. Fail at
once on a component the snapshot showed disabled, or a `--timeout` option?

`Q_browser_download`: Playwright downloads every browser on its first start, 1.3 GB
(`R_playwright_thread_safety`). With `--browser-executable`, the server could set
`PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1` for itself — but the adapter calls `Playwright.create()`
without options, so the variable has to be in the process environment: the launcher script, or
an upstream option.

`Q_rich_text_editor`: RichTextEditor (commercial, not in `vaadin-core`) is a field whose `value`
property is its Delta JSON, while Flow's value is HTML. Show its text, as the user reads it? Not
until test-app can run it.
