package com.vaadin.unifiedtestmcp;

import com.github.mvysny.tinymcpserver.InputSchemaBuilder;
import com.microsoft.playwright.Page;
import com.github.mvysny.tinymcpserver.MCPProtocol;
import com.github.mvysny.tinymcpserver.ToolDescriptor;
import com.github.mvysny.tinymcpserver.ToolFunction;
import com.github.mvysny.tinymcpserver.ToolRequest;

/**
 * {@code navigate(path)}: loads the view at {@code path} relative to the app's URL, then answers with the snapshot
 * of the page it landed on.
 *
 * <pre>
 * navigate(path: "form")   → Navigated to /form
 *
 *                            Path: /form
 *                            - TextField "Name" [ref=1, id=name, value="Alice"]
 *                            …
 * </pre>
 *
 * A full page load, so the view starts afresh. The snapshot's path is where the browser is, which tells a reroute,
 * to a login view say, from the path asked for.
 *
 * <p>A path with no view is not a tool error: the page did load, and its snapshot is valid. Flow's own not-found
 * view is told apart, {@code No view at /orders: …}; an app's own not-found view only shows in the snapshot.
 *
 * <h2>Implementation detail</h2>
 * Flow's not-found view is found by its text, the same in dev and production mode. Not by HTTP status: the page
 * and the request that resolves the route both answer 200 for any path.
 */
final class NavigateTool implements ToolFunction {
    static final ToolDescriptor DESCRIPTOR = new ToolDescriptor("navigate",
            "Open a view of the Vaadin app by its path relative to the app's URL, as a user typing it into the "
                    + "address bar would: \"orders\", \"orders/3\", \"orders?status=open\"; \"\" is the start view. "
                    + "A full page load: the view starts afresh. Waits until the server is idle, then returns the "
                    + "snapshot of the page, whose Path line says where the browser landed. A path with no view is "
                    + "not an error: the result starts \"No view at\" when the app shows Vaadin's own not-found "
                    + "page; an app's own not-found view shows only in the snapshot.",
            new InputSchemaBuilder().requiredString("path", "The view's path, without the app's URL").build());

    /** Whether the page shows Flow's {@code RouteNotFoundError}: its templates open with this text. */
    private static final String NOT_FOUND_SCRIPT = """
            () => [...document.querySelectorAll('div')].some(d =>
                d.firstChild && d.firstChild.nodeType === Node.TEXT_NODE
                && d.firstChild.textContent.trim().startsWith("Could not navigate to '"))""";

    @Override
    public MCPProtocol.Content call(ToolRequest request) {
        String path = request.arguments().getString("path");
        String shown = "/" + (path.startsWith("/") ? path.substring(1) : path);
        return MCPProtocol.Content.text(ToolCall.get((page, refs) -> {
            page.navigate(path);
            boolean notFound = (Boolean) page.unwrap(Page.class).evaluate(NOT_FOUND_SCRIPT);
            return (notFound ? "No view at " + shown + ": the app shows Vaadin's \"Could not navigate\" page"
                    : "Navigated to " + shown) + "\n\n" + Snapshot.take(page, refs);
        }));
    }
}
