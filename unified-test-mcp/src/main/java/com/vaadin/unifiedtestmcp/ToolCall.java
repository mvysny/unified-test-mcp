package com.vaadin.unifiedtestmcp;

import com.github.mvysny.tinymcpserver.MCPErrorResponseException;
import com.github.mvysny.tinymcpserver.MCPSession;
import com.vaadin.testing.unified.UnifiedPage;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * How a tool reaches the browser: the action runs on the page of the calling MCP session's {@link UnifiedBrowser},
 * with that session's {@link Refs}, and a failure — a lookup that timed out, an app that isn't up — becomes a tool
 * error whose message the model reads.
 *
 * <pre>{@code
 * String text = ToolCall.get((page, refs) -> refs.resolve(page, 7).element().getText());
 * }</pre>
 *
 * Call it from a tool's {@code call} only: the session is bound to that thread. A timed-out lookup throws
 * {@link AssertionError}, which TinyMCPServer does not catch.
 */
final class ToolCall {
    private static final Logger LOG = Logger.getLogger(ToolCall.class.getName());

    private ToolCall() {}

    /** Runs on the browser thread. */
    @FunctionalInterface
    interface Action<T> {
        T apply(UnifiedPage page, Refs refs);
    }

    static <T> T get(Action<T> action) {
        MCPSession session = MCPSession.getCurrent();
        UnifiedBrowser browser = session.getAttribute(UnifiedBrowser.class);
        Refs refs = session.getAttribute(Refs.class);
        try {
            return browser.call(page -> action.apply(page, refs));
        } catch (MCPErrorResponseException e) {
            throw e;
        } catch (RuntimeException | AssertionError e) {
            LOG.log(Level.FINE, "Tool call failed", e);
            throw new MCPErrorResponseException(e.getMessage());
        }
    }
}
