package com.vaadin.unifiedtestmcp.testapp;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * At {@code form}: one of each case a snapshot names differently — a field by its label and value, a button by
 * its text, an icon-only button by its {@code aria-label} alone, a disabled button, a component without an id. The
 * {@code Save} button, which has no id, sets the status text. The one view with a page title. Hints: a field and
 * a button with a tooltip, a field with a helper text, a Checkbox with one longer than the snapshot shows. Then
 * FormItems, as Bakery's order details: a "Customer" over a Span; a "Comment" over a field with a text prefix and, in
 * a Div in its suffix, a {@code Send} button setting the status to {@code Sent: <comment>}.
 */
@Route("form")
@PageTitle("Form")
public class FormView extends VerticalLayout {
    public FormView() {
        TextField name = new TextField("Name");
        name.setId("name");
        name.setValue("Alice");
        name.setTooltipText("Your full name, as on your ID card");

        TextField email = new TextField("Email");
        email.setRequired(true);
        email.setHelperText("We never share it");

        Checkbox subscribe = new Checkbox("Subscribe", true);
        subscribe.setHelperText("One email a month: what changed in the app, what is coming next, and how to get"
                + " the most out of it. Unsubscribe at any time, from the link at the bottom of every email.");

        Span status = new Span("Not saved");
        Button save = new Button("Save", e -> status.setText("Saved"));

        Button close = new Button(VaadinIcon.CLOSE.create());
        close.setAriaLabel("Close");
        close.setId("close");
        close.setTooltipText("Close without saving");

        Button delete = new Button("Delete");
        delete.setEnabled(false);

        TextField comment = new TextField();
        comment.setPlaceholder("Add comment");
        comment.setPrefixComponent(new Span("Note:"));
        comment.setSuffixComponent(new Div(new Button("Send", e -> status.setText("Sent: " + comment.getValue()))));
        FormLayout order = new FormLayout();
        order.addFormItem(new Span("Grace Hopper"), "Customer");
        order.addFormItem(comment, "Comment");

        add(name, email, subscribe, new HorizontalLayout(save, close, delete), order, status);
    }
}
