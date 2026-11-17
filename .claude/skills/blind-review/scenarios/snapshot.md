# snapshot

## Task

A Vaadin web app is running at {{url}}, and a browser is ready to drive it. Open its page at
the path `form` and describe that form to me as a tester would before testing it: each field with its label and
current value, which fields are required, which checkboxes are ticked, each button and whether I
can press it, any hint the form gives about a field or a button, and any status text. Also: is
there a "Phone" field on that page? Last, on the page at `fields/readonly`: which fields can I
type into, and what range can the Volume slider be set to? Then, on the page at `dialog`: press
Delete, and in what opens press Why?; tell me what each showed, and whether I could still press
the page's Notes button while they were open. Close both without deleting anything.

## Done when

- Name = "Alice" (id `name`); Email empty and required; Subscribe ticked.
- Buttons Save, Close (icon-only: no text, aria-label "Close"), Delete (disabled); status "Not saved".
- Hints: Name's tooltip "Your full name, as on your ID card", Email's helper "We never share it",
  Subscribe's helper (cut in the snapshot), Close's tooltip "Close without saving".
- No Phone field: said plainly, without a failed call or a guess.
- `fields/readonly`: every field is read-only except the two inside Phone (Prefix and Number),
  which a read-only CustomField leaves editable. Volume: 0 to 100.
- `dialog`: Delete opens a modal "Delete order?" with a Reason field and Why?, Cancel and Delete
  buttons; Why? opens a second modal "Why?" over it, "Deleting can't be undone." and OK. Notes
  can't be pressed while either is open. OK, then Cancel, closes both; the status stays "Not
  deleted".

## Watch for

- Whether it calls `navigate` and reads the snapshot that comes back, or calls `snapshot` again.
- Whether it composes the URL `{{url}}/form` at once, or first tries a bare path.
- Whether it reads the Close button's line, no quoted name and `aria-label="Close"`, as an
  icon-only button, and understands the `actions:` suffix, without guessing.
- Whether it tells `helper=` from `tooltip=`, and notices Subscribe's is cut (`…`).
- Phone's TextFields: did it notice they lack `readonly`, or assume the whole CustomField is
  read-only?
- Whether it sends both navigations at once and then uses a `form` ref after `fields/readonly`
  replaced the page. It did, before `navigate`'s description said the refs of the page before are
  gone; did the stale-ref error, naming the current page, send it back in one step?
- Whether the `Modal:` line tells it a dialog opened, not a navigation, and which dialog is on
  top; whether it answers the Notes question from that line, or tries a click with Notes' old ref
  and reads the error that names the modal.
- Whether it tells the dialog's Delete button from the page's, and presses Cancel, not Delete.
- After the dialogs close: does it notice the page's refs came back unchanged, or snapshot again?
