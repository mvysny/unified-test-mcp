# Beyond Buttons: more component types, faster failures

What the click / get_text / snapshot work left open.

`Q_custom_values`: a ComboBox with `allowCustomValue` takes a text that isn't among its items;
`set_value` refuses one as it would any label the field doesn't offer. Type it in and press Enter
when the ComboBox allows it, read back as for any other value?

`Q_typed_dates`: `set_value` takes a DatePicker's date as ISO, `"2026-11-16"`, and hands the
Unified API a `LocalDate`; it refuses `"16.11.2026"`. An agent testing the picker's own parser
would want to type the text as a user does, in the app's locale (`UnifiedDatePicker.typeText`).

`Q_timeout`: a ref that isn't in the latest snapshot, or whose component is gone, fails at once.
What still costs the full 10 s is an action on a component that is there but stays disabled or
hidden: `click` on the snapshot's `Button "Delete" [disabled]` waits for it to be enabled.
`set_value` refuses a disabled field at once; `click` the same, or a `--timeout` option?

`Q_browser_download`: Playwright downloads every browser on its first start, 1.3 GB
(`R_playwright_thread_safety`). With `--browser-executable`, the server could set
`PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1` for itself — but the adapter calls `Playwright.create()`
without options, so the variable has to be in the process environment: the launcher script, or
an upstream option.

`Q_rich_text_editor`: RichTextEditor (commercial, not in `vaadin-core`) is a field whose `value`
property is its Delta JSON, while Flow's value is HTML. Show its text, as the user reads it? Not
until test-app can run it.
