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
- Excluding `vaadin-dev` from it breaks nothing for an app that runs in another JVM.
  **[verified 2026-11-16, 0.1.0-alpha2]**

## R_playwright_thread_safety — Playwright for Java: threads and browser downloads

- Playwright for Java is not thread-safe: every method of `Playwright` and of every object made
  from it must be called on the thread that created the `Playwright`. **[docs]**
- `Playwright.create()` without options downloads every browser it supports on first use —
  Chromium, Chromium headless shell, Firefox, WebKit, and ffmpeg — into `~/.cache/ms-playwright`,
  1.3 GB unpacked. **[verified 2026-11-16, Playwright 1.63.0]**
- `PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1` in the environment skips that download. **[docs]**
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
