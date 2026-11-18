package com.vaadin.unifiedtestmcp.testapp;

import com.vaadin.flow.component.breadcrumbs.Breadcrumbs;
import com.vaadin.flow.component.breadcrumbs.BreadcrumbsItem;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

/**
 * At {@code nav} and under it, inside {@link NavLayout}: Breadcrumbs, and a heading naming where the user is.
 *
 * <pre>
 * Home › Mail › Inbox        Breadcrumbs, each but the last a link
 * nav/archive/2025           the heading: the current path
 * </pre>
 */
@Route(value = "nav", layout = NavLayout.class)
@RouteAlias(value = "nav/archive", layout = NavLayout.class)
@RouteAlias(value = "nav/archive/2025", layout = NavLayout.class)
@RouteAlias(value = "nav/archive/2026", layout = NavLayout.class)
public class NavigationView extends VerticalLayout implements BeforeEnterObserver {
    private final H2 where = new H2();

    public NavigationView() {
        Breadcrumbs breadcrumbs = new Breadcrumbs(Breadcrumbs.Mode.MANUAL);
        breadcrumbs.add(new BreadcrumbsItem("Home", ""), new BreadcrumbsItem("Mail", "nav"),
                new BreadcrumbsItem("Inbox"));
        add(breadcrumbs, where);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        where.setText(event.getLocation().getPath());
    }
}
