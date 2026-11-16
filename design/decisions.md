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
lifecycle is its own: the browser starts on an empty page and goes only where the agent
navigates, so the server knows no app URL, and the app need not be up when the client starts it. The browser is a process outside both JVMs
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
anything else. When upstream gains the entry point, asked for in
[unified-testing-tool#219](https://github.com/parttio/unified-testing-tool/issues/219), this
class is deleted.

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

## D_browser_per_session — Why does each MCP session get a browser of its own rather than the server sharing one?

The handler creates a `UnifiedBrowser` when a session starts and closes it when the session
closes; nothing about the browser outlives the session. A browser is its own Vaadin session —
cookies, the UI, a view's state — so two agents connected over HTTP each drive their own UI
and never click in each other's; a client that reconnects starts on a fresh page instead of
inheriting where the last one left off; and the browser's lifetime is the session's, which
TinyMCPServer already ends on stdin's EOF, on DELETE, on 30 minutes idle and on `stop()`, so an
abandoned HTTP client does not keep a Chromium alive. Per-agent state that comes later (refs
from a snapshot) belongs to the same session. Why not one browser for the server: on stdio, the
only way the server ships, there is one session anyway, so sharing would buy nothing there and
cost isolation on HTTP. Why not swing-mcp's one session at a time, a newcomer evicting the old:
there, every session drives the one Swing UI in the one
JVM, so two would fight; here sessions share nothing but the app's server. The cost: each HTTP
session launches a browser of its own, a second or two and a Chromium's memory; still lazy,
on the session's first tool call.

## D_refs_not_ids — Why does every tool take a snapshot's ref rather than the component's id?

Every tool that acts on a component takes a `ref`, a number the snapshot shows on the
component's line; the snapshot mints one for every component it shows, id or not. One kind of
handle, so every tool has the same schema, as in swing-mcp. Why not ids: an id isn't something a
person sees, plenty of components have none, and an agent's session is transient, not a test
anyone keeps. The snapshot still shows the id (`Button "Save" [ref=3, id=save]`): TestBench, the
Unified API and Karibu-Testing all tell an app to set ids on what tests touch, and so do we; an
agent fixing a Java test reads it there. Why not take both, `id` or `ref`: every tool would carry
two optional parameters and a rule that exactly one is given.

How a ref finds its component again is ours, per session (`Refs`). A component whose id was
unique on the page is found by the id: a Unified API wrapper re-runs its lookup on every call
(`R_unified_lazy_elements`), so Flow re-creating the element doesn't break the ref. Any other by
a `data-mcp-ref` attribute the snapshot stamped on it. Why a stamp: without an id, the only
lookup left is positional, `all().get(i)`, which silently names a *different* component once
one is added before it; the stamp sits on the actual DOM node, and when Flow re-creates the
element the stamp goes with the old node, so the lookup finds nothing and the tool says the
component is gone, instead of clicking the wrong one. Flow keeps the attribute through
round-trips and never sends it to the server (`R_flow_foreign_attributes`). Why not a JavaScript
property, which keeps the DOM clean: the Unified API's filters match attributes only. Why not a
Java handle, as TestBench's `ButtonElement`: a Unified API wrapper holds its lookup, not an
element, so for a component without an id there is nothing stable to hold.

A ref is valid while the latest snapshot shows it: every snapshot (the `snapshot` tool's, or
the one an action returns) replaces the session's refs; a read or a failed action leaves them,
so the agent can retry without a new snapshot. The script keeps a stamp already on an element,
so a component keeps its number from one snapshot to the next, through a text change too (the
Playwright MCP mints a new ref when the accessible name changes, `R_playwright_mcp_refs`; ours
names the element, not its text). So a second action sent with an older snapshot's ref still
works if its component survived, where swing-mcp, clearing its refs after each action, fails it.
The numbers come from a per-session counter in Java that never goes back, not from the page: a
counter in the page would start again at 1 after every reload, and an agent's ref from before a
`navigate` would name a different component after it. Each way a ref fails names the next step:
no snapshot yet, a ref not in the latest snapshot (listing those that are), a component gone.

## D_snapshot_from_javascript — Why is the snapshot one script in the page rather than Unified API lookups?

The Unified API can't list what's on a page: every lookup starts from a component type
(`R_unified_no_enumeration`). So the snapshot is one script, run through the escape hatch: it
walks the light DOM from `<body>`, stamps, and reads each component's tag, id, own text, label,
`aria-label`, placeholder, value and states, in one browser round-trip. The actions still go
through the Unified API, for its waiting and its component semantics. Why not ask the Unified API
for `all()` of every wrapper type: a flat list with no nesting, nothing for a component without
a wrapper, and one round-trip per type. Why not the browser's ARIA snapshot, as the Playwright
MCP: it names roles and DOM, not Vaadin components, and shows the dev tools' own buttons.

A line names the Flow class a Java developer writes (`TextField`, not `vaadin-text-field`), from
`ComponentTypes`; an unlisted `vaadin-*` tag gets a name derived from it (`SplitLayout`), any other
custom element shows its tag. The quoted name is the property a person tells the component apart
by, in Flow's and the Unified API's words, which agree: the text for a `HasText` component
(`withText`), the label for a `HasLabel` one (`withLabel`), else the `aria-label`, then the
placeholder, the bracket saying which. Never Vaadin 8's "caption". The bracket shows the id, a
field's value capped at 80 characters (a name never is: it says which component this is), and
only the states that are on. `actions` lists only the tools made for that type, and only tools
this server has: any component takes a click, so `click` on every Div would be noise; what the
agent can't tell from the type is which tool fits. Left out, so lines stay short: a layout without
an id and plain HTML with neither id nor text of its own (their children take their place, as
swing-mcp does with unnamed panels), Flow's bootstrap wrappers (`#outlet`, `flow-container-*`),
the parts a Vaadin component renders into its own light DOM (its input, label, messages), icons,
the dev tools, and anything `hidden`. A layout with an id stays: the app named it. Revisit the
pruning when an agent can't tell two identical forms apart.

## D_action_returns_snapshot — Why does an action answer with the whole snapshot rather than an echo?

`click` and `navigate` answer with an echo line, then the snapshot taken once the server has
handled the action. The agent sees what its action did without a second call, and the refs in it
are current; `navigate`'s snapshot starts with the URL the browser landed on, which tells a
reroute, to a login view say, from the URL asked for. The Playwright MCP does the same. Why not
swing-mcp's echo, "Dispatched …, call swing_snapshot to verify": every action would cost the
agent a second call, and a second chance to act on a stale picture. The cost, a snapshot's tokens
per action, stays within the promise: one line per component on the screen. A read, `get_text`,
returns only what it read.
