package com.vaadin.unifiedtestmcp;

import com.github.mvysny.tinymcpserver.InputSchemaBuilder;
import com.github.mvysny.tinymcpserver.MCPProtocol;
import com.github.mvysny.tinymcpserver.ToolDescriptor;
import com.github.mvysny.tinymcpserver.ToolFunction;
import com.github.mvysny.tinymcpserver.ToolRequest;

/**
 * {@code click(id)}: clicks a Button, returns once the server has handled the click.
 *
 * <pre>
 * click(id: "clicker")   → Clicked Button#clicker
 * </pre>
 */
final class ClickTool implements ToolFunction {
    static final ToolDescriptor DESCRIPTOR = new ToolDescriptor("click",
            "Click a Vaadin Button, found by its id. Waits until the button is visible and enabled, clicks it, "
                    + "then waits until the server has handled the click.",
            new InputSchemaBuilder().requiredString("id", "The Button's id").build());

    private final UnifiedBrowser browser;

    ClickTool(UnifiedBrowser browser) {
        this.browser = browser;
    }

    @Override
    public MCPProtocol.Content call(ToolRequest request) {
        String id = request.arguments().getString("id");
        ToolCall.run(browser, page -> ComponentLookup.button(page, id).click());
        return MCPProtocol.Content.text("Clicked Button#" + id);
    }
}
