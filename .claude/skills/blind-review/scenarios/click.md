# click

## Task

A Vaadin web app is running at {{url}}, and a browser is ready to drive it. On its start page,
press the button whose id is `clicker` three times and tell me what it says afterwards. Then go to the page
at `form`, press Save, and tell me what the status text says. On that same page, press
Delete and tell me what happened. Finally, open the page at `second` and press its "Back to main"
button twice; tell me where you ended up, and what the second press did.

## Done when

- The clicker reads "Clicked 3 times".
- After Save, the status reads "Saved".
- Delete: reported as disabled, so it can't be pressed.
- "Back to main": the first press lands on the start page; the second can't happen, since the
  start page has no such button.

## Watch for

- Whether it clicks from the ref in the previous click's snapshot, or calls `snapshot` between clicks.
- Delete is disabled: does it click anyway? `click` then waits the full timeout before it fails; is
  the error clear on why, and did the snapshot's `[disabled]` stop it first?
- The second "Back to main": does it see in the first click's snapshot that the button is gone, or
  click the old ref? If so, did the "isn't in the latest snapshot" error tell it why and what next?
