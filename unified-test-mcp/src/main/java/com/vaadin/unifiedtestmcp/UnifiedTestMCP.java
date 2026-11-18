package com.vaadin.unifiedtestmcp;

import com.github.mvysny.tinymcpserver.MCPHandler;
import com.github.mvysny.tinymcpserver.MCPProtocol;

import java.time.Duration;
import java.util.Map;

/**
 * The MCP server's handler, ready for either transport: every tool registered, and for each MCP session a
 * {@link UnifiedBrowser} of its own, closed when the session closes, and its {@link Refs}.
 *
 * <pre>{@code
 * MCPHandler handler = UnifiedTestMCP.createHandler(
 *         Map.of("vaadin.testing.playwright.headless", "false"), Duration.ofSeconds(10), "1.0");
 * new StdioMCPServer(handler).runStdio(System.in, System.out);
 * }</pre>
 *
 * One class per tool — {@link SnapshotTool}, {@link NavigateTool}, {@link ClickTool}, {@link GetTextTool},
 * {@link GetValueTool}, {@link SetValueTool}, {@link GetHelperTool}, {@link GetTooltipTool} — each
 * holding its description and schema.
 */
public final class UnifiedTestMCP {
    private UnifiedTestMCP() {}

    private static final String INSTRUCTIONS = """
            Drives a running Vaadin app in a real browser, in terms of its Vaadin components: Button, TextField, \
            Grid. The browser starts on an empty page.

            ## Workflow

            1. `navigate` to the app's URL: it shows the components on the page, each with a `ref`; so does \
            `snapshot`, for the page as it is now.
            2. Act on a component by its `ref` (`click`, `set_value`, `get_text`, `get_value`, `get_helper`, \
            `get_tooltip`).
            3. An action that changes the page (`click`, `set_value`, `navigate`) returns the new snapshot: read what it did \
            there, no need to call `snapshot` again.

            A ref stays valid as long as the latest snapshot shows it; a component keeps its ref from one snapshot \
            to the next.""";

    /**
     * Also starts {@link BrowserInstall} in the background, so a first download is under way before the first tool
     * call.
     *
     * @param settings what each session's {@link UnifiedBrowser} is created with
     * @param timeout  how long a lookup or an action waits before it fails
     */
    public static MCPHandler createHandler(Map<String, String> settings, Duration timeout, String version) {
        BrowserInstall.start(settings);
        MCPHandler handler = new MCPHandler(new MCPProtocol.Implementation("unified-test-mcp", version), INSTRUCTIONS);
        handler.setOnSessionStarted(session -> {
            session.setAttribute(UnifiedBrowser.class, new UnifiedBrowser(settings, timeout));
            session.setAttribute(Refs.class, new Refs());
        });
        handler.setOnSessionClosed(session -> {
            UnifiedBrowser browser = session.getAttribute(UnifiedBrowser.class);
            if (browser != null) {
                browser.close();
            }
        });
        handler.addTool(SnapshotTool.DESCRIPTOR, new SnapshotTool());
        handler.addTool(NavigateTool.DESCRIPTOR, new NavigateTool());
        handler.addTool(ClickTool.DESCRIPTOR, new ClickTool());
        handler.addTool(GetTextTool.DESCRIPTOR, new GetTextTool());
        handler.addTool(GetValueTool.DESCRIPTOR, new GetValueTool());
        handler.addTool(SetValueTool.DESCRIPTOR, new SetValueTool());
        handler.addTool(GetHelperTool.DESCRIPTOR, new GetHelperTool());
        handler.addTool(GetTooltipTool.DESCRIPTOR, new GetTooltipTool());
        return handler;
    }
}
