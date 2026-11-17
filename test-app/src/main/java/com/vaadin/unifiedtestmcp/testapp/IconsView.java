package com.vaadin.unifiedtestmcp.testapp;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.FontIcon;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoIcon;

/**
 * At {@code icons}: an icon of each kind, where a snapshot shows it and where it doesn't.
 *
 * <pre>
 * [🔍 Search]            shown: vaadin:search, in the prefix slot
 * [Next →]               not shown: after the text, in the suffix slot
 * [✕]                    shown: lumo:cross, an icon-only Button's; aria-label "Dismiss"
 * Filter [🔍         ]   shown: vaadin:search, a field's prefix
 * [home Home]            shown: a FontIcon's ligature, home
 * [👤 Profile]           shown: a FontIcon's classes, fa-solid fa-user
 * [★ Star]               not shown: a FontIcon by its char code has no name
 * [▣ Logo]               not shown: an SvgIcon has no name
 * </pre>
 */
@Route("icons")
public class IconsView extends VerticalLayout {
    /** A black square, inline: no file to serve. */
    private static final String LOGO = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 1 1'%3E"
            + "%3Crect width='1' height='1'/%3E%3C/svg%3E";

    public IconsView() {
        Button next = new Button("Next", VaadinIcon.ARROW_RIGHT.create());
        next.setIconAfterText(true);

        Button dismiss = new Button(LumoIcon.CROSS.create());
        dismiss.setAriaLabel("Dismiss");

        TextField filter = new TextField("Filter");
        filter.setPrefixComponent(VaadinIcon.SEARCH.create());

        FontIcon home = new FontIcon();
        home.setLigature("home");

        FontIcon star = new FontIcon();
        star.setCharCode("2605");

        add(new Button("Search", VaadinIcon.SEARCH.create()), next, dismiss, filter,
                new Button("Home", home), new Button("Profile", new FontIcon("fa-solid", "fa-user")),
                new Button("Star", star), new Button("Logo", new SvgIcon(LOGO)));
    }
}
