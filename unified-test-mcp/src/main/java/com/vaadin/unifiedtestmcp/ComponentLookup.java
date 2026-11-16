package com.vaadin.unifiedtestmcp;

import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedButton;

/**
 * Finds the component a tool's arguments name. Every lookup waits up to the browser's timeout for the component
 * to show up, then fails with the Unified API's "No component matches …".
 */
final class ComponentLookup {
    private ComponentLookup() {}

    static UnifiedButton button(UnifiedPage page, String id) {
        return page.find(UnifiedButton.class).withId(id).single();
    }
}
