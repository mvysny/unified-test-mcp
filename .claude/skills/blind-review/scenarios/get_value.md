# get_value

## Task

A Vaadin web app is running at {{url}}, and a browser is ready to drive it. Open the page at
`form` and tell me exactly what's in each field: Name, Email, and whether Subscribe is ticked.
Then tell me what's in the Save button and in the status text below the buttons — use the same
approach you used for the fields, if it works.

## Done when

- Name holds "Alice"; Email is empty; Subscribe is ticked.
- Save and the status hold no value: their text is "Save" and "Not saved".

## Watch for

- Whether it calls `get_value` at all, or reads the fields off the snapshot's `value=` and
  `checked` (both fine; did the descriptions say `get_value` is the uncut one?).
- Email: `get_value` answers an empty string. Did it read that as "empty", or as a failure, and
  retry?
- Subscribe: `true`. Did it need to guess that means ticked?
- Save and the status: `get_value` refuses them, "it has no value. Call get_text for its text".
  Did it follow that to `get_text` in one step?
