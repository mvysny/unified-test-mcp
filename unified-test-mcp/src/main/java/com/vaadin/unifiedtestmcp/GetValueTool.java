package com.vaadin.unifiedtestmcp;

import com.github.mvysny.tinymcpserver.InputSchemaBuilder;
import com.github.mvysny.tinymcpserver.MCPErrorResponseException;
import com.github.mvysny.tinymcpserver.MCPProtocol;
import com.github.mvysny.tinymcpserver.ToolDescriptor;
import com.github.mvysny.tinymcpserver.ToolFunction;
import com.github.mvysny.tinymcpserver.ToolRequest;
import com.vaadin.testing.unified.UnifiedElement;

/**
 * {@code get_value(ref)}: what a field holds, uncut, or whether a Checkbox is ticked.
 *
 * <pre>
 * get_value(ref: 1)   → Alice
 * get_value(ref: 3)   → true
 * get_value(ref: 4)   → error: ref 4 is a Button: it has no value. Call get_text for its text.
 * </pre>
 *
 * The web component's {@code value} property, as the Unified API's {@code UnifiedTextField.getValue()} reads it; a
 * Checkbox's {@code checked}, as {@code UnifiedCheckbox.isChecked()}. A read: the refs stay as they are.
 */
final class GetValueTool implements ToolFunction {
    static final ToolDescriptor DESCRIPTOR = new ToolDescriptor("get_value",
            "The value of a field, as the field holds it: a TextField's text, a DatePicker's date as 2026-11-16, "
                    + "a Checkbox's true or false; empty for an empty field. Unlike the snapshot's value=, never "
                    + "cut short. A Button or a Span has a text instead: get_text. Doesn't change the page, so the "
                    + "refs stay valid.",
            new InputSchemaBuilder().requiredInteger("ref", SnapshotTool.REF_DESCRIPTION).build());

    @Override
    public MCPProtocol.Content call(ToolRequest request) {
        int ref = request.arguments().getInt("ref");
        return MCPProtocol.Content.text(ToolCall.get((page, refs) -> {
            UnifiedElement element = refs.resolve(page, ref).element();
            ComponentTypes.Type type = ComponentTypes.of(element.getTag());
            if (type.holds() == ComponentTypes.Holds.CHECKED) {
                return String.valueOf(Boolean.TRUE.equals(element.getPropertyBoolean("checked")));
            }
            String value = type.holds().hasValue() ? element.getProperty("value") : null;
            if (value == null) {
                throw new MCPErrorResponseException("ref " + ref + " is a " + type.flowName()
                        + ": it has no value. Call get_text for its text.");
            }
            return value;
        }));
    }
}
