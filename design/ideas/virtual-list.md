# VirtualList

A scrolling list whose every item is components, rendered only while in view. It shares the
Grid's problem of items out of view (`grid.md`'s `Q_rows_beyond_view`); whichever lands first
answers it for both. Unlike a Grid it has no columns, header or sort, and an item can hold any
components, so an item is closer to a small snapshot than to a row.

## Today

test-app's `virtual-list` view: 200 contacts, each `Name [Call]`, 300 px high.

```
- VirtualList [ref=1]
  - Span "Alice Novak" [ref=2]
  - Button "Call" [ref=3] actions: click
  - Span "Bob Novak" [ref=4]
  - Button "Call" [ref=5] actions: click
  …                                     (the 9 items rendered, to Ivan Novak)
- Span "Idle" [ref=20]
```

The rendered items show, flat: each item's `<div role="listitem" aria-posinset="1"
aria-setsize="200">` and its `<vaadin-horizontal-layout>` stand aside, so nothing groups a name
with its button, nor says there are 200. An earlier survey saw no items at all; they show as of
`D_visible_only`'s change, cause not traced. No Unified API wrapper.

## Open questions

`Q_item_shape`: each item a line of its own with its components under it, `- Item 1 of 200
[ref=…]`, `aria-posinset` and `aria-setsize` give both numbers.

`Q_item_refs`: the item elements are recycled while scrolling, as a Grid's cells are
(`R_vaadin_grid_dom`)? Then the Grid's `Q_row_refs` answer applies.
