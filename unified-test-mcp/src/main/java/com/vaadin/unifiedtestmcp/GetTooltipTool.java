package com.vaadin.unifiedtestmcp;

import com.github.mvysny.tinymcpserver.InputSchemaBuilder;
import com.github.mvysny.tinymcpserver.MCPProtocol;
import com.github.mvysny.tinymcpserver.ToolDescriptor;
import com.github.mvysny.tinymcpserver.ToolFunction;
import com.github.mvysny.tinymcpserver.ToolRequest;
import com.vaadin.testing.unified.UnifiedElement;

/**
 * {@code get_tooltip(ref)}: the text a component shows on hover, uncut, where the snapshot's {@code tooltip=} is cut.
 *
 * <pre>
 * get_tooltip(ref: 5)   → Close without saving
 * get_tooltip(ref: 4)   → (no tooltip)
 * </pre>
 *
 * The {@code text} property of the component's {@code <vaadin-tooltip>} child, which Flow's {@code setTooltipText}
 * puts there (R_vaadin_hints); read without hovering. A read: the refs stay as they are.
 */
final class GetTooltipTool implements ToolFunction {
    static final ToolDescriptor DESCRIPTOR = new ToolDescriptor("get_tooltip",
            "The tooltip of a component, uncut: the text it shows on hover, \"Close without saving\", read without "
                    + "hovering. The snapshot shows it as tooltip=, cut at 120 characters with …; \"(no tooltip)\" "
                    + "when there is none. Doesn't change the page, so the refs stay valid.",
            new InputSchemaBuilder().requiredInteger("ref", SnapshotTool.REF_DESCRIPTION).build());

    static final String NO_TOOLTIP = "(no tooltip)";

    @Override
    public MCPProtocol.Content call(ToolRequest request) {
        int ref = request.arguments().getInt("ref");
        return MCPProtocol.Content.text(ToolCall.get((page, refs) -> {
            UnifiedElement element = refs.resolve(page, ref).element();
            return element.findChildren("vaadin-tooltip").stream()
                    .map(tooltip -> tooltip.getProperty("text"))
                    .filter(text -> text != null && !text.isBlank())
                    .findFirst()
                    .orElse(NO_TOOLTIP);
        }));
    }
}
