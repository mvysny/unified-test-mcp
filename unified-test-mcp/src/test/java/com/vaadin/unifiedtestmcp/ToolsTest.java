package com.vaadin.unifiedtestmcp;

import com.github.mvysny.tinymcpserver.HttpMCPServer;
import com.github.mvysny.tinymcpserver.MCPHandler;
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
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The tools end to end: test-app in this JVM, a real browser on it, the tools called over MCP's HTTP transport.
 * {@link #client} is one MCP session, with one browser, for the whole class.
 */
public class ToolsTest {
    private static VaadinBoot app;
    private static MCPHandler handler;
    private static HttpMCPServer server;
    private static TinyMCPClient client;
    /** test-app's URL, {@code http://localhost:<port>}. */
    private static String appUrl;

    @BeforeAll
    public static void start() throws Exception {
        int appPort = freePort();
        app = new VaadinBoot().withPort(appPort).localhostOnly().openBrowserInDevMode(false);
        app.start();
        appUrl = "http://localhost:" + appPort;
        handler = UnifiedTestMCP.createHandler(Map.of(), Duration.ofSeconds(10), "test");
        server = new HttpMCPServer(0, "/mcp", handler);
        server.start();
        client = connect();
    }

    @AfterAll
    public static void stop() throws IOException {
        if (client != null) {
            client.close();
        }
        if (server != null) {
            server.stop();
        }
        if (app != null) {
            app.stop("test done");
        }
    }

    @BeforeEach
    public void startOnMainView() throws IOException {
        navigate(client, "");
    }

    @Test
    public void snapshotShowsEachKindOfName() throws IOException {
        assertEquals("""
                Navigated to APP/form

                URL: APP/form
                Title: Form
                - TextField "Name" [ref=N, id=name, value="Alice"]
                - TextField "Email" [ref=N, required]
                - Checkbox "Subscribe" [ref=N, checked]
                - Button "Save" [ref=N] actions: click
                - Button "Close" [ref=N, id=close, aria-label] actions: click
                - Button "Delete" [ref=N, disabled] actions: click
                - Span "Not saved" [ref=N]
                """, masked(navigate(client, "form")));
        assertEquals(masked(call(client, "snapshot", Map.of())), masked(navigate(client, "form")).substring(
                "Navigated to APP/form\n\n".length()));
    }

    @Test
    public void clickWithoutIdFindsTheStampedComponent() throws IOException {
        int save = ref(navigate(client, "form"), "Button \"Save\"");
        String after = call(client, "click", ref(save));
        assertTrue(after.startsWith("Clicked ref=" + save + "\n\nURL: " + appUrl + "/form\n"), after);
        assertTrue(after.contains("- Span \"Saved\" ["), after);
        assertEquals(save, ref(after, "Button \"Save\""), "a component keeps its ref");
    }

    @Test
    public void clickChangesText() throws IOException {
        int clicker = ref(call(client, "snapshot", Map.of()), "id=clicker");
        assertEquals("Click me", call(client, "get_text", ref(clicker)));
        String after = call(client, "click", ref(clicker));
        assertTrue(after.contains("- Button \"Clicked 1 times\" [ref=" + clicker + ", id=clicker]"), after);
        call(client, "click", ref(clicker));
        assertEquals("Clicked 2 times", call(client, "get_text", ref(clicker)));
    }

    @Test
    public void navigateOpensView() throws IOException {
        String second = navigate(client, "second");
        assertTrue(masked(second).startsWith("Navigated to APP/second\n\nURL: APP/second\n"), second);
        String main = call(client, "click", ref(ref(second, "id=back")));
        assertTrue(main.contains("\nURL: " + appUrl + "/\n- Button \"Click me\" [ref="), main);
    }

    @Test
    public void navigateToMissingRouteSaysSo() throws IOException {
        String orders = navigate(client, "orders");
        assertTrue(masked(orders).startsWith("No view at APP/orders: the app shows Vaadin's \"Could not navigate\""
                + " page\n\nURL: APP/orders\n"), orders);
        assertTrue(masked(navigate(client, "form")).startsWith("Navigated to APP/form\n"),
                "a view's text doesn't trip it");
    }

    @Test
    public void navigateStartsViewAfresh() throws IOException {
        int before = ref(call(client, "snapshot", Map.of()), "id=clicker");
        call(client, "click", ref(before));
        String reloaded = navigate(client, "");
        assertTrue(reloaded.contains("- Button \"Click me\" ["), reloaded);
        assertNotEquals(before, ref(reloaded, "id=clicker"), "a new page's components get new numbers");
    }

    @Test
    public void refFromBeforeNavigationIsRejected() throws IOException {
        int clicker = ref(call(client, "snapshot", Map.of()), "id=clicker");
        int back = ref(navigate(client, "second"), "id=back");
        assertEquals("ref " + clicker + " isn't in the latest snapshot; its refs are " + back
                + ". Call snapshot to see the components on the page now.", error(client, "get_text", ref(clicker)));
    }

    @Test
    public void refBeforeAnySnapshotIsRejected() throws IOException {
        try (TinyMCPClient other = connect()) {
            assertEquals("No refs yet: call snapshot if a page is open, or navigate to the app's URL; then use a ref"
                    + " from the snapshot either returns.",
                    error(other, "click", ref(1)));
        }
    }

    @Test
    public void browserStartsOnNoPage() throws IOException {
        try (TinyMCPClient other = connect()) {
            assertEquals("No page open yet: call navigate with the app's URL.\n", call(other, "snapshot", Map.of()));
        }
    }

    @Test
    public void navigateWithoutSchemeIsRejected() throws IOException {
        assertEquals("navigate takes a full URL, with its http:// or https://; the browser is at " + appUrl + "/.",
                error(client, "navigate", Map.of("url", "form")));
        try (TinyMCPClient other = connect()) {
            assertEquals("navigate takes a full URL, with its http:// or https://, as http://localhost:8080/orders.",
                    error(other, "navigate", Map.of("url", "form")));
        }
    }

    @Test
    public void navigateToNoServerSaysSo() throws IOException {
        String url = "http://localhost:" + freePort() + "/";
        String message = error(client, "navigate", Map.of("url", url));
        assertEquals("Can't open " + url + ": is the app running there? net::ERR_CONNECTION_REFUSED at " + url,
                message);
        assertTrue(masked(navigate(client, "")).startsWith("Navigated to APP/\n"), "the next navigate works");
    }

    @Test
    public void eachSessionHasItsOwnBrowser() throws IOException {
        int clicker = ref(call(client, "snapshot", Map.of()), "id=clicker");
        call(client, "click", ref(clicker));
        try (TinyMCPClient other = connect()) {
            int otherClicker = ref(navigate(other, ""), "id=clicker");
            call(other, "click", ref(otherClicker));
            String twice = call(other, "click", ref(otherClicker));
            assertTrue(twice.contains("\"Clicked 2 times\""), twice);
        }
        assertEquals("Clicked 1 times", call(client, "get_text", ref(clicker)));
        assertEquals(1, handler.getSessionCount());
    }

    private static TinyMCPClient connect() throws IOException {
        TinyMCPClient connected = new TinyMCPClient(URI.create(server.getUrl()));
        connected.initialize();
        return connected;
    }

    private static String call(TinyMCPClient session, String tool, Map<String, Object> arguments) throws IOException {
        MCPProtocol.CallToolResult result = session.callTool(request(tool, arguments));
        String text = result.getContent().get(0).getText();
        assertTrue(result.getIsError() == null || !result.getIsError(), text);
        return text;
    }

    /** Navigates to {@code path} in test-app: {@code "form"}, or {@code ""} for the start view. */
    private static String navigate(TinyMCPClient session, String path) throws IOException {
        return call(session, "navigate", Map.of("url", appUrl + "/" + path));
    }

    private static String error(TinyMCPClient session, String tool, Map<String, Object> arguments)
            throws IOException {
        MCPProtocol.CallToolResult result = session.callTool(request(tool, arguments));
        assertEquals(Boolean.TRUE, result.getIsError(), result.getContent().get(0).getText());
        return result.getContent().get(0).getText();
    }

    private static Map<String, Object> ref(int ref) {
        return Map.of("ref", ref);
    }

    /** The ref on the one snapshot line containing {@code part}. */
    private static int ref(String snapshot, String part) {
        List<String> lines = snapshot.lines().filter(l -> l.contains(part)).toList();
        assertEquals(1, lines.size(), "one line with " + part + " in\n" + snapshot);
        Matcher m = Pattern.compile("ref=(\\d+)").matcher(lines.get(0));
        assertTrue(m.find(), lines.get(0));
        return Integer.parseInt(m.group(1));
    }

    /** Refs as {@code ref=N}, their numbers depending on what ran before; test-app's URL as {@code APP}. */
    private static String masked(String snapshot) {
        return snapshot.replaceAll("ref=\\d+", "ref=N").replace(appUrl, "APP");
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
