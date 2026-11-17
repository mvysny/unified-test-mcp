# get_tooltip

## Task

A Vaadin web app is running at {{url}}, and a browser is ready to drive it. On the page at `form`,
what would I see if I hovered over each button and over the Name field? Tell me the text that
pops up for each, word for word, or that nothing does. Then: on the start page, what pops up
over the button with id `clicker`?

## Done when

- The X button (Close): "Close without saving".
- Name: "Your full name, as on your ID card".
- Save and Delete: nothing pops up. Delete is disabled, which doesn't change the answer.
- `clicker`: nothing pops up.

## Watch for

- Whether it reads the tooltips off the snapshot's `tooltip=`, or calls `get_tooltip`; both fine.
  Did the description say `get_tooltip` is for a cut one?
- Save, Delete, `clicker`: the snapshot shows no `tooltip=`, which the description says means none.
  Did it still call `get_tooltip` on them to confirm? (It did, before the description said so.)
- Whether it looks for a hover action and, finding none, guesses; the description says it reads
  "without hovering".
- Save and `clicker`: `get_tooltip` answers `(no tooltip)`. Read as "none", or as a failure?
- Delete, disabled: does it think the disabled state keeps it from reading the tooltip?
- `clicker` is on another page: did it navigate first, or try a ref from `form`?
