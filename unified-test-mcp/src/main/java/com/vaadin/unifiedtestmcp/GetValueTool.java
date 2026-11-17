package com.vaadin.unifiedtestmcp;

import com.github.mvysny.tinymcpserver.InputSchemaBuilder;
import com.github.mvysny.tinymcpserver.MCPErrorResponseException;
import com.github.mvysny.tinymcpserver.MCPProtocol;
import com.github.mvysny.tinymcpserver.ToolDescriptor;
import com.github.mvysny.tinymcpserver.ToolFunction;
import com.github.mvysny.tinymcpserver.ToolRequest;
import com.vaadin.testing.unified.UnifiedElement;
import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedComboBox;
import com.vaadin.testing.unified.component.UnifiedMultiSelectComboBox;
import com.vaadin.testing.unified.component.UnifiedRadioButtonGroup;

import java.math.BigDecimal;
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
 * get_value(ref: 8)   → 30                       (a Slider; a RangeSlider's [8,17])
 * get_value(ref: 4)   → error: ref 4 is a Button: it has no value. Call get_text for its text.
 * </pre>
 *
 * {@link Json}, the notation of the snapshot's {@code value=}. An empty field answers {@code ""}, not nothing: an MCP
 * client shows an empty result as "no output", which an agent can't tell from a call that failed quietly.
 *
 * A field's {@code value} property, as the Unified API's {@code UnifiedTextField.getValue()} reads it; a Checkbox's
 * {@code checked}, as {@code UnifiedCheckbox.isChecked()}. An item, as {@code UnifiedComboBox.getSelectedItem()}
 * reads it: its label, what the user sees and picks by; the browser has no other trace of the item, its
 * {@code value} property being Flow's key for it ({@code "3"}). A CheckboxGroup's and a ListBox's items by their
 * text, the checked or selected ones. A Slider's number from its {@code <input>}. A read: the refs stay as they are.
 */
final class GetValueTool implements ToolFunction {
    static final ToolDescriptor DESCRIPTOR = new ToolDescriptor("get_value",
            "The value of a field as JSON, as the field holds it: a TextField's text, \"Alice\", or \"\" when "
                    + "empty; a DatePicker's date, \"2026-11-16\"; a Checkbox's true or false. A ComboBox's, a "
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
            UnifiedElement element = refs.resolve(page, ref).element();
            String tag = element.getTag();
            ComponentTypes.Type type = ComponentTypes.of(element);
            Object value = switch (type.holds()) {
                case TEXT -> throw noValue(ref, type);
                case CHECKED -> Boolean.TRUE.equals(element.getPropertyBoolean("checked"));
                case SELECTED_ITEM -> selectedItem(page, refs, ref, tag, element);
                case SELECTED_ITEMS -> selectedItems(page, refs, ref, tag, element);
                case NUMBER -> {
                    List<BigDecimal> numbers = element.findChildren("input").stream()
                            .map(input -> new BigDecimal(input.getProperty("value"))).toList();
                    yield numbers.size() == 1 ? numbers.get(0) : numbers;
                }
                case PARTS -> throw new MCPErrorResponseException("ref " + ref + " is a " + type.flowName()
                        + ": its value is in the fields inside it. Call get_value on each; the snapshot shows them"
                        + " under it.");
                case VALUE, UNKNOWN -> {
                    String property = element.getProperty("value");
                    if (property == null) {
                        throw noValue(ref, type);
                    }
                    yield property;
                }
            };
            return Json.of(value);
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
            case "vaadin-list-box" -> {
                Integer index = element.getPropertyInteger("selected");
                List<UnifiedElement> items = element.findChildren("vaadin-item");
                yield index == null || index < 0 || index >= items.size() ? null : items.get(index).getText().strip();
            }
            // no Unified API wrapper for a Select: the button shows the selected item, as the user sees it
            default -> {
                List<UnifiedElement> button = element.findChildren("vaadin-select-value-button");
                yield button.isEmpty() ? null : button.get(0).getText();
            }
        };
    }

    /** The selected items' labels, in the order the field lists them. */
    private static List<String> selectedItems(UnifiedPage page, Refs refs, int ref, String tag,
                                              UnifiedElement element) {
        return switch (tag) {
            case "vaadin-multi-select-combo-box" ->
                    refs.resolve(page, ref, UnifiedMultiSelectComboBox.class).getSelectedItems();
            // no Unified API wrapper for these: the browser marks the chosen children, which show their labels
            case "vaadin-checkbox-group" -> selectedItems(element, "vaadin-checkbox", "checked");
            default -> selectedItems(element, "vaadin-item", "selected");
        };
    }

    /** The text of each {@code tag} child that has the {@code marker} attribute. */
    private static List<String> selectedItems(UnifiedElement element, String tag, String marker) {
        return element.findChildren(tag).stream()
                .filter(child -> child.getAttribute(marker) != null)
                .map(child -> child.getText().strip())
                .toList();
    }
}
