# Benchmark scenarios: what they need, what they showed

The scenarios themselves are `benchmark/scenarios/*.md`; their checks, each fork's
`BenchmarkEndpoint`. Read from the apps' code at `vaadin/flow-crm-tutorial` 7db838e and
`vaadin/bakery-app-starter-flow-spring` 59c7c46 (both 2026-11-03).

## What our server lacks, by scenario

| Capability | Scenarios that need it |
|---|---|
| **Components inside a Lit template's shadow root.** `Snapshot` walks `e.children`, light DOM only; Bakery's storefront, order editor and search bar are `LitTemplate`s with `@Id` fields, so today the snapshot sees nothing inside them. `idUnique` asks `document.querySelectorAll`, which doesn't reach them either. | every Bakery storefront scenario |
| **Grid**: read rows, rows beyond the rendered ones, open a row, renderer-made content (Bakery's order cards) | crm-move-contact, crm-count, crm-delete-closed, bakery-reschedule, bakery-today-count, bakery-edit-items |
| **Crud** (`vaadin-crud`, commercial): the edit column's icon buttons, the editor, its Save / Delete | bakery-price-cap, bakery-new-user, bakery-locked-user |
| **Notifications** (Bakery's last 4 s) | bakery-new-order, bakery-locked-user |
| **Links and tabs**: CRM's drawer `RouterLink`s, Bakery's menu `Tabs` | any scenario whose agent navigates by clicking instead of by URL |
| **A field without a label**, named by its prefix icon or placeholder | bakery-new-order, bakery-edit-items |
| Login: the CRM's `vaadin-login-form` shows already (`LoginForm "Log in"` with its two fields); Bakery's `vaadin-login-overlay` — check | all |

What the competitors stumble on, from a first look in the browser (2026-11-18):

- Playwright MCP's `browser_fill_form` fails on a Vaadin ComboBox ("Element is not a `<select>`
  element"); typing into it and pressing Enter works. Its `browser_click` on a CSS target needs
  `vaadin-button`, not `button`.
- Playwright MCP snapshots right after `browser_navigate` before Vaadin has rendered: the login
  page showed only "Online". Ours waits for the page to settle.
- Its snapshot of the CRM's list holds ~38 of the 200 rows, as a `treegrid`.

The first round (`benchmark/results/2026-11-18-crm/`, its `NOTES.md`):

- Both competitors pass the CRM's forms; Playwright MCP is the cheaper of the two in every
  scenario (smaller context, $0.06–0.22 a run against $0.12–0.29).
- Counting a virtual grid is where they fail: their agents read it with a page script, count the
  rendered rows only, and answer wrong — 2 of 6 runs.
- Our probe passed all five, but a grid it can't see past its rendered rows cost it dearly: on
  crm-count it filtered by each last name in turn, 210 turns, $1.23, a 167k-token context. The
  next tools: Grid rows beyond the rendered ones, and a compact row form in the snapshot (it shows
  five `GridCellContent` lines per contact, a ref each).

## The whole-toolset session

An app's scenarios, one after another in one session, for the review run only: refs carried over,
navigation between views, which tool comes next. Each as its own `--resume` prompt, or one long
task?
