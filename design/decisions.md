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

## D_browser_install — Why does the server install its browser itself rather than leave it to Playwright?

Left to itself, `Playwright.create()` on a fresh machine downloads every browser it supports, 1.3
GB, with `--browser-executable` too, and writes the download's progress to the process's stdout:
on stdio, progress bars land in the protocol between the JSON-RPC lines, and the first tool call
answers only once all of it is in (`R_playwright_thread_safety`). Playwright keeps both by design.
So `BrowserInstall` asks for Playwright's driver first, without the download — the driver is one
per JVM, and every `Playwright` the Unified API creates later shares it — and installs only the
browser the settings open: Chromium's headless shell, 114 MB to download, or Chromium itself with
`--headed`; nothing with `--browser-executable`. Its output goes to the log. It starts when the
server does, so a first download is under way while the client is still connecting, and the
first tool call waits only for what is left.

Why not `PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1` in the README's `claude mcp add` line: every user
would have to know it, one who doesn't gets a broken first session, and without an executable it
still leaves the download to Playwright, onto stdout. Why not wait for the Unified API to skip the
download when given an executable (parttio/unified-testing-tool#299): that covers the executable
alone. Why not a `System.setOut` around the first browser: Playwright's install inherits the
process's file descriptor, which `System.setOut` doesn't move. The cost: `Driver` is Playwright's
internal class, `com.microsoft.playwright.impl.driver`, so a Playwright upgrade may change it
without notice; a renamed method fails the build, but a driver that stops being one per JVM would
bring the full download back silently.

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
(`withText`), the label for a `HasLabel` one (`withLabel`). Never Vaadin 8's "caption". A component
with neither has no quoted name, and its bracket shows `aria-label="Close"`, else
`placeholder="…"`: an icon-only Button reads as one at a glance. Why not quote the aria-label as
the name, the bracket marking it with a bare `aria-label`: two blind agents and the owner read
that as an empty aria-label beside a caption. The bracket shows the id, a
field's value, and only the states that are on. Every field shows `value=`, written as `get_value`
answers it — `""` for an empty field, `null` for nothing selected, `["Finland","Sweden"]` — and
capped at 80 characters (a name never is: it says which component this is). So a missing `value=`
means one thing, not a field; why not leave an empty field's out to save tokens, as the states
are: then it reads the same as a Button's, and a blind agent called `get_value` to tell them
apart. A Checkbox shows `checked` instead, its value already. `actions` lists only the tools made for that type, and only tools
this server has: any component takes a click, so `click` on every Div would be noise; what the
agent can't tell from the type is which tool fits. Left out, so lines stay short: a layout without
an id and plain HTML with neither id nor text of its own (their children take their place, as
swing-mcp does with unnamed panels), Flow's bootstrap wrappers (`#outlet`, `flow-container-*`),
the parts a Vaadin component renders into its own light DOM (its input, label, messages; a field's
whole inside, a ComboBox's scroller, a RadioButtonGroup's buttons), icons,
the dev tools, and anything the user can't see (`D_visible_only`). A layout with an id stays: the app named it. Revisit the
pruning when an agent can't tell two identical forms apart.

## D_modal_shows_alone — Why does a snapshot show only the modal dialog on top, rather than the whole page with it?

While a modal Dialog or ConfirmDialog is open, the snapshot walks from the topmost modal only,
and a line above it names the stack, top first: `Modal: Dialog "Why?" over Dialog "Delete
order?" over the page; only the dialog on top is shown: nothing under it takes input`. This is
swing-mcp's rule, that tools see only the windows a user could interact with right now: a ref
addresses what a user could act on now. A Button under the backdrop can't be clicked: the Unified API would wait for it to become
actionable and time out, and under `STRICT` modality the server ignores its event anyway
(`R_vaadin_dialog_dom`). A ref on it misfires; a line for it is a non-option on the agent's
decision surface.

**Why the `Modal:` line**, as swing-mcp's modal-stack header: without it the page vanishes
after a click, and the agent can't tell a dialog from a navigation, nor what closing it returns
to. Why on every modal, where swing-mcp shows one only for a stack of two: a browser modal always
has the page under it.

**Why the topmost is the last open `aria-modal="true"` element in document order**: Flow appends
a dialog to `<body>`, or inside the modal that opened it (`R_vaadin_dialog_dom`), so a later one
is on top. Why `aria-modal` rather than a list of tags: Dialog and ConfirmDialog both set it, and
so does any web component that tells a screen reader it is modal. One closed but still in the DOM
is told by its `opened`.

**Why not keep the page's lines and mark them blocked**: every tool would then need its own
is-this-blocked check — swing-mcp rejected that too. The stamps stay on the page's elements, so
once the dialog closes the page comes back with its old refs (`D_refs_not_ids`); swing-mcp,
numbering from 1 on every snapshot, can't do that. A ref under the modal fails with an error
naming the modal and how to get past it.

A modeless dialog shows with the page, where its element is: the page still takes input.

## D_visible_only — Why does the snapshot list only what the user can see, by `checkVisibility()`, rather than everything not `hidden`?

Vaadin hides most things without `hidden`: a closed Details' content, a closed drawer, a closed
menu stay in the DOM (`R_vaadin_hidden_in_dom`). Listed, each is a non-option on the agent's
decision surface, as `D_modal_shows_alone` says of what's under a modal: its fields offer
`set_value`, and the Unified API waits for one to become actionable and times out. So a
component shows only if the browser's `checkVisibility({ visibilityProperty: true })` holds: it
has a box, and isn't `visibility: hidden`. That one function, `Snapshot.IS_VISIBLE`, is what
`Refs` checks too before a tool acts: the page changes without a tool call, by a server push, a
poll, a timer, and a ref to what has been hidden since fails at once, saying so, rather than after
the Unified API's 10 s wait.

**Why one the user can't see stands aside rather than taking what's inside with it**: a
`display: contents` element has no box while its children have, a Dialog's footer `<div>` holding
its buttons; and a child may be `visibility: visible` under a hidden parent. So each element
answers for itself; a closed Details' content fails anyway, being under `display: none`.

**Why `visibility` and not opacity**: a closed drawer is `visibility: hidden`, which the default
`checkVisibility()` ignores. Opacity, which the other option catches only at exactly 0, hid
nothing in test-app but the connection indicator, skipped anyway, and an overlay fading in would be
missed on its first frame. Playwright ignores opacity too, so `click` and the snapshot agree. Why
not Playwright's visible, a non-empty box: a zero-size element the app named, an empty status
Span, would vanish.

**Why the snapshot waits for animations, and only some**: what a transition hides is visible
until it ends, a closing drawer's SideNav for 200 ms after the click, so the click's own answer
would list it (`R_vaadin_animations`). So the snapshot waits, up to 1 s, for keyframe animations,
an overlay opening or closing, and for transitions of `visibility` or `display`; it looks into the
shadow roots, which `document.getAnimations()` leaves out. Not for the other transitions: a
Button's 1.4 s fade after every click, a field's on focus, held up nearly every answer and change
nothing it shows. Not for one that never ends, a spinner's. A dialog told to close, `opened`
false, is left out at once: its exit animation may not have begun when the snapshot looks, so the
click on Cancel would answer with the dialog still listed.

**What it doesn't catch, accepted**: a component covered by another, a modal aside, which
`aria-modal` gives away. Telling one apart takes hit-testing at a point, a guess for one partly
covered or scrolled out of view; `click` meets it, Playwright reporting what intercepts the
pointer. Opacity near 0, zero size, clipping, a component placed off screen: off the viewport
can't be the test, as one below the fold is there for a user who scrolls. A hint isn't a
component: `tooltip=` reads a tooltip that shows only on hover.

## D_action_returns_snapshot — Why does an action answer with the whole snapshot rather than an echo?

`click` and `navigate` answer with an echo line, then the snapshot taken once the server has
handled the action. The agent sees what its action did without a second call, and the refs in it
are current; `navigate`'s snapshot starts with the URL the browser landed on, which tells a
reroute, to a login view say, from the URL asked for. The Playwright MCP does the same. Why not
swing-mcp's echo, "Dispatched …, call swing_snapshot to verify": every action would cost the
agent a second call, and a second chance to act on a stale picture. The cost, a snapshot's tokens
per action, stays within the promise: one line per component on the screen. A read, `get_text`
or `get_value`, returns only what it read.

## D_text_or_value — Why does `get_text` refuse a field rather than return the field's DOM text?

A field holds a value, not a text, as in Flow: `TextField` implements `HasValue`, not `HasText`.
Its DOM text is its label, so `get_text` on the Name field would answer "Name", and an agent
asked what the field says would report the label as its content, without an error to tell it
otherwise. So the two reads split by what the component holds (`ComponentTypes.Holds`):
`get_text` takes a Button, a Span, a Paragraph; `get_value` takes the fields and a Checkbox,
whose value is whether it's ticked. A field holding an item — a ComboBox, a Select — answers with
the item's label, as the snapshot's `value=` does: the browser has only Flow's key for the item,
meaningless to the user, and the label is what the user sees and the Unified API picks by
(`R_flow_item_keys`). swing-mcp's `swing_get_selection` reports a `JComboBox` the same way, by
its rendered name. Each refuses the other kind, naming the right tool. A type
`ComponentTypes` doesn't list takes either. Why not one `read` tool that picks by type: a tester
asks either "what does it say" or "what's in it", and a tool named after the question is the one
an agent finds.

## D_password_shown — Why does a PasswordField show its password, where swing-mcp refuses to read one?

The snapshot's `value=` and `get_value` show a PasswordField's value as it is, `"secret"`, like
any TextField's. swing-mcp refuses to read a password field, so
the server can't become a channel for reading credentials. Here, refusing would protect nothing:
the password is the `value` property of an element in a browser the agent drives, which the
Unified API reads (`UnifiedTextField.getValue()`), as does any script on the page or the
browser's console. It would cost the agent the check it needs: did the login form get what I
typed. The cost of showing it: a session's log holds the password typed.

## D_hints_as_keys — Why does the snapshot show `helper=` and `tooltip=`, each read whole by its own tool, rather than swing-mcp's one description slot?

swing-mcp puts a component's description, its `accessibleDescription` or else its tooltip, in a
second quoted string after the name, cut at 120 characters, and reads it whole with
`get_description`. Here each hint is a key in the bracket, `helper="We never share it"`,
`tooltip="Close without saving"`: the agent knows where the text came from, below the field or on
hover, without a legend, and a component with both shows both rather than one as the other's
fallback. Each is read uncut by a tool named after its key, as `value=` is by `get_value`, so the
`…` of a cut `helper=` points at `get_helper`. Why not one tool for both: its answer would mix two
texts when the agent wants one, and its name would say neither. The cut is swing-mcp's 120
characters: a hint is advice, so its tail is cheap to lose; a name is never cut, as it says which
component this is.

## D_login_from_i18n — Why does the snapshot read a LoginForm's heading and error from its `i18n`, rather than the text its shadow DOM shows?

A LoginForm renders both in its shadow DOM, and nothing in its light DOM changes when its error
shows (`R_vaadin_login_dom`); the snapshot walks the light DOM only
(`D_snapshot_from_javascript`). What the shadow DOM renders is the component's `i18n`, which
Flow sends whole, so the script reads that, as it reads a field's `errorMessage` property rather
than its rendered message: `LoginForm "Log in"`, and once the app's `setError(true)` lands,
`error "Incorrect username or password: Check that you have entered…"`. A LoginOverlay's error
the same. Why not the shadow DOM: its parts are the web component's internals, free to move
between versions, and the property holds the same text.

**Why `error "…"`, shaped as a field's `invalid "…"`**: one notation for "this shows an error,
saying so", the state in Flow's word (`setError`), then its message. Why not `error="…"`, a key
as `helper=` is: two notations for one thing, the agent learning both. **Why the title and the
message both**, where a hint is cut: no tool reads the message otherwise, and an app may put the
substance there, "Account locked for 15 minutes"; it costs tokens only while the error shows.

**A LoginOverlay is named by its title**, as a Dialog by its header, on its line and on the
`Modal:` line. It has no `aria-label`, its `aria-labelledby` naming its title element
(`R_vaadin_login_dom`), so a dialog's name is its `aria-label`, else the text of its title
element; the snapshot leaves that element out, as a Dialog's, so the text moves into the name
rather than showing twice. **Its description shows as `description="Your mail, anywhere"`**,
Flow's word, uncut as a name is. Why not `helper=`: that is the text below a field, and its `…`
points at `get_helper`, which refuses anything but a field (`D_hints_as_keys`). Why show it at
all: it is text the user sees, and an agent that can't find it may report it missing.

## D_set_value_reads_back — Why does `set_value` read the field back and answer with a note, rather than trust its action or fail when the field didn't take the value?

A field can refuse a value in two ways, and only one is known before acting. What the page says
up front is refused before anything is touched: a disabled or read-only field, a value of the
wrong kind, a label the field doesn't offer — that refusal lists the labels it does, the one thing
an agent can't guess. The rest is the app's, past what any table of field types foresees: a
value-change listener that puts the old value back, a `maxLength`, Flow's own validation, a
TimePicker that takes typing only on Enter. So once the server has answered, `set_value` reads the
field as `get_value` does and compares; `value` is in `get_value`'s notation, so the two compare as
they are, a selection in any order, a number at any scale. The Unified API's `selectItem` does the
same: it checks the label first and reads the selection after.

Why not a tool error when the field holds something else: the typing happened, the app's listeners
ran, the page may have changed, and that is often what the agent is testing — type 130 into Age,
expect "At most 120". An error says nothing happened, and comes without the snapshot. So the echo
line says what the field holds now and its error message when it shows invalid, and the snapshot
follows (`D_action_returns_snapshot`). A DatePicker's text it can't parse is the one case where
what the field shows and what it holds part: the `<input>` keeps `dsdds` while the value is empty
(`R_flow_field_states`), so the note says the field can't read it and holds none, rather than "it
holds it". Why not swing-mcp's split into `set_text`, `set_value` and
`set_selection`: those follow Swing's accessibility interfaces; here `get_value` already reads
every field in one notation, and the tool that writes them mirrors it, as `get_value` mirrors
`value=`.

## D_values_as_shown — Why does a DatePicker's value read and take `"31.1.2000"`, the text the user sees, rather than ISO `"2000-01-31"`?

The agent drives a browser, so it gets what a person there sees (**Vaadin components, not
DOM**): a DatePicker in a `fi` app shows `31.1.2000`, and that is its value in the snapshot, in
`get_value` and in `set_value`. A TimePicker the same, `7.30`; a DateTimePicker its two inputs,
`["17.11.2026","10.00"]`, two texts because splitting one between two locale formats would be a
guess (`7:30 AM` has a space). A BigDecimalField's `value` property already is the locale's text,
`9,90` (`R_flow_field_states`), and a JSON number for it is typed with that separator. ISO is the
server's view, right for a browserless test that sets a `LocalDate`; here it would be a format no
user sees.

`set_value` types the text as it is and presses Enter, unparsed, through `UnifiedDatePicker.typeText`
or the field's `<input>`. So the app's own parser judges it, in its locale and with its custom
`DatePickerI18n` format, and text it can't read, `dsdds` or a DateTimePicker's date alone, is how
the agent tests its invalid input; the note says so (`D_set_value_reads_back`). Why not
`UnifiedDatePicker.setValue(LocalDate)`: on Playwright it sets the `value` property from behind
(`R_flow_field_states`), no typing, no parser, against **An agent drives the app the way a person
does**. Why not a `type_text` tool beside an ISO `set_value`: two notations for one field, and the
agent would read ISO in the snapshot and still have to guess what to type. A ComboBox that allows
custom values follows the same rule: a text none of its items has is typed and committed, and the
app's custom-value listener decides what it becomes.

The cost: the value depends on the app's locale, which without an `I18NProvider` is the browser's
language; test-app fixes `fi-FI`. A format that drops information (`MMM yyyy`, or `1/2/2026` read
either way) leaves the agent to check the stored date where the app shows it.

## D_app_layout_regions — Why does an AppLayout show its navbar, drawer and content as regions, and a closed drawer as a line, rather than its children flat?

Flat, the app's chrome and its view read as one list: the DrawerToggle, the app's title, every
SideNavItem, then the view's first field, nothing saying where the view starts. An agent testing a
view wants its content, one getting around wants the navbar and the drawer. So an AppLayout's
children show under `- Navbar:`, `- Drawer:` and `- Content:`, its slots
(`R_vaadin_app_layout_dom`): lines without a ref, as no tool acts on a region; one showing nothing
is left out.

A closed drawer's content isn't listed (`D_visible_only`), so nothing but the DrawerToggle's
`aria-label` would say there is a drawer to open. `- Drawer: closed; click the DrawerToggle, ref=2,
to open it` says it and names the way in; the ref only while the toggle is shown, as an app may
open its drawer by other means.

The cost: every line under an AppLayout is indented once more.

## D_nav_item_states — Why does a SideNavItem show `path=` and `collapsed`, and expand by a `toggle_expand` tool, rather than hide where it links, or set a state through `expand` and `collapse`?

`path=` shows where a SideNavItem or a BreadcrumbsItem links, as the app gave it
(`R_vaadin_side_nav_dom`): a tester checks that a link leads where it should without following
it. It tempts the agent to `navigate` straight there rather than click as a user does; but
`navigate` takes any URL anyway, so leaving it out would keep nothing from the agent but that check.

A collapsed item's items aren't listed (`D_visible_only`), so without a mark nothing tells the
agent that "Archive" holds more; `collapsed` does, as the arrow tells a user. `expanded` isn't
shown: the items listed under the item say it. `current` marks the SideNavItem and the
BreadcrumbsItem of the page shown, as their highlight does for a user: the URL line says where the
browser is, not which item that is.

Two ways to show an item's items, as for a user (`R_vaadin_side_nav_dom`): `click` opens its page,
and the item, now `current`, expands; `toggle_expand` clicks its arrow, which expands or collapses
it where it is, and is the only way to collapse one with a page. A toggle, as the arrow is and as
swing-mcp's `swing_toggle_expand` is for a JTree node. Why not `expand` and `collapse`, which say
the state wanted and do nothing when it's there: the answer is the new snapshot
(`D_action_returns_snapshot`), so the agent always toggles from the state it has just seen, and
the answer's first line says which way it went. Why not `expand_nav_item`: a TreeGrid's rows want
the same, and a tool per component would grow the list. Its `actions:` entry is the one listed by
a component's state rather than its type: only an item with items expands.

`click` and `toggle_expand` click a part of the item's shadow DOM, its link and its arrow:
`click` at an expanded item's centre lands on its first item. No Unified API wrapper clicks
either, so `ShadowParts` clicks through the escape hatch and waits for the server as the Unified
API does, through Dramafinder's `AutoWait`.
