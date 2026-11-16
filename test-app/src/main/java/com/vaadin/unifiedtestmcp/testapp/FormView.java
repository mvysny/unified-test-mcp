package com.vaadin.unifiedtestmcp.testapp;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

/**
 * At {@code form}: one of each case a snapshot names differently — a field by its label and value, a button by
 * its text, an icon-only button by its {@code aria-label}, a disabled button, a component without an id. The
 * {@code Save} button, which has no id, sets the status text.
 */
@Route("form")
public class FormView extends VerticalLayout {
    public FormView() {
        TextField name = new TextField("Name");
        name.setId("name");
        name.setValue("Alice");

        TextField email = new TextField("Email");
        email.setRequired(true);

        Checkbox subscribe = new Checkbox("Subscribe", true);

        Span status = new Span("Not saved");
        Button save = new Button("Save", e -> status.setText("Saved"));

        Button close = new Button(VaadinIcon.CLOSE.create());
        close.setAriaLabel("Close");
        close.setId("close");

        Button delete = new Button("Delete");
        delete.setEnabled(false);

        add(name, email, subscribe, new HorizontalLayout(save, close, delete), status);
    }
}
