# Navigation: AppLayout, SideNav, Breadcrumbs

Nearly every app has an AppLayout with a SideNav; the agent wants to know where it is and how to go
elsewhere, and to tell the app's chrome from the view's content.

## Today

test-app's `nav` view inside `NavLayout`: a navbar with a DrawerToggle and "Mail", a drawer with a
SideNav (Inbox; Archive with 2025 and 2026 under it), and in the view, Breadcrumbs and a heading
naming the path.

```
- AppLayout [ref=1]
  - DrawerToggle [ref=2, aria-label="Toggle navigation panel"]
  - H1 "Mail" [ref=3]
  - SideNav [ref=4]
    - SideNavItem "Inbox" [ref=5]
    - SideNavItem "Archive" [ref=6]
      - SideNavItem "2025" [ref=7]
      - SideNavItem "2026" [ref=8]
  - Breadcrumbs [ref=9]
    - BreadcrumbsItem "Home" [ref=10]
    - BreadcrumbsItem "Mail" [ref=11]
    - BreadcrumbsItem "Inbox" [ref=12]
  - H2 "nav" [ref=13]
```

Works already: `click` on a SideNavItem navigates, a nested one too; the answer's URL line shows
where to.

Doesn't: navbar, drawer and content are flattened into one list; nothing marks the current item;
an item doesn't say where it links; 2025 and 2026 show while Archive is collapsed;
`click` on the DrawerToggle closes the drawer but the snapshot doesn't change, the SideNav still
listed (`R_vaadin_hidden_in_dom`); no `actions: click` on SideNavItem, BreadcrumbsItem or
DrawerToggle. No Unified API wrapper for any of them (`UnifiedNav` is HTML's `<nav>`).

## A shape to start from

```
- AppLayout [ref=1]
  - Navbar:
    - DrawerToggle "Toggle navigation panel" [ref=2] actions: click
    - H1 "Mail" [ref=3]
  - Drawer [open]:
    - SideNav [ref=4]
      - SideNavItem "Inbox" [ref=5, current, path=nav] actions: click
      - SideNavItem "Archive" [ref=6, collapsed, path=nav/archive] actions: click
  - Content:
    …
```

## Open questions

`Q_regions`: show the navbar, drawer and content as AppLayout's slots, or leave them flat?

`Q_current`: mark the current SideNavItem and BreadcrumbsItem: from the `current` attribute the
web component sets? Not checked.

`Q_path`: a link's path lets an agent `navigate` straight there, but tempts it past what a user
clicks. Show it?

`Q_collapsed`: a collapsed item's children: leave them out, as hidden things are, and say
`[collapsed]`? Expanding is a click on the item's toggle button, in its shadow DOM; a click on the
item itself navigates. A `toggle` action, or `click` on the item's ref toggles when it has
children?

`Q_closed_drawer`: leave a closed drawer's content out, and say `Drawer [closed]`? Unlike the
other hidden things, `checkVisibility()` doesn't catch it; `snapshot-cross-cutting.md`'s
`Q_visibility`.

`Q_narrow_viewport`: at a phone's width the drawer is an overlay over the content: modal, as
`D_modal_shows_alone`? The server's browser has a desktop viewport; a `--viewport` option?
