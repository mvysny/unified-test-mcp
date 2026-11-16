package com.vaadin.unifiedtestmcp;

import com.github.mvysny.tinymcpserver.InputSchemaBuilder;
import com.github.mvysny.tinymcpserver.MCPProtocol;
import com.github.mvysny.tinymcpserver.ToolDescriptor;
import com.github.mvysny.tinymcpserver.ToolFunction;
import com.github.mvysny.tinymcpserver.ToolRequest;

/**
 * {@code navigate(path)}: loads the view at {@code path} relative to the app's URL, returns once the view is shown
 * and the server is idle.
 *
 * <pre>
 * navigate(path: "orders?status=open")   → Navigated to /orders?status=open
 * </pre>
 *
 * A full page load, so the view starts afresh. A path with no route still succeeds: the browser shows Vaadin's
 * "not found" view.
 */
final class NavigateTool implements ToolFunction {
    static final ToolDescriptor DESCRIPTOR = new ToolDescriptor("navigate",
            "Open a view of the Vaadin app by its path relative to the app's URL, as a user typing it into the "
                    + "address bar would: \"orders\", \"orders/3\", \"orders?status=open\"; \"\" is the start view. "
                    + "A full page load: the view starts afresh. Waits until the server is idle.",
            new InputSchemaBuilder().requiredString("path", "The view's path, without the app's URL").build());

    private final UnifiedBrowser browser;

    NavigateTool(UnifiedBrowser browser) {
        this.browser = browser;
    }

    @Override
    public MCPProtocol.Content call(ToolRequest request) {
        String path = request.arguments().getString("path");
        ToolCall.run(browser, page -> page.navigate(path));
        return MCPProtocol.Content.text("Navigated to /" + (path.startsWith("/") ? path.substring(1) : path));
    }
}
