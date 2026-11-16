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
- Both paths drop matches inside the shadow DOM of a `vaadin-*` or `copilot-*` host — the dev
  tools' own components (`BrowserQueries.APP_CONTENT`). **[src]**
- `UnifiedElement.findChildren` / `findDescendants(tag)` walk light DOM only; in a browser that
  includes the light DOM web components add themselves, such as a text field's `input` and
  `label`. **[docs]**
- `UnifiedButton.getText()` is `element().getText()`: the DOM `textContent`, whitespace collapsed
  and trimmed. **[src]**
- Escape hatch: `UnifiedPage.unwrap(com.microsoft.playwright.Page.class)` returns the session's
  Playwright `Page`; a wrapper unwraps to its Dramafinder element or a `Locator`. **[src]**

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
