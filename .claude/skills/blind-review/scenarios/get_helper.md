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
- Save: no hint; a Button has no helper text, and no tooltip either.

## Watch for

- Subscribe's `helper="…"` ends in `…`: did it see the cut and call `get_helper` without being
  asked twice, or report the cut text as whole?
- Email's helper is short and whole in the snapshot: did it still call `get_helper` on it, and
  say why?
- Name: `get_helper` answers `(no helper text)`. Did the agent read that as "none", or as a
  failed call?
- Save: `get_helper` refuses it, "only a field has a helper text", pointing at `get_tooltip`. Did
  the description keep it from trying, or did it follow the error in one step?
- Whether it confuses `helper=` with `tooltip=`, or reports Name's tooltip as a helper text.
- It may ask the descriptions to say that `…` marks a cut, so a text without one is whole. Dropped
  (2026-11-17): `…` is the universal mark of a cut; an extra `get_helper` on a short, whole
  helper costs one cheap call.
