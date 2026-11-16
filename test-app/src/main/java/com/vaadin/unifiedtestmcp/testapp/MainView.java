package com.vaadin.unifiedtestmcp.testapp;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

/**
 * One button, found by its id {@code clicker}, whose caption counts its clicks.
 */
@Route("")
public class MainView extends VerticalLayout {
    private int clicks;

    public MainView() {
        Button button = new Button("Click me");
        button.setId("clicker");
        button.addClickListener(e -> button.setText("Clicked " + ++clicks + " times"));
        add(button);
    }
}
