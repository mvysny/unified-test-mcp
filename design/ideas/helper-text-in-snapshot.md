# A field's helper text in the snapshot

The helper text below a field often says what it accepts — "8 characters or more", "dd.mm.yyyy".
An agent filling a form needs it before it types, not after the field turns invalid.

## What swing-mcp does

Its snapshot line has a second quoted slot, the description, after the name:

```
- JButton "Save" "Writes the file to disk" [ref=4] actions: click
```

- The description is the component's `accessibleDescription`, else its tooltip.
- Whitespace collapsed, `"` escaped, like the name; then cut at 120 characters with `…`.
- The name is never cut: it is identity, the agent's disambiguator. The description is advice,
  so cutting it loses nothing load-bearing (its `D_quoted_slot_sanitizing`).
- When the cut bites, the line advertises `get_description`, a tool answering the full text up
  to 1000 characters.

## Proposal

The same slot, from the field's `helperText` property:

```
- PasswordField "Password" "8 characters or more" [ref=7, value="", required]
```

## Open

`Q_tooltip`: swing-mcp falls back to the tooltip. Flow's `setTooltipText` puts a
`<vaadin-tooltip slot="tooltip" text="…">` inside the component, which the snapshot skips today.
Fall back to it for a field with no helper text, and show it for a Button too (an icon-only
Button's tooltip is often all that says what it does)?

`Q_helper_component`: `setHelperComponent` puts a component in `slot="helper"`; its text, or
nothing?

`Q_cap`: 120 characters as swing-mcp, or `value=`'s 80? And a cut description: a
`get_description` tool, or let `get_text` read it?

`Q_slot_or_key`: a bare second quoted string is what swing-mcp does, but `helper="…"` in the
bracket says what it is without a legend. The snapshot's tool description would have to explain
the bare slot.
