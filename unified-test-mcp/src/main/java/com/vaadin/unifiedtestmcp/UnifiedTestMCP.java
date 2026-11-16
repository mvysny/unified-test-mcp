package com.vaadin.unifiedtestmcp;

import com.github.mvysny.tinymcpserver.MCPHandler;
import com.github.mvysny.tinymcpserver.MCPProtocol;

import java.time.Duration;
import java.util.Map;

/**
 * The MCP server's handler, ready for either transport: every tool registered, and a {@link UnifiedBrowser} of its
 * own for each MCP session, closed when the session closes.
 *
 * <pre>{@code
 * MCPHandler handler = UnifiedTestMCP.createHandler(
 *         Map.of("vaadin.testing.base-url", "http://localhost:8080"), Duration.ofSeconds(10), "1.0");
 * new StdioMCPServer(handler).runStdio(System.in, System.out);
 * }</pre>
 *
 * One class per tool — {@link NavigateTool}, {@link ClickTool}, {@link GetTextTool} — each holding its description
 * and schema. For now the component tools know Buttons only, found by id.
 */
public final class UnifiedTestMCP {
    private UnifiedTestMCP() {}

    /**
     * @param settings what each session's {@link UnifiedBrowser} is created with
     * @param timeout  how long a lookup or an action waits before it fails
     */
    public static MCPHandler createHandler(Map<String, String> settings, Duration timeout, String version) {
        MCPHandler handler = new MCPHandler(new MCPProtocol.Implementation("unified-test-mcp", version),
                "Drives a running Vaadin app in a real browser. The browser opens the app on the first tool call.");
        handler.setOnSessionStarted(session ->
                session.setAttribute(UnifiedBrowser.class, new UnifiedBrowser(settings, timeout)));
        handler.setOnSessionClosed(session -> {
            UnifiedBrowser browser = session.getAttribute(UnifiedBrowser.class);
            if (browser != null) {
                browser.close();
            }
        });
        handler.addTool(NavigateTool.DESCRIPTOR, new NavigateTool());
        handler.addTool(ClickTool.DESCRIPTOR, new ClickTool());
        handler.addTool(GetTextTool.DESCRIPTOR, new GetTextTool());
        return handler;
    }
}
