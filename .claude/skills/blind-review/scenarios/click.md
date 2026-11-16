# click

## Task

A Vaadin web app is running, and a browser is ready to drive it. On its start page, press the
button whose id is `clicker` three times and tell me what it says afterwards. Then go to the page
at `form`, press Save, and tell me what the status text says. Finally, on that same page, press
Delete and tell me what happened.

## Done when

- The clicker reads "Clicked 3 times".
- After Save, the status reads "Saved".
- Delete: reported as disabled, so it can't be pressed.

## Watch for

- Whether it clicks from the ref in the previous click's snapshot, or calls `snapshot` between clicks.
- Delete is disabled: does it click anyway? `click` then waits the full timeout before it fails; is
  the error clear on why, and did the snapshot's `[disabled]` stop it first?
