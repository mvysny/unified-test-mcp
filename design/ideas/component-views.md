# Four views of a component

What a component shows depends on where it shows: four views, each one more than the last. The
vocabulary the snapshot's rules and the tools' descriptions speak. A Grid needs all four first
(`grid.md`); a Select's items and a MenuBar's submenus next.

| View | Shows | A Span | A Button | A Grid |
|---|---|---|---|---|
| preview | the text a user reads off it, bare | `foo` | its short view | its short view |
| short | its type, name and bracket, one fragment | `Span "foo" [ref=4, id=x]` | `Button "Edit" [ref=3]` | `Grid [ref=1, id=people, rows=200, selection=single, 1 selected]` |
| middle | a line: its short view, its actions, then its contents, cut to an overview | `- Span "foo" [ref=4, id=x]` | `- Button "Edit" [ref=3] actions: click` | that line, then its `Header:` lines, at most 10 rows, its `Footer:` |
| full | the middle view, nothing cut | as the middle | as the middle | every row in view, every component in a cell in its short view |

## Preview

A component no user acts on, as the text a user reads off it: no type, no quotes, no ref. Its own
text and its children's previews, joined by a space: a layout holding an Avatar and a Span "Frank
Rossi" previews as `Frank Rossi`. Cut at 50 characters, with `…`.

A component a user acts on has no preview: in its place it shows its short view, so a preview may
hold short views. They and the runs of text between them are joined by `; `, so where one ends
and the next begins reads plainly:

```
Frank Rossi; Button "Edit" [ref=3]; Button "Delete" [ref=4]
```

A `;` in a text stays as it is: read as two parts, it's still the same text. A component shown as
its preview takes no ref; the full view gives it one.

A user acts on a component when:

- its type has tools in `ComponentTypes`: a Button, the fields, a Checkbox, a SideNavItem;
- it's an Anchor: a link a user follows; `click` works on any element (`benchmark-gaps.md` §4);
- it's a Grid: `get_rows`, `sort`, `set_selection` take it.

Not a Span, a Div, a Paragraph, a heading, an Icon, an Avatar, a badge; nor a grid's sorter or tree
toggle, whose tools take the grid's ref. A MenuBar has no type yet (`menus.md`).

## Short

`Type "name" [bracket]`: the type, the name a user sees on it in quotes, and its whole bracket:
ref, id, value, the hints, the flags. The snapshot's line today, without `- `, without
`actions:` and without what's indented under it: no children, no rows, no items.

- **The whole bracket, for every type**: a Grid in a cell reads `Grid [ref=9, rows=3,
  selection=single, 0 selected]`. No exceptions; a bracket that reads oddly in a cell gets cut
  then.
- **No `actions:`**: the type says the action, and the snapshot's lines teach it: a Button clicks,
  a field takes `set_value`. If agents miss it, `actions:` comes back; the `; ` between parts
  keeps that readable.

## Middle

A line: `- `, the short view, ` actions: …`, then the component's contents, indented below it:

- a container, a layout or a dialog: its children, each in its middle view, lines of their own.
  Today's snapshot.
- a field: nothing; its insides are its own.
- a Grid: its `Header:` lines, at most 10 of the rows in view, each with its `Details:`, a line
  naming the rows not shown, its `Footer:` line. In a cell, header, footer or details, each
  component in its preview (`grid.md`).

The middle view cuts only what grows with the data: a grid's rows, a preview's length; someday a
Select's items. Everything else shows whole.

## Full

The middle view with nothing cut: a Grid's every row in view; in a cell, every component in its
short view, a preview only for bare text, and no preview cut. Its children in their full view.

## Where each shows

- `snapshot`: the page, each component in its middle view.
- `snapshot(ref)`: that component alone, in its full view. Its refs join the latest snapshot's
  rather than replacing them, so the page's other refs still work; `D_refs_not_ids` says so once
  this graduates. Why not a tool per type, `get_grid`: each type with a full view, a Select's
  items, a MenuBar's submenus, would want one.
- `get_rows`: a grid's rows as its middle view writes them.

## Elsewhere

Where else a preview reads better than a line per component is decided with each component: a
Tab holding an Anchor, in `disclosure.md`'s `Q_tab_preview`.
