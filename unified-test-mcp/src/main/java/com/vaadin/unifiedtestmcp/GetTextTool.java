package com.vaadin.unifiedtestmcp;

import com.github.mvysny.tinymcpserver.InputSchemaBuilder;
import com.github.mvysny.tinymcpserver.MCPProtocol;
import com.github.mvysny.tinymcpserver.ToolDescriptor;
import com.github.mvysny.tinymcpserver.ToolFunction;
import com.github.mvysny.tinymcpserver.ToolRequest;

/**
 * {@code get_text(id)}: a Button's text.
 *
 * <pre>
 * get_text(id: "clicker")   → Clicked 2 times
 * </pre>
 */
final class GetTextTool implements ToolFunction {
    static final ToolDescriptor DESCRIPTOR = new ToolDescriptor("get_text",
            "The text of a Vaadin Button, found by its id.",
            new InputSchemaBuilder().requiredString("id", "The Button's id").build());

    @Override
    public MCPProtocol.Content call(ToolRequest request) {
        String id = request.arguments().getString("id");
        return MCPProtocol.Content.text(ToolCall.get(page -> ComponentLookup.button(page, id).getText()));
    }
}
