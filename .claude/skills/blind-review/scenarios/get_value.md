# get_value

## Task

A Vaadin web app is running at {{url}}, and a browser is ready to drive it. Open the page at
`form` and tell me exactly what's in each field: Name, Email, and whether Subscribe is ticked.
Then tell me what's in the Save button and in the status text below the buttons — use the same
approach you used for the fields, if it works. Last, on the page at `choices`, tell me exactly
what's selected in each field, and which countries are picked in "Visited".

## Done when

- Name holds "Alice"; Email is empty; Subscribe is ticked.
- Save and the status hold no value: their text is "Save" and "Not saved".
- `choices`: Country is Czechia, Visited is Finland and Sweden, Home is Czechia, Favourite is
  Sweden, Next has nothing selected.

## Watch for

- Whether it calls `get_value` at all, or reads the fields off the snapshot's `value=` and
  `checked` (both fine; did the descriptions say `get_value` is the uncut one?).
- Email and Next: `get_value` answers `""` and `null`. Did it read those as "empty" and "nothing
  selected" without guessing? (A plain empty answer showed as "(completed with no output)" and
  left the agent inferring; hence JSON.)
- Subscribe: `true`. Did it need to guess that means ticked?
- Save and the status: the description says they have a text, so the agent usually goes straight
  to `get_text` and the refusal, "it has no value. Call get_text for its text", never runs; that's
  the description working. If it does call `get_value`, did it follow the error in one step?
- Visited: a JSON array from `get_value`, `Finland, Sweden` in the snapshot. Did either form
  trip it?
