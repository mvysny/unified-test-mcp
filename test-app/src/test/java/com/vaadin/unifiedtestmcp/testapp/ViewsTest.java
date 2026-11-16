package com.vaadin.unifiedtestmcp.testapp;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.github.mvysny.kaributesting.v10.Routes;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import org.junit.jupiter.api.*;

import static com.github.mvysny.kaributesting.v10.LocatorJ.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ViewsTest {
    private static Routes routes;

    @BeforeAll
    public static void createRoutes() {
        routes = new Routes().autoDiscoverViews("com.vaadin.unifiedtestmcp.testapp");
    }

    @BeforeEach
    public void setupVaadin() { MockVaadin.setup(routes); }

    @AfterEach
    public void teardownVaadin() { MockVaadin.tearDown(); }

    @Test
    public void clickCountsInCaption() {
        Button button = _get(Button.class, s -> s.withId("clicker"));
        assertEquals("Click me", button.getText());
        _click(button);
        assertEquals("Clicked 1 times", button.getText());
        _click(button);
        assertEquals("Clicked 2 times", button.getText());
    }

    @Test
    public void backButtonReturnsToMainView() {
        UI.getCurrent().navigate(SecondView.class);
        _click(_get(Button.class, s -> s.withId("back")));
        _assertOne(MainView.class);
    }
}
