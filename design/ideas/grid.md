# Grid and TreeGrid

Most apps' main view, and what the snapshot handles worst today: a line per cell, refs that
silently change rows, and no way to reach the rows out of view. The DOM it works from is
`R_vaadin_grid_dom`.

## Today

test-app's `grid` view: 200 people, Name and Age sortable, a column of Edit buttons, single
select; under it a multi-select TreeGrid of departments.

```
- Grid [ref=1]
  - GridColumn [ref=2]                 (×4, one per column)
  - GridCellContent [ref=6]
    - GridSorter "Name" [ref=7]
  - GridCellContent [ref=8]
    - GridSorter "Age" [ref=9]
  - GridCellContent "City" [ref=10]
  - GridCellContent "Actions" [ref=11]
  - GridCellContent [ref=12]           (×8, 12–19, empty: another header row and the footer? not checked)
  - GridCellContent "Alice Novak" [ref=20]
  - GridCellContent "20" [ref=21]
  - GridCellContent "Turku" [ref=22]
  - GridCellContent [ref=23]
    - Button "Edit" [ref=24] actions: click
  … 10 rows of 200; nothing says there are more
- Grid [ref=70]                        (the TreeGrid)
  - GridFlowSelectionColumn [ref=71]
  - GridCellContent [ref=74]
    - Checkbox [ref=75, id=selectAllCheckbox] actions: set_value
  …
  - GridCellContent [ref=85]
    - Checkbox [ref=86] actions: set_value
  - GridCellContent [ref=87]
    - GridTreeToggle "Engineering" [ref=88]
  - GridCellContent "12" [ref=89]
```

What the existing tools already do:

- `click` on `GridSorter "Age"` sorts. The cell refs then show other rows: ref 25 was "Bob Novak",
  now "Frank Rossi". A ref an agent kept now names another person, with no error.
- `click` on a cell selects its row; nothing in the snapshot says which row is selected.
- `click` on an Edit button works, in the row the agent meant, if it took the ref after the sort.
- `click` on `GridTreeToggle "Engineering"` expands it; nothing shows expanded, collapsed or depth.
- `set_value true` on a row's checkbox selects it; the select-all box turns `checked, indeterminate`.
- `get_text` on the Grid: `NameAgeCityActionsAlice Novak20TurkuEdit…`, the cells run together.
- `get_value` refuses: "a Grid: it has no value".

No `actions:` on sorter, toggle or cell, though `click` works on each.

## The Unified API

`UnifiedGrid` fits well: `getRowCount()`, `row(index)`, `getColumnHeaders()`, `column(header)`;
a row's `getCellText(header)`, `select()`, `isSelected()`, `click()`, `toggleDetails()`,
`details()`, `cell(header)` for the components in a cell; a column's `sort(direction)`,
`getSortDirection()`, `getCellTexts()`. Row access scrolls the row into view and waits for its
data; `getCellTexts()` pages through a lazy grid a page at a time (Dramafinder's
`getColumnTexts`).

## How swing-mcp shows a JTable

At most 5 rows, `- row 0: 1 | Alice | NY`, 0-based, at most 10 columns, then `... and 45 more
rows`; the header as `columns: [ID, Name, City]` on the table's line. Rows take no ref; the
tools name them by index: `get_items(ref, offset, length)`, `set_selection(ref, [rows])`,
`clear_selection`, `select_all`, `get_selection`. No sort indicator, no selected mark. No cell
tools on a table (swing-mcp's `D_no_jtable_cells`): its cells are painted, nothing in them to act
on. A Grid's cells hold real components, and a row's Edit button is what an agent clicks most, so
we take the row line but keep the components.

## v1 and v2

The prototype builds what the benchmark's scenarios need, and what comes free with it; what can
be left out without a hack waits for v2. The scenarios read a grid's rows (CRM, five columns,
200 contacts read whole by crm-count and crm-delete-closed), click a row to open its form (CRM)
or its order (Bakery's storefront), click a row's Edit button (Bakery's users), and count rows
(Bakery's today). None sorts, selects more than a click does, expands a tree, opens details or
edits in a grid.

## Views

In the vocabulary of `component-views.md`: the snapshot shows a Grid in its middle view,
`snapshot(ref)` in its full view, both v1. In a cell, a header or a footer, each component shows
in its preview, or in its short view if a user acts on it.

## The middle view: a Grid in the snapshot

An overview of the data; a Grid is usually a view's centrepiece, so its ten lines are worth it.

```
- Grid [ref=1, id=people, rows=200, selection=single, 1 selected]
  Header: | Name ↑ | Age | City | Actions |
  Header: | TextField [ref=2, placeholder="Filter", value=""] | | | |
  - row 0 [selected]: Frank Rossi | 20 | Lyon | Button "Edit" [ref=3]
  - row 1: Alice Novak | 21 | Turku | Button "Edit" [ref=5]
  …
  - rows 10–199 not shown: get_rows ref=1
  Footer: | 200 people | | | |
```

A multi-select grid, its selection column a column like any other in v1:

```
- Grid [ref=20, rows=4, selection=multi, 1 selected]
  Header: | Checkbox [ref=21, id=selectAllCheckbox, indeterminate] | Department | People |
  - row 0 [selected]: Checkbox [ref=22, checked] | Engineering | 12
```

Each line says what it is: `Header:`, `- row 3`, `Footer:`, and in v2 `Details:`. So a header line
can't read as a row, nor the footer as the last row.

- **Named by its id and `aria-label` only**, as any component: the captions tell what it lists.
- **The bracket**: `rows=` every row, not the rendered ones; `selection=single|multi` and how many
  rows are selected, for a grid that selects: `1 selected`, not `selected=1`, which reads as row 1.
  No column count: the header line shows them.
- **The header**: no `GridColumn` lines. A `Header:` line for each header row with a cell per
  column, a filter row too. A row of groups, a cell spanning columns,
  waits for v2. No row numbers, `Header #0:`: no tool takes one, and with the group rows left out
  they would skip, or disagree with the app's `getHeaderRows()`.
- **Sorting**: `↑` / `↓` on a sorted column, the order the rows are in. With two or more sorted,
  each its order, as the sorter shows it: `| Name ↑2 | Age ↓1 |`. `⇅`, sortable and not sorted,
  comes in v2 with `sort`: the snapshot offers nothing a tool can't do.
- **The footer**: a `Footer:` line for its first row, after the rows as on screen; groups v2.
- **The rows**: up to 10 of those in the grid's viewport. A grid showing all its rows has them
  all in its viewport, hence the cap. Then a line naming the rows not shown, on both sides of a
  grid scrolled midway: `- rows 0–49, 60–199 not shown: get_rows ref=1`.
- **Rows still loading** show empty cells: the snapshot waits until no grid is loading, as
  `benchmark-gaps.md` §3 has it.
- **A row is no component, so it takes no ref**: a tool names it by the grid's ref and its
  0-based index, and scrolls the grid to it if need be.
- **The selection column**: a column like any other in v1, its checkboxes short views that
  `set_value` ticks, so a multi-select grid works with no code of its own.
- **A cell**: what it holds as above, cells joined by ` | `, in the DOM's order. Every column,
  however long the row: a long line costs no more than its cells (swing-mcp stops at 10). A `|`
  inside a cell, in its text or in a component's quoted name or value, is written `\|`, Markdown's
  table escape; `get_value` on that component reads it unescaped.
- **A TreeGrid** shows as a flat grid in v1, its rows in order without their depth.

## The full view

`snapshot(ref)` (`component-views.md`), v1: cheap, the snapshot's walk already starts from an
element other than `<body>`, a modal; the full view flips two switches in the grid's code; its
refs join the latest ones. Every row in view, not 10, and every component in a cell in its short
view, not only those a user acts on; still one line per row, however long: the tokens are the
same as on lines of their own, less their labels and indentation. On a richer grid than
test-app's, a Name cell holding an Avatar and a Span:

```
middle: - row 0 [selected]: Frank Rossi | 20 | Lyon | Button "Edit" [ref=3]
full:   - row 0 [selected]: Avatar [ref=5, name="Frank Rossi"]; Span "Frank Rossi" [ref=6] | 20 | Lyon | Button "Edit" [ref=3]
```

## Tools

Each takes the grid's ref and a row by its 0-based index, and scrolls the grid to the row if need
be. A tool that scrolls a grid changes the page, so it answers with the new snapshot
(`D_action_returns_snapshot`).

| Tool | | swing-mcp's |
|---|---|---|
| `get_rows(ref, offset, length)` | rows beyond the view, as the snapshot's row lines, then the snapshot; the grid left at `offset` | `get_items` |
| `click(ref, row)` | clicks a spot of the row free of components, as the Unified API's `row.click()`: selects it, or opens its form | `click` |

v2, each when a scenario or a real app needs it:

| Tool | | swing-mcp's |
|---|---|---|
| `sort(ref, column, direction)` | ascending, descending or none; a column by its caption | — |
| `set_selection(ref, rows)` | exactly these rows selected, `[]` clears, `all=true` the header's checkbox | `set_selection`, `clear_selection`, `select_all` |
| `get_value` on a Grid | its selected rows: Flow's Grid selection is its `HasValue` | `get_selection` |
| `toggle_expand(ref, row)` | a TreeGrid row | `toggle_expand` |
| `click(ref, row, double)` | a double click, for an app opening a row's editor or page so | — |

`get_rows` leaves the grid at `offset`: scrolling back would not undo what it did, the grid having
reused its cells for other rows, and the answer's snapshot shows plainly what the grid shows now,
its first rows those read, their components with refs.

`get_rows` of 200 rows scrolls the grid, a page of rendered rows at a time. TestBench's
`GridElement` (`R_testbench_grid_element`) pages no more than we do: its `getRows(first, last)`
filters the rendered rows. Ours, one script in the page: scroll to `offset`, wait, read every
rendered row from there, scroll to the first row not read, until `length`; TestBench's scroll
call and wait borrowed. In the page, the rows are rendered by the snapshot's own code, so
`get_rows` and the snapshot write a row the same.

## Refs in cells

A stamp stays on its element (`D_refs_not_ids`), and the grid reuses its cell elements for other
rows (`R_vaadin_grid_dom`): after a sort or a scroll, ref 3 is the same button in another row.
Accepted: every action answers with the snapshot, which shows where ref 3 is now; an agent acting
on an older ref may click another row's button. A grid tool dropping the stamps inside the grid,
so an older ref fails as not in the latest snapshot, waits for v2, if this bites. The app moves
rows too, by a delete or a refresh, with no tool of ours.

## v2

- **`⇅` and `sort`**, the tools in the table above.
- **The selection column as no column**, `selected` standing for its checkbox, with
  `set_selection`.
- **Details**: an open row's details on one line under it, `Details:` without a dash, so it can't
  read as a TreeGrid's child row; their content as a cell's. In v1 they don't show.
- **A TreeGrid row** indented by its depth, `collapsed` when it has children hidden; an expanded
  one shows them below it, as a SideNavItem (`D_nav_item_states`).
- **Cells in the order on screen**: columns by `_order`, as TestBench sorts them
  (`R_testbench_grid_element`), so a column the user dragged shows where they put it.
- **An empty grid**: an `Empty:` line with the empty state's preview, Vaadin's
  `setEmptyStateText` / `setEmptyStateComponent`; in v1 only `rows=0`.
- **Grid's Editor**: no scenario edits in a grid. Grid Pro's editing only after graduation.

## Depends on

`benchmark-gaps.md` §1, the flat tree: a cell whose renderer's element keeps its text in a shadow
root, Bakery's order cards, previews as nothing until the snapshot reads the flat tree; and the
storefront's grid sits in `<storefront-view>`'s shadow root.

## test-app

The `grid` view, extended to look at the DOM: a header group, Person over
Name and Age; a filter row; a footer counting the rows the filter leaves; a selected row's
details. Still to come for v1: a component no user acts on in a cell, for its preview; a `|` in a
cell. For v2: an empty grid.

## Open questions

`Q_selected_out_of_view`: does `selectedItems` hold a selected item Flow hasn't loaded, for
`1 selected`? What does `size` say for a lazy data provider without a count? Neither in the
benchmark; to check when an app shows one.

v2's:

`Q_column_name`: `sort` names a column by its caption. A column without one, two with the same?
The 0-based column index then, as the Unified API's `column(index)`?

`Q_get_value_shape`: `get_value` on a Grid answers its selected rows as row lines, `row 0: Frank
Rossi | 20 | Lyon`; past how many is it cut, with the count? swing-mcp's `get_selection` stops at
100.
