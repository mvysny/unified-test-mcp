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
| crm-add-contact | passes, $0.24 (Playwright MCP $0.14) | cost: the grid's cells in every answer (§2) |
| crm-invalid-email | passes, $0.19 ($0.12) | the same |
| crm-move-contact | passes, $0.16 ($0.12) | the same |
| crm-count | passes, 210 turns, $1.23 ($0.06) | cost: rows beyond the rendered ones (§2); the snapshot lagging the filter (§3) |
| crm-delete-closed | passes, 130 turns, $1.06 ($0.22) | the same |
| bakery-locked-user | passes in the scripted walk: Edit, Role, Save, the persistent notification in Save's answer | — |
| bakery-price-cap | untried since §1 | the Crud editor shows inline, no `Modal:` line (§1) |
| bakery-new-user | untried since §1 | the same |
| bakery-new-order | passes in the walk, the app's check included | each product pick waits 10 s (§1) |
| bakery-edit-items | untried since §1 | its picks and deletes re-create the item editors, as new-order's (§1) |
| bakery-reschedule | blocked | the comment's Send button, in a field's suffix (§4); a card's row takes no `click(ref, row)` (§1) |
| bakery-today-count | passes in the walk: Bakery, Ready, 3, off the first snapshot's rows | a Delivered badge shows only an icon (§4) |

Ours: the probe, one run each, of a work-in-progress tree (its `NOTES.md`). In brackets: Playwright
MCP's median, the cheaper competitor in every CRM scenario.

## 1. Lit templates — walked on Bakery

Built: the snapshot walks the app's shadow roots (`D_app_shadow_roots`), test-app's `template`
view its test; the Unified API's lookups reach into them only in the local `-shadow` build
(`R_unified_no_enumeration`). Bakery's DOM: `R_bakery_template_dom`. On Bakery, Lit's own
rendering shows whole: the search bar, the order editor and details, `<buttons-bar>`'s slotted
buttons, each `<order-item-editor>`'s repeated ids with refs of their own. Left:

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
- **A field the app replaces while it takes the value** waits 10 s: `set_value` answers "it isn't
  shown now" (`D_set_value_reads_back`), but first the Unified API checks the pick on the old
  element (`SelectionSupport.select`, `access.selectedItems()`), and Playwright waits its timeout
  for it. parttio/unified-testing-tool#377: no check on a field the lookup no longer finds.
- **The Crud editor** shows inline at the end of the page, no `Modal:` line, the grid under it
  listed as if it took clicks: its `<vaadin-crud-dialog>` is in `<vaadin-crud>`'s shadow root, a
  Vaadin component's, which no walk enters. Crud is commercial; price-cap and new-user may pass
  regardless, as locked-user does.

## 2. Grid — the CRM's cost, and the storefront's cards

Of the 195 lines in the probe's CRM snapshot, 180 were `GridCellContent`, and every action answers
with the snapshot (`D_action_returns_snapshot`): a 51k-token context on crm-add-contact where
Playwright MCP's was 29k, though ours took fewer turns. Built since, untried on the benchmark yet:

- **A row a line**, at most 10 in view (`D_grid_as_rows`): the context, in all five CRM scenarios.
- **Rows beyond the rendered ones**, `get_rows` (`D_get_rows_scrolls`): crm-count and
  crm-delete-closed read all 200 contacts, of which the CRM's grid renders ~36–38. The probe's
  agent filtered by each last name in turn instead.
- **A component in a row stays a ref**: the Crud's `CrudEdit [aria-label="Edit"]` in every row.
- **A row clicked by its number**, `click(ref, row)`: the CRM opens a contact's form so.
- **Refs in cells move with the recycled cells**, accepted (`D_grid_as_rows`): crm-delete-closed
  deletes rows one after another, each action answering with the snapshot of the rows now.

The storefront has 158 orders, 23 cards rendered: bakery-today-count's "Today" group sits on top,
and bakery-reschedule's target is found by the search, so neither needs rows beyond the rendered
ones on seed 1. A row is one `<order-card>`, its preview cut at 50 characters in the snapshot
and in `get_rows`: `This week Fri, Nov 20 - Sun, Nov 22 ready Fri 20 8…` loses the location
and the customer on a group's first row. `snapshot(ref)` cuts nothing but gives each Span its ref,
~15 a row.

## 3. Settling before the answer — extra calls in every scenario

An action's answer is taken too early in two cases, and the agent calls `snapshot` again, or
polls:

- **A full page load**: the login form POSTs, and the page after it loads anew. Each CRM probe
  run's login `click` answered `URL: …/?continue` and `(no components)`, and the agent's next call
  was `snapshot`; on Bakery three snapshots back to back after the login still showed no
  components.
- **A debounced value change, and the grid fetch it starts**: the CRM's filter is
  `ValueChangeMode.LAZY` (400 ms), Bakery's search a `@DomEvent` debounced 300 ms. `set_value
  "Brennan"` answered with an empty grid; the probe's agent: "Filtering works, but the snapshot
  lags". crm-delete-closed's 53 `get_value` calls on the filter were its way to wait.

`Q_debounce`: nothing in the browser says a debounced event is pending, nor which mode a field has.
Wait until Flow's client is idle and no grid is `loading`, after a pause past the longest default
debounce? Or flush it: does a blur send a LAZY field's value at once? The navigation wait in
`Snapshot` was the same problem, and upstream fixed that one in Auto-Wait
(`AdapterSession.settle()`, parttio/unified-testing-tool#233); this fix may belong there too.

## 4. Smaller, each found walking Bakery

- **A field's own prefix and suffix components** — blocks bakery-reschedule. The order details'
  comment field holds its Send button in its `suffix` slot, and `FIELDS` show none of their
  children. Enter in the field sends too (`_onCommentKeydown`), but `set_value` presses no Enter.
  Show the components the app put in a field's `prefix` and `suffix` slots (an icon there already
  shows as `icon=`)?
- **A state shown only by an icon** — bakery-today-count when its status is Delivered:
  `<order-status-badge status="delivered">` hides its text and shows `vaadin:check`, and the
  snapshot skips every `vaadin-icon`. The scenario picks any (location, status) group of today's
  orders. `Q_lone_icon`: a `vaadin-icon` with no text beside it as a line of its own,
  `Icon "vaadin:check"`?
- **A LoginOverlay's description is wrong**: the snapshot shows `description="Application
  description"`, the web component's default property; the user sees `i18n.header.description`,
  Bakery's demo credentials. Read it from the `i18n` first, as `D_login_from_i18n` does a
  LoginForm's heading.
- **A FormItem's label**: the order details label their values with `<label slot="label">` in a
  `<vaadin-form-item>` ("Customer", "Phone number", "Due", "History"), which the snapshot drops as
  a component's own part: `H3 "Grace Hopper"` shows without "Customer".
- **Links and tabs**: Bakery's menu shows as `Tab [value="Storefront"]` with `Anchor "Storefront"`
  inside it, the CRM's drawer as `Anchor "List"`: no `href`, no `actions: click`, no `selected` on
  the current tab. An agent clicks the Anchor anyway, or guesses `/users`; `path=` as on a
  SideNavItem (`D_nav_item_states`) and the cleanups in `disclosure.md` and
  `snapshot-cross-cutting.md` would make it sure. Not a blocker.
- **Notifications** work: the locked user's persistent one shows in Save's answer. "Order was
  created" lasts 4 s; `notification.md`'s `Q_vanished` matters if an agent ever looks late.

Crud is commercial, out of scope while the project is a prototype: the three Crud scenarios need
only what's generic, §1's search bar and §2's rows.

## Order

1. §1: the row click (#298), the upstream PR, the replaced field's 10 s (#377).
2. §3: drops the extra calls from every scenario, the storefront's search included.
3. §2: the CRM's cost, crm-count and crm-delete-closed; the storefront's cards as rows.
4. §4's suffix components (reschedule) and lone icons (today-count), then the rest of §4.

Then a CRM round on a committed tree, and Bakery's first.
