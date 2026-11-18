package com.vaadin.unifiedtestmcp.testapp;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;

/**
 * At {@code virtual-list}: a VirtualList of 200 contacts, more than it shows, each rendered as components.
 *
 * <pre>
 * Alice Novak [Call]      each [Call] sets the status
 * Bob Novak   [Call]
 * …
 * Status: Idle
 * </pre>
 */
@Route("virtual-list")
public class VirtualListView extends VerticalLayout {
    public VirtualListView() {
        Span status = new Span("Idle");

        VirtualList<GridView.Person> contacts = new VirtualList<>();
        contacts.setRenderer(new ComponentRenderer<HorizontalLayout, GridView.Person>(person -> new HorizontalLayout(
                new Span(person.name()), new Button("Call", e -> status.setText("Calling " + person.name())))));
        contacts.setItems(GridView.PEOPLE);
        contacts.setHeight("300px");

        add(contacts, status);
    }
}
