package com.vaadin.unifiedtestmcp.testapp;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

/**
 * At {@code edit}: fields whose server-side code answers a change, each in its own way:
 *
 * <pre>
 * Name      [Alice]    → greeting "Hello, Alice"    a listener reads the new value
 * Username  [alice]    "admin" is refused           a listener puts back the value it held
 * Age       [42]       above 120 shows invalid      Flow's own validation, "At most 120"
 * </pre>
 */
@Route("edit")
public class EditView extends VerticalLayout {
    public EditView() {
        Span greeting = new Span("Hello, Alice");
        greeting.setId("greeting");
        TextField name = new TextField("Name", "Alice", "");
        name.addValueChangeListener(e -> greeting.setText("Hello, " + e.getValue()));

        TextField username = new TextField("Username", "alice", "");
        username.addValueChangeListener(e -> {
            if (e.getValue().equals("admin")) {
                username.setValue(e.getOldValue());
            }
        });

        IntegerField age = new IntegerField("Age");
        age.setValue(42);
        age.setMax(120);
        age.setI18n(new IntegerField.IntegerFieldI18n().setMaxErrorMessage("At most 120"));

        add(name, greeting, username, age);
    }
}
