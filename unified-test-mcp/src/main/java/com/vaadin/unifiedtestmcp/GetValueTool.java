package com.vaadin.unifiedtestmcp;

import com.github.mvysny.tinymcpserver.InputSchemaBuilder;
import com.github.mvysny.tinymcpserver.MCPErrorResponseException;
import com.github.mvysny.tinymcpserver.MCPProtocol;
import com.github.mvysny.tinymcpserver.ToolDescriptor;
import com.github.mvysny.tinymcpserver.ToolFunction;
import com.github.mvysny.tinymcpserver.ToolRequest;

/**
 * {@code get_value(ref)}: what a field holds, uncut, or whether a Checkbox is ticked.
 *
 * <pre>
 * get_value(ref: 1)   → "Alice"
 * get_value(ref: 2)   → ""                       (an empty TextField)
 * get_value(ref: 3)   → true
 * get_value(ref: 4)   → "31.1.2000"              (a DatePicker in a fi app: its text, not ISO)
 * get_value(ref: 5)   → "Czechia"                (a ComboBox: its selected item's label)
 * get_value(ref: 6)   → ["Finland","Sweden"]     (a MultiSelectComboBox)
 * get_value(ref: 7)   → null                     (a ComboBox with nothing selected)
 * get_value(ref: 8)   → 30                       (a Slider; a RangeSlider's [8,17])
 * get_value(ref: 4)   → error: ref 4 is a Button: it has no value. Call get_text for its text.
 * </pre>
 *
 * {@link Json}, the notation of the snapshot's {@code value=}. An empty field answers {@code ""}, not nothing: an MCP
 * client shows an empty result as "no output", which an agent can't tell from a call that failed quietly.
 *
 * Read by {@link FieldValue}. An item by its label, what the user sees and picks by: the browser has no other trace of
 * the item, its {@code value} property being Flow's key for it ({@code "3"}). A read: the refs stay as they are.
 */
final class GetValueTool implements ToolFunction {
    static final ToolDescriptor DESCRIPTOR = new ToolDescriptor("get_value",
            "The value of a field as JSON, as the user sees it: a TextField's text, \"Alice\", or \"\" when "
                    + "empty; a DatePicker's or a TimePicker's text in the app's format, \"16.11.2026\"; a "
                    + "DateTimePicker's date and time, [\"16.11.2026\",\"10.00\"]; a Checkbox's true or false. A "
                    + "ComboBox's, a "
                    + "Select's or a RadioButtonGroup's selected item by the label the user sees, \"Czechia\", or "
                    + "null when none; a MultiSelectComboBox's, a CheckboxGroup's or a MultiSelectListBox's as an "
                    + "array of labels. A Slider's number, 30; a RangeSlider's [8,17]. A CustomField has none of its "
                    + "own: read the fields inside it. Unlike the snapshot's value=, "
                    + "never cut short. A Button or a Span has a text instead: get_text. Doesn't change the page, so "
                    + "the refs stay valid.",
            new InputSchemaBuilder().requiredInteger("ref", SnapshotTool.REF_DESCRIPTION).build());

    @Override
    public MCPProtocol.Content call(ToolRequest request) {
        int ref = request.arguments().getInt("ref");
        return MCPProtocol.Content.text(ToolCall.get((page, refs) -> {
            FieldValue.Field field = FieldValue.Field.resolve(page, refs, ref);
            ComponentTypes.Type type = field.type();
            return switch (type.holds()) {
                case TEXT -> throw noValue(ref, type);
                case PARTS -> throw new MCPErrorResponseException("ref " + ref + " is a " + type.flowName()
                        + ": its value is in the fields inside it. Call get_value on each; the snapshot shows them"
                        + " under it.");
                case VALUE, UNKNOWN -> {
                    Object value = FieldValue.read(field);
                    if (value == null) {
                        throw noValue(ref, type);
                    }
                    yield Json.of(value);
                }
                default -> Json.of(FieldValue.read(field));
            };
        }));
    }

    private static MCPErrorResponseException noValue(int ref, ComponentTypes.Type type) {
        return new MCPErrorResponseException("ref " + ref + " is a " + type.flowName()
                + ": it has no value. Call get_text for its text.");
    }
}
