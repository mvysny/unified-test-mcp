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
 * What each {@link ComponentTypes.Holds} holds: a {@code VALUE} field a text, its {@code value} property or, for a
 * DatePicker or a TimePicker, what its {@code <input>} shows, {@code "31.1.2000"}; a {@code DATE_AND_TIME} one its two
 * inputs' texts; a {@code CHECKED} one {@code true} or {@code false}; a {@code SELECTED_ITEM} one an item's label, or
 * {@code null}; a {@code SELECTED_ITEMS} one a list of labels; a {@code NUMBER} one a {@link BigDecimal}, or a list of
 * two for a RangeSlider. A field of another kind has none: {@link #read} and {@link #write} don't take it.
 *
 * A write is clicks and typing through the Unified API, each waiting for the server's answer: a Select is opened and
 * its item clicked, a CheckboxGroup's boxes clicked, a Slider's {@code <input>} filled. The Unified API's wrapper
 * where it has one for the type, the generic element's actions where it has none. A date or a time is typed as it
 * is and committed with Enter, unparsed: the field's own parser, in the app's locale, makes of it what it can
 * ({@link #unparsed}).
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
            case VALUE, UNKNOWN -> parses(field) != null ? shown(element) : element.getProperty("value");
            case DATE_AND_TIME -> List.of(shown(half(element, "date-picker")), shown(half(element, "time-picker")));
            case TEXT, PARTS -> throw new IllegalArgumentException(field.type().flowName() + " has no value");
        };
    }

    /**
     * What the field's parser makes of the text it shows: {@code "date"}, {@code "time"}, {@code "date and time"};
     * {@code null} for a field whose value is its text as it is.
     */
    static @Nullable String parses(Field field) {
        return switch (field.tag()) {
            case "vaadin-date-picker" -> "date";
            case "vaadin-time-picker" -> "time";
            case "vaadin-date-time-picker" -> "date and time";
            default -> null;
        };
    }

    /**
     * Whether the field shows a text its parser made nothing of: {@code "dsdds"} typed into a DatePicker, or only a
     * DateTimePicker's date. Its input keeps the text while its value, and the server's, is empty.
     */
    static boolean unparsed(Field field) {
        if (parses(field) == null) {
            return false;
        }
        String value = field.element().getProperty("value");
        Object shown = read(field);
        boolean typed = shown instanceof List<?> halves ? halves.stream().anyMatch(h -> !((String) h).isEmpty())
                : !((String) shown).isEmpty();
        return typed && (value == null || value.isEmpty());
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
     * @param value in the notation {@link #read} answers in, checked to fit the field: a number field's text a number,
     *              a label among the {@link #items} unless a ComboBox allows custom values; a {@code SELECTED_ITEM}
     *              field other than a ComboBox never takes {@code null}, no user can clear one
     */
    static void write(Field field, @Nullable Object value) {
        UnifiedElement element = field.element();
        switch (field.holds()) {
            case VALUE, UNKNOWN -> writeText(field, (String) value);
            case DATE_AND_TIME -> {
                @SuppressWarnings("unchecked") List<String> halves = (List<String>) value;
                if (halves.stream().allMatch(String::isEmpty)) {
                    field.refs().resolve(field.page(), field.ref(), UnifiedDateTimePicker.class).clear();
                } else {
                    type(half(element, "date-picker"), halves.get(0));
                    type(half(element, "time-picker"), halves.get(1));
                }
            }
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
                        } else if (box.getItems().contains(label)) {
                            box.selectItem(label);
                        } else {
                            type(element, label); // a custom value: the app's listener decides what it becomes
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
                    picker.typeText(text);
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
            case "vaadin-time-picker" -> type(field.element(), text);
            // no wrapper: the generic element types into the field's <input>
            default -> field.element().setValueAsUser(text);
        }
    }

    /**
     * Types into the field's {@code <input>} and presses Enter: a TimePicker, a DateTimePicker's half, a ComboBox's
     * custom value are taken on Enter or blur, not on change. No Unified API wrapper types into these.
     */
    private static void type(UnifiedElement field, String text) {
        UnifiedElement input = field.findChildren("input").get(0);
        input.setValueAsUser(text);
        input.pressKey("Enter");
    }

    /** The text the field's {@code <input>} shows. */
    private static String shown(UnifiedElement field) {
        String text = field.findChildren("input").get(0).getProperty("value");
        return text == null ? "" : text;
    }

    /** A DateTimePicker's DatePicker or TimePicker, by its slot: {@code "date-picker"}, {@code "time-picker"}. */
    private static UnifiedElement half(UnifiedElement picker, String slot) {
        return picker.children().stream().filter(child -> slot.equals(child.getAttribute("slot"))).findFirst()
                .orElseThrow(() -> new IllegalStateException("a DateTimePicker without its " + slot));
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
