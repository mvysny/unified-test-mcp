package com.vaadin.unifiedtestmcp;

import com.github.mvysny.tinymcpserver.InputSchemaBuilder;
import com.github.mvysny.tinymcpserver.MCPProtocol;
import com.github.mvysny.tinymcpserver.ToolDescriptor;
import com.github.mvysny.tinymcpserver.ToolFunction;
import com.github.mvysny.tinymcpserver.ToolRequest;

/**
 * {@code click(ref)}: clicks a component, then answers with the snapshot taken once the server has handled the
 * click.
 *
 * <pre>
 * click(ref: 3)   → Clicked ref=3
 *
 *                   URL: http://localhost:8080/form
 *                   - Span "Saved" [ref=8]
 *                   …
 * </pre>
 *
 * Any component takes a click; the snapshot lists {@code click} among the actions only of those made for it.
 */
final class ClickTool implements ToolFunction {
    static final ToolDescriptor DESCRIPTOR = new ToolDescriptor("click",
            "Click a component, as a user would. Waits until it is visible and enabled, clicks it, waits until the "
                    + "server has handled the click, then returns the new snapshot of the page.",
            new InputSchemaBuilder().requiredInteger("ref", SnapshotTool.REF_DESCRIPTION).build());

    @Override
    public MCPProtocol.Content call(ToolRequest request) {
        int ref = request.arguments().getInt("ref");
        String snapshot = ToolCall.get((page, refs) -> {
            refs.resolve(page, ref).element().click();
            return Snapshot.take(page, refs);
        });
        return MCPProtocol.Content.text("Clicked ref=" + ref + "\n\n" + snapshot);
    }
}
