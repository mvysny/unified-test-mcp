# get_rows

## Task

A Vaadin web app is running at {{url}}, and a browser is ready to drive it. Open the page at
`grid`. Its first table lists people. How many of them live in Graz? Who is in row 150, counting
from 0, and what do their Tags say? Press that person's Edit button and tell me what the status
line says then. Next, select Bob Novak in the table and tell me the status line again. Last:
which of the table's columns can I sort by?

## Done when

- 33 people live in Graz, out of 200: every row read, not only those on screen.
- Row 150: Alice Popescu, Tags "Turku | 30s", the `|` read as part of the text, not as a column
  break.
- Her Edit button pressed: the status says "Editing Alice Popescu".
- Bob Novak selected, by a click on his row: the status says "Selected Bob Novak"; his row shows
  `[selected]`.
- Sortable: Name and Age.

## Watch for

- Graz: does it find `get_rows` from the `rows 5–199 not shown: get_rows ref=…` line, and read the
  200 rows in one call or page through them? Does it count from the City column, or the Tags
  column, which holds the city too?
- Row 150's Edit: `get_rows`' rows carry no refs. Does it take the Edit button's ref from the
  snapshot that follows them, where row 150 is in view, or try a ref it never saw, or click the
  row?
- Bob Novak: does it find `click` with `row`, or try to click a cell's text, or `set_value`? The
  first review's agent first scrolled back with `get_rows(0, 1)`, though `click` with `row`
  scrolls to the row itself.
- `Turku \| 30s`: does it read the `\|` as an escaped `|`?
- Sorting: `⇅` marks the columns that sort. The first review's agent, before `⇅`, found them by
  `snapshot(ref)` "on a hunch". Does it answer from the header line now?
