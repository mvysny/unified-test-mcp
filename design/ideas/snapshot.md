# Snapshot: the Vaadin components on the page, as text

Next up: it comes before the blind-agent review (`blind-agent-review.md`), since every lookup
error should send the agent here ("call `snapshot` to see the components on the page").

The goal, swing-mcp style — one line per component, a handle for the ones you can act on:

```
- Button "Save" [ref=3, id=save] actions: click
- TextField "Name" [ref=4, id=name, value="Alice", invalid "Required"] actions: set_value
- Button "Close" [ref=5, aria-label]
```

## A line: the type, one name, the state, the actions

The words are Flow's and the Unified API's, which agree; never Vaadin 8's "caption".

**The name**, quoted: the one property a person tells the component apart by, chosen by type —
- the text, for a `HasText` component (Button, Span): the Unified API's `withText`, `getText()`;
- the label, for a `HasLabel` one (the fields, Checkbox, Tab): `withLabel`, `getLabel()`;
- with neither, its `aria-label` (`HasAriaLabel`, an icon-only Button), then its placeholder
  (`HasPlaceholder`), the bracket saying which (`[aria-label]`, `[placeholder]`).

The tool description says where the name comes from per type, so an agent fixing a Java test
writes `withText("Save")` for a Button and `withLabel("Name")` for a TextField.

**The bracket**: the `ref`; the `id` and test id when set; the value, capped (`HasValue`); and
only the flags that are on — disabled, invalid with its error message (`HasValidation`), checked,
required, read-only. Helper text, and a placeholder that isn't the name, stay out until an agent
needs them: one short line per component.

**Actions: the ones that belong to the type**, from its Unified API wrapper's methods — Button
`click`, TextField `set_value`, Checkbox `check, uncheck`. Any component can be clicked, so `click`
on every Div would be noise; what the agent can't tell from the type is which tool fits, `select`
on Tabs or `set_value` on a ComboBox. A layout or a Div lists none; `click` still takes any ref.

**One table holds the per-type knowledge**, keyed by tag: the Flow class, the Unified API
wrapper, which property is the name, the actions. The snapshot reads it to render a line, and a
tool reads it to turn a ref into a wrapper; `ComponentLookup` grows into it.

## Handles: always a ref; the id shown, and used behind the ref

**Every tool names a component by a `ref`, minted by the snapshot for every component, id or
not.** One kind of handle: every tool has the same schema, as in swing-mcp. An id is not
something a person sees, and an agent's session is transient, not a recorded test, so nothing
needs ids as handles.

**The snapshot still shows the id** (`Button "Save" [ref=7, id=save]`): TestBench, the Unified API
and Karibu-Testing all recommend that an app set ids on what tests touch, and so do we. An agent
fixing a failing Java test reads the id there and writes it into the test.

**How a ref resolves is ours, per session: a map ref → lookup** (`R_unified_lazy_elements`):
- a component with an id unique on the page resolves by it, `withId("save")`: a wrapper re-runs
  its lookup on every call, so Flow re-creating the element doesn't break the ref;
- one without resolves by a stamped attribute, `data-mcp-ref="7"`:
  `page.find(<wrapper>).withAttribute("data-mcp-ref", "7").single()`. Its only other lookup would
  be positional (`all().get(i)`), which silently names a *different* component once one is added
  before it; the stamp pins the actual DOM node. When Flow re-creates the element, the stamp goes
  with the old node, the lookup finds nothing, and the tool says "stale ref, call `snapshot`
  again" instead of clicking the wrong button. A JS property would hide the stamp from the DOM,
  but the Unified API's filters can't query one.

The agent never sees the difference; an app that sets ids just gets fewer stale refs.

**Ref lifetime: a ref lives as long as it is in the latest snapshot.** From swing-mcp (its
`AbstractSwingTool` and `SwingToolContext`) and the Playwright MCP (`R_playwright_mcp_refs`):
- every snapshot — the `snapshot` tool's, or the one an action answers with — replaces the
  session's ref map with the refs it shows;
- a read (`get_text`) leaves the map alone, so refs hold across reads;
- a failed action leaves it alone too, so the agent can retry without a new snapshot (swing-mcp);
- an element keeps its number from one snapshot to the next, since the stamp stays on the element
  (Playwright reuses refs too, but mints a new one when the accessible name changes; ours stays
  through a text change, the component being the same). So a ref the agent still holds after an
  action usually still works, and a second action sent with an old snapshot's ref doesn't fail
  just because the first replaced the map, as it does in swing-mcp.

Errors, each naming the next step:
- no snapshot yet: "no refs yet: call `snapshot`";
- a ref not in the latest snapshot: "ref 7 isn't in the latest snapshot: refs 1–12 are; call
  `snapshot`" (swing-mcp lists the valid refs too);
- a ref in the map whose component is gone — removed, or re-created by Flow: "ref 7: the
  component is gone from the page, re-created or removed; call `snapshot`".

**Stamping is safe** (`R_flow_foreign_attributes`): Flow keeps an attribute it didn't set through
round-trips that change the component, and never sends it to the server; a navigation's new
view starts unstamped, so an old ref finds nothing rather than a new node. Still to check once
the code exists: `withAttribute` through Dramafinder's Button query finds the stamped button.

## The gap

The Unified API cannot list what's on a page: every lookup starts from a type
(`R_unified_no_enumeration`). So the snapshot comes from somewhere else, and the actions should
still go through the Unified API (auto-wait, component semantics).

Seen with the Playwright MCP on `test-app` in dev mode: besides the one `button "Click me"`, its
ARIA snapshot shows the Copilot dev toolbar — a `radiogroup "Mode"` with four radios and a
`button "DevTools"` — noise the Unified API's `APP_CONTENT` rule already filters out.

## A bridge: JavaScript snapshot, attribute refs

1. Through the escape hatch (`page.unwrap(Page.class).evaluate(...)`) one script walks the
   light DOM from `document.body`, keeps Vaadin components (`vaadin-*` tags, plus custom
   elements generally), skips the inside of Vaadin's own shadow roots and the dev tools (the
   `APP_CONTENT` rule the Unified API already uses), reads per element: tag, label / text /
   accessible name, value, disabled / invalid / checked, the id, and stamps `data-mcp-ref="N"`
   on every component — those with an id too, so a node keeps its number from one snapshot to
   the next.
   One round trip for the whole page.
2. An action takes a ref and resolves it with the Unified API:
   `page.find(<wrapper for the tag>).withAttribute("data-mcp-ref", "N").single()`.
3. A ref that resolves to nothing — Flow re-created the element — is a "stale, snapshot again"
   error.

**Component identity: the Flow class.** A line names the component as a Java developer writes it
(`Button`, `TextField`), not its tag (`vaadin-button`). The table comes from the Unified API's
wrappers: each `@UnifiedComponent(Button.class)` names its Flow class, whose `@Tag` is the tag;
the same table gives the wrapper an action tool looks the component up with
(`page.find(UnifiedButton.class)`). There's no registry of the wrappers, so we list them (or
scan the `component` package). A custom element with no wrapper shows its tag: `<my-rating>`.

**Pruning, until it bites: a layout without an id is transparent**, its children promoted a
level, as swing-mcp does with unnamed `JPanel`s. A layout with an id stays, since the app named it.
Revisit when an agent can't tell which of two identical forms it is looking at.

**An action returns the fresh snapshot**, as the Playwright MCP does: `click`, `navigate` and every
later tool that changes the page answer with their echo line, then the snapshot taken after the
server round-trip. The agent sees what its action did without a second call, and a ref it reads
there is current. A read (`get_text`) returns only what it read. The size stays within the
token promise: one line per component on the screen.

Alternative rejected for now: snapshot by asking the Unified API for `all()` of every wrapper
type — a flat list, no tree, misses components without a wrapper, one browser round trip per
type.
