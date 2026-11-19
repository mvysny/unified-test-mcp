# What the server lacks for the benchmark

The benchmark's 12 scenarios (`benchmark/scenarios/`, `benchmark.md`) through this server: which
pass today, which can't, and what each needs. From the first CRM round's probe
(`benchmark/results/2026-11-18-crm/`), and on 2026-11-19 a walk through Bakery (seed 1) with the
server as of 5f4434c, driven over stdio, its DOM looked up in Playwright beside it.

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
| bakery-price-cap | blocked | "New product" is inside `<search-bar>`'s shadow root (§1) |
| bakery-new-user | blocked | "New user", the same |
| bakery-new-order | blocked | "New order" the same; the order editor inside `<storefront-view>`'s (§1) |
| bakery-edit-items | blocked | the order cards, the editor and its item editors are Lit templates (§1) |
| bakery-reschedule | blocked | the same; then the comment's Send button, in a field's suffix (§4) |
| bakery-today-count | blocked | the order cards (§1); then a Delivered badge shows only an icon (§4) |

Ours: the probe, one run each, of a work-in-progress tree (its `NOTES.md`). In brackets: Playwright
MCP's median, the cheaper competitor in every CRM scenario.

## 1. Lit templates — blocks six of Bakery's seven

What the walk saw: the storefront is `- <storefront-view> [ref=14]` with nothing under it; the Crud
views show `- <search-bar> [ref=18]`, neither its search field nor its "New user" / "New product"
button. `Snapshot` walks `e.children`, the light DOM, and a `LitTemplate`'s `@Id` components are in
its shadow root.

Bakery's DOM (Vaadin 25.3.5):

- `<storefront-view>` has no light-DOM children; its shadow root holds `<search-bar id=search>`,
  `<vaadin-grid id=grid>` and `<vaadin-dialog id=dialog aria-modal=true>`.
- `<search-bar>`'s shadow root: a TextField with the placeholder "Search", a "Show past orders"
  Checkbox (twice, one hidden by CSS per viewport width), "Clear search" (hidden until a search),
  and the action button: "New order", "New user", "New product".
- The dialog's light-DOM child is `<order-editor>` or `<order-details>`, added by Flow; each a
  template with its fields in its shadow root. The editor's item list is a Flow `Div` in its
  `#itemsContainer`, an `<order-item-editor>` per item, each with `#products`, `#amount`,
  `#comment` and `#delete` in a shadow root of its own: the same ids, repeated.
- `<buttons-bar>` slots the footer's buttons, Flow's light-DOM children of it, through
  `<slot name=left|info|right>`.
- A grid cell's `<order-card>` holds all its text in its shadow root: the cell's `textContent` is
  empty. Inside, the status is `<order-status-badge>`, another template.

What it takes:

- **Walk the flat tree** of a custom element that isn't `vaadin-*` and has a shadow root: the
  shadow root's children in its place, a `<slot>` standing for the elements assigned to it, its
  fallback content when none. A `vaadin-*` component stays walked by its light DOM: its shadow root
  is the component's own.
- **A modal in a shadow root**: `document.querySelectorAll('[aria-modal=true]')` misses the
  storefront's dialog, and the Crud editor's `<vaadin-crud-dialog>` in `<vaadin-crud>`'s shadow
  root. Seen today: the open Crud editor shows inline at the end of the page, no `Modal:` line, the
  grid under it listed as if it took clicks.
- **`idUnique` across shadow roots**: it counts with `document.querySelectorAll`, which sees into
  no shadow root, while the Unified API's `withId` (Playwright's CSS) pierces them. An id unique in
  the light DOM but repeated inside a template resolves to two elements.
- **Refs inside shadow roots**: `Refs.locate` finds `[data-mcp-ref=N]` through Dramafinder's CSS,
  which pierces open shadow roots, so `click` and `set_value` should reach them: to be proved by a
  test.
- **A test-app view built as a `LitTemplate`**: `@Id` fields, a slot Flow fills, a template in a
  template, a Dialog inside, a Grid column whose LitRenderer's element has a shadow root. The
  tools' tests run on test-app, never on Bakery.
- Unlabelled fields need nothing more: the bracket already shows `id=dueTime,
  icon="vaadin:clock"`, `id=products`, `placeholder="Details"`; the template's `@Id`s name them.

Decided: walk every non-`vaadin-*` custom element's shadow root, an add-on's too. A third-party
add-on's shadow DOM is its implementation, as a Vaadin component's is, but nothing in the browser
tells it from an app template: being a Lit element doesn't, since Vaadin's components and many
add-ons are Lit elements too. Revisit when an add-on's insides show up as noise.

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

Still to do: **a row's text from the flat tree** (§1): an order card's text is in its shadow
root, which `textContent` doesn't reach.

The storefront has 158 orders, 23 cards rendered: bakery-today-count's "Today" group sits on top,
and bakery-reschedule's target is found by the search, so neither needs rows beyond the rendered
ones on seed 1.

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
only what's generic, §1's search bar and modal in a shadow root, and §2's rows.

## Order

1. §1 with its test-app view: unblocks price-cap and new-user, and opens the storefront.
2. §3: drops the extra calls from every scenario, the storefront's search included.
3. §2: the CRM's cost, crm-count and crm-delete-closed; the storefront's cards as rows.
4. §4's suffix components (reschedule) and lone icons (today-count), then the rest of §4.

Then a CRM round on a committed tree, and Bakery's first.
