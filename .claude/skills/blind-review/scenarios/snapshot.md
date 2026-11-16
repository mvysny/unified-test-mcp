# snapshot

## Task

A Vaadin web app is running at {{url}}, and a browser is ready to drive it. Open its page at
the path `form` and describe that form to me as a tester would before testing it: each field with its label and
current value, which fields are required, which checkboxes are ticked, each button and whether I
can press it, and any status text. Also: is there a "Phone" field on that page?

## Done when

- Name = "Alice" (id `name`); Email empty and required; Subscribe ticked.
- Buttons Save, Close (icon-only, named by its aria-label), Delete (disabled); status "Not saved".
- No Phone field: said plainly, without a failed call or a guess.

## Watch for

- Whether it calls `navigate` and reads the snapshot that comes back, or calls `snapshot` again.
- Whether it composes the URL `{{url}}/form` at once, or first tries a bare path.
- Whether it understands `[aria-label]` and the `actions:` suffix without guessing.
