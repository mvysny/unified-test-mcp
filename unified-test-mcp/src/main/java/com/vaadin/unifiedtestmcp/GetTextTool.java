package com.vaadin.unifiedtestmcp;

import com.github.mvysny.tinymcpserver.InputSchemaBuilder;
import com.github.mvysny.tinymcpserver.MCPErrorResponseException;
import com.github.mvysny.tinymcpserver.MCPProtocol;
import com.github.mvysny.tinymcpserver.ToolDescriptor;
import com.github.mvysny.tinymcpserver.ToolFunction;
import com.github.mvysny.tinymcpserver.ToolRequest;
import com.vaadin.testing.unified.UnifiedElement;

/**
 * {@code get_text(ref)}: a component's text, all of it, its children's included.
 *
 * <pre>
 * get_text(ref: 1)   → Clicked 2 times
 * get_text(ref: 5)   → (no text)                (an icon-only Button)
 * get_text(ref: 2)   → error: ref 2 is a TextField: it has a value, not a text. Call get_value; …
 * </pre>
 *
 * A field or a Checkbox is refused, pointing at {@link GetValueTool}: its DOM text is its label, which an agent
 * would take for what the field holds (D_text_or_value). No text answers {@value #NO_TEXT}, not nothing: an MCP
 * client shows an empty result as "no output", which an agent can't tell from a call that failed quietly. A read:
 * the refs stay as they are.
 */
final class GetTextTool implements ToolFunction {
    static final ToolDescriptor DESCRIPTOR = new ToolDescriptor("get_text",
            "The text of a component, its children's included, whitespace collapsed: a Button's text, a "
                    + "Paragraph's; \"(no text)\" for one without, as an icon-only Button. A field or a Checkbox has "
                    + "a value instead: get_value. Doesn't change the page, so the refs stay valid.",
            new InputSchemaBuilder().requiredInteger("ref", SnapshotTool.REF_DESCRIPTION).build());

    static final String NO_TEXT = "(no text)";

    @Override
    public MCPProtocol.Content call(ToolRequest request) {
        int ref = request.arguments().getInt("ref");
        return MCPProtocol.Content.text(ToolCall.get((page, refs) -> {
            UnifiedElement element = refs.resolve(page, ref).element();
            ComponentTypes.Type type = ComponentTypes.of(element.getTag());
            if (!type.holds().hasText()) {
                throw new MCPErrorResponseException("ref " + ref + " is a " + type.flowName()
                        + ": it has a value, not a text. Call get_value; its label is its name in the snapshot.");
            }
            String text = element.getText();
            return text.isEmpty() ? NO_TEXT : text;
        }));
    }
}
