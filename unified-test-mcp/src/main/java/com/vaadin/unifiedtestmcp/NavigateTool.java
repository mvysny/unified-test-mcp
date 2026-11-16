package com.vaadin.unifiedtestmcp;

import com.github.mvysny.tinymcpserver.InputSchemaBuilder;
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
 */
final class NavigateTool implements ToolFunction {
    static final ToolDescriptor DESCRIPTOR = new ToolDescriptor("navigate",
            "Open a view of the Vaadin app by its path relative to the app's URL, as a user typing it into the "
                    + "address bar would: \"orders\", \"orders/3\", \"orders?status=open\"; \"\" is the start view. "
                    + "A full page load: the view starts afresh. Waits until the server is idle, then returns the "
                    + "snapshot of the page, whose Path line says where the browser landed.",
            new InputSchemaBuilder().requiredString("path", "The view's path, without the app's URL").build());

    @Override
    public MCPProtocol.Content call(ToolRequest request) {
        String path = request.arguments().getString("path");
        String snapshot = ToolCall.get((page, refs) -> {
            page.navigate(path);
            return Snapshot.take(page, refs);
        });
        return MCPProtocol.Content.text("Navigated to /" + (path.startsWith("/") ? path.substring(1) : path)
                + "\n\n" + snapshot);
    }
}
