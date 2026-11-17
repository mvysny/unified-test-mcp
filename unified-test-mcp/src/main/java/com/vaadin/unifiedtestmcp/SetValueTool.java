package com.vaadin.unifiedtestmcp;

import com.github.mvysny.tinymcpserver.InputSchemaBuilder;
import com.github.mvysny.tinymcpserver.MCPErrorResponseException;
import com.github.mvysny.tinymcpserver.MCPProtocol;
import com.github.mvysny.tinymcpserver.ToolDescriptor;
import com.github.mvysny.tinymcpserver.ToolFunction;
import com.github.mvysny.tinymcpserver.ToolRequest;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * {@code set_value(ref, value)}: changes a field as a user would, then answers with what it holds once the server has
 * handled the change, and the new snapshot.
 *
 * <pre>
 * set_value(ref: 1, value: "Bob")        → Set ref=1 to "Bob"
 *                                          (the snapshot)
 * set_value(ref: 2, value: "admin")      → Set ref=2 to "admin"; it holds "alice" now, without showing invalid
 * set_value(ref: 3, value: 130)          → Set ref=3 to "130"; it holds it but shows invalid "At most 120"
 * set_value(ref: 4, value: "dsdds")      → Set ref=4 to "dsdds"; it can't read that as a date, so holds none, and
 *                                          shows invalid
 * set_value(ref: 5, value: "Norway")     → error: ref 5 is a Select without the item "Norway". Its items: …
 * </pre>
 *
 * {@code value} is in {@code get_value}'s notation, {@link Json}: what the user sees, a date in the app's format
 * (D_values_as_shown). Refused before anything is touched: a component that isn't a field, a disabled or read-only
 * one, a value of the wrong kind, a label the field doesn't offer. What the app does with the change is no refusal,
 * only a note on the first line (D_set_value_reads_back). The note says what the field holds, never why: a listener
 * putting the old value back, a {@code maxLength} and a parser's normalising look the same from the browser, so "the
 * app reverted it" would be a guess.
 */
final class SetValueTool implements ToolFunction {
    static final ToolDescriptor DESCRIPTOR = descriptor();

    private static ToolDescriptor descriptor() {
        MCPProtocol.InputSchema schema = new InputSchemaBuilder()
                .requiredInteger("ref", SnapshotTool.REF_DESCRIPTION)
                .optionalString("value", "The new value, passed as itself, never as JSON-encoded text: a string, "
                        + "\"value\": \"Alice\" (not \"\\\"Alice\\\"\"); a number, true or false; an array of strings. "
                        + "Leave it out to empty the field, as null does (not the string \"null\").")
                .build();
        // any JSON type: InputSchemaBuilder has no such type, and a property schema without one takes them all
        schema.getProperties().get("value").setType(null);
        return new ToolDescriptor("set_value",
                "Change a field's value, as a user would: types into a TextField, picks an item of a ComboBox or a "
                        + "Select, ticks a Checkbox. The value is what get_value answers, passed as itself rather than "
                        + "as JSON text: a text, \"value\": \"Alice\" (\"\" empties it); a number field's 42; a "
                        + "Checkbox's or a Switch's true or false. A DatePicker's or a TimePicker's text as the user "
                        + "types it, in the app's format as get_value shows it, \"16.11.2026\"; a DateTimePicker's "
                        + "date and time as two texts, [\"16.11.2026\",\"10.00\"]. Typed as it is, then Enter: text "
                        + "the field can't read, \"dsdds\", tests its invalid input. An item by the label the "
                        + "user sees, \"Czechia\"; leave value out for none (a ComboBox only: no user can clear a "
                        + "Select, a RadioButtonGroup or a ListBox); a ComboBox that allows custom values also takes "
                        + "a text none of its items has. The whole selection of a MultiSelectComboBox, a "
                        + "CheckboxGroup or a MultiSelectListBox as an array of labels, [\"Finland\",\"Sweden\"]; [] "
                        + "for none. A Slider's number, 30, within its min and max; a RangeSlider's [8,17]. A "
                        + "CustomField has none of its own: set the fields inside it. Refuses a disabled or read-only "
                        + "field, a value of the wrong kind, and a label the field doesn't offer (it lists those it "
                        + "does). Waits until the server has handled the change, then returns the new snapshot. The "
                        + "app may refuse or change the value: the first line then says what the field holds, and "
                        + "the error message when the field shows itself invalid.",
                schema);
    }

    /** How many of a field's items a refusal lists: a ComboBox may have thousands. */
    private static final int ITEMS_SHOWN = 20;

    @Override
    public MCPProtocol.Content call(ToolRequest request) {
        int ref = request.arguments().getInt("ref");
        // optional, as TinyMCPServer drops a null argument as missing: null clears the field
        Object value = request.arguments().raw().get("value");
        return MCPProtocol.Content.text(ToolCall.get((page, refs) -> {
            FieldValue.Field field = FieldValue.Field.resolve(page, refs, ref);
            String what = "ref " + ref + " is a " + field.type().flowName();
            switch (field.holds()) {
                case TEXT -> throw new MCPErrorResponseException(what + ": it has no value to set. "
                        + (field.type().actions().contains("click") ? "Call click to press it." : "It only shows a text."));
                case PARTS -> throw new MCPErrorResponseException(what + ": its value is in the fields inside it. "
                        + "Call set_value on each; the snapshot shows them under it.");
                case UNKNOWN -> {
                    if (FieldValue.read(field) == null) {
                        throw new MCPErrorResponseException(what + ": it has no value to set.");
                    }
                }
                default -> {}
            }
            if (!field.element().isEnabled()) {
                throw new MCPErrorResponseException("ref " + ref + " is a disabled " + field.type().flowName()
                        + ": a user can't change it.");
            }
            if (Boolean.TRUE.equals(field.element().getPropertyBoolean("readonly"))) {
                throw new MCPErrorResponseException("ref " + ref + " is a read-only " + field.type().flowName()
                        + ": a user can't change it.");
            }
            Object wanted = wanted(field, value, what);

            FieldValue.write(field, wanted);

            String done = "Set ref=" + ref + " to " + Json.of(wanted);
            FieldValue.Field after;
            try {
                after = FieldValue.Field.resolve(page, refs, ref);
            } catch (MCPErrorResponseException gone) {
                return done + "; it is gone from the page now\n\n" + Snapshot.take(page, refs);
            }
            Object holds = FieldValue.read(after);
            boolean kept = same(field.holds(), wanted, holds);
            String invalid = null;
            if (Boolean.TRUE.equals(after.element().getPropertyBoolean("invalid"))) {
                String message = after.element().getProperty("errorMessage");
                invalid = message == null || message.isBlank() ? "shows invalid" : "shows invalid " + Json.of(message);
            }
            String outcome;
            if (FieldValue.unparsed(after)) {
                // the input keeps the text, so "it holds it" would be wrong: the value under it is empty
                outcome = done + (kept ? "" : "; it shows " + Json.of(holds)) + "; it can't read that as a "
                        + FieldValue.parses(after) + ", so holds none"
                        + (invalid == null ? ", without showing invalid" : ", and " + invalid);
            } else if (invalid != null) {
                // the field holding the value is news too: "shows invalid" alone reads as the value refused
                outcome = kept ? done + "; it holds it but " + invalid
                        : done + "; it holds " + Json.of(holds) + " now and " + invalid;
            } else {
                outcome = kept ? done : done + "; it holds " + Json.of(holds) + " now, without showing invalid";
            }
            return outcome + "\n\n" + Snapshot.take(page, refs);
        }));
    }

    /**
     * {@code value} as {@link FieldValue#write} takes it for this field.
     *
     * @param what {@code "ref 5 is a Select"}, the start of each refusal
     * @throws MCPErrorResponseException if the field can't take it, saying what it takes
     */
    private static @Nullable Object wanted(FieldValue.Field field, @Nullable Object value, String what) {
        return switch (field.holds()) {
            case VALUE, UNKNOWN -> {
                boolean decimal = field.tag().equals("vaadin-big-decimal-field");
                String text = value == null ? "" : value instanceof Number n ? plain(n) : value instanceof String s
                        ? s : refuse(what, "a text, \"Alice\"", value);
                if (!text.isEmpty() && (decimal || field.tag().equals("vaadin-integer-field")
                        || field.tag().equals("vaadin-number-field"))) {
                    try {
                        // a BigDecimalField shows the locale's separator, "9,90"
                        new BigDecimal(text.strip().replace(',', '.'));
                    } catch (NumberFormatException e) {
                        refuse(what, "a number, 42", value);
                    }
                }
                if (decimal && value instanceof Number) {
                    // typed as the user would, with the separator the field shows: Flow sets it from the locale
                    String separator = field.element().getProperty("_decimalSeparator");
                    yield separator == null || separator.isEmpty() ? text : text.replace(".", separator);
                }
                yield text;
            }
            case DATE_AND_TIME -> {
                if (value == null) {
                    yield List.of("", "");
                }
                if (!(value instanceof List<?> list) || list.size() != 2
                        || !list.stream().allMatch(String.class::isInstance)) {
                    yield refuse(what, "its date and its time as two texts, as get_value shows them, "
                            + Json.of(FieldValue.read(field)), value);
                }
                yield list.stream().map(String.class::cast).toList();
            }
            case CHECKED -> value instanceof Boolean b ? b : refuse(what, "true or false", value);
            case SELECTED_ITEM -> {
                if (value == null) {
                    if (!field.tag().equals("vaadin-combo-box") && FieldValue.read(field) != null) {
                        throw new MCPErrorResponseException(what + ": no user can clear it, only pick another of "
                                + "its items: " + quoted(FieldValue.items(field)) + ".");
                    }
                    yield null;
                }
                if (!(value instanceof String label)) {
                    yield refuse(what, "an item's label, \"Czechia\"", value);
                }
                boolean combo = field.tag().equals("vaadin-combo-box");
                if (!(combo && Boolean.TRUE.equals(field.element().getPropertyBoolean("allowCustomValue")))) {
                    offered(field, List.of(label), what, combo && label.equals("null")
                            ? " To empty it, leave value out." : "");
                }
                yield label;
            }
            case SELECTED_ITEMS -> {
                if (value == null) {
                    yield List.of();
                }
                if (!(value instanceof List<?> list) || !list.stream().allMatch(String.class::isInstance)) {
                    yield refuse(what, "an array of labels, [\"Finland\",\"Sweden\"]", value);
                }
                List<String> labels = list.stream().map(String.class::cast).distinct().toList();
                offered(field, labels, what, "");
                yield labels;
            }
            case NUMBER -> numbers(field, value, what);
            case TEXT, PARTS -> throw new IllegalStateException("refused before");
        };
    }

    /** A Slider's number, a RangeSlider's two, each checked to be within its min and max. */
    private static Object numbers(FieldValue.Field field, @Nullable Object value, String what) {
        boolean range = field.tag().equals("vaadin-range-slider");
        List<?> given = range && value instanceof List<?> list ? list : value instanceof Number ? List.of(value) : null;
        if (given == null || given.size() != (range ? 2 : 1) || !given.stream().allMatch(Number.class::isInstance)) {
            return refuse(what, range ? "two numbers, [8,17]" : "a number, 30", value);
        }
        List<BigDecimal> numbers = given.stream().map(n -> new BigDecimal(plain((Number) n))).toList();
        BigDecimal min = new BigDecimal(field.element().getProperty("min"));
        BigDecimal max = new BigDecimal(field.element().getProperty("max"));
        for (BigDecimal number : numbers) {
            if (number.compareTo(min) < 0 || number.compareTo(max) > 0) {
                throw new MCPErrorResponseException(what + " from " + Json.of(min) + " to " + Json.of(max) + ": "
                        + Json.of(number) + " is outside it.");
            }
        }
        if (range && numbers.get(0).compareTo(numbers.get(1)) > 0) {
            throw new MCPErrorResponseException(what + ": its lower number comes first, " + Json.of(numbers.reversed())
                    + ".");
        }
        return range ? numbers : numbers.get(0);
    }

    /**
     * @param hint added to the refusal, {@code ""} for none
     * @throws MCPErrorResponseException naming the labels the field doesn't offer, and those it does
     */
    private static void offered(FieldValue.Field field, List<String> labels, String what, String hint) {
        List<String> items = FieldValue.items(field);
        List<String> missing = labels.stream().filter(label -> !items.contains(label)).toList();
        if (!missing.isEmpty()) {
            throw new MCPErrorResponseException(what + " without the item" + (missing.size() == 1 ? " " : "s ")
                    + quoted(missing) + ". Its items: " + quoted(items) + "." + hint);
        }
    }

    /** @throws MCPErrorResponseException always; typed to sit where a value is expected */
    private static <T> T refuse(String what, String takes, @Nullable Object value) {
        throw new MCPErrorResponseException(what + ": it takes " + takes + "; not " + Json.of(value) + ".");
    }

    /** Whether the field holds what was asked: a selection in any order, a number at any scale. */
    private static boolean same(ComponentTypes.Holds kind, @Nullable Object wanted, @Nullable Object holds) {
        return switch (kind) {
            case SELECTED_ITEMS -> new HashSet<>((List<?>) wanted).equals(new HashSet<>((List<?>) holds));
            case NUMBER -> {
                List<?> w = wanted instanceof List<?> list ? list : List.of(wanted);
                List<?> h = holds instanceof List<?> list ? list : List.of(holds);
                yield w.size() == h.size() && IntStream.range(0, w.size())
                        .allMatch(i -> ((BigDecimal) w.get(i)).compareTo((BigDecimal) h.get(i)) == 0);
            }
            default -> Objects.equals(wanted, holds);
        };
    }

    /** {@code 42}, {@code 72.5}: a JSON number as a user types it, without the {@code .0} a double prints. */
    private static String plain(Number number) {
        return new BigDecimal(number.toString()).stripTrailingZeros().toPlainString();
    }

    /** {@code "Finland", "Czechia"}, the first {@value #ITEMS_SHOWN} and how many more. */
    private static String quoted(List<String> labels) {
        String shown = labels.stream().limit(ITEMS_SHOWN).map(Json::of).collect(Collectors.joining(", "));
        return labels.size() <= ITEMS_SHOWN ? shown : shown + " and " + (labels.size() - ITEMS_SHOWN) + " more";
    }
}
