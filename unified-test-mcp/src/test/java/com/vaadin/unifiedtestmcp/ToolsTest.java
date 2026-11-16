package com.vaadin.unifiedtestmcp;

import com.github.mvysny.tinymcpserver.HttpMCPServer;
import com.github.mvysny.tinymcpserver.MCPProtocol;
import com.github.mvysny.tinymcpserver.ToolRequest;
import com.github.mvysny.tinymcpserver.client.TinyMCPClient;
import com.github.mvysny.vaadinboot.VaadinBoot;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The tools end to end: test-app in this JVM, a real browser on it, the tools called over MCP's HTTP transport.
 */
public class ToolsTest {
    private static VaadinBoot app;
    private static UnifiedBrowser browser;
    private static HttpMCPServer server;
    private static TinyMCPClient client;

    @BeforeAll
    public static void start() throws Exception {
        int appPort = freePort();
        app = new VaadinBoot().withPort(appPort).localhostOnly().openBrowserInDevMode(false);
        app.start();
        browser = new UnifiedBrowser(Map.of("vaadin.testing.base-url", "http://localhost:" + appPort),
                Duration.ofSeconds(10));
        server = new HttpMCPServer(0, "/mcp", UnifiedTestMCP.createHandler(browser, "test"));
        server.start();
        client = new TinyMCPClient(URI.create(server.getUrl()));
        client.initialize();
    }

    @AfterAll
    public static void stop() throws IOException {
        if (client != null) {
            client.close();
        }
        if (server != null) {
            server.stop();
        }
        if (browser != null) {
            browser.close();
        }
        if (app != null) {
            app.stop("test done");
        }
    }

    @BeforeEach
    public void startOnMainView() throws IOException {
        call("navigate", Map.of("path", ""));
    }

    @Test
    public void clickChangesCaption() throws IOException {
        assertEquals("Click me", call("get_text", id("clicker")));
        assertEquals("Clicked Button#clicker", call("click", id("clicker")));
        assertEquals("Clicked 1 times", call("get_text", id("clicker")));
        call("click", id("clicker"));
        assertEquals("Clicked 2 times", call("get_text", id("clicker")));
    }

    @Test
    public void navigateOpensView() throws IOException {
        assertEquals("Navigated to /second", call("navigate", Map.of("path", "second")));
        assertEquals("Back to main", call("get_text", id("back")));
        call("click", id("back"));
        assertEquals("Click me", call("get_text", id("clicker")));
    }

    @Test
    public void navigateStartsViewAfresh() throws IOException {
        call("click", id("clicker"));
        assertEquals("Clicked 1 times", call("get_text", id("clicker")));
        call("navigate", Map.of("path", "/"));
        assertEquals("Click me", call("get_text", id("clicker")));
    }

    @Test
    public void unknownIdIsToolError() throws IOException {
        MCPProtocol.CallToolResult result = client.callTool(request("get_text", id("no-such-id")));
        assertEquals(Boolean.TRUE, result.getIsError());
        String message = result.getContent().get(0).getText();
        assertTrue(message.startsWith("No component matches "), message);
    }

    private static String call(String tool, Map<String, Object> arguments) throws IOException {
        MCPProtocol.CallToolResult result = client.callTool(request(tool, arguments));
        String text = result.getContent().get(0).getText();
        assertTrue(result.getIsError() == null || !result.getIsError(), text);
        return text;
    }

    private static Map<String, Object> id(String id) {
        return Map.of("id", id);
    }

    private static ToolRequest request(String tool, Map<String, Object> arguments) {
        return new ToolRequest(tool, arguments, Map.of(), null);
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
