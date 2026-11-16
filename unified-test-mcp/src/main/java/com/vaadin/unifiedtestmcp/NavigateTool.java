package com.vaadin.unifiedtestmcp;

import com.github.mvysny.tinymcpserver.InputSchemaBuilder;
import com.github.mvysny.tinymcpserver.MCPErrorResponseException;
import com.github.mvysny.tinymcpserver.MCPProtocol;
import com.github.mvysny.tinymcpserver.ToolDescriptor;
import com.github.mvysny.tinymcpserver.ToolFunction;
import com.github.mvysny.tinymcpserver.ToolRequest;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.TimeoutError;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@code navigate(url)}: loads {@code url} in the browser, then answers with the snapshot of the page it landed on.
 *
 * <pre>
 * navigate(url: "http://localhost:8080/form")   → Navigated to http://localhost:8080/form
 *
 *                                                 URL: http://localhost:8080/form
 *                                                 - TextField "Name" [ref=1, id=name, value="Alice"]
 *                                                 …
 * </pre>
 *
 * A full page load, so the view starts afresh. The snapshot's URL is where the browser is, which tells a reroute,
 * to a login view say, from the URL asked for.
 *
 * <p>A path with no view is not a tool error: the page did load, and its snapshot is valid. Flow's own not-found
 * view is told apart, {@code No view at http://…/orders: …}; an app's own not-found view only shows in the
 * snapshot.
 *
 * <h2>Implementation detail</h2>
 * A URL without a scheme is refused here: the Unified API would resolve it against its base URL, which this server
 * doesn't set, so it would land on {@code http://localhost:8080} unasked.
 *
 * <p>Flow's not-found view is found by its text, the same in dev and production mode. Not by HTTP status: the page
 * and the request that resolves the route both answer 200 for any path.
 */
final class NavigateTool implements ToolFunction {
    private static final Logger LOG = Logger.getLogger(NavigateTool.class.getName());

    static final ToolDescriptor DESCRIPTOR = new ToolDescriptor("navigate",
            "Open a URL in the browser, as a user typing it into the address bar would: "
                    + "\"http://localhost:8080/orders?status=open\". A full page load: the view starts afresh. "
                    + "Waits until the server is idle, then returns the snapshot of the page, whose URL line says "
                    + "where the browser landed. A path with no view is not an error: the result starts \"No view "
                    + "at\" when the app shows Vaadin's own not-found page; an app's own not-found view shows only "
                    + "in the snapshot.",
            new InputSchemaBuilder().requiredString("url", "The full URL, with its http:// or https://").build());

    private static final Pattern HAS_SCHEME = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*://.*");

    /** {@code Error {\n  message='net::ERR_CONNECTION_REFUSED at …\n  name='Error …}. */
    private static final Pattern PLAYWRIGHT_MESSAGE = Pattern.compile("message='([^\\n]*)");

    /** Whether the page shows Flow's {@code RouteNotFoundError}: its templates open with this text. */
    private static final String NOT_FOUND_SCRIPT = """
            () => [...document.querySelectorAll('div')].some(d =>
                d.firstChild && d.firstChild.nodeType === Node.TEXT_NODE
                && d.firstChild.textContent.trim().startsWith("Could not navigate to '"))""";

    @Override
    public MCPProtocol.Content call(ToolRequest request) {
        String url = request.arguments().getString("url");
        return MCPProtocol.Content.text(ToolCall.get((page, refs) -> {
            Page browser = page.unwrap(Page.class);
            if (!HAS_SCHEME.matcher(url).matches()) {
                throw new MCPErrorResponseException("navigate takes a full URL, with its http:// or https://"
                        + (Snapshot.isBlank(browser) ? ", as http://localhost:8080/orders"
                        : "; the browser is at " + browser.url()) + ".");
            }
            try {
                page.navigate(url);
            } catch (RuntimeException e) {
                settleOnErrorPage(browser);
                throw new MCPErrorResponseException("Can't open " + url + ": is the app running there? "
                        + playwrightMessage(e));
            }
            boolean notFound = (Boolean) browser.evaluate(NOT_FOUND_SCRIPT);
            return (notFound ? "No view at " + url + ": the app shows Vaadin's \"Could not navigate\" page"
                    : "Navigated to " + url) + "\n\n" + Snapshot.take(page, refs);
        }));
    }

    /**
     * Waits for the browser's own error page after a load that failed. The browser moves there a moment after the
     * failure is reported, and that navigation would interrupt the agent's next one: a wrong port, then the right
     * one, would fail twice.
     */
    private static void settleOnErrorPage(Page browser) {
        try {
            browser.waitForURL(url -> url.startsWith("chrome-error:") || url.startsWith("about:neterror"),
                    new Page.WaitForURLOptions().setTimeout(2000));
        } catch (TimeoutError e) {
            LOG.log(Level.FINE, "No error page after a failed load; the browser stays at " + browser.url(), e);
        }
    }

    /** {@code net::ERR_CONNECTION_REFUSED at http://localhost:8081/}, out of Playwright's multi-line dump. */
    private static String playwrightMessage(RuntimeException e) {
        String message = String.valueOf(e.getMessage());
        Matcher m = PLAYWRIGHT_MESSAGE.matcher(message);
        return m.find() ? m.group(1) : message.lines().findFirst().orElse("");
    }
}
