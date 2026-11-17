package com.vaadin.unifiedtestmcp;

import com.github.mvysny.tinymcpserver.InputSchemaBuilder;
import com.github.mvysny.tinymcpserver.MCPProtocol;
import com.github.mvysny.tinymcpserver.ToolDescriptor;
import com.github.mvysny.tinymcpserver.ToolFunction;
import com.github.mvysny.tinymcpserver.ToolRequest;
import com.vaadin.testing.unified.UnifiedElement;

/**
 * {@code get_helper(ref)}: the helper text below a field, uncut, where the snapshot's {@code helper=} is cut.
 *
 * <pre>
 * get_helper(ref: 3)   → One email a month: what changed in the app, … from the link at the bottom of every email.
 * get_helper(ref: 4)   → (no helper text)
 * </pre>
 *
 * The text of the component's {@code slot="helper"} child, where it shows its {@code helperText} (R_vaadin_hints).
 * A read: the refs stay as they are.
 */
final class GetHelperTool implements ToolFunction {
    static final ToolDescriptor DESCRIPTOR = new ToolDescriptor("get_helper",
            "The helper text below a field, uncut: what it accepts, \"8 characters or more\". The snapshot shows it "
                    + "as helper=, cut at 120 characters with …; \"(no helper text)\" when there is none. Doesn't "
                    + "change the page, so the refs stay valid.",
            new InputSchemaBuilder().requiredInteger("ref", SnapshotTool.REF_DESCRIPTION).build());

    static final String NO_HELPER = "(no helper text)";

    @Override
    public MCPProtocol.Content call(ToolRequest request) {
        int ref = request.arguments().getInt("ref");
        return MCPProtocol.Content.text(ToolCall.get((page, refs) -> {
            UnifiedElement element = refs.resolve(page, ref).element();
            return element.findChildren("*").stream()
                    .filter(child -> "helper".equals(child.getAttribute("slot")))
                    .map(UnifiedElement::getText)
                    .filter(text -> !text.isBlank())
                    .findFirst()
                    .orElse(NO_HELPER);
        }));
    }
}
