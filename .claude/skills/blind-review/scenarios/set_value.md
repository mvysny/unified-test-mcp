# set_value

## Task

A Vaadin web app is running at {{url}}, and a browser is ready to drive it. On the page at `edit`,
change Name to "Bob" and tell me what the greeting below it says then. Change Username to "admin",
then Age to 130; for each, tell me what the field holds afterwards and whether the app objected.
On the page at `choices`, make Favourite "Norway"; if you can't, tell me why and what you could
pick instead. Then make Visited hold Czechia alone, and empty Country. Last, on the page at
`fields/readonly`, try to change Text to "Bob".

## Done when

- `edit`: the greeting reads "Hello, Bob". Username still holds "alice": the app put it back.
  Age holds 130 and shows invalid, "At most 120".
- `choices`: Favourite stays Sweden, as there is no Norway; the agent names Finland, Czechia and
  Sweden as the choices. Visited holds Czechia alone; Country has nothing selected.
- `fields/readonly`: refused, the field is read-only; Text still holds "Alice".

## Watch for

- Whether it finds `set_value` from the snapshot's `actions: set_value` and the description, or
  reaches for `click` and typing that this server doesn't have.
- The greeting: `set_value` returns the snapshot. Did it read the greeting from there, or call
  `snapshot` or `get_text` again?
- Username: the first line says `it holds "alice" now, without showing invalid`. Did it report that the app refused, or
  report "admin" because the call didn't fail?
- Age: did it pass `130` or `"130"`, and did either cause trouble? Did `it holds it but shows
  invalid "At most 120"` let it report the field as holding 130 yet invalid without checking the
  snapshot?
- Norway: did the error's item list answer "what could you pick instead" without another call?
- Visited: did it pass an array, `["Czechia"]`, the whole selection, or try to deselect one by
  one? Country: both earlier runs sent the string `"null"`, their call interface having no way to
  type a real null; the second cleared it by leaving `value` out. Did the description's "leave it
  out" get it there first time, or the refusal's hint in one step?
- Did it pass a text JSON-encoded, `"\"Bob\""`? The first run did, from a description that said
  "the value is JSON"; it now says "passed as itself".
- Username: the line says what, never why; three runs asked for "the app reverted it". Kept out:
  the browser can't tell a listener from a `maxLength` (SetValueTool's javadoc). Did "without
  showing invalid" answer "did the app object?"
- A stray `Div "Czechia added to selection …"` in the snapshot, Vaadin's screen-reader
  announcement; runs 2 and 3 saw it, the snapshot now leaves it out.
- Read-only Text: did it stop at the refusal, or try to work around it?
