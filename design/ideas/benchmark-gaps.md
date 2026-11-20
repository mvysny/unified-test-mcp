# What the server lacks for the benchmark

The benchmark's 12 scenarios (`benchmark/scenarios/`, `benchmark.md`) through this server: what
each still needs. From the rounds of 2026-11-20, all three arms, seeds 1–3
(`benchmark/results/2026-11-20-crm/`, `…-bakery/`, each with its `NOTES.md`), and the walks through
Bakery before them.

Two kinds of gap: a **blocker** leaves a scenario undoable through our tools; a **cost** lets it
pass at many times a competitor's tokens, which is what the benchmark measures. No blocker is
left.

## Where each scenario stands

| Scenario | Ours: passed, median cost and time (the cheaper competitor's) | What stands in the way |
|---|---|---|
| crm-add-contact | 3 of 3, $0.064, 20 s ($0.128, 106 s) | — |
| crm-invalid-email | 3 of 3, $0.064, 23 s ($0.092, 43 s) | — |
| crm-move-contact | 3 of 3, $0.055, 27 s ($0.097, 33 s) | — |
| crm-count | 3 of 3, $0.062, 12 s ($0.085, 123 s, 2 of 3) | — |
| crm-delete-closed | 3 of 3, $0.282, 71 s ($0.118, 81 s, 2 of 3) | cost: two clicks a contact, each answered with the page's snapshot (§2) |
| bakery-locked-user | 3 of 3, $0.046, 20 s ($0.056, 19 s) | — |
| bakery-price-cap | 3 of 3, $0.050, 22 s ($0.056, 17 s) | — the Crud editor inline, no `Modal:` line (§1), didn't matter |
| bakery-new-user | 3 of 3, $0.063, 22 s ($0.054, 21 s) | the same |
| bakery-new-order | 3 of 3, $0.088, 36 s ($0.262, 72 s, 2 of 3) | each product pick waits 10 s (`D_set_value_reads_back`) |
| bakery-edit-items | 3 of 3, $0.070, 24 s ($0.156, 35 s) | — |
| bakery-reschedule | 3 of 3, $0.076, 35 s ($0.138, 58 s, 2 of 3) | — |
| bakery-today-count | 1 of 3 as checked, 3 of 3 right, $0.030, 18 s ($0.058, 12 s) | the harness: the fork's check reads the answer's first integer (§3) |

## 1. Lit templates — walked on Bakery

The snapshot walks the app's shadow roots (`D_app_shadow_roots`, `R_bakery_template_dom`); the
Unified API's lookups reach into them only in the local `-shadow` build
(`R_unified_no_enumeration`). Left:

- **Upstream**: parttio/unified-testing-tool#347 asks whether an app template's shadow root may be
  searched on every Testing Tool, the spec saying not since their #251; once they answer, a PR
  from the clone's `shadow-dom-queries-alpha3`, ported to `main`. Dramafinder's row click is
  parttio/dramafinder#311, open. Until releases have them, the build needs both `-shadow`s in
  `~/.m2`.
- **The Crud editor** shows inline at the end of the page, no `Modal:` line, the grid under it
  listed as if it took clicks: its `<vaadin-crud-dialog>` is in `<vaadin-crud>`'s shadow root, a
  Vaadin component's, which no walk enters. Crud is commercial; price-cap and new-user may pass
  regardless, as locked-user does.

## 2. Grid

The CRM's cost, the grid's cells in every answer and the rows beyond the rendered ones, is built
(`D_grid_as_rows`, `D_get_rows_scrolls`) and measured: crm-count 6 turns, where the probe of
2026-11-18 took 210. The storefront has 158 orders, 23 cards rendered: bakery-today-count's
"Today" group sits on top, and bakery-reschedule's target is found by the search, so neither needs
rows beyond the rendered ones.

- **crm-delete-closed costs ours $0.28**, playwright's $0.12: its agent reads all 190 rows in one
  `get_rows`, then clicks each contact's row and the form's Delete, 13 contacts on seed 3, every
  click answered with the page's snapshot, the contact form's fields included; the context grows
  to 46k. `Q_lean_answer`: an action answering with what changed, rather than the whole page
  (`D_action_returns_snapshot`)?

## 3. Smaller, each found walking Bakery

- **bakery-today-count's check** takes the answer's first integer (`Answers.firstInteger` in the
  fork), and agents explain before the number: two of ours ("rows 0–6 … **2**" read as 0) and
  one of playwright's misjudged. The answer's last integer reads every answer of the round right.
  The fork's change, the same for all arms.
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

1. §3's check, in the fork, and today-count's three runs judged again.
2. §2's `Q_lean_answer`, for crm-delete-closed.
3. §1: the Unified API's upstream PR; Dramafinder's #311 merged and released. §3's links and tabs.
