package com.vaadin.unifiedtestmcp;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.vaadin.testing.unified.UnifiedElement;
import com.vaadin.testing.unified.UnifiedPage;
import org.vaadin.addons.dramafinder.AutoWait;

import java.util.Map;

/**
 * A real click on a part of a component's shadow DOM, which no Unified API wrapper clicks:
 *
 * <pre>{@code
 * ShadowParts.click(page, item, "link");            // a SideNavItem's own row, not its items below it
 * ShadowParts.click(page, item, "toggle-button");   // its arrow: expands or collapses it
 * }</pre>
 *
 * Waits for the server afterwards, as the Unified API does after a click.
 *
 * <h2>Implementation detail</h2>
 * Clicks the component's own box at the part's centre. Why not a locator into the shadow DOM: it would find that part
 * of every nested component too, a SideNavItem's items being SideNavItems.
 */
final class ShadowParts {
    private ShadowParts() {}

    /** Where the part is centred in the component's box, as a JavaScript function of the component and the part. */
    private static final String CENTRE = """
            (e, part) => {
              const box = e.getBoundingClientRect();
              const at = e.shadowRoot.querySelector(`[part~="${part}"]`).getBoundingClientRect();
              return { x: at.left - box.left + at.width / 2, y: at.top - box.top + at.height / 2 };
            }""";

    /** @param part the name the component's shadow DOM gives the part, its {@code part} attribute */
    static void click(UnifiedPage page, UnifiedElement component, String part) {
        Locator locator = component.unwrap(Locator.class);
        Map<?, ?> at = (Map<?, ?>) locator.evaluate(CENTRE, part);
        locator.click(new Locator.ClickOptions()
                .setPosition(((Number) at.get("x")).doubleValue(), ((Number) at.get("y")).doubleValue()));
        AutoWait.afterAction(page.unwrap(Page.class));
    }
}
