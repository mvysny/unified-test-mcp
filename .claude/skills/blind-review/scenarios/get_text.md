# get_text

## Task

A Vaadin web app is running at {{url}}, and a browser is ready to drive it. On its start page,
what exactly does the button with id `clicker` say? Then on the page at `form`, what does the status text below
the buttons say, before and after pressing Save? On that page, what does the Name field say, and what
does the button with the X icon say? Last, what does the button with id `submit` on that page say?

## Done when

- The clicker says "Click me".
- The status says "Not saved", then "Saved".
- The Name field says "Alice": its value, not its label "Name".
- The X button says nothing: it is an icon with no text, its accessible name "Close".
- There is no button with id `submit`: said plainly.

## Watch for

- Whether it calls `get_text` at all, or reads every answer off the snapshot (both fine; did the
  descriptions tell it when one is worth the other?).
- The Name field: `get_text` refuses it, "it has a value, not a text. Call get_value". Did it go
  straight to `get_value` or the snapshot's `value=`, or did the description keep it from trying?
- The X button: `get_text` answers an empty string. Did the agent read that as "no text", or as
  a failure, and retry?
- `submit`: does it try a ref it made up, and does that error tell it what to do next?
