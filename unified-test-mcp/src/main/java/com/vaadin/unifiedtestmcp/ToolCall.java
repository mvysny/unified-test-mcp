package com.vaadin.unifiedtestmcp;

import com.github.mvysny.tinymcpserver.MCPErrorResponseException;
import com.vaadin.testing.unified.UnifiedPage;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * How a tool reaches the browser: the action runs on the page, and a failure — a lookup that timed out, an app that
 * isn't up — becomes a tool error whose message the model reads.
 *
 * <pre>{@code
 * ToolCall.run(browser, page -> page.navigate("second"));
 * String caption = ToolCall.get(browser, page -> ComponentLookup.button(page, "clicker").getText());
 * }</pre>
 *
 * A timed-out lookup throws {@link AssertionError}, which TinyMCPServer does not catch.
 */
final class ToolCall {
    private static final Logger LOG = Logger.getLogger(ToolCall.class.getName());

    private ToolCall() {}

    static void run(UnifiedBrowser browser, Consumer<UnifiedPage> action) {
        get(browser, page -> {
            action.accept(page);
            return null;
        });
    }

    static <T> T get(UnifiedBrowser browser, Function<UnifiedPage, T> action) {
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
