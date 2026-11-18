package com.vaadin.unifiedtestmcp.testapp;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;

/**
 * The app shell of {@link NavigationView}: a navbar with a drawer toggle and a title, and a drawer with a SideNav.
 *
 * <pre>
 * [≡] Mail
 * ┌──────────┐
 * │ Inbox    │  nav
 * │ Archive ▾│  nav/archive, with 2025 and 2026 under it
 * │ Trash   ▾│  disabled, no page, with Old under it
 * └──────────┘
 * </pre>
 */
public class NavLayout extends AppLayout {
    public NavLayout() {
        SideNavItem archive = new SideNavItem("Archive", "nav/archive");
        archive.addItem(new SideNavItem("2025", "nav/archive/2025"), new SideNavItem("2026", "nav/archive/2026"));
        SideNavItem trash = new SideNavItem("Trash");
        trash.addItem(new SideNavItem("Old"));
        trash.setEnabled(false);
        SideNav nav = new SideNav();
        nav.addItem(new SideNavItem("Inbox", "nav"), archive, trash);

        addToNavbar(new DrawerToggle(), new H1("Mail"));
        addToDrawer(nav);
    }
}
