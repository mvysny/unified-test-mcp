# get_text

## Task

A Vaadin web app is running at {{url}}, and a browser is ready to drive it. On its start page,
what exactly does the button with id `clicker` say? Then on the page at `form`, what does the status text below
the buttons say, before and after pressing Save? Last, what does the button with id `submit` on
that page say?

## Done when

- The clicker says "Click me".
- The status says "Not saved", then "Saved".
- There is no button with id `submit`: said plainly.

## Watch for

- Whether it calls `get_text` at all, or reads every answer off the snapshot (both fine; did the
  descriptions tell it when one is worth the other?).
- `submit`: does it try a ref it made up, and does that error tell it what to do next?
