# Navigation: AppLayout, SideNav, Breadcrumbs

Nearly every app has an AppLayout with a SideNav; the agent wants to know where it is and how to go
elsewhere, and to tell the app's chrome from the view's content.

## Today

test-app's `nav` view inside `NavLayout`: a navbar with a DrawerToggle and "Mail", a drawer with a
SideNav (Inbox; Archive with 2025 and 2026 under it), and in the view, Breadcrumbs and a heading
naming the path.

```
- AppLayout [ref=1]
  - Navbar:
    - DrawerToggle [ref=2, aria-label="Toggle navigation panel"] actions: click
    - H1 "Mail" [ref=3]
  - Drawer:
    - SideNav [ref=4]
      - SideNavItem "Inbox" [ref=5, path="nav", current] actions: click
      - SideNavItem "Archive" [ref=6, path="nav/archive", collapsed] actions: click, toggle_expand
  - Content:
    - Breadcrumbs [ref=7]
      - BreadcrumbsItem "Home" [ref=8, path=""] actions: click
      - BreadcrumbsItem "Mail" [ref=9, path="nav"] actions: click
      - BreadcrumbsItem "Inbox" [ref=10, current] actions: click
    - H2 "nav" [ref=11]
```

Closed: `- Drawer: closed; click the DrawerToggle, ref=2, to open it` (`D_app_layout_regions`,
`D_nav_item_states`). No Unified API wrapper for any of them (`UnifiedNav` is HTML's `<nav>`).

## Open questions

`Q_collapsible_side_nav`: a SideNav with a label folds all its items away; its label is the
toggle. `toggle_expand` on the SideNav? Not tried.

`Q_narrow_viewport`: at a phone's width the drawer is an overlay over the content: modal, as
`D_modal_shows_alone`? The server's browser has a desktop viewport; a `--viewport` option?

`Q_breadcrumbs_overflow`: Breadcrumbs that don't fit hide items behind an overflow button, in an
overlay. Not tried.
