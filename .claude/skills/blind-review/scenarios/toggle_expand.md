# toggle_expand

## Task

A Vaadin web app is running at {{url}}, and a browser is ready to drive it. Open the page at
`nav`. Its side menu has an Archive section: tell me which entries Archive holds, but stay on the
page you're on, don't open Archive's own page. Tell me the address you're on afterwards. Then fold
Archive away again. Next, try to fold out the Inbox entry and the Trash entry, and tell me what
happened with each.

## Done when

- Archive holds "2025" and "2026"; the address is still `…/nav`, not `…/nav/archive`.
- Archive folded away: its entries are no longer listed, and it shows `collapsed` again.
- Inbox: has no entries under it, nothing to fold out.
- Trash: disabled, so it can't be folded out.

## Watch for

- Whether it finds `toggle_expand` from the snapshot's `actions:`, or clicks Archive, which opens
  `nav/archive`. If it clicks, does it notice the address changed?
- After the first `toggle_expand`, does it read the answer's snapshot, or call `snapshot` again?
- Inbox and Trash: does it call `toggle_expand` anyway, though their `actions:` don't list it on
  Inbox? Does each error say why, and what to do instead?
