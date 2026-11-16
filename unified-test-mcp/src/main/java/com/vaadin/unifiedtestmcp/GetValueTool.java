package com.vaadin.unifiedtestmcp;

import com.github.mvysny.tinymcpserver.InputSchemaBuilder;
import com.github.mvysny.tinymcpserver.MCPErrorResponseException;
import com.github.mvysny.tinymcpserver.MCPProtocol;
import com.github.mvysny.tinymcpserver.ToolDescriptor;
import com.github.mvysny.tinymcpserver.ToolFunction;
import com.github.mvysny.tinymcpserver.ToolRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vaadin.testing.unified.UnifiedElement;
import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedComboBox;
import com.vaadin.testing.unified.component.UnifiedMultiSelectComboBox;
import com.vaadin.testing.unified.component.UnifiedRadioButtonGroup;

import java.util.List;

/**
 * {@code get_value(ref)}: what a field holds, uncut, or whether a Checkbox is ticked.
 *
 * <pre>
 * get_value(ref: 1)   → "Alice"
 * get_value(ref: 2)   → ""                       (an empty TextField)
 * get_value(ref: 3)   → true
 * get_value(ref: 5)   → "Czechia"                (a ComboBox: its selected item's label)
 * get_value(ref: 6)   → ["Finland","Sweden"]     (a MultiSelectComboBox)
 * get_value(ref: 7)   → null                     (a ComboBox with nothing selected)
 * get_value(ref: 4)   → error: ref 4 is a Button: it has no value. Call get_text for its text.
 * </pre>
 *
 * JSON, so an empty field answers {@code ""}, not nothing: an MCP client shows an empty result as "no output",
 * which an agent can't tell from a call that failed quietly.
 *
 * A field's {@code value} property, as the Unified API's {@code UnifiedTextField.getValue()} reads it; a Checkbox's
 * {@code checked}, as {@code UnifiedCheckbox.isChecked()}. An item, as {@code UnifiedComboBox.getSelectedItem()}
 * reads it: its label, what the user sees and picks by; the browser has no other trace of the item, its
 * {@code value} property being Flow's key for it ({@code "3"}). A read: the refs stay as they are.
 */
final class GetValueTool implements ToolFunction {
    static final ToolDescriptor DESCRIPTOR = new ToolDescriptor("get_value",
            "The value of a field as JSON, as the field holds it: a TextField's text, \"Alice\", or \"\" when "
                    + "empty; a DatePicker's date, \"2026-11-16\"; a Checkbox's true or false. A ComboBox's, a "
                    + "Select's or a RadioButtonGroup's selected item by the label the user sees, \"Czechia\", or "
                    + "null when none; a MultiSelectComboBox's as an array of labels. Unlike the snapshot's value=, "
                    + "never cut short. A Button or a Span has a text instead: get_text. Doesn't change the page, so "
                    + "the refs stay valid.",
            new InputSchemaBuilder().requiredInteger("ref", SnapshotTool.REF_DESCRIPTION).build());

    /** Without TinyMCPServer's HTML escaping: {@code "Tom & Jerry"}, not {@code "Tom \u0026 Jerry"}. */
    private static final Gson JSON = new GsonBuilder().disableHtmlEscaping().serializeNulls().create();

    @Override
    public MCPProtocol.Content call(ToolRequest request) {
        int ref = request.arguments().getInt("ref");
        return MCPProtocol.Content.text(ToolCall.get((page, refs) -> {
            UnifiedElement element = refs.resolve(page, ref).element();
            String tag = element.getTag();
            ComponentTypes.Type type = ComponentTypes.of(tag);
            Object value = switch (type.holds()) {
                case TEXT -> throw noValue(ref, type);
                case CHECKED -> Boolean.TRUE.equals(element.getPropertyBoolean("checked"));
                case SELECTED_ITEM -> selectedItem(page, refs, ref, tag, element);
                case SELECTED_ITEMS -> refs.resolve(page, ref, UnifiedMultiSelectComboBox.class).getSelectedItems();
                case VALUE, UNKNOWN -> {
                    String property = element.getProperty("value");
                    if (property == null) {
                        throw noValue(ref, type);
                    }
                    yield property;
                }
            };
            return JSON.toJson(value);
        }));
    }

    private static MCPErrorResponseException noValue(int ref, ComponentTypes.Type type) {
        return new MCPErrorResponseException("ref " + ref + " is a " + type.flowName()
                + ": it has no value. Call get_text for its text.");
    }

    /** The selected item's label, {@code null} for none. */
    private static String selectedItem(UnifiedPage page, Refs refs, int ref, String tag, UnifiedElement element) {
        return switch (tag) {
            case "vaadin-combo-box" -> refs.resolve(page, ref, UnifiedComboBox.class).getSelectedItem();
            case "vaadin-radio-group" -> refs.resolve(page, ref, UnifiedRadioButtonGroup.class).getSelectedItem();
            // no Unified API wrapper for a Select: the button shows the selected item, as the user sees it
            default -> {
                List<UnifiedElement> button = element.findChildren("vaadin-select-value-button");
                yield button.isEmpty() ? null : button.get(0).getText();
            }
        };
    }
}
