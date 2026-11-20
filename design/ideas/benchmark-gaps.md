# What the server lacks for the benchmark

The benchmark's 12 scenarios (`benchmark/scenarios/`, `benchmark.md`) through this server: which
pass today, which can't, and what each needs. From the first CRM round's probe
(`benchmark/results/2026-11-18-crm/`), and on 2026-11-19 two walks through Bakery (seed 1), driven
over stdio, its DOM looked up beside it: with the server as of 5f4434c, and as of e3010d1, which
walks the templates' shadow roots.

Two kinds of gap: a **blocker** leaves a scenario undoable through our tools; a **cost** lets it
pass at many times a competitor's tokens, which is what the benchmark measures.

## Where each scenario stands

| Scenario | Today | What stands in the way |
|---|---|---|
| crm-add-contact | passes, $0.24 (Playwright MCP $0.14) | unmeasured since a row a line (`D_grid_as_rows`) |
| crm-invalid-email | passes, $0.19 ($0.12) | the same |
| crm-move-contact | passes, $0.16 ($0.12) | the same |
| crm-count | passes, 210 turns, $1.23 ($0.06) | unmeasured since `get_rows` (`D_get_rows_scrolls`) |
| crm-delete-closed | passes, 130 turns, $1.06 ($0.22) | the same |
| bakery-locked-user | passes in the scripted walk: Edit, Role, Save, the persistent notification in Save's answer | — |
| bakery-price-cap | untried since §1 | the Crud editor shows inline, no `Modal:` line (§1) |
| bakery-new-user | untried since §1 | the same |
| bakery-new-order | passes in the walk, the app's check included | each product pick waits 10 s (`D_set_value_reads_back`) |
| bakery-edit-items | untried since §1 | its picks and deletes re-create the item editors, as new-order's |
| bakery-reschedule | blocked | a card's row takes no `click(ref, row)` (§1); the Send button shows since `D_field_affixes` |
| bakery-today-count | passes in the walk: Bakery, Ready, 3, off the first snapshot's rows | a Delivered badge shows only an icon (§3) |

Ours: the probe, one run each, of a work-in-progress tree whose grid showed a line per cell (its
`NOTES.md`). In brackets: Playwright MCP's median, the cheaper competitor in every CRM scenario.

## 1. Lit templates — walked on Bakery

The snapshot walks the app's shadow roots (`D_app_shadow_roots`, `R_bakery_template_dom`); the
Unified API's lookups reach into them only in the local `-shadow` build
(`R_unified_no_enumeration`). Left:

- **Upstream**: parttio/unified-testing-tool#347 asks whether an app template's shadow root may be
  searched on every Testing Tool, the spec saying not since their #251; once they answer, a PR
  from the clone's `shadow-dom-queries-alpha3`, ported to `main`. Until a release has it, the
  build needs the `-shadow` in `~/.m2`.
- **A row of a template takes no `click(ref, row)`** — blocks bakery-reschedule: "Cannot click
  row 0: the row has no data cell with a spot free of interactive content". Dramafinder's
  `freePoint` (`GridElement`, 2.2.0) counts every custom element on the hit's path as
  interactive, and `<order-card>` fills the cell. A click on the card's own ref, from
  `snapshot(ref)`, opens the order. parttio/dramafinder#298: look inside an app's custom element,
  its shadow root's `elementFromPoint`, and test what that hits.
- **The Crud editor** shows inline at the end of the page, no `Modal:` line, the grid under it
  listed as if it took clicks: its `<vaadin-crud-dialog>` is in `<vaadin-crud>`'s shadow root, a
  Vaadin component's, which no walk enters. Crud is commercial; price-cap and new-user may pass
  regardless, as locked-user does.

## 2. Grid — the storefront's cards

The CRM's cost, the grid's cells in every answer and the rows beyond the rendered ones, is built
(`D_grid_as_rows`, `D_get_rows_scrolls`); the next CRM round measures it.

The storefront has 158 orders, 23 cards rendered: bakery-today-count's "Today" group sits on top,
and bakery-reschedule's target is found by the search, so neither needs rows beyond the rendered
ones on seed 1. A row is one `<order-card>`, its preview cut at 50 characters in the snapshot
and in `get_rows`: `This week Fri, Nov 20 - Sun, Nov 22 ready Fri 20 8…` loses the location
and the customer on a group's first row. `snapshot(ref)` cuts nothing but gives each Span its ref,
~15 a row.

## 3. Smaller, each found walking Bakery

- **A state shown only by an icon** — bakery-today-count when its status is Delivered:
  `<order-status-badge status="delivered">` hides its text and shows `vaadin:check`, and the
  snapshot skips every `vaadin-icon`. The scenario picks any (location, status) group of today's
  orders. `Q_lone_icon`: a `vaadin-icon` with no text beside it as a line of its own,
  `Icon "vaadin:check"`?
- **Links and tabs**: Bakery's menu shows as `Tab [value="Storefront"]` with `Anchor "Storefront"`
  inside it, the CRM's drawer as `Anchor "List"`: no `href`, no `actions: click`, no `selected` on
  the current tab. An agent clicks the Anchor anyway, or guesses `/users`; `path=` as on a
  SideNavItem (`D_nav_item_states`) and the cleanups in `disclosure.md` and
  `snapshot-cross-cutting.md` would make it sure. Not a blocker.

Crud is commercial, out of scope while the project is a prototype: the three Crud scenarios need
only what's generic, §1's search bar and §2's rows. A row's `<vaadin-crud-edit>` has no type of
ours, so by the code it isn't one a user acts on and shows as its text, none
(`D_component_views`); `snapshot(ref)` gives it a ref. Unchecked on Bakery since the rows.

## Order

1. §1: the row click (#298), the upstream PR.
2. §2: the storefront's cards cut at 50 characters.
3. §3's lone icons (today-count), then the rest of §3.

Then a CRM round on a committed tree, and Bakery's first.
