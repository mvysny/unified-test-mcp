package com.vaadin.unifiedtestmcp;

import com.github.mvysny.tinymcpserver.MCPHandler;
import com.github.mvysny.tinymcpserver.MCPProtocol;

/**
 * The MCP server's handler: every tool registered over one {@link UnifiedBrowser}, ready for either transport.
 * One class per tool — {@link NavigateTool}, {@link ClickTool}, {@link GetTextTool} — each holding its
 * description and schema. For now the component tools know Buttons only, found by id.
 */
public final class UnifiedTestMCP {
    private UnifiedTestMCP() {}

    public static MCPHandler createHandler(UnifiedBrowser browser, String version) {
        MCPHandler handler = new MCPHandler(new MCPProtocol.Implementation("unified-test-mcp", version),
                "Drives a running Vaadin app in a real browser. The browser opens the app on the first tool call.");
        handler.addTool(NavigateTool.DESCRIPTOR, new NavigateTool(browser));
        handler.addTool(ClickTool.DESCRIPTOR, new ClickTool(browser));
        handler.addTool(GetTextTool.DESCRIPTOR, new GetTextTool(browser));
        return handler;
    }
}
