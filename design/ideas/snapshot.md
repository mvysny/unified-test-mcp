# Snapshot: the Vaadin components on the page, as text

Next up: it comes before the blind-agent review (`blind-agent-review.md`), since every lookup
error should send the agent here ("call `snapshot` to see the components on the page").

The goal, swing-mcp style — one line per component, a handle for the ones you can act on:

```
- VerticalLayout
  - Button "Click me" [id=clicker] actions: click
  - TextField "Name" [ref=2, value="Alice", invalid] actions: set_value
```

## Handles: the id first, a stamped ref where there is none

**Ids are the handle.** TestBench, the Unified API and Karibu-Testing all recommend that an app
set ids on what tests touch; so does this server. A component with an id unique on the page is
named by it. **A ref is the fallback**: a component with no id, or one whose id isn't unique,
gets a ref from the snapshot.

**The ref is a stamped attribute, `data-mcp-ref="7"`, not a Java handle** (`R_unified_lazy_elements`).
A Unified API wrapper holds its lookup, not an element, so it never goes stale like TestBench's
`ButtonElement` did; but for a component with no id, the only lookup left is positional
(`all().get(i)`), which silently names a *different* component once one is added before it. The
stamp pins the actual DOM node: `page.find(<wrapper>).withAttribute("data-mcp-ref", "7").single()`.
When Flow re-creates the element, the attribute goes with the old node, the lookup finds nothing,
and the tool says "stale ref, call `snapshot` again" instead of clicking the wrong button. A JS
property would hide the stamp from the DOM, but the Unified API's filters can't query one.

`Q_handle_parameter`: one parameter that takes either (`"clicker"` or `"ref=7"`), or `id` and
`ref` as two optional parameters, exactly one given? swing-mcp has `ref` only.

`Q_ref_lifetime`: swing-mcp clears refs after every successful mutation. Here a stamp survives
until Flow replaces the element, and a stale one fails cleanly — so keep refs across actions, and
only re-stamp on the next snapshot?

`Q_stamp_safety`: Flow leaves an attribute it didn't set alone (to verify): it neither syncs it
to the server nor removes it on the next round-trip. An app's own CSS matching `[data-mcp-ref]`
is not a realistic worry.

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
   on what has no unique id.
   One round trip for the whole page.
2. An action takes a ref and resolves it with the Unified API:
   `page.find(<wrapper for the tag>).withAttribute("data-mcp-ref", "N").single()`.
3. A ref that resolves to nothing — Flow re-created the element — is a "stale, snapshot again"
   error.

`Q_component_identity`: name a component by its Flow class (`Button`, `TextField`) — what a Java
developer writes — mapped from the tag through the wrappers' `@UnifiedComponent`; an unknown
custom element shows its tag (`<my-rating>`).

`Q_pruning`: layouts without a name are transparent (children promoted), as swing-mcp does with
unnamed `JPanel`s? Or does the agent need to see `HorizontalLayout` to reason about the screen?

`Q_after_action`: should `click` return the fresh snapshot (Playwright MCP does) to save a
round trip?

Alternative rejected for now: snapshot by asking the Unified API for `all()` of every wrapper
type — a flat list, no tree, misses components without a wrapper, one browser round trip per
type.
