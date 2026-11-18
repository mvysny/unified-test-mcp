# VirtualList

A scrolling list whose every item is components, rendered only while in view. It shares the
Grid's problem of items out of view (`grid.md`'s `Q_rows_beyond_view`); whichever lands first
answers it for both. Unlike a Grid it has no columns, header or sort, and an item can hold any
components, so an item is closer to a small snapshot than to a row.

## Today

test-app's `virtual-list` view: 200 contacts, each `Name [Call]`, 300 px high.

```
- VirtualList [ref=1]
- Span "Idle" [ref=2]
```

The items aren't shown at all, a second snapshot later neither. `get_text` on the list:
`Alice NovakCallBob NovakCall…` (9 items). In a browser of its own the items are there: light-DOM
`<div role="listitem" aria-posinset="1" aria-setsize="200">` children of the
`<vaadin-virtual-list>`, no `hidden`, each holding the item's `<vaadin-horizontal-layout>`. No
Unified API wrapper.

## Open questions

`Q_why_empty`: why does the snapshot script skip the items? Reading `visit` in `Snapshot`, a
`<div>` with no id and no text of its own stands aside and its children take its place, so
the Span and the Button should show. Debug it in the server's own browser.

`Q_item_shape`: each item a line of its own with its components under it, `- Item 1 of 200
[ref=…]`, `aria-posinset` and `aria-setsize` give both numbers.

`Q_item_refs`: the item elements are recycled while scrolling, as a Grid's cells are
(`R_vaadin_grid_dom`)? Then the Grid's `Q_row_refs` answer applies.
