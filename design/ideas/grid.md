# Grid and TreeGrid: v2

v1 is built: a row a line in the snapshot, `get_rows`, `click(ref, row)` and `snapshot(ref)`'s
full view (`D_grid_as_rows`, `D_get_rows_scrolls`, `D_component_views`). It covers what the
benchmark's scenarios do with a grid: read rows, click a row, click a row's button, count. What
follows waits for a scenario or a real app that needs it: no benchmark scenario sorts, selects
more than a click does, expands a tree, opens details or edits in a grid.

## Tools

Each takes the grid's ref and a row by its number from 0, scrolling the grid to it if need be, as
`click(ref, row)` does.

| Tool | | swing-mcp's |
|---|---|---|
| `sort(ref, column, direction)` | ascending, descending or none; a column by its caption | — |
| `set_selection(ref, rows)` | exactly these rows selected, `[]` clears, `all=true` the header's checkbox | `set_selection`, `clear_selection`, `select_all` |
| `get_value` on a Grid | its selected rows: Flow's Grid selection is its `HasValue` | `get_selection` |
| `toggle_expand(ref, row)` | a TreeGrid row | `toggle_expand` |
| `click(ref, row, double)` | a double click, for an app opening a row's editor or page so | — |

The Unified API has most: a row's `select()`, `deselect()`, `doubleClick()`, `toggleDetails()`; a
column's `sort(direction)`; the grid's `selectAll()`.

## The snapshot

- **`⇅` on a sortable column**, with `sort`.
- **The selection column as no column**, `selected` standing for its checkbox, with
  `set_selection`; the bracket then says what its header checkbox says: `none selected`, `some
  selected`, `all selected`. A lazy multi-select grid has no header checkbox by default (Flow's
  `setSelectAllCheckboxVisibility`), so not even that.
- **Details**: an open row's details on one line under it, `Details:` without a dash, so it can't
  read as a TreeGrid's child row; their content as a cell's. In v1 they don't show.
- **A TreeGrid row** indented by its depth, `collapsed` when it has children hidden; an expanded
  one shows them below it, as a SideNavItem (`D_nav_item_states`). In v1 a TreeGrid is flat.
- **Cells in the order on screen**: columns by `_order`, as TestBench sorts them
  (`R_testbench_grid_element`), so a column the user dragged shows where they put it. v1 takes
  the DOM's order.
- **An empty grid**: an `Empty:` line with the empty state's preview, Vaadin's
  `setEmptyStateText` / `setEmptyStateComponent`; in v1 only `rows=0`.
- **Header and footer groups**, the rows v1 leaves out.
- **A lazy grid without a count**: `get_rows` reading `size` anew as it grows with each scroll to
  the end (`R_vaadin_grid_dom`); v1 reads up to the `size` it starts with.
- **Grid's Editor**. Grid Pro's editing only after graduation.

## Depends on

`benchmark-gaps.md` §1, the flat tree: a cell whose renderer's element keeps its text in a shadow
root, Bakery's order cards, previews as nothing until the snapshot reads the flat tree; and the
storefront's grid sits in `<storefront-view>`'s shadow root.

## test-app

For v2: an empty grid; details opening by a tool; a TreeGrid's depth to show.

## Open questions

`Q_column_name`: `sort` names a column by its caption. A column without one, two with the same?
The 0-based column index then, as the Unified API's `column(index)`?

`Q_get_value_shape`: `get_value` on a Grid answers its selected rows as row lines, `row 0: Frank
Rossi | 20 | Lyon`; past how many is it cut, with the count? swing-mcp's `get_selection` stops at
100. Out of view, the browser doesn't have them by row (`R_vaadin_grid_dom`): a selected row
drops out of `selectedItems` once the grid scrolls far from it. So it pages through the grid, as
`get_rows` does?
