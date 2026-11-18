# Snapshot problems across components

What the survey of the components not yet covered (one test-app view per idea file: `grid.md`,
`virtual-list.md`, `notification.md`, `navigation.md`, `disclosure.md`, `menus.md`, `upload.md`,
`messages.md`, `display-only.md`) found in every group. Each is one change to `Snapshot`
or `ComponentTypes`, not a component's, so it gets fixed once.

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

`click` works on `GridSorter`, `GridTreeToggle`, a grid cell, `Tab`, `MenuBarItem`, a Details'
summary, `MessageInputButton`; none of them says so. `Q_actions`: list `click` for each of these
types in `ComponentTypes`, so the agent knows, as for `SideNavItem`, `BreadcrumbsItem` and
`DrawerToggle`.

## Lines nothing needs

`GridColumn`, `GridFlowSelectionColumn`, `MenuBarButton`, an empty `MenuBarSubmenu`,
`NotificationContainer`, `UploadIcon`, an empty `UploadFileList`. `Q_stand_aside`: a list beside
`LAYOUTS` of tags that stand aside, their children taking their place, and one of tags left out
with what's inside, as `SKIPPED`.

## A click that times out answers with Playwright's call log

`click` on a Button under an open menu failed with Playwright's `TimeoutError`, its stack and 20
lines of "`<html>` intercepts pointer events", where a disabled MenuBarItem got the Unified API's
one-line "is not visible and enabled (waited 10000 ms)". `Q_click_errors`: `ToolCall` words a
Playwright timeout as the Unified API does: "Can't click ref=10: something over the page takes the
click"?

## Unknown HTML tags show by tag

Markdown's `<li>`, `<strong>`: `ComponentTypes` lists only the tags Flow has a class for. Fine for
a custom element; for HTML from Markdown or an `Html` component, `ListItem`, or the text alone?
