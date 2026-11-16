package com.vaadin.unifiedtestmcp;

import com.github.mvysny.tinymcpserver.MCPErrorResponseException;
import com.github.mvysny.tinymcpserver.MCPSession;
import com.vaadin.testing.unified.UnifiedPage;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * How a tool reaches the browser: the action runs on the page of the calling MCP session's {@link UnifiedBrowser},
 * and a failure — a lookup that timed out, an app that isn't up — becomes a tool error whose message the model reads.
 *
 * <pre>{@code
 * ToolCall.run(page -> page.navigate("second"));
 * String caption = ToolCall.get(page -> ComponentLookup.button(page, "clicker").getText());
 * }</pre>
 *
 * Call it from a tool's {@code call} only: the session is bound to that thread. A timed-out lookup throws
 * {@link AssertionError}, which TinyMCPServer does not catch.
 */
final class ToolCall {
    private static final Logger LOG = Logger.getLogger(ToolCall.class.getName());

    private ToolCall() {}

    static void run(Consumer<UnifiedPage> action) {
        get(page -> {
            action.accept(page);
            return null;
        });
    }

    static <T> T get(Function<UnifiedPage, T> action) {
        UnifiedBrowser browser = MCPSession.getCurrent().getAttribute(UnifiedBrowser.class);
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
