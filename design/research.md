# Research — the Unified API for Vaadin UI tests, on Playwright through Dramafinder

What the things we don't own actually do. About *them*, never us: a sentence starting "we chose"
is a `D_`. `## R_<slug> — <title>`, one claim per bullet, one provenance marker per claim —
**[docs]**, **[src]**, **[verified <date>, <version>]**, **[unverified]** (a hypothesis; a design
built on it says so). A claim is earned by its provenance, or by having cost real work to find
out. Checked against unified-testing-tool 0.1.0-alpha2 (`main` at `76ab776`), Dramafinder 2.0.2,
Vaadin 25.3.5; a version-sensitive claim names the version it was seen on. Cite by slug,
`R_<slug>`, never by position; `grep '^## R_' design/research.md` is the index. When you have
written an entry, re-read it against the one above for consistency; a marker already says where
a claim came from, so the prose doesn't repeat it.

---

## R_unified_junit_bound — Unified API: a page is opened only by its JUnit extension

- A `UnifiedPage` is made only by `UnifiedExtension.resolveParameter`: it loads the
  `TestingToolAdapter` for the run's tool through `ServiceLoader`, calls
  `adapter.open(AdapterContext)`, and closes the returned `AdapterSession` in `afterEach`. **[src]**
- `AdapterContext.extensionContext()` returns a JUnit `ExtensionContext`. `PlaywrightAdapter.open`
  keeps one `Playwright` and one `Browser` per JVM in its root store
  (`getRoot().getStore(GLOBAL).computeIfAbsent(...)`), which JUnit closes at the end of the run. **[src]**
- Each `open` makes a fresh `BrowserContext` and `Page`. The session class, `PlaywrightSession`,
  is package-private: only the adapter can make one. **[src]**
- `UnifiedPageImpl` and `TestRun` are public, in package `com.vaadin.testing.unified.internal`;
  `new UnifiedPageImpl(new TestRun(TestingTool.PLAYWRIGHT, session, timeout))` wraps any
  `AdapterSession`. **[src]**
- The base URL is Spring's port when `spring-test` is on the classpath, else the
  `vaadin.testing.base-url` setting, else `http://localhost:8080`; settings come through
  `AdapterContext.configuration(key)`, which the extension backs with JUnit configuration
  parameters and system properties. **[src]**
- The test-user login (`LoginSupport`) is skipped when `unified-test-spring` is not on the
  classpath. **[src]**
- Of the `ExtensionContext`, opening a page and a whole click / read session call only
  `getRoot()` and `getStore(GLOBAL)`. **[verified 2026-11-16, 0.1.0-alpha2]**
- Of the store, the adapter calls only `computeIfAbsent(key, creator, type)`. **[src]**
- A lookup that never singles out a component throws `AssertionError`, not an exception, after
  the timeout: `No component matches page.find(UnifiedButton).withId("x").single() (waited
  10000 ms)`; several matches read `N components match …, expected exactly one`. **[src]**

## R_unified_no_enumeration — Unified API: every lookup starts from a component type

- `Scope` offers `find(wrapperType)` and `findComponent(flowComponentClass)` only, and the
  adapter's `QuerySpec` carries exactly one `componentType`; no call lists every component on a
  page, and none finds by id alone. **[src]**
- On Playwright, a type with a Dramafinder element (e.g. `Button`) is found through Dramafinder's
  `ElementQueries.find`; any other type through a CSS selector built from its `@Tag` or
  `@UnifiedComponent(tag, selector)`; a type with neither fails. **[src]**
- On the Dramafinder path the filters map to Dramafinder query methods by reflection: id →
  `withId`, text → `withText` / `withTextContaining`, label → `withLabel`, attribute →
  `withAttribute`; a Dramafinder query lacking the method fails with
  `UnsupportedOperationException`. **[src]**
- Dramafinder's Button query has `withId`: `page.find(UnifiedButton.class).withId("clicker")`
  finds `<vaadin-button id="clicker">`. **[verified 2026-11-16, Dramafinder 2.0.2]**
- Both paths drop matches inside the shadow DOM of a `vaadin-*` or `copilot-*` host — the dev
  tools' own components (`BrowserQueries.APP_CONTENT`). **[src]**
- `UnifiedElement.findChildren` / `findDescendants(tag)` walk light DOM only; in a browser that
  includes the light DOM web components add themselves, such as a text field's `input` and
  `label`. **[docs]**
- `UnifiedButton.getText()` is `element().getText()`: the DOM `textContent`, whitespace collapsed
  and trimmed. **[src]**
- Escape hatch: `UnifiedPage.unwrap(com.microsoft.playwright.Page.class)` returns the session's
  Playwright `Page`; a wrapper unwraps to its Dramafinder element or a `Locator`. **[src]**
- `UnifiedContainer` is bound to any tag (`@UnifiedComponent(tag = "*")`): `page.find(UnifiedContainer.class)`
  with `withAttribute(name, value)` or `withId(id)` finds an element of any type by a CSS selector,
  `*[data-mcp-ref="3"]`. **[verified 2026-11-16, unified-test-playwright 0.1.0-alpha2]**
- `Query.exists()` reads the page once, without waiting; `single()` waits, on first use, until
  exactly one matches. **[src]**

## R_unified_lazy_elements — Unified API: a wrapper holds its query, not an element

- A wrapper such as `UnifiedButton` sits on a `LazyElement`, which keeps the lookup (component
  type, filters) and, after `all().get(i)`, an index; it holds no element. Every read and action
  runs the lookup again, waiting until exactly one component matches (`single()`) or the indexed
  one exists. **[src]**
- So a wrapper never goes stale the way a Selenium `WebElement` or TestBench's `ButtonElement`
  does: when Vaadin re-renders a component, the next call finds the new element. An adapter
  that loses an element mid-call throws `StaleElementException`, and the core finds the element
  again and retries, within the timeout (`StaleRetry`). **[src]**
- The flip side: a wrapper names *whatever matches its lookup now*. One found by index, as
  `all().get(2)`, is the third match at the time of each call, a different component once one
  is added before it; one found by id stays the component with that id. **[src]**
- On Playwright each match is a `Locator` built from the query, `candidates.nth(i)` among them —
  a selector re-run on every use, never a pinned DOM node. **[src]**
- Filters query attributes, not JavaScript properties: `withAttribute(name, value)`, `withId`,
  `withTestId`, `withClassName`. A value set only as a JS property on an element can't be found
  without the escape hatch. **[src]**

## R_unified_playwright_packaging — unified-test-playwright as a dependency

- Compiled for Java 21 (`maven.compiler.release`) against the Vaadin 25.3.5 BOM and Dramafinder
  2.0.2. **[src]**
- `unified-test-api` depends on `junit-jupiter-api` at compile scope and on `vaadin-core` as
  `provided`: a consumer brings Vaadin itself. **[src]**
- `unified-test-playwright` depends on `vaadin-dev` at compile scope, so that an app under test
  in the same JVM runs in dev mode. **[src]**
- `vaadin.testing.playwright.executable` launches the given browser binary, with
  `--no-sandbox`, instead of Playwright's own download; `vaadin.testing.playwright.browser`
  picks `chromium` (default), `firefox` or `webkit`; `vaadin.testing.playwright.headless`
  defaults to `true`. **[src]**
- The browser context is `browser.newContext()` without options: no setting reaches its locale,
  timezone or viewport. **[src, unified-testing-tool `6c15e62`]**
- Excluding `vaadin-dev` from it breaks nothing for an app that runs in another JVM.
  **[verified 2026-11-16, 0.1.0-alpha2]**

## R_playwright_thread_safety — Playwright for Java: threads and browser downloads

- Playwright for Java is not thread-safe: every method of `Playwright` and of every object made
  from it must be called on the thread that created the `Playwright`. **[docs]**
- `Playwright.create()` without options downloads every browser it supports on first use —
  Chromium, Chromium headless shell, Firefox, WebKit, and ffmpeg — into `~/.cache/ms-playwright`,
  1.3 GB unpacked. **[verified 2026-11-16, Playwright 1.63.0]**
- `PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1` in the environment skips that download. **[docs]**
- The download is decided once per JVM: `Playwright.create()` goes through
  `Driver.ensureDriverInstalled(env, true)`, a singleton whose first call extracts the driver and
  installs the browsers or not; a later call reuses it, whatever it passes. So
  `Driver.ensureDriverInstalled(Map.of(), false)`, called first, skips the download for every
  `Playwright` after it. **[src, Playwright 1.63.0; verified 2026-11-18, an empty
  `PLAYWRIGHT_BROWSERS_PATH`]**
- The driver's `install --only-shell chromium` fetches Chromium's headless shell (114 MiB) and
  ffmpeg, 266 MB unpacked; a headless Chromium launches with nothing else.
  `--no-shell chromium` fetches Chromium itself (187 MiB) instead; `--no-progress` leaves one line
  per download. **[verified 2026-11-18, Playwright 1.63.0]**
- The download runs the driver's `install` with stdout inherited: the JVM's file descriptor 1,
  which `System.setOut` doesn't move. **[src, Playwright 1.63.0]**
- So on stdio, a first start's progress bars land in the protocol between the JSON-RPC lines, with
  `--browser-executable` too, and the first tool call, which opens the session's browser, answers
  only once the download is done. **[verified 2026-11-18, Playwright 1.63.0, an empty
  `PLAYWRIGHT_BROWSERS_PATH`]**
- `Playwright.CreateOptions.setEnv` reaches that check before the process environment does, so
  `PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD` there skips the download too. **[src, Playwright 1.63.0]**
- Playwright's maintainers keep both as intended: every browser on `create()`, a narrower set
  installed by hand behind `PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD` (playwright-java #1989); the driver's
  output on the process's own stdout and stderr (#1149), no logging framework (#1418).
  **[docs, issue tracker, 2026-11-18]**
- The download's host check warns about libraries WebKit lacks (`libavif16`, `libmanette-0.2-0`
  on Ubuntu 26.04); Chromium runs without them. **[verified 2026-11-16, Playwright 1.63.0]**

## R_playwright_driver_bundle — Playwright for Java: the driver jar

- `com.microsoft.playwright:driver-bundle` is 194 MB: Playwright's Node.js driver once per
  platform — `linux`, `linux-arm64`, `mac`, `mac-arm64`, `win32_x64` — under `driver/` in the
  jar. Every other dependency of `unified-test-mcp` together is about 15 MB.
  **[verified 2026-11-16, Playwright 1.63.0]**
- On start, the driver is copied out of the jar into a `playwright-java-*` folder under
  `java.io.tmpdir` and run from there; this works the same from a jar with every dependency
  merged in. **[verified 2026-11-16, Playwright 1.63.0]**

## R_vaadin_boot_jar_webapp — Vaadin Boot serving `webapp/` from a jar

- When the app's `webapp/` comes from a jar on the classpath — a Gradle project dependency, as in
  `unified-test-mcp`'s tests — `VaadinBoot.stop()` logs `stop() failed:
  java.nio.file.ClosedFileSystemException` from Jetty's `WebInfConfiguration.deconfigure`; the
  server has stopped anyway. **[verified 2026-11-16, Vaadin Boot 13.7]**

## R_flow_foreign_attributes — Flow and an attribute the browser sets on a component's element

Tried on `test-app` in dev mode: `data-mcp-ref` set by script on a `vaadin-button` and its
`vaadin-vertical-layout`, then real clicks.

- The attribute survives a server round-trip that changes the component: on a text change the
  server's response clears the button's children and splices a new text node in, and keeps the
  element, and the attribute on it. **[verified 2026-11-16, Vaadin 25.3.5]**
- Flow doesn't send the attribute to the server: a click's UIDL request carries the event's data
  only. **[verified 2026-11-16, Vaadin 25.3.5]**
- Navigating to another view and back, by `UI.navigate` from a click, builds new elements: no
  attribute survives anywhere on the page. **[verified 2026-11-16, Vaadin 25.3.5]**

## R_flow_item_keys — Flow's item fields in the browser: a key, and the label beside it

- A ComboBox's, RadioButtonGroup's and Select's `value` property is Flow's key for the item, `"1"`,
  `"2"`, from its key mapper; the item itself never reaches the browser. With an item label
  generator: ComboBox Czechia → `value="1"`. **[verified 2026-11-16, Vaadin 25.3.5]**
- The label the generator gave is in the page beside the key: a ComboBox's
  `selectedItem[itemLabelPath]`, a MultiSelectComboBox's `selectedItems`, the checked
  `<vaadin-radio-button>`'s `<label>`, the text of a Select's `<vaadin-select-value-button>`; all
  light DOM. **[verified 2026-11-16, Vaadin 25.3.5]**
- The Unified API names an item by that label on every backend: `UnifiedComboBox.getSelectedItem()`,
  `selectItem(label)`. Playwright reads `selectedItem[itemLabelPath]` through Dramafinder's
  `ComboBoxElement.getSelectedItemLabel()`; Browserless applies the component's
  `ItemLabelGenerator` to `getValue()` on the server. **[src]**
- Dramafinder's `ComboBoxElement.getValue()` reads the input's text instead: a typed filter, or a
  custom value no item matches, shows there while `getSelectedItemLabel()` is `null`. **[src]**
- The Unified API has no wrapper for a Select (Dramafinder has `SelectElement`).
  **[src, unified-testing-tool `76ab776`]**
- `find(UnifiedComboBox.class).withAttribute(name, value)` finds a stamped element through
  Dramafinder's query, as it does for the RadioButtonGroup and the MultiSelectComboBox.
  **[verified 2026-11-16, unified-test-playwright 0.1.0-alpha2]**

## R_flow_field_states — Flow's fields in the browser: value, disabled, read-only

Observed on test-app's `fields` view, every field of `vaadin-core`.

- A text or number field's `value` is a string as typed, `"42"`; a BigDecimalField's has the
  locale's separator, `"9,90"` in `fi` (Flow's formatter puts it in, `_decimalSeparator`). A
  DatePicker's is ISO, `"2000-01-31"`, a DateTimePicker's `"2026-11-17T10:00"`, while their
  light-DOM `<input>` (`inputElement`) shows the locale's text, `31.1.2000`; a TimePicker in `fi`
  shows `7.30`. A DateTimePicker's halves are a DatePicker and a TimePicker in its light DOM, slots
  `date-picker` and `time-picker`, each with its own `<input>`. **[verified 2026-11-17, Vaadin 25.3.5]**
- Without an `I18NProvider`, a UI's locale is the browser's language, so these texts differ
  between machines; test-app sets `fi-FI` in a `UIInitListener`. **[verified 2026-11-17, Vaadin 25.3.5]**
- Text a DatePicker can't parse, typed and committed with Enter, stays in its `<input>` while its
  `value` is `""` and it shows invalid; the same for a DateTimePicker with one half filled.
  **[verified 2026-11-17, Vaadin 25.3.5]**
- Dramafinder's `DatePickerElement.setValue(LocalDate)` and `TimePickerElement.setValue(LocalTime)`
  set the `value` property from behind; `setValue(String)` fills the `<input>` and presses Enter.
  **[src, Dramafinder 2.0.1]**
- A Checkbox's and a Switch's `value` is `"on"`; `checked` holds the state. **[verified 2026-11-17, Vaadin 25.3.5]**
- A CheckboxGroup's `value` is Flow's keys, `["1","3"]`; the ticked `<vaadin-checkbox>` children
  carry the `checked` attribute and their label as text. **[verified 2026-11-17, Vaadin 25.3.5]**
- ListBox and MultiSelectListBox are both `<vaadin-list-box>`, the second with `multiple`. The
  selection is an index into `items`: `selected`, `1`; `selectedValues`, `[0,2]`.
  **[verified 2026-11-17, Vaadin 25.3.5]**
- A disabled single-select ListBox keeps `selected`, but the `<vaadin-item>` there loses its
  `selected` attribute; a disabled MultiSelectListBox's items keep theirs.
  **[verified 2026-11-17, Vaadin 25.3.5]**
- IntegerSlider and DecimalSlider are `<vaadin-slider>`, `value` a number, `30`; the range sliders
  `<vaadin-range-slider>`, `value` `[8,17]`; each value also in a light-DOM `<input>`.
  **[verified 2026-11-17, Vaadin 25.3.5]**
- A CustomField's `value` is its inner fields' values joined by a tab, `"+420\t123456"`; Flow's
  value stays on the server. **[verified 2026-11-17, Vaadin 25.3.5]**
- `setEnabled(false)` sets the `disabled` property and attribute; on a CustomField the attribute
  only. **[verified 2026-11-17, Vaadin 25.3.5]**
- A disabled layout disables every field inside it the same way, the property and attribute on
  each. **[verified 2026-11-17, Vaadin 25.3.5]**
- `setReadOnly(true)` sets the `readonly` property on every field, and the attribute on all but
  ListBox and CustomField, whose web components have no `readonly`.
  **[verified 2026-11-17, Vaadin 25.3.5]**
- A read-only CustomField leaves its inner fields editable: the app's subclass has to pass
  `setReadOnly` on to them. **[verified 2026-11-17, Vaadin 25.3.5]**
- A read-only RadioButtonGroup disables its unchecked buttons (`RadioButtonGroup.updateEnabled`).
  **[src, vaadin-radio-button-flow 25.3.5]**

## R_vaadin_hints — a Vaadin component's helper text and tooltip in the browser

- `setHelperText` puts a `<div slot="helper">` holding the text in the component's light DOM, on
  a field and on a Checkbox. **[verified 2026-11-17, Vaadin 25.3.5]**
- `setTooltipText` puts a `<vaadin-tooltip slot="tooltip">` in the component's light DOM, its text
  in the `text` property, no attribute; on a Button and on a field alike.
  **[verified 2026-11-17, Vaadin 25.3.5]**
- The component's `textContent` includes the tooltip's text: an icon-only Button's is "Close
  without saving". **[verified 2026-11-17, Vaadin 25.3.5]**
- So the Unified API's `getText()` includes it on Playwright, which reads `textContent`
  (`BrowserQueries.TEXT`), but not on Browserless, which reads the server-side elements' text;
  `BrowserQueries.ITEM_NAME`, a tab's or menu item's name, leaves out children in a named slot.
  **[src, unified-testing-tool `76ab776`]**

## R_vaadin_icons — a Vaadin component's icon in the browser

- An `Icon` is a `<vaadin-icon>` whose `icon` attribute is its set and name, `vaadin:close`;
  `LumoIcon.CROSS.create()`'s is `lumo:cross`. **[verified 2026-11-17, Vaadin 25.3.5]**
- A `FontIcon` is a `<vaadin-icon>` too, its name, if any, in the `ligature` property, `home`, or
  the `iconClass` property, `fa-solid fa-user`; one made by `setCharCode` has neither, `char`
  instead. An `SvgIcon` has only `src`. **[verified 2026-11-17, Vaadin 25.3.5]**
- A Button with text puts its icon in the `prefix` slot, or `suffix` after `setIconAfterText`;
  an icon-only Button's icon has no `slot` attribute (`Button.updateIconSlot`).
  **[src, vaadin-button-flow 25.3.5]**
- A field's `setPrefixComponent` puts the icon in the `prefix` slot.
  **[verified 2026-11-17, Vaadin 25.3.5]**

## R_vaadin_dialog_dom — a Vaadin Dialog and ConfirmDialog in the browser

- An open `Dialog` is a `<vaadin-dialog>`, its content its light-DOM children; the overlay,
  `<vaadin-dialog-overlay popover="manual">` in its shadow root, slots them.
  **[verified 2026-11-17, Vaadin 25.3.5]**
- A Dialog the app didn't add anywhere is attached on `open()` as the last child of `<body>`, and
  removed once closed. One opened while a modal Dialog is open goes inside that dialog's
  element instead, after its content. **[verified 2026-11-17, Vaadin 25.3.5]**
- A Dialog the app added to a layout stays there in the DOM while closed, content and all, its
  `opened` property false. **[verified 2026-11-17, Vaadin 25.3.5]**
- A modal Dialog, the default, has `aria-modal="true"` and `with-backdrop`;
  `setModality(ModalityMode.MODELESS)` sets `modeless` instead, with no `aria-modal`.
  `setModal(boolean)` is deprecated for removal since 25.0. **[verified 2026-11-17, Vaadin 25.3.5]**
- `ModalityMode.STRICT` blocks the server's handling of events from components outside the
  dialog; `VISUAL` is the same in the browser, backdrop and focus trap, but doesn't block them.
  **[docs]**
- The page under a modal gets no `inert`; the backdrop is what stands in the pointer's way.
  **[verified 2026-11-17, Vaadin 25.3.5]**
- A Dialog's `aria-label` is its header title, which the web component renders as
  `<h2 slot="title">` while open; its footer's components sit in a
  `<div slot="footer" style="display: contents">`. **[verified 2026-11-17, Vaadin 25.3.5]**
- A `ConfirmDialog` is a `<vaadin-confirm-dialog role="alertdialog" aria-modal="true">`, attached
  to `<body>` like a Dialog: its header in `aria-label` and rendered as `<h3 slot="header">`, its
  message a `<div>`, its buttons `<vaadin-button>`s in the slots `cancel-button`,
  `reject-button` and `confirm-button`, those not shown `hidden`.
  **[verified 2026-11-17, Vaadin 25.3.5]**

## R_vaadin_grid_dom — a Vaadin Grid and TreeGrid in the browser

- Each cell's content, the header's included, is a `<vaadin-grid-cell-content>`, a light-DOM
  child of the `<vaadin-grid>`; a component column's component is inside its cell's. A TreeGrid
  is a `<vaadin-grid>` too. **[verified 2026-11-18, Vaadin 25.3.5]**
- Only the rows in view have cells: a Grid of 200 items at its default height had 10 rows'.
  **[verified 2026-11-18, Vaadin 25.3.5]**
- The cells are recycled: after a sort, the same `<vaadin-grid-cell-content>` element shows another
  item's value. **[verified 2026-11-18, Vaadin 25.3.5]**
- A sortable column's header cell holds a `<vaadin-grid-sorter>` with the header text, a
  TreeGrid's hierarchy cell a `<vaadin-grid-tree-toggle>` with the item's text. A multi-select
  Grid's selection column has a `<vaadin-checkbox>` per row and one `id=selectAllCheckbox` in the
  header, `checked` and `indeterminate` once some rows are selected.
  **[verified 2026-11-18, Vaadin 25.3.5]**
- A click on a cell selects its row in a single-select Grid; a click on a sorter sorts; a click
  on a tree toggle expands the row, its children's cells added after its own. Each reaches the
  server's listeners. **[verified 2026-11-18, Vaadin 25.3.5]**

## R_vaadin_hidden_in_dom — what a Vaadin component keeps in the DOM while it doesn't show it

None of these carries `hidden`.

- A `Details` or an `AccordionPanel` keeps its content in its light DOM while closed. Its summary,
  a `<vaadin-details-summary>` or `<vaadin-accordion-heading>` with a generated id, comes after the
  content. **[verified 2026-11-18, Vaadin 25.3.5]**
- A `Popover` with a target is in the DOM, content and all, before it was ever opened.
  **[verified 2026-11-18, Vaadin 25.3.5]**
- An `AvatarGroup`'s avatars beyond `maxItemsVisible` are `<vaadin-avatar-group-menu-item>`s in a
  `<vaadin-avatar-group-menu>` in its light DOM while its overflow menu is closed.
  **[verified 2026-11-18, Vaadin 25.3.5]**
- A `MenuBar` renders a submenu once it opens, its items `<vaadin-menu-bar-item>`s in a
  `<vaadin-menu-bar-list-box>` inside `<vaadin-menu-bar-submenu>`, in the bar's light DOM; they
  are still there after a click on one of them. **[verified 2026-11-18, Vaadin 25.3.5]**
- An `AppLayout`'s drawer content stays in the DOM after its `DrawerToggle` closes the drawer,
  `drawerOpened` false, on a 1280×720 viewport. **[verified 2026-11-18, Vaadin 25.3.5]**
- A `SideNavItem`'s child items are in its light DOM while it isn't `expanded`.
  **[verified 2026-11-18, Vaadin 25.3.5]**
- The browser's `checkVisibility()` is false for a closed Details' or AccordionPanel's content, a
  never-opened Popover's, and a collapsed SideNavItem's children; but true for a closed drawer's
  SideNav. **[verified 2026-11-18, Vaadin 25.3.5, Chromium]**
- `checkVisibility()` is false for an element with `display: contents`, which has no box of its
  own, while its children's is true. **[verified 2026-11-18, Chromium]**
- A `TabSheet` is the exception: only the selected tab's content is in the DOM, and selecting
  another tab replaces it. So is a `ContextMenu`: before it first opens, there is no
  `<vaadin-context-menu>` in the DOM, and its target is a plain child of its layout.
  **[verified 2026-11-18, Vaadin 25.3.5]**
- While a MenuBar submenu is open, a click elsewhere on the page doesn't reach its target:
  Playwright retries until its timeout, reporting that `<html>` intercepts pointer events.
  **[verified 2026-11-18, Vaadin 25.3.5]**

## R_vaadin_login_dom — a Vaadin LoginForm in the browser

- A `<vaadin-login-form>`'s fields are a `<vaadin-text-field id="vaadinLoginUsername">` and a
  `<vaadin-password-field id="vaadinLoginPassword">` in its light DOM, beside an
  `<input id="csrf" type="hidden">`, whose `checkVisibility()` is false.
  **[verified 2026-11-18, Vaadin 25.3.5]**
- Flow sends the form its whole `i18n`, there before any error. After the app's `setError(true)`,
  the form's `error` property is true and its message is rendered in the shadow DOM of a
  `<vaadin-login-form-wrapper>`, itself in the form's shadow root: `i18n.errorMessage.title` in a
  `<strong part="error-message-title">`, `i18n.errorMessage.message` below it. Nothing in the
  light DOM changes. **[verified 2026-11-18, Vaadin 25.3.5]**
- The form's heading, "Log in", is its `i18n.form.title`, rendered in its shadow DOM as
  `part="form-title"`; the form has no `aria-label`. **[verified 2026-11-18, Vaadin 25.3.5]**
- An open `<vaadin-login-overlay>` has `role="dialog"` and `aria-modal="true"`, no `aria-label`:
  its `aria-labelledby` names a `<div slot="title" role="heading">` in its light DOM holding the
  title, its id generated, `title-vaadin-login-overlay-34`. Its `setTitle` and `setDescription` are
  its `title` and `description` properties; the description is rendered in its shadow DOM only,
  `part="description"`. **[verified 2026-11-18, Vaadin 25.3.5]**

## R_flow_navigation_url — Flow: a server-side navigation changes the URL after its round-trip

- `UI.navigate` from a listener updates the address bar only after the response that carries the
  new view has been handled, so a wait for the server to be idle returns with the new view on
  the page and the old URL in `location.href`. **[verified 2026-11-16, Vaadin 25.3.5]**
- Without React, `History.pushState` sends `setTimeout(() => window.history.pushState(…))`.
  With React (`flow-react` on the classpath, Vaadin 25's default), it dispatches `vaadin-navigate`;
  `Flow.tsx` sets `window.Vaadin.Flow.navigation = true`, queues the navigation for React
  Router behind a `setTimeout`, and deletes the flag in a `setTimeout` after the location changed.
  **[src, flow-server 25.3.7]**
- The flag is Flow's internal, not an API. **[src, flow-server 25.3.7]**
- The Unified API's Auto-Wait doesn't wait for it: parttio/unified-testing-tool#233.
  **[verified 2026-11-16, unified-test-playwright 0.1.0-alpha2]**

## R_playwright_mcp_refs — the Playwright MCP's element refs

Read in playwright-core 1.66.0-alpha-2026-11-09 under `@playwright/mcp` 0.0.85.

- A ref, `e<N>` from a page-wide counter, is kept as a JavaScript property on the element
  (`_ariaRef`), with the role and accessible name it was minted for. A later snapshot gives the
  element the same ref while its role and name are unchanged, and a new number otherwise. **[src]**
- A ref resolves against the latest snapshot only, and only while its element is still in the
  document; otherwise the tool fails with "Ref e7 not found in the current page snapshot. Try
  capturing new snapshot." **[src]**
- An action tool answers with a snapshot taken after the action, the whole page unless the
  server is configured otherwise (`snapshot.mode`). **[src]**
