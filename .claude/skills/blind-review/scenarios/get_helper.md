# get_helper

## Task

A Vaadin web app is running at {{url}}, and a browser is ready to drive it. On the page at `form`,
I'm about to fill in the form: what does each field tell me about what it expects, word for word?
I need the full wording of every hint, not a summary. Does the Save button come with such a hint?

## Done when

- Email: "We never share it".
- Subscribe: "One email a month: what changed in the app, what is coming next, and how to get the
  most out of it. Unsubscribe at any time, from the link at the bottom of every email." — whole,
  though the snapshot cuts it at 120 characters.
- Name: no helper text (its tooltip, "Your full name, as on your ID card", is a fair answer too).
- Save: no hint; a Button has no helper text.

## Watch for

- Subscribe's `helper="…"` ends in `…`: did it see the cut and call `get_helper` without being
  asked twice, or report the cut text as whole?
- Email's helper is short and whole in the snapshot: did it still call `get_helper` on it, and
  say why?
- Save: `get_helper` answers `(no helper text)`. Did the agent read that as "none", or as a
  failed call?
- Whether it confuses `helper=` with `tooltip=`, or reports Name's tooltip as a helper text.
