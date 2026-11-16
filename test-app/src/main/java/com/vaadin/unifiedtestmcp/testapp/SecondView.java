package com.vaadin.unifiedtestmcp.testapp;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

/**
 * A view reached by its path {@code second}, with one button, found by its id {@code back}, that navigates to
 * {@link MainView}.
 */
@Route("second")
public class SecondView extends VerticalLayout {
    public SecondView() {
        Button back = new Button("Back to main", e -> e.getSource().getUI().ifPresent(ui -> ui.navigate(MainView.class)));
        back.setId("back");
        add(back);
    }
}
