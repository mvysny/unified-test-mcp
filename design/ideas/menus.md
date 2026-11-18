# Menus and popovers: MenuBar, ContextMenu, Popover

What opens in an overlay on a click, a right-click or a hover, and takes the pointer while open.

## Today

test-app's `menus` view: a MenuBar (File: New, Open; Edit: Copy, Paste disabled; Help), a Span
with a ContextMenu (Rename, Delete), and an Info button with a Popover.

```
- MenuBar [ref=1]
  - MenuBarSubmenu [ref=2]
  - MenuBarButton [ref=3]
    - MenuBarItem "File" [ref=4, value="File"]
  - MenuBarButton [ref=5]
    - MenuBarItem "Edit" [ref=6, value="Edit"]
  - MenuBarButton [ref=7]
    - MenuBarItem "Help" [ref=8, value="Help"]
- Span "Right-click me" [ref=9]
- Button "Info" [ref=10] actions: click
- Span "Nothing chosen" [ref=11]
- Popover [ref=12, id=vaadin-popover-18]
  - Span "Opens on click, closes on Esc" [ref=13]
  - Button "Got it" [ref=14] actions: click
```

Works already: `click` on "File" opens its submenu, which the answer shows under `MenuBarSubmenu`,
before the bar's buttons; `click` on "New" there runs its listener.

Doesn't:

- After "New" the submenu's items are still listed (`R_vaadin_hidden_in_dom`); the snapshot can't
  tell an open submenu from a closed one.
- `click` on the disabled "Paste" waits 10 s, then fails (`beyond-buttons.md`'s `Q_timeout`).
- With Edit's submenu still open, `click` on "Info" fails after 10 s: the open submenu takes the
  pointer. The error is Playwright's whole call log, "`<html>` intercepts pointer events" 20 times.
- Nothing says the Span has a context menu, and nothing opens one: `click` is a left click.
- The Popover, never opened, shows its content and offers "Got it".

`UnifiedMenuBar` has `clickItem(topLevel, nested…)`, `getItems(…)`, `isItemEnabled(…)`. Nothing
for ContextMenu or Popover.

## A shape to start from

An open menu as `D_modal_shows_alone` shows a modal dialog: alone, since it takes the pointer.

```
- MenuBar [ref=1]
  - MenuItem "File" [ref=4, submenu] actions: click
  - MenuItem "Help" [ref=8] actions: click
```
```
Menu: "Edit" open; only the menu is shown: a click elsewhere closes it
- Menu "Edit"
  - MenuItem "Copy" [ref=19] actions: click
  - MenuItem "Paste" [ref=20, disabled]
```

## Open questions

`Q_open_menu_alone`: show an open menu alone, as a modal? Its overlay blocks the page, so a
snapshot listing the page under it invites a click that can't land.

`Q_context_menu`: how does the snapshot know a component has a ContextMenu? Until it first opens,
nothing in the DOM says so (`R_vaadin_hidden_in_dom`). And what opens it: a `right_click` tool, a
`button` argument on `click`? Without a marker, the agent right-clicks on a guess, as a user does.

`Q_menu_path`: `click` on each level's item, as a user does, or one call with the item's path, as
`UnifiedMenuBar.clickItem` takes?

`Q_disabled_fails_fast`: a disabled item, like a disabled Button, refused at once.

`Q_popover`: leave a closed Popover out; show an open one alone if modal, inline after its target
if not? Popovers that open on hover or focus need a `hover` tool, or none if test-app's only opens
on click.
