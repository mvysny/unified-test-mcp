# Decisions

Why this project is the way it is and not otherwise — FAQ-shaped: each entry is a question and
its current answer. Rewrite the answer when it changes; delete the entry when nobody asks any
more. An entry is earned by what it would cost to reverse — half the code base — or by research
the next person would otherwise redo (cited as its `R_`). Not an entry: windows → panels
"because that's the trend", this red over that red, `get_foo` over `is_foo?`, the testing library,
the CI host, a version bump — a comment at the site of the choice, or nothing; nothing about
`design/` itself. Cite by slug, `D_<slug>`, never by position; `grep '^## D_' design/decisions.md`
is the index. An entry is as long as its question needs: every road not taken keeps all of its
losing reasons. When you have written an entry, re-read it against the one above for consistency,
and open the doc comments it touches and delete what they already say.

---

## D_unified_api_backend — Why drive the browser through the Unified API rather than Playwright or TestBench directly?

The tools speak Vaadin components (**Vaadin components, not DOM**), and the Unified API already
has that vocabulary: a typed wrapper per Flow component (`UnifiedButton`, `UnifiedTextField`,
`UnifiedGrid`, …), lookups by label, text, accessible name or id, and actions that wait until
the component is enabled and the server round-trip is done. On Playwright it runs through
Dramafinder, so it needs no license, and the Playwright session stays reachable through its
escape hatch for whatever the wrappers lack. Why not Playwright directly, or the Playwright MCP
the agent already has: it sees DOM and ARIA, so how each Vaadin component is built (shadow DOM,
overlays, a Grid's lazily loaded and recycled rows) would be ours to reverse-engineer, and to
follow on every Vaadin release; the Unified API and Dramafinder maintain that. Why not
TestBench: it drives Selenium only and needs a license; we revisit it once TestBench can drive
Playwright. The cost we carry: the Unified API is a 0.1.0 alpha whose only entry point is a
JUnit extension (`R_unified_junit_bound`), and it has no query that lists the components on a
page (`R_unified_no_enumeration`), so a snapshot cannot come from it alone.

## D_separate_process — Why run as a process of its own rather than inside the app, as swing-mcp does?

The server is a separate JVM that an MCP client spawns over stdio, and it reaches the app only
through a browser pointed at the app's URL. The app needs no change and no extra dependency, it
can be any Vaadin app on any server (Spring Boot, Vaadin Boot, a WAR on Tomcat), and its
lifecycle is its own: the browser opens the start page on the first tool call, so the app need
not be up when the client starts the server. The browser is a process outside both JVMs
anyway. Why not inside the app, as a `-javaagent` like swing-mcp: swing-mcp has to be there,
because the Swing UI exists only in that JVM; a Vaadin UI is reachable over HTTP, and putting a
Playwright driver, its browser and the Unified API on the app's classpath makes them the app's
dependencies, at the app's Vaadin version. The cost: no Browserless backend, which needs the
app's classes and session in the same JVM; when that becomes worth having, it is a second entry
point, not a rewrite of the tools. Why stdio rather than HTTP: the client owns the process, so
the browser lives exactly as long as the client's session and nothing listens on a port.

## D_junit_free_context — Why open the Unified API page through a fake JUnit context rather than wait for an upstream entry point?

The only way the Unified API opens a page is its JUnit extension; the adapter it calls takes an
`AdapterContext` whose `extensionContext()` is a JUnit `ExtensionContext`, and the Playwright
adapter keeps its browser in that context's root store (`R_unified_junit_bound`).
`JUnitFreeContext` hands the adapter a proxy that answers `getRoot()` and `getStore(..)` with a
map-backed store and throws on anything else, then wraps the session in the API's own
`UnifiedPageImpl`. That is one class, and it works on 0.1.0-alpha2. Why not wait for upstream:
a JUnit-free factory (say `UnifiedPages.open(tool, settings)`, with `extensionContext()`
optional) is the right fix and worth asking for, but the tools should not wait on it. Why not
copy the adapter's code: it would fork the part — the session, the Dramafinder bridge — the
Unified API exists to maintain (`D_unified_api_backend`). The cost we carry: we lean on the
`internal` package (`UnifiedPageImpl`, `TestRun`) and on which `ExtensionContext` methods the
adapter happens to call, so an upgrade of the Unified API is checked by `ToolsTest` before
anything else. When upstream gains the entry point, this class is deleted.

## D_single_jar_release — Why release one jar with every dependency on GitHub Releases, rather than a zip of start scripts, a jar per OS, or Maven Central?

A release is one file, `unified-test-mcp-<version>.jar`, built by Shadow with every dependency
merged in, attached to a GitHub release (`RELEASING.md`). An MCP client starts it as
`java -jar <path>`, and that one line is the same on every OS: there is nothing to unpack, and
the only prerequisite is a Java 21 or newer. Why not the `application` plugin's zip of a `bin/`
script and `lib/`: it has to be unpacked before it can be configured, its path points inside the
unpacked folder, and on Windows the script is a `.bat` that an MCP client's config has to wrap in
`cmd /c`; it needs the same Java anyway. Why not a jar per OS, keeping only that platform's
Playwright driver: the driver bundle is most of the size (`R_playwright_driver_bundle`), so a
Linux-only jar would be roughly a quarter of it, but five artifacts per release make the user
pick the right one, and each wants its own smoke test; one jar runs everywhere. Why not
Maven Central: a 210 MB jar is too big to publish there. A thin jar and its POM could go there
instead, since the driver bundle already is, for a launcher that resolves dependencies (jbang);
that is worth adding when someone asks for it, not a reason to give up the one-file download.
