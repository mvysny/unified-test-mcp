package com.vaadin.unifiedtestmcp;

import com.github.mvysny.tinymcpserver.InputSchemaBuilder;
import com.github.mvysny.tinymcpserver.MCPErrorResponseException;
import com.github.mvysny.tinymcpserver.MCPHandler;
import com.github.mvysny.tinymcpserver.MCPProtocol;
import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedButton;

import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The MCP tools over a {@link UnifiedBrowser}. For now the component tools know Buttons only, found by id.
 */
public final class UnifiedTestMCP {
    private static final Logger LOG = Logger.getLogger(UnifiedTestMCP.class.getName());

    private UnifiedTestMCP() {}

    /**
     * A handler with the tools registered, ready for either transport:
     * <ul>
     *   <li>{@code navigate(path)} — loads the view at {@code path} relative to the app's URL, {@code "orders?id=3"};
     *       returns once the view is shown and the server is idle</li>
     *   <li>{@code click(id)} — clicks the Button, returns once the server has handled the click</li>
     *   <li>{@code get_text(id)} — the Button's caption</li>
     * </ul>
     */
    public static MCPHandler createHandler(UnifiedBrowser browser, String version) {
        MCPHandler handler = new MCPHandler(new MCPProtocol.Implementation("unified-test-mcp", version),
                "Drives a running Vaadin app in a real browser. The browser opens the app on the first tool call.");

        handler.addTool("navigate",
                "Open a view of the Vaadin app by its path relative to the app's URL, as a user typing it into the "
                        + "address bar would: \"orders\", \"orders/3\", \"orders?status=open\"; \"\" is the start view. "
                        + "A full page load: the view starts afresh. Waits until the server is idle.",
                new InputSchemaBuilder().requiredString("path", "The view's path, without the app's URL").build(),
                request -> {
                    String path = request.arguments().getString("path");
                    call(browser, page -> {
                        page.navigate(path);
                        return null;
                    });
                    return MCPProtocol.Content.text("Navigated to /" + (path.startsWith("/") ? path.substring(1) : path));
                });

        handler.addTool("click",
                "Click a Vaadin Button, found by its id. Waits until the button is visible and enabled, clicks it, "
                        + "then waits until the server has handled the click.",
                new InputSchemaBuilder().requiredString("id", "The Button's id").build(),
                request -> {
                    String id = request.arguments().getString("id");
                    call(browser, page -> {
                        button(page, id).click();
                        return null;
                    });
                    return MCPProtocol.Content.text("Clicked Button#" + id);
                });

        handler.addTool("get_text", "The caption of a Vaadin Button, found by its id.",
                new InputSchemaBuilder().requiredString("id", "The Button's id").build(),
                request -> {
                    String id = request.arguments().getString("id");
                    return MCPProtocol.Content.text(call(browser, page -> button(page, id).getText()));
                });
        return handler;
    }

    private static UnifiedButton button(UnifiedPage page, String id) {
        return page.find(UnifiedButton.class).withId(id).single();
    }

    /**
     * Runs a browser action; a failure — a lookup that timed out, an app that isn't up — becomes a tool error whose
     * message the model reads.
     */
    private static <T> T call(UnifiedBrowser browser, Function<UnifiedPage, T> action) {
        try {
            return browser.call(action);
        } catch (MCPErrorResponseException e) {
            throw e;
        } catch (RuntimeException | AssertionError e) {
            LOG.log(Level.FINE, "Tool call failed", e);
            throw new MCPErrorResponseException(e.getMessage());
        }
    }
}
