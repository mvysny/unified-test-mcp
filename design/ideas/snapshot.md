# Snapshot: the Vaadin components on the page, as text

On hold: the prototype finds components by id. Parked here so the thinking isn't lost.

The goal, swing-mcp style — one line per component, refs for the ones you can act on:

```
- VerticalLayout
  - Button "Click me" [ref=1] actions: click
  - TextField "Name" [ref=2, value="Alice", invalid] actions: set_value
```

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
   accessible name, value, disabled / invalid / checked, and stamps `data-mcp-ref="N"`.
   One round trip for the whole page.
2. An action takes a ref and resolves it with the Unified API:
   `page.find(<wrapper for the tag>).withAttribute("data-mcp-ref", "N").single()`.
3. A ref that resolves to nothing — Flow re-created the element — is a "stale, snapshot again"
   error. A successful mutation clears the refs, as in swing-mcp.

`Q_ref_mechanism`: attribute stamping writes into the app's DOM (harmless? Flow leaves unknown
attributes alone as far as I know — to check). The alternative, keeping Playwright `Locator`s
per ref server-side, can't be turned into a `UnifiedElement`: `PlaywrightElement`'s constructor
is package-private.

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
