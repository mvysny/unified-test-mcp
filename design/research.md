# Research — the Unified API for Vaadin UI tests, on Playwright through Dramafinder

What the things we don't own actually do. About *them*, never us: a sentence starting "we chose"
is a `D_`. `## R_<slug> — <title>`, one claim per bullet, one provenance marker per claim —
**[docs]**, **[src]**, **[verified <date>, <version>]**, **[unverified]** (a hypothesis; a design
built on it says so). A claim is earned by its provenance, or by having cost real work to find
out. Checked against unified-testing-tool 0.1.0-alpha3, Dramafinder 2.2.0, Vaadin 25.3.5; a
version-sensitive claim names the version it was seen on. Cite by slug,
`R_<slug>`, never by position; `grep '^## R_' design/research.md` is the index. When you have
written an entry, re-read it against the one above for consistency; a marker already says where
a claim came from, so the prose doesn't repeat it.

---

## R_unified_page_lifecycle — Unified API: how a page is opened, and what an adapter is given

- A `UnifiedPage` is made by `TestLifecycle.open(AdapterContext, StepRecorder)`, in package
  `com.vaadin.testing.unified.internal`: it loads the `TestingToolAdapter` for the run's tool
  through `ServiceLoader`, calls `adapter.open(context)`, and wraps the `AdapterSession`. Its one
  caller is the JUnit extension, `internal.junit.UnifiedExtension`; no public API opens a page
  outside a Test Runner. **[src]**
- `AdapterContext` names no Test Runner type (ADR 0008): `configuration(key)`, `timeout()`,
  `testClass()`, `testMethod()`, `springContext()`, and `runScoped(key, create)`, one instance
  per run and key, closed when the run ends. **[src]**
- `PlaywrightAdapter.open` keeps one `Playwright` and one `Browser` per run, through
  `runScoped(SharedBrowser.class, …)`. **[src]**
- Each `open` makes a fresh `BrowserContext` and `Page`. The session class, `PlaywrightSession`,
  is package-private: only the adapter can make one. **[src]**
- `UnifiedPageImpl` and `TestRun` are public, in package `internal`;
  `new UnifiedPageImpl(new TestRun(TestingTool.PLAYWRIGHT, session, timeout))` wraps any
  `AdapterSession`, with a `StepRecorder` of its own. **[src]**
- The `StepRecorder` keeps every User Step of the page in memory. Only `TestLifecycle`'s close
  writes them out: to `user-steps.txt` / `.json`, and to stdout as
  `vaadin.testing.user-steps.log` says. **[src]**
- The base URL is the Spring context's port when `springContext()` has one, else the
  `vaadin.testing.base-url` setting, else `http://localhost:8080`; settings come through
  `AdapterContext.configuration(key)`, which the JUnit extension backs with configuration
  parameters and system properties. **[src]**
- The test-user login (`LoginSupport`) is skipped when `unified-test-spring` is not on the
  classpath. **[src]**
- Every Unified failure is a `UnifiedAssertionError`, an `AssertionError`, thrown once a lookup
  or an action has waited out the timeout. Its message is labelled lines: `Expected`, `Found`,
  `Nearby` (the components close to a match), `Did you mean` (a Java lookup), `Waited`, `Tool`,
  `Code` (the call sites), the failed User Step. **[src]**

## R_unified_no_enumeration — Unified API: every lookup starts from a component type

- `Scope` offers `find(wrapperType)` and `findComponent(flowComponentClass)` only, and the
  adapter's `QuerySpec` carries exactly one `componentType`; no call lists every component on a
  page, and none finds by id alone. **[src]**
- On Playwright, a type with a Dramafinder element (e.g. `Button`) is found through Dramafinder's
  `ElementQueries.find(page, elementClass)`; any other type through Dramafinder's tag query on
  its `@Tag` or `@UnifiedComponent(tag)`, with the filters CSS can express, the rest checked in
  the page in one round trip; a type with neither fails. **[src]**
- On the Dramafinder path the filters map to Dramafinder query methods by reflection: id →
  `withId`, text → `withText` / `withTextContaining`, attribute → `withAttribute`; a Dramafinder
  query lacking the method fails with `UnsupportedOperationException`. A label is checked in the
  page instead, so a FormLayout item's label matches too. **[src]**
- Dramafinder's Button query has `withId`: `page.find(UnifiedButton.class).withId("clicker")`
  finds `<vaadin-button id="clicker">`. **[verified 2026-11-16, Dramafinder 2.0.2]**
- Both paths search the light DOM only: Dramafinder 2.2's queries are `css:light=` unless
  `includingShadowDom()` follows `find`, and the Unified API never calls it. No lookup finds a
  component in a template's shadow root, on any Testing Tool, as its spec says since its #251;
  `BrowserQueries.APP_CONTENT`'s comment is older. Asked about in
  parttio/unified-testing-tool#347.
  **[src; verified 2026-11-19, 0.1.0-alpha3: a ref inside one is "gone from the page"]**
- `0.1.0-alpha3-shadow`, a local build of the clone's branch `shadow-dom-queries-alpha3`, adds
  `includingShadowDom()` to both paths, and drops every match inside the shadow root of a
  `vaadin-*` or `copilot-*` host through `APP_CONTENT` on both: an id, an attribute, a type is
  found in an app's template as in the light DOM.
  **[verified 2026-11-19, 0.1.0-alpha3-shadow]**
- `UnifiedElement.findChildren` / `findDescendants(tag)` walk light DOM only; in a browser that
  includes the light DOM web components add themselves, such as a text field's `input` and
  `label`. **[docs]**
- `UnifiedButton.getText()` is `element().getText()`: the DOM's text nodes, a slotted tooltip's
  left out (`BrowserQueries.TEXT`), whitespace collapsed and trimmed. **[src]**
- Escape hatch: `UnifiedPage.unwrap(com.microsoft.playwright.Page.class)` returns the session's
  Playwright `Page`; a wrapper unwraps to its Dramafinder element or a `Locator`. **[src]**
- `UnifiedContainer` is bound to any tag (`@UnifiedComponent(tag = "*")`): `page.find(UnifiedContainer.class)`
  with `withAttribute(name, value)` or `withId(id)` finds an element of any type by a CSS selector,
  `*[data-mcp-ref="3"]`. **[verified 2026-11-19, unified-test-playwright 0.1.0-alpha3]**
- `find(type)` returns the wrapper itself, and each filter a new wrapper (ADR 0007); every use
  of it waits until exactly one component matches. `all()` reads the page once, without
  waiting; `assertThat(wrapper).exists()` waits. **[src]**

## R_unified_lazy_elements — Unified API: a wrapper holds its query, not an element

- A wrapper such as `UnifiedButton` sits on a `LazyElement`, which keeps the lookup (component
  type, filters) and, after `all().get(i)`, an index; it holds no element. Every read and action
  runs the lookup again, waiting until exactly one component matches or the indexed one
  exists. **[src]**
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
- `selectItem` reads the pick back through the same wrapper, `SelectionSupport.select` asking
  `access.selectedItems()`: a field the app replaced as it took the pick, Bakery's order item
  editors, matches the lookup no more, and Playwright waits its timeout, 10 s, though the pick went
  through. Filed as parttio/unified-testing-tool#377.
  **[src; verified 2026-11-19, 0.1.0-alpha3-shadow on Bakery]**

## R_unified_playwright_packaging — unified-test-playwright as a dependency

- Compiled for Java 21 (`maven.compiler.release`) against the Vaadin 25.3.5 BOM and Dramafinder
  2.2.0, which brings Playwright 1.63.0. **[src]**
- `unified-test-api` depends on `junit-jupiter-api` as `optional` and on `vaadin-core` as
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
  **[verified 2026-11-19, 0.1.0-alpha3]**

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
  custom value no item matches, shows there while `getSelectedItemLabel()` is `null`.
  **[src, Dramafinder 2.0.2]**
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
- The Unified API's `getText()` leaves it out on every tool: on Playwright,
  `BrowserQueries.TEXT` skips a `<vaadin-tooltip slot="tooltip">`, as Browserless's server-side
  text does; `BrowserQueries.ITEM_NAME`, a tab's or menu item's name, leaves out children in a
  named slot. **[src, unified-testing-tool 0.1.0-alpha3]**

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
- Only the rows near the view have cells: a Grid of 200 items at its default height had 10 rows',
  of which 7 were in its viewport, the last one in part, the rest a buffer below it. The viewport runs from the bottom
  of its header (`grid.$.header`) to the top of its footer (`grid.$.footer`).
  **[verified 2026-11-18 and 2026-11-19, Vaadin 25.3.5]**
- A row is a `<tr>` in the grid's shadow DOM, each cell a `<td>` whose `<slot>` is assigned the
  cell's `<vaadin-grid-cell-content>`. The `<tr>` has `index`, 0-based among every row, and the
  attributes `selected`, `expanded`, `details-opened`, and in a TreeGrid `aria-level` from 1. The
  details are a `<td part~="details-cell">`, hidden until open; Flow opens them on a click that
  selects the row. **[verified 2026-11-19, Vaadin 25.3.5]**
- `size` counts every row, not the rendered ones: 200. In a TreeGrid it counts an expanded row's
  children too: 2 before expanding Engineering, 4 after. **[verified 2026-11-19, Vaadin 25.3.5]**
- Flow puts the selection mode on the grid's shadow `<table>`, as `aria-multiselectable`: `"true"`
  multi, `"false"` single, absent for none (`gridConnector.ts`, `#updateAriaMultiSelectable`).
  `selectedItems` holds the item a click selected. **[src; verified 2026-11-19, Vaadin 25.3.5]**
- Selection beyond the loaded rows, test-app's `unloaded-rows`: `selectedItems` holds the
  selected items among the pages the browser holds, and one the server has just selected. A
  single-select grid: row 150 selected on the server before the grid showed is there, its `key`
  no row index (`"1"`); gone once the grid scrolled to row 0 and that page left the cache, though
  still selected on the server; back when the grid scrolled near it again. Row 1 a user clicked:
  gone at row 150, back at row 0. **[verified 2026-11-19, Vaadin 25.3.5]**
- A multi-select grid's `selectedItems` likewise. Row 180 selected on the server, out of view: `[]` until the grid scrolled there;
  a row ticked at the top dropped out of it once the grid scrolled away. After `selectAll()`, the
  server's or the header checkbox's: 50 of 200, then 100. A loaded row's `selected` attribute is
  right every time. **[verified 2026-11-19, Vaadin 25.3.5]**
- Of a multi-select grid's whole selection, the browser has its header checkbox: unchecked none,
  `checked` all, `checked` and `indeterminate` some; the selection column's `selectAll` property
  true only while all are. Deselecting one row after select-all makes it indeterminate.
  **[verified 2026-11-19, Vaadin 25.3.5]**
- A lazy grid whose data provider gives no count: `size` 200, Flow's estimate, then 400 and 600
  as the grid scrolls to its end; a counted one's 1000 from the start. Nothing in the browser
  tells an estimate from a count: the connector's `updateSize(size)` sets `size` and nothing
  else. **[src `gridConnector.ts`; verified 2026-11-19, Vaadin 25.3.5]**
- Every header row is a row of `<vaadin-grid-column-group>`s in the browser, the captions'
  included, one `<td>` per column with `colspan=1`. A joined cell, `prependHeaderRow().join(…)`,
  is one `<td>` with `colspan=2`, so its row has fewer cells than columns. The first footer row is
  the one `setFooter` fills; the group levels' rows after it are `hidden` while empty.
  **[verified 2026-11-19, Vaadin 25.3.5]**
- The cells are recycled: after a sort, the same `<vaadin-grid-cell-content>` element shows another
  item's value. **[verified 2026-11-18, Vaadin 25.3.5]**
- A sortable column's header cell holds a `<vaadin-grid-sorter>` with the header text, a
  TreeGrid's hierarchy cell a `<vaadin-grid-tree-toggle>` with the item's text, its `leaf` and
  `expanded` properties saying whether it has children and shows them. A sorter's `direction`
  goes `null` → `"asc"` → `"desc"` → `null`, a click each. On a multi-sort grid a click that
  sorts a column puts it first, `Name ↑2 | Age ↑1` after Name then Age; a sorter shows its order
  in its shadow `[part="order"]`, empty while it alone sorts. **[verified 2026-11-19, Vaadin
  25.3.5]** A multi-select
  Grid's selection column has a `<vaadin-checkbox>` per row and one `id=selectAllCheckbox` in the
  header, `checked` and `indeterminate` once some rows are selected.
  **[verified 2026-11-18, Vaadin 25.3.5]**
- A click on a cell selects its row in a single-select Grid; a click on a sorter sorts; a click
  on a tree toggle expands the row, its children's cells added after its own. Each reaches the
  server's listeners. **[verified 2026-11-18, Vaadin 25.3.5]**

## R_unified_grid_headers — the Unified API's Grid columns under a header group row

- On test-app's people grid, a header group row over Name and Age above the captions, every
  `UnifiedGrid.column(caption).sort(…)` fails with "No header cell found at column index 4",
  for a caption the grid has and for one it hasn't: its header lookup reads the first header row,
  here the group row, whose cells span columns and number fewer than the columns
  (`R_vaadin_grid_dom`). **[verified 2026-11-19, unified-test-playwright 0.1.0-alpha2, Vaadin
  25.3.5]**
- The adapter's `columnHeaders()` and `headerCell(column)` call Dramafinder's
  `getHeaderCellContents()` and `findHeaderCell(column)`, both reading the first header row;
  upstream `main` still does, on Dramafinder 2.2.0. Filed as parttio/unified-testing-tool#338.
  **[src, unified-testing-tool `0d35489`]**

## R_dramafinder_row_click — Dramafinder: where a row click lands, and a template's grid

- The Unified API's `row(i).click()` is Dramafinder's `RowElement.click()`; `select()` and
  `doubleClick()` pick their spot alike. Each clicks a data cell's content at its first free point:
  the middle of its left padding, its top-left corner, then its centre, free if the hit has
  nothing on its way up to the cell content matching `a[href], button, input, select, textarea,
  label, [tabindex], [contenteditable]`, nor any custom element, which "may be interactive inside
  its shadow root". No data cell with one: `IllegalStateException`, "Cannot click row 0: the row
  has no data cell with a spot free of interactive content, padding included".
  **[src, Dramafinder 2.2.0 `GridElement`]**
- A grid in a template's shadow root fails on every row: the hit is `document.elementFromPoint`'s,
  the template's host, which no cell contains. A cell an app's element covers, padding included,
  fails too: Bakery's storefront, its grid in `<storefront-view>`'s shadow root, its cells all
  `<order-card>`; test-app's `template` view likewise. A click on the card itself selects the row,
  and Bakery opens the order. **[verified 2026-11-19 on Bakery, 2026-11-20 on test-app]**
- `2.2.0-shadow`, a local build of the clone's branch `row-click-app-elements`, hit-tests from the
  cell's own root, and follows a hit on an app's element with an open shadow root, one not
  `vaadin-*`, into it by the shadow root's `elementFromPoint`, nested ones too, testing what it
  lands on instead; a Vaadin component stays as before. Asked for in parttio/dramafinder#298,
  proposed upstream as parttio/dramafinder#311.
  **[verified 2026-11-20, 2.2.0-shadow: Dramafinder's grid and TreeGrid ITs, test-app]**

## R_testbench_grid_element — how TestBench's GridElement reaches rows, the ones out of view too

TestBench's `GridElement`, `com.vaadin:vaadin-grid-testbench`, ships in flow-components under the
Apache License 2.0, unlike TestBench itself: its code may be borrowed, with attribution.

- `scrollToRow(i)` calls the grid's `scrollToIndex(i)`, a TreeGrid's `_scrollToFlatIndex(i)`, then
  waits while `_dataProviderController.isLoading()` or `$connector.hasRootRequestQueue()`.
- `getRows(first, last)` pages nothing: it filters `_getRenderedRows()` by `index`.
- `getFirstVisibleRowIndex()` / `getLastVisibleRowIndex()` read the grid's `_firstVisibleIndex`
  and `_lastVisibleIndex`: 0 and 9 in a grid of 15 rendered rows, 118 and 127 after
  `scrollToIndex(118)`. **[verified 2026-11-19, Vaadin 25.3.5]**
- Columns, in the order on screen: `_getColumns().filter(c => !c.hidden).sort((a, b) => a._order -
  b._order)`.
- Each is an internal property of `<vaadin-grid>`, not its API.

**[src, the scripts in vaadin-grid-testbench 25.3.5's `GridElement.class`]**

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
  never-opened Popover's, and a collapsed SideNavItem's children. A closed drawer's SideNav is
  `visibility: hidden`, which `checkVisibility()` ignores unless asked:
  `checkVisibility({ visibilityProperty: true })` is false for it, as for a closed MenuBar
  submenu's items and an AvatarGroup's overflow avatars. **[verified 2026-11-18, Vaadin 25.3.5, Chromium]**
- A closed Dialog's element is `display: none`, its content too, whether attached on `open()` or
  added to a layout; an open one's is `display: block`. A `<vaadin-notification>` is
  `display: none` while open: its content shows in a `<vaadin-notification-card>`. A MenuBar's
  overflow button, `<vaadin-menu-bar-button>` with "···", is `display: none` while every item fits.
  **[verified 2026-11-18, Vaadin 25.3.5, Chromium]**
- `checkVisibility()` is false for an element with `display: contents`, which has no box of its
  own, while its children's is true: a Dialog's `<div slot="footer">` and `<h2 slot="title">` are
  such. **[verified 2026-11-18, Vaadin 25.3.5, Chromium]**
- A closing drawer turns `visibility: hidden` at the end of a 200 ms transition: right after a
  click on its DrawerToggle, its SideNav is still visible. **[verified 2026-11-18, Vaadin 25.3.5,
  Chromium]** (`R_vaadin_animations`)
- A `TabSheet` is the exception: only the selected tab's content is in the DOM, and selecting
  another tab replaces it. So is a `ContextMenu`: before it first opens, there is no
  `<vaadin-context-menu>` in the DOM, and its target is a plain child of its layout.
  **[verified 2026-11-18, Vaadin 25.3.5]**
- While a MenuBar submenu is open, a click elsewhere on the page doesn't reach its target:
  Playwright retries until its timeout, reporting that `<html>` intercepts pointer events.
  **[verified 2026-11-18, Vaadin 25.3.5]**

## R_vaadin_animations — what animates on a Vaadin page, and where the browser lists it

- `document.getAnimations()`, and `getAnimations({ subtree: true })` on `<html>`, leave out the
  animations in shadow roots; each shadow root's `getAnimations()` lists its own. A closing
  drawer's `transform` and `visibility` transitions run on the `[part=drawer]` in AppLayout's
  shadow root; the document lists only the layout's `padding-left`. **[verified 2026-11-18,
  Vaadin 25.3.5, Chromium]**
- A Dialog opens and closes with keyframe animations in its overlay's shadow root, the longest
  `lumo-overlay-dummy-animation`, 250 ms; a ComboBox's or Select's dropdown closes in 140 ms.
  **[verified 2026-11-18, Vaadin 25.3.5, Chromium]**
- Transitions of no consequence to what shows run after most actions: a Button's opacity, 1.4 s
  after a click; a field's input container's opacity, up to 1 s, and its label's colour, on focus;
  in dev mode, Copilot's toolbar's width. **[verified 2026-11-18, Vaadin 25.3.5, Chromium]**

## R_vaadin_login_dom — a Vaadin LoginForm in the browser

- A `<vaadin-login-form>`'s fields are a `<vaadin-text-field id="vaadinLoginUsername">` and a
  `<vaadin-password-field id="vaadinLoginPassword">` in its light DOM, beside an
  `<input id="csrf" type="hidden">`, whose `checkVisibility()` is false.
  **[verified 2026-11-18, Vaadin 25.3.5]**
- Flow sends a login `LoginI18n.createDefault()`, whole, until the app calls `setI18n`; the
  component's `i18n` property returns what was set. What it renders from is its private
  `__effectiveI18n`: its defaults with the app's merged over them. Vaadin 25.3 deprecates
  `createDefault()` for a `new LoginI18n()` holding only what the app changes; then `i18n` has no
  `errorMessage`, and the error rendered is the defaults'. **[src @vaadin/login and
  vaadin-login-flow 25.3.5; verified 2026-11-20, Vaadin 25.3.5]**
- After the app's `setError(true)`, the form's `error` property is true and its message is
  rendered in the shadow DOM of a
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
- The overlay renders its effective i18n's `header.title` and `header.description` when not null,
  else its `title` and `description` properties, by default "App name" and "Application
  description" (`LoginOverlayMixin.willUpdate`). **[src @vaadin/login 25.3.5; verified 2026-11-20,
  Vaadin 25.3.5]**

## R_vaadin_form_item_dom — a Vaadin FormItem in the browser

- Flow's `addFormItem(component, "Customer")` makes a `<vaadin-form-item>` holding the component
  and a `<label slot="label">`, a `NativeLabel`. **[src vaadin-form-layout-flow 25.3.5; verified
  2026-11-20, Vaadin 25.3.5]**
- The item links its first label to its first field: the label's id, generated if it has none,
  in the `aria-labelledby` of the field's ARIA target. A click on the label focuses the field.
  **[src @vaadin/form-layout 25.3.5, `vaadin-form-item-mixin.js`]**

## R_vaadin_app_layout_dom — a Vaadin AppLayout in the browser

- A `<vaadin-app-layout>` holds the app's chrome and its view as light-DOM children, told apart
  by `slot`: Flow's `addToNavbar` gives `navbar`, `addToNavbar(true, …)` `navbar
  touch-optimized`, `addToDrawer` `drawer`; the view, `setContent`, has none.
  **[verified 2026-11-18, Vaadin 25.3.5]** On a touch device the layout moves a `navbar
  touch-optimized` child to `navbar-bottom`. **[src, @vaadin/app-layout 25.3.5]**
- Its `drawerOpened` property is false once a `DrawerToggle` closes the drawer, whose content
  stays in the DOM (`R_vaadin_hidden_in_dom`). **[verified 2026-11-18, Vaadin 25.3.5]**

## R_vaadin_side_nav_dom — a Vaadin SideNav and Breadcrumbs in the browser

- A `<vaadin-side-nav-item>`'s items are `<vaadin-side-nav-item slot="children">`s in its light
  DOM, and while it's `expanded` its box holds them below its own row: a click at the box's centre
  lands on one, on Archive with two items the first, and opens its page.
  **[verified 2026-11-18, Vaadin 25.3.5, Chromium]**
- Its row is a `<div part="content">` in its shadow DOM, an `<a part="link">` and a
  `<button part="toggle-button">` in it. A click on the row, the toggle aside, follows the link
  to its `path`; an item without one toggles `expanded` instead, if it has items (its
  `has-children` attribute). Only the toggle button expands or collapses an item that has a
  `path`. **[src, @vaadin/side-nav 25.3.5]**
- An item is `current` while its `path`, or one of its `pathAliases`, is the browser's path; a
  path below it only with `matchNested`. On becoming current, it expands if it has items, and its
  parent items expand unless the SideNav has `noAutoExpand`. **[src, @vaadin/side-nav 25.3.5]** A
  click on a collapsed item with a path opens its page with its items shown.
  **[verified 2026-11-18, Vaadin 25.3.5]**
- A `<vaadin-breadcrumbs>` sets `current` on its last `<vaadin-breadcrumbs-item>` if it has no
  `path`; such an item renders a `<span part="nolink">`, not a link. **[docs]** test-app's
  "Inbox", added last without a path, is `current`. **[verified 2026-11-18, Vaadin 25.3.5]**
- Either item's `path` property is the path the app gave Flow, relative to the app's root:
  `"nav/archive"`, and `""` for the root itself. **[verified 2026-11-18, Vaadin 25.3.5]**

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
- The Unified API's Auto-Wait waits for it after each of its own actions: `AdapterSession.settle()`,
  on Playwright while the flag is set (at most 2 s), then until Vaadin is idle. A click through
  the escape hatch gets no such wait. **[src, unified-testing-tool 0.1.0-alpha3]**

## R_flow_idle_wait — Flow: what Auto-Wait's "idle" sees, and a page load it misses

- Dramafinder's `AutoWait.waitForVaadinIdle` waits until no client in `window.Vaadin.Flow.clients`
  is `isActive()`; a page without `Vaadin.Flow.clients` counts as idle. An "Execution context was
  destroyed" during the wait makes it wait for `load` and ask again. **[src, Dramafinder 2.2.0]**
- After a click that loads a page (a LoginForm's POST to Spring Security, `Page.setLocation`), it
  returns while the page is leaving or before Flow runs in the new one: a snapshot then found no
  components on Bakery and the CRM, and on test-app failed, reading a property of null.
  **[verified 2026-11-19, Vaadin 25.3.5]**
- For `Page.setLocation`, the browser's request for the next page can come after Flow is idle and
  the Unified API's wait has returned: in 3 of 25 tries on test-app, after the snapshot began.
  **[verified 2026-11-19, Vaadin 25.3.5]**
- `Flow`'s constructor registers a client named `TypeScript`, active only while `flowInit` runs;
  that runs when the router asks for the server's routes, and registers the UI's own client under
  its app id, `ROOT`. **[src, Flow 25.3.7]**
- Between the page's `load` and `flowInit`, no client is active: waited for right after `load`,
  the idle check returned with an empty page in 6 of 25 tries on test-app.
  **[verified 2026-11-19, Vaadin 25.3.5]**
- A load can end within the Unified API's own wait, its evaluate cut off by the navigation, which
  then hits that gap: Bakery's Logout link answered with no components in 1 of 4 tries.
  **[verified 2026-11-19, Vaadin 25.3.5]**
- The Navigation API's `navigate` event fires in the page as it asks to leave, a location change,
  a link, a form's submit, before the request goes out; `destination.sameDocument` is false for
  another document, true for `pushState`. **[docs]**
- Neither Flow's client nor the frontends of test-app and Bakery listen to `navigate`. **[src]**

## R_flow_debounce — Flow: a debounced change in the browser

- A TextField sends its value on the event `ValueChangeMode.eventForMode` names: `input` for
  EAGER, LAZY and TIMEOUT, `blur` for ON_BLUR, `change` for ON_CHANGE, the default. LAZY debounces
  `input` by the field's timeout, `HasValueChangeMode.DEFAULT_CHANGE_TIMEOUT`, 400 ms; TIMEOUT
  throttles it. **[src, flow-data 25.3.7]**
- `@DomEvent(debounce = …)` debounces an event of the app's own: Bakery's search sends
  `value-changed` 300 ms after the last one, and its TextField is EAGER besides. **[src]**
- While the timer runs, nothing is queued: Flow's client isn't active, and Auto-Wait's idle wait
  (`R_flow_idle_wait`) returns at once. `set_value` on the CRM's filter, Bakery's search and a
  LAZY field in test-app answered with the grid before its filter. **[verified 2026-11-19, Vaadin 25.3.5]**
- The client keeps its pending debouncers in a private static map, and sends them early only
  before an event it sends at once (`Debouncer.flushAll`); a LAZY TextField listens to `input`
  alone, so nothing flushes it. Seen in the client's TypeScript port; the live page still runs the
  GWT client. **[src, Flow 25.3.7]**

## R_flow_shadow_root — Flow: a shadow root attached from Java

- `Element.attachShadow()` gives the element an open shadow root in the browser; elements
  appended to the `ShadowRoot` are its children there, and Flow components among them keep their
  listeners: a Button's click, a TextField's value change reach the server.
  **[verified 2026-11-19, Vaadin 25.3.5]**
- A `<slot>` appended to it shows the host's light-DOM children, by their `slot` attribute.
  **[verified 2026-11-19, Vaadin 25.3.5]**
- A modal Dialog in it opens with `aria-modal="true"`, its content its light-DOM children, and
  its buttons' listeners run under its modality; `document.querySelectorAll('[aria-modal=true]')`
  doesn't find it. **[verified 2026-11-19, Vaadin 25.3.5]**
- Vaadin adds `<style>` elements to a shadow root that holds its components.
  **[verified 2026-11-19, Vaadin 25.3.5]**
- It needs no frontend build; a `LitTemplate`'s `@JsModule` does, the dev bundle rebuilt with npm
  for a module it lacks. **[docs]**

## R_bakery_template_dom — Vaadin's Bakery: its LitTemplates in the browser

- `<storefront-view>` has no light-DOM children; its shadow root holds `<search-bar id=search>`,
  `<vaadin-grid id=grid>` and `<vaadin-dialog id=dialog aria-modal=true>`.
  **[verified 2026-11-19, Vaadin 25.3.5]**
- `<search-bar>`'s shadow root: a TextField with the placeholder "Search", a "Show past orders"
  Checkbox twice (one hidden by CSS per viewport width), "Clear search" (hidden until a search),
  and the action button, "New order", "New user" or "New product".
  **[verified 2026-11-19, Vaadin 25.3.5]**
- The dialog's light-DOM child is `<order-editor>` or `<order-details>`, added by Flow, each a
  template with its fields in its shadow root. The editor's items are a Flow `Div` in its
  `#itemsContainer`, an `<order-item-editor>` per item, each with `#products`, `#amount`,
  `#comment` and `#delete` in a shadow root of its own: the same ids, repeated.
  **[verified 2026-11-19, Vaadin 25.3.5]**
- `<buttons-bar>` slots the footer's buttons, Flow's light-DOM children of it, through
  `<slot name=left|info|right>`. **[verified 2026-11-19, Vaadin 25.3.5]**
- `<order-details>` labels its values with FormItems, `<label slot="label">Customer</label>` beside
  an `<h3>`; its comment's `<vaadin-text-field id=commentField>` holds a `<div slot="suffix">`
  with the `<vaadin-button id=sendComment>` "Send" in it. **[src, `order-details.js`]**
- A grid cell's `<order-card>` holds all its text in its shadow root, the cell's `textContent`
  empty; its status is `<order-status-badge>`, another template.
  **[verified 2026-11-19, Vaadin 25.3.5]**
- The Crud views' editor is a `<vaadin-crud-dialog>` in `<vaadin-crud>`'s shadow root.
  **[verified 2026-11-19, Vaadin 25.3.5]**

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

## R_playwright_mcp_settle — the Playwright MCP's wait after an action

- After every action it waits 500 ms (`timeouts.settle`), noting the requests the page makes
  meanwhile. **[src, playwright-core 1.66.0-alpha-2026-11-18]**
- After a navigation request among them, it waits for `load`, 10 s at most; otherwise for their
  responses, 5 s at most, then, if there were any, 500 ms more.
  **[src, playwright-core 1.66.0-alpha-2026-11-18]**
