# Beyond Buttons: more component types, faster failures

What the tools so far leave open: the locale, a wait that can't succeed.

`Q_browser_locale`: a field's value is its text in the app's locale (`D_values_as_shown`), which
without an `I18NProvider` is the browser's language, and Chromium follows the machine's. A
`--locale` option for the browser context, for an app that follows the browser? The Unified API
opens the context without options (`R_unified_playwright_packaging`), so it needs a setting
upstream beside `vaadin.testing.playwright.executable`.

`Q_number_field_display`: a NumberField's `<input>` is `type=number`; does Chromium show it in
the browser's locale (`72,5`) while its `value` stays `72.5`? If so, `value=` isn't what the user
sees there.

`Q_timeout`: a ref that isn't in the latest snapshot, or whose component is gone, fails at once.
What still costs the full 10 s is an action on a component that is there but stays disabled or
hidden: `click` on the snapshot's `Button "Delete" [disabled]` waits for it to be enabled.
`set_value` refuses a disabled field at once; `click` the same, or a `--timeout` option? A
disabled MenuBarItem and a Button under an open menu wait the same 10 s (`menus.md`).
