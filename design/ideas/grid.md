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
`details()`; `withCellText(header, text)` to find a row. How `row(index)` reaches a row out of
view (scrolls?) to be read in its source.

## A shape to start from

A table, one line per row, the row the unit an agent names:

```
- Grid [ref=1, rows=200, selection=single]
  | Name ⇅ | Age ↑ | City | Actions |
  - Row 1 [ref=20, selected]: Frank Rossi | 20 | Lyon | Button "Edit" [ref=24]
  …
  - (rows 11–200 not shown: get_rows ref=1)
```

## Open questions

`Q_grid_name`: a Grid has no label. Its `aria-label`, an id, the heading above it, or just
`Grid` with its column headers?

`Q_row_refs`: refs bound to the recycled cell elements break `D_refs_not_ids` under sorting and
scrolling. A ref per row bound to the item, through Flow's item key (`R_flow_item_keys`), so that a
row gone from view fails loudly instead of resolving to another item?

`Q_rows_beyond_view`: a `get_rows(ref, from, count)` tool, as swing-mcp's `get_cells` and
`get_cell_count`? It scrolls the grid, which a user does too. How many rows the snapshot shows
inline before it stops: the token promise says the snapshot scales with the screen, and the screen
shows 10.

`Q_cells_with_components`: an Edit button per row must stay a ref the agent can click. Inline in
the row line, `Button "Edit" [ref=24]`, or the row's components as its children?

`Q_selection`: `[selected]` on rows; selecting through `click` on a row (single) and `set_value` on
its checkbox (multi), or a `select` action on the row? Is a Grid's selection its `get_value`?

`Q_sort_state`: `click` on a header sorts today; the header line shows the direction, `Age ↑`?

`Q_tree`: depth by indentation, `[expanded]` / `[collapsed]`, `click` on the row's toggle to
expand?

`Q_header_components`: a filter TextField in a header cell, a component in the footer: not in
test-app yet; they must stay refs.

`Q_item_details` and `Q_editor`: the details renderer and Grid's Editor, neither in test-app yet.
`UnifiedGridRow.toggleDetails()` exists. Grid Pro's editing only after graduation.
