package com.vaadin.unifiedtestmcp;

import com.vaadin.testing.unified.UnifiedElement;
import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedBigDecimalField;
import com.vaadin.testing.unified.component.UnifiedCheckbox;
import com.vaadin.testing.unified.component.UnifiedComboBox;
import com.vaadin.testing.unified.component.UnifiedDatePicker;
import com.vaadin.testing.unified.component.UnifiedDateTimePicker;
import com.vaadin.testing.unified.component.UnifiedIntegerField;
import com.vaadin.testing.unified.component.UnifiedMultiSelectComboBox;
import com.vaadin.testing.unified.component.UnifiedNumberField;
import com.vaadin.testing.unified.component.UnifiedRadioButtonGroup;
import com.vaadin.testing.unified.component.UnifiedTextArea;
import com.vaadin.testing.unified.component.UnifiedTextField;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A field's value in {@link Json}'s notation, read from the browser and written to it the way a user changes it:
 *
 * <pre>{@code
 * FieldValue.read(field)                  // => "Czechia"
 * FieldValue.write(field, "Sweden");      // opens the Select, clicks "Sweden", waits for the server
 * FieldValue.read(field)                  // => "Sweden", unless the app put something else back
 * }</pre>
 *
 * What each {@link ComponentTypes.Holds} holds: a {@code VALUE} field its {@code value} property, a text; a
 * {@code CHECKED} one {@code true} or {@code false}; a {@code SELECTED_ITEM} one an item's label, or {@code null};
 * a {@code SELECTED_ITEMS} one a list of labels; a {@code NUMBER} one a {@link BigDecimal}, or a list of two for a
 * RangeSlider. A field of another kind has none: {@link #read} and {@link #write} don't take it.
 *
 * A write is clicks and typing through the Unified API, each waiting for the server's answer: a Select is opened and
 * its item clicked, a CheckboxGroup's boxes clicked, a Slider's {@code <input>} filled. The Unified API's wrapper
 * where it has one for the type, the generic element's actions where it has none.
 */
final class FieldValue {
    private FieldValue() {}

    /** A component found by its ref, with what the snapshot knows about its type. */
    record Field(UnifiedPage page, Refs refs, int ref, UnifiedElement element, ComponentTypes.Type type) {
        static Field resolve(UnifiedPage page, Refs refs, int ref) {
            UnifiedElement element = refs.resolve(page, ref).element();
            return new Field(page, refs, ref, element, ComponentTypes.of(element));
        }

        String tag() {
            return element.getTag();
        }

        ComponentTypes.Holds holds() {
            return type.holds();
        }
    }

    /**
     * @return {@code null} for nothing selected; for a {@code VALUE} or {@code UNKNOWN} field, {@code null} when it
     *         has no {@code value} property, so isn't a field
     */
    static @Nullable Object read(Field field) {
        UnifiedElement element = field.element();
        return switch (field.holds()) {
            case CHECKED -> Boolean.TRUE.equals(element.getPropertyBoolean("checked"));
            case SELECTED_ITEM -> selectedItem(field);
            case SELECTED_ITEMS -> selectedItems(field);
            case NUMBER -> {
                List<BigDecimal> numbers = element.findChildren("input").stream()
                        .map(input -> new BigDecimal(input.getProperty("value"))).toList();
                yield numbers.size() == 1 ? numbers.get(0) : numbers;
            }
            case VALUE, UNKNOWN -> element.getProperty("value");
            case TEXT, PARTS -> throw new IllegalArgumentException(field.type().flowName() + " has no value");
        };
    }

    /** The labels of the items a {@code SELECTED_ITEM} or {@code SELECTED_ITEMS} field offers, in its order. */
    static List<String> items(Field field) {
        return switch (field.tag()) {
            case "vaadin-combo-box" -> field.refs().resolve(field.page(), field.ref(), UnifiedComboBox.class).getItems();
            case "vaadin-multi-select-combo-box" ->
                    field.refs().resolve(field.page(), field.ref(), UnifiedMultiSelectComboBox.class).getItems();
            case "vaadin-radio-group" ->
                    field.refs().resolve(field.page(), field.ref(), UnifiedRadioButtonGroup.class).getItems();
            default -> choices(field).stream().map(FieldValue::label).toList();
        };
    }

    /**
     * Changes the field to {@code value}, as a user would, and waits for the server's answer.
     *
     * @param value in the notation {@link #read} answers in, checked to fit the field: a {@code VALUE} field's text
     *              parsed as its type takes it, a label among the {@link #items}; a {@code SELECTED_ITEM} field other
     *              than a ComboBox never takes {@code null}, no user can clear one
     */
    static void write(Field field, @Nullable Object value) {
        UnifiedElement element = field.element();
        switch (field.holds()) {
            case VALUE, UNKNOWN -> writeText(field, (String) value);
            case CHECKED -> {
                boolean checked = (Boolean) value;
                if (field.tag().equals("vaadin-checkbox")) {
                    field.refs().resolve(field.page(), field.ref(), UnifiedCheckbox.class).setChecked(checked);
                } else if (!Boolean.valueOf(checked).equals(read(field))) {
                    element.click();
                }
            }
            case SELECTED_ITEM -> {
                if (Objects.equals(value, read(field))) {
                    return;
                }
                String label = (String) value;
                switch (field.tag()) {
                    case "vaadin-combo-box" -> {
                        UnifiedComboBox box = field.refs().resolve(field.page(), field.ref(), UnifiedComboBox.class);
                        if (label == null) {
                            box.clear();
                        } else {
                            box.selectItem(label);
                        }
                    }
                    case "vaadin-radio-group" ->
                            field.refs().resolve(field.page(), field.ref(), UnifiedRadioButtonGroup.class)
                                    .selectItem(label);
                    case "vaadin-select" -> {
                        element.click();
                        choice(field, label).click();
                    }
                    default -> choice(field, label).click();
                }
            }
            case SELECTED_ITEMS -> {
                @SuppressWarnings("unchecked") List<String> wanted = (List<String>) value;
                if (field.tag().equals("vaadin-multi-select-combo-box")) {
                    writeMultiSelectComboBox(field, wanted);
                } else {
                    for (UnifiedElement choice : choices(field)) {
                        if (isChosen(choice) != wanted.contains(label(choice))) {
                            choice.click();
                        }
                    }
                }
            }
            case NUMBER -> writeNumbers(field, value instanceof List<?> list ? list : List.of(value));
            case TEXT, PARTS -> throw new IllegalArgumentException(field.type().flowName() + " has no value");
        }
    }

    private static void writeText(Field field, String text) {
        Refs refs = field.refs();
        UnifiedPage page = field.page();
        int ref = field.ref();
        switch (field.tag()) {
            case "vaadin-date-picker" -> {
                UnifiedDatePicker picker = refs.resolve(page, ref, UnifiedDatePicker.class);
                if (text.isEmpty()) {
                    picker.clear();
                } else {
                    picker.setValue(LocalDate.parse(text));
                }
            }
            case "vaadin-date-time-picker" -> {
                UnifiedDateTimePicker picker = refs.resolve(page, ref, UnifiedDateTimePicker.class);
                if (text.isEmpty()) {
                    picker.clear();
                } else {
                    picker.setValue(LocalDateTime.parse(text));
                }
            }
            case "vaadin-text-field" -> refs.resolve(page, ref, UnifiedTextField.class).element().setValueAsUser(text);
            case "vaadin-text-area" -> refs.resolve(page, ref, UnifiedTextArea.class).element().setValueAsUser(text);
            case "vaadin-integer-field" ->
                    refs.resolve(page, ref, UnifiedIntegerField.class).element().setValueAsUser(text);
            case "vaadin-number-field" ->
                    refs.resolve(page, ref, UnifiedNumberField.class).element().setValueAsUser(text);
            case "vaadin-big-decimal-field" ->
                    refs.resolve(page, ref, UnifiedBigDecimalField.class).element().setValueAsUser(text);
            case "vaadin-time-picker" -> {
                UnifiedElement input = field.element().findChildren("input").get(0);
                input.setValueAsUser(text);
                input.pressKey("Enter"); // it takes what was typed on Enter or blur, not on change
            }
            // no wrapper: the generic element types into the field's <input>
            default -> field.element().setValueAsUser(text);
        }
    }

    private static void writeMultiSelectComboBox(Field field, List<String> wanted) {
        UnifiedMultiSelectComboBox box = field.refs().resolve(field.page(), field.ref(),
                UnifiedMultiSelectComboBox.class);
        if (wanted.isEmpty()) {
            box.clear();
            return;
        }
        List<String> selected = box.getSelectedItems();
        String[] deselect = selected.stream().filter(label -> !wanted.contains(label)).toArray(String[]::new);
        String[] select = wanted.stream().filter(label -> !selected.contains(label)).toArray(String[]::new);
        if (deselect.length > 0) {
            box.deselectItems(deselect);
        }
        if (select.length > 0) {
            box.selectItems(select);
        }
    }

    /** Fills each thumb's {@code <input type=range>}; the upper one first when the lower is to pass it. */
    private static void writeNumbers(Field field, List<?> numbers) {
        List<UnifiedElement> inputs = new ArrayList<>(field.element().findChildren("input"));
        List<Object> values = new ArrayList<>(numbers);
        if (inputs.size() == 2
                && ((BigDecimal) values.get(0)).compareTo(new BigDecimal(inputs.get(1).getProperty("value"))) > 0) {
            inputs = inputs.reversed();
            values = values.reversed();
        }
        for (int i = 0; i < inputs.size(); i++) {
            inputs.get(i).setValueAsUser(((BigDecimal) values.get(i)).toPlainString());
        }
    }

    /** The selected item's label, {@code null} for none. */
    private static @Nullable String selectedItem(Field field) {
        return switch (field.tag()) {
            case "vaadin-combo-box" ->
                    field.refs().resolve(field.page(), field.ref(), UnifiedComboBox.class).getSelectedItem();
            case "vaadin-radio-group" ->
                    field.refs().resolve(field.page(), field.ref(), UnifiedRadioButtonGroup.class).getSelectedItem();
            // by its index: a disabled ListBox's item loses its selected attribute
            case "vaadin-list-box" -> {
                Integer index = field.element().getPropertyInteger("selected");
                List<UnifiedElement> items = choices(field);
                yield index == null || index < 0 || index >= items.size() ? null : label(items.get(index));
            }
            // the button shows the selected item, as the user sees it
            default -> {
                List<UnifiedElement> button = field.element().findChildren("vaadin-select-value-button");
                yield button.isEmpty() || button.get(0).getText().isBlank() ? null : button.get(0).getText().strip();
            }
        };
    }

    /** The selected items' labels, in the order the field lists them. */
    private static List<String> selectedItems(Field field) {
        if (field.tag().equals("vaadin-multi-select-combo-box")) {
            return field.refs().resolve(field.page(), field.ref(), UnifiedMultiSelectComboBox.class)
                    .getSelectedItems();
        }
        return choices(field).stream().filter(FieldValue::isChosen).map(FieldValue::label).toList();
    }

    /**
     * The children a user clicks to choose an item: a CheckboxGroup's boxes, a ListBox's items, a Select's items in
     * its drop-down. No Unified API wrapper for these; the browser has them in the light DOM.
     */
    private static List<UnifiedElement> choices(Field field) {
        UnifiedElement element = field.element();
        return switch (field.tag()) {
            case "vaadin-checkbox-group" -> element.findChildren("vaadin-checkbox");
            case "vaadin-select" -> element.findChildren("vaadin-select-list-box").stream()
                    .flatMap(list -> list.findChildren("vaadin-select-item").stream()).toList();
            default -> element.findChildren("vaadin-item");
        };
    }

    private static UnifiedElement choice(Field field, String label) {
        return choices(field).stream().filter(choice -> label(choice).equals(label)).findFirst().orElseThrow();
    }

    private static String label(UnifiedElement choice) {
        return choice.getText().strip();
    }

    /** A box ticked, an item selected: the browser marks each with an attribute. */
    private static boolean isChosen(UnifiedElement choice) {
        return choice.getAttribute(choice.getTag().equals("vaadin-checkbox") ? "checked" : "selected") != null;
    }
}
