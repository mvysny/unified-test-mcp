package com.vaadin.unifiedtestmcp;

import com.github.mvysny.tinymcpserver.InputSchemaBuilder;
import com.github.mvysny.tinymcpserver.MCPProtocol;
import com.github.mvysny.tinymcpserver.ToolDescriptor;
import com.github.mvysny.tinymcpserver.ToolFunction;
import com.github.mvysny.tinymcpserver.ToolRequest;

/**
 * {@code get_text(ref)}: a component's text, all of it, its children's included.
 *
 * <pre>
 * get_text(ref: 1)   → Clicked 2 times
 * </pre>
 *
 * A read: the refs stay as they are.
 */
final class GetTextTool implements ToolFunction {
    static final ToolDescriptor DESCRIPTOR = new ToolDescriptor("get_text",
            "The text of a component, its children's included, whitespace collapsed: a Button's text, a "
                    + "Paragraph's. Doesn't change the page, so the refs stay valid.",
            new InputSchemaBuilder().requiredInteger("ref", SnapshotTool.REF_DESCRIPTION).build());

    @Override
    public MCPProtocol.Content call(ToolRequest request) {
        int ref = request.arguments().getInt("ref");
        return MCPProtocol.Content.text(ToolCall.get((page, refs) -> refs.resolve(page, ref).element().getText()));
    }
}
