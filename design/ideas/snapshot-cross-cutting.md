# Snapshot problems across components

What the survey of the components not yet covered (one test-app view per idea file: `grid.md`,
`virtual-list.md`, `notification.md`, `navigation.md`, `disclosure.md`, `menus.md`, `upload.md`,
`messages.md`, `display-only.md`) found in every group. Each is one change to `Snapshot`
or `ComponentTypes`, not a component's, so it gets fixed once.

## Hidden content shows

The snapshot leaves out an element only when it has `hidden`, and Vaadin hides many things
without it (`R_vaadin_hidden_in_dom`): a closed Details' content, a closed AccordionPanel's, a
Popover never opened, an AvatarGroup's overflow menu, a closed MenuBar submenu, a closed drawer.
Each shows as if visible, its fields offering `set_value`.

`Q_visibility`: `checkVisibility()` gets most of them, a closed Details' or panel's content, a
closed Popover's, a collapsed SideNavItem's children, but not a closed drawer, which slides off
screen (`R_vaadin_hidden_in_dom`). `checkVisibility()` for all plus a rule for the drawer
(`drawerOpened`), as dialogs have theirs (`opened`)? A closed MenuBar submenu and the AvatarGroup
menu: not checked. It leaves out every `<input type=hidden>` too, a LoginForm's csrf one
(`R_vaadin_login_dom`), with no rule of its own. But it is false for a `display: contents`
element as well, a Dialog's footer `<div>` (`R_vaadin_dialog_dom`), while its children show
(`R_vaadin_hidden_in_dom`): such an element stands aside, its children taking its place, rather
than being left out with them.

## Generated ids

`id=content-vaadin-details-18`, `id=summary-vaadin-details-19`, `id=tabsheet-tab-8c7914ab-…`,
`id=tabsheet-panel-42`, `id=vaadin-popover-18`, `id=card-title-vaadin-card-21`: ids the web
components give themselves, meaning nothing to
the app's developer and changing between page loads.

`Q_generated_ids`: drop ids matching the web components' patterns (`vaadin-…-<n>`,
`tabsheet-tab-<uuid>`)? Flow's own `setId` ids can't be told apart in the browser.

## `value=` repeating the name

`Tab "Overview" [value="Overview"]`, `MenuBarItem "File" [value="File"]`,
`AvatarGroupMenuItem "Carol White" [value="Carol White"]`: items with a `value` property, which the
snapshot reads for any unknown `vaadin-*` tag. Drop it when it equals the name, or read `value`
only for the types `ComponentTypes` knows hold one?

## Clickable without `actions: click`

`click` works on `GridSorter`, `GridTreeToggle`, a grid cell, `SideNavItem`, `BreadcrumbsItem`,
`DrawerToggle`, `Tab`, `MenuBarItem`, a Details' summary, `MessageInputButton`; none of them says
so. `Q_actions`: list `click` for each of these types in `ComponentTypes`, so the agent knows.

## Lines nothing needs

`GridColumn`, `GridFlowSelectionColumn`, `MenuBarButton`, an empty `MenuBarSubmenu`, the empty
`Notification` elements, `NotificationContainer`, `UploadIcon`, an empty `UploadFileList`;
`<input id=csrf>`, unless `Q_visibility` leaves it out. `Q_stand_aside`: a list beside `LAYOUTS` of tags that stand aside, their
children taking their place, and one of tags left out with what's inside, as `SKIPPED`.

## A click that times out answers with Playwright's call log

`click` on a Button under an open menu failed with Playwright's `TimeoutError`, its stack and 20
lines of "`<html>` intercepts pointer events", where a disabled MenuBarItem got the Unified API's
one-line "is not visible and enabled (waited 10000 ms)". `Q_click_errors`: `ToolCall` words a
Playwright timeout as the Unified API does: "Can't click ref=10: something over the page takes the
click"?

## Unknown HTML tags show by tag

Markdown's `<li>`, `<strong>`: `ComponentTypes` lists only the tags Flow has a class for. Fine for
a custom element; for HTML from Markdown or an `Html` component, `ListItem`, or the text alone?
