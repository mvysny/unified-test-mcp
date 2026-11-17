# A component's icon in the snapshot

A tester names an icon-only button by its icon: "the button with the X icon". The snapshot shows
`Button "Close" [ref=6, id=close, aria-label, tooltip="Close without saving"]` and leaves the
`<vaadin-icon>` out (`ComponentTypes.SKIPPED`), so the agent has to guess which button is the X.
Seen in the `get_text` blind review, 2026-11-17.

## Proposal

The icon as a key in the bracket, by its name in the icon set, as Flow's `VaadinIcon.CLOSE.create()`
sets it: the `icon` attribute of the `<vaadin-icon>` inside, `vaadin:close`.

```
- Button "Close" [ref=6, id=close, aria-label, icon=vaadin:close, tooltip="Close without saving"] actions: click
```

## Open

`Q_which_components`: a Button's icon only (its `prefix` slot or its only child), or any
component's — a TextField's prefix icon, a Tab's, a SideNav item's?

`Q_other_icons`: an SVG icon (`new SvgIcon(...)`, `src=`), a font icon (`FontIcon`, `font-icon-content`),
a Lumo icon (`lumo:cross`): show each by what it has, or only the named ones?

`Q_name_or_meaning`: `vaadin:close` is the icon's name, not what a user reads off it ("X"). The name is
close enough for an agent to match "X icon" to `close`?
