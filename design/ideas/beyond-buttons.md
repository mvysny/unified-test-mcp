# Beyond Buttons: more component types, faster failures

What the click / get_text / snapshot work left open.

`Q_browser_locale`: a field's value is its text in the app's locale (`D_values_as_shown`), which
without an `I18NProvider` is the browser's language, and Chromium follows the machine's. A
`--locale` option for the browser context, for an app that follows the browser?

`Q_number_field_display`: a NumberField's `<input>` is `type=number`; does Chromium show it in
the browser's locale (`72,5`) while its `value` stays `72.5`? If so, `value=` isn't what the user
sees there.

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
