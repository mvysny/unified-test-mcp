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

## Three views of a component

A component shows at one of three depths, by where it shows:

| View | Where | A Grid | A Select |
|---|---|---|---|
| short | inside a grid cell, header or footer | `Grid [ref=9, id=orders]` | `Select "Role" [ref=7, value="Admin"]` |
| middle | the snapshot | the bracket, header, ≤ 10 rows, footer | its line; a few of its items, someday |
| full | asked for by its ref | every row in view, each cell's components as lines | all its items |

The short view is one fragment: no children, no rows, no items, no `actions:`. A component no
user acts on shows as its text: a Span, a badge, a tree toggle, a sorter. Cross-cutting: it moves
to `snapshot-cross-cutting.md` once a second component needs it.

## The middle view: a Grid in the snapshot

An overview of the data; a Grid is usually a view's centrepiece, so its ten lines are worth it.

```
- Grid [ref=1, id=people, rows=200, selection=single, 1 selected]
  | Name ↑ | Age ⇅ | City | Actions |
  | TextField [ref=2, placeholder="Filter", value=""] | | | |
  - row 0 [selected]: Frank Rossi | 20 | Lyon | Button "Edit" [ref=3]
    Details: Frank Rossi lives in Lyon
  - row 1: Alice Novak | 21 | Turku | Button "Edit" [ref=5]
  …
  - rows 10–199 not shown: get_rows ref=1
  | 200 people | | | |
```

- **Named by its id and `aria-label` only**, as any component: the captions tell what it lists.
- **The bracket**: `rows=` every row, not the rendered ones; `selection=single|multi` and how many
  rows are selected, for a grid that selects: `1 selected`, not `selected=1`, which reads as row 1.
  No column count: the header line shows them.
- **The header**: no `GridColumn` lines. Each header row with a cell per column, a filter row
  too, its cells in the short view. A row of groups, a cell spanning columns, waits for v2.
- **Sorting**: `↑` / `↓` sorted, `⇅` sortable and not sorted. With two or more sorted, each its
  order, as the sorter shows it: `| Name ↑2 | Age ↓1 |`.
- **The footer**: its first row, after the rows as on screen; groups v2.
- **The rows**: up to 10 of those in the grid's viewport. A grid showing all its rows has them
  all in its viewport, hence the cap. Then a line naming the rows not shown.
- **A row is no component, so it takes no ref**: a tool names it by the grid's ref and its
  0-based index, and scrolls the grid to it if need be. The selection column is no column:
  `selected` stands for its checkbox.
- **A cell**: in the short view, cells joined by ` | `, in the order on screen: columns by
  `_order`, as TestBench sorts them, so a column the user dragged shows where they put it. A `|`
  inside a cell, in its text or in a component's quoted name or value, is written `\|`, Markdown's
  table escape; `get_value` reads the value unescaped.
- **Details**: an open row's details on one line under it, `Details:` without a dash, so it can't
  read as a TreeGrid's child row; their content in the short view, as a cell's.
- **A TreeGrid row**: indented by its depth, `collapsed` when it has children hidden; an
  expanded one shows them below it, as a SideNavItem (`D_nav_item_states`).

## The full view

`snapshot` takes an optional `ref`: that component alone, in full. For a Grid, every row in view,
not 10, and every component in a cell named in the short view, not only those a user acts on;
still one line per row, however long: the tokens are the same as on lines of their own, less
their labels and indentation. On a richer grid than test-app's, a Name cell holding an Avatar and
a Span:

```
middle: - row 0 [selected]: Frank Rossi | 20 | Lyon | Button "Edit" [ref=3]
full:   - row 0 [selected]: Avatar [ref=5, name="Frank Rossi"] Span "Frank Rossi" [ref=6] | 20 | Lyon | Button "Edit" [ref=3]
```

Why not a `get_grid` tool: each type with a full view, a Select's items, a MenuBar's submenus,
would want one of its own.

## Tools

Each takes the grid's ref, a row by its 0-based index, a column by its caption; each scrolls the
grid to the row if need be.

| Tool | | swing-mcp's |
|---|---|---|
| `get_rows(ref, offset, length)` | rows beyond the view, as the snapshot's row lines | `get_items` |
| `sort(ref, column, direction)` | ascending, descending or none | — |
| `set_selection(ref, rows)` | exactly these rows selected, `[]` clears | `set_selection`, `clear_selection`, `select_all` |
| `get_value` on a Grid | its selected rows: Flow's Grid selection is its `HasValue` | `get_selection` |
| `toggle_expand(ref, row)` | a TreeGrid row | `toggle_expand` |

A tool that scrolls or sorts a grid changes the page, so it answers with the new snapshot
(`D_action_returns_snapshot`). Details open on selecting a row, Flow's default; `toggle_details`
when an app needs it.

`get_rows` of 200 rows scrolls the grid, a page of rendered rows at a time. TestBench's
`GridElement` (`R_testbench_grid_element`) pages no more than we do: its `getRows(first, last)`
filters the rendered rows. Ours, one script in the page: scroll to `offset`, wait, read every
rendered row from there, scroll to the first row not read, until `length`; TestBench's scroll
call and wait borrowed. In the page, the rows are rendered by the snapshot's own code, so
`get_rows` and the snapshot write a row the same.

## test-app

The `grid` view, extended to look at the DOM: a header group, Person over
Name and Age; a filter row; a footer counting the rows the filter leaves; a selected row's
details. Still to come: an empty grid, a component no user acts on in a cell, a grid in a cell.

## Open questions

`Q_full_view`: `snapshot(ref)` or a tool of its own? Every snapshot replaces the session's refs
(`D_refs_not_ids`); a full view of one component would rather add its refs to the latest ones.
With rows inline, a Grid's full view differs from its middle view only by naming the components
no user acts on and by showing more than 10 rows: is that worth a view, or a flag on `get_rows`?

`Q_selected_count`: `1 selected`, or `selection length=1`?

`Q_cell_refs`: a stamp stays on its element (`D_refs_not_ids`), and the grid reuses its cell
elements for other rows (`R_vaadin_grid_dom`). After a sort, ref 3 stays on the same button, now
in another row: the answer's snapshot shows it there, so an agent acting on the latest snapshot
is right, one acting on an older ref clicks another row's Edit. Accept that, or have a grid tool
that scrolls or sorts drop the stamps inside the grid, so an older ref fails as not in the latest
snapshot? The app moves rows too, by a delete or a refresh, with no tool of ours.

`Q_get_rows_scroll`: `get_rows` scrolls to read rows a page at a time; the cells it leaves behind
show other rows. Leave the grid at `offset`, so the answer's snapshot shows those rows, their
components with refs?

`Q_short_which`: which components stay inline in a cell: those `ComponentTypes` gives actions,
the fields, an Anchor, a Grid. A row's ⋮ MenuBar has no type yet (`menus.md`).

`Q_selected_out_of_view`: does `selectedItems` hold a selected item Flow hasn't loaded, for
`selected=` and `get_value`? What does `size` say for a lazy data provider without a count?

`Q_editor`: Grid's Editor, not in test-app yet. Grid Pro's editing only after graduation.
