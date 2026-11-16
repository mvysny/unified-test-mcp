package com.vaadin.unifiedtestmcp;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * What the snapshot knows about a component by its tag: the Flow class it shows, which property names it, and the
 * tools that act on it.
 *
 * <pre>{@code
 * ComponentTypes.of("vaadin-button")        // => Button, named by its text, actions: click
 * ComponentTypes.of("vaadin-split-layout")  // => SplitLayout: not listed, its name derived from the tag
 * ComponentTypes.of("my-rating")            // => <my-rating>: a custom element shows its tag
 * }</pre>
 */
final class ComponentTypes {
    private ComponentTypes() {}

    /** Which property is a component's name in a snapshot line. */
    enum NameFrom {
        /** The text of its own, Flow's {@code HasText}: a Button, a Span. */
        TEXT,
        /** Its label, Flow's {@code HasLabel}: the fields, a Checkbox. */
        LABEL,
        /** The label if it has one, else its text: a type this table doesn't list. */
        LABEL_OR_TEXT
    }

    /**
     * @param flowName  the Flow class's simple name, {@code Button}, or {@code <tag>} for an element with none
     * @param showValue whether its {@code value} property is worth a line: a field's is, a Checkbox's ({@code "on"})
     *                  isn't
     * @param actions   the tools that act on it beyond what every component allows; only tools this server has
     */
    record Type(String flowName, NameFrom nameFrom, boolean showValue, List<String> actions) {}

    private static final Map<String, Type> TYPES = Map.ofEntries(
            Map.entry("vaadin-button", new Type("Button", NameFrom.TEXT, false, List.of("click"))),
            Map.entry("vaadin-checkbox", new Type("Checkbox", NameFrom.LABEL, false, List.of())),
            Map.entry("vaadin-text-field", field("TextField")),
            Map.entry("vaadin-text-area", field("TextArea")),
            Map.entry("vaadin-password-field", field("PasswordField")),
            Map.entry("vaadin-email-field", field("EmailField")),
            Map.entry("vaadin-integer-field", field("IntegerField")),
            Map.entry("vaadin-number-field", field("NumberField")),
            Map.entry("vaadin-big-decimal-field", field("BigDecimalField")),
            Map.entry("vaadin-date-picker", field("DatePicker")),
            Map.entry("vaadin-time-picker", field("TimePicker")),
            Map.entry("vaadin-date-time-picker", field("DateTimePicker")),
            Map.entry("vaadin-combo-box", field("ComboBox")),
            Map.entry("vaadin-select", field("Select")),
            Map.entry("div", html("Div")),
            Map.entry("span", html("Span")),
            Map.entry("p", html("Paragraph")),
            Map.entry("h1", html("H1")),
            Map.entry("h2", html("H2")),
            Map.entry("h3", html("H3")),
            Map.entry("h4", html("H4")),
            Map.entry("h5", html("H5")),
            Map.entry("h6", html("H6")),
            Map.entry("a", html("Anchor")),
            Map.entry("label", html("NativeLabel")));

    /** Layouts: without an id, their children take their place in the snapshot. */
    static final Set<String> LAYOUTS = Set.of("vaadin-vertical-layout", "vaadin-horizontal-layout",
            "vaadin-form-layout", "vaadin-form-item", "vaadin-form-row", "vaadin-scroller");

    /** Never in a snapshot, nor anything inside them: decoration, and the dev tools. */
    static final Set<String> SKIPPED = Set.of("vaadin-icon", "svg", "img", "vaadin-tooltip", "script", "style",
            "template", "noscript", "link", "meta", "vaadin-dev-tools", "vaadin-connection-indicator");

    static Type of(String tag) {
        Type known = TYPES.get(tag);
        if (known != null) {
            return known;
        }
        if (tag.startsWith("vaadin-")) {
            return new Type(className(tag.substring("vaadin-".length())), NameFrom.LABEL_OR_TEXT, true, List.of());
        }
        return new Type("<" + tag + ">", NameFrom.LABEL_OR_TEXT, true, List.of());
    }

    private static Type field(String flowName) {
        return new Type(flowName, NameFrom.LABEL, true, List.of());
    }

    private static Type html(String flowName) {
        return new Type(flowName, NameFrom.TEXT, false, List.of());
    }

    /** {@code split-layout} → {@code SplitLayout}. */
    private static String className(String dashed) {
        StringBuilder name = new StringBuilder();
        for (String part : dashed.split("-")) {
            if (!part.isEmpty()) {
                name.append(part.substring(0, 1).toUpperCase(Locale.ROOT)).append(part.substring(1));
            }
        }
        return name.toString();
    }
}
