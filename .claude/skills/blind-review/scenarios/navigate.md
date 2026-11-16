# navigate

## Task

A Vaadin web app is running, and a browser is ready to drive it. Open the page at the path
`second` and tell me what's on it. Then open the page at `orders`, which may or may not exist in
this app, and tell me whether it does. Finish on the app's start page and tell me what's there.

## Done when

- `second`: one button, "Back to main" (id `back`).
- `orders`: reported as not existing.
- Start page (`""`): one button, "Click me" (id `clicker`).

## Watch for

- `orders`: `navigate` answers "No view at /orders", not an error. Did the agent take it from
  that line, without parsing the page's text?
- How it names the start page: `""`, `"/"`, or a guess that failed first.
