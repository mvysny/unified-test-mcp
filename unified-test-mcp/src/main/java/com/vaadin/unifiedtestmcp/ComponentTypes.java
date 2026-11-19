package com.vaadin.unifiedtestmcp;

import com.vaadin.testing.unified.UnifiedElement;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * What the snapshot knows about a component by its tag: the Flow class it shows, which property names it, and the
 * tools that act on it.
 *
 * <pre>{@code
 * ComponentTypes.of("vaadin-button", false)        // => Button, named by its text, actions: click
 * ComponentTypes.of("vaadin-text-field", false)    // => TextField, named by its label, actions: set_value
 * ComponentTypes.of("vaadin-split-layout", false)  // => SplitLayout: not listed, its name derived from the tag
 * ComponentTypes.of("vaadin-list-box", true)       // => MultiSelectListBox: the one tag two Flow classes share
 * ComponentTypes.of("my-rating", false)            // => <my-rating>: a custom element shows its tag
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
        LABEL_OR_TEXT,
        /**
         * Its title, the heading it shows: a Dialog's header, a LoginOverlay's title, a LoginForm's "Log in". A
         * dialog's is its {@code aria-label}, else the text of its title element (D_login_from_i18n).
         */
        TITLE
    }

    /** What a component holds, and so which of {@code get_text} and {@code get_value} reads it. */
    enum Holds {
        /** A text, Flow's {@code HasText}: a Button, a Span. No value. */
        TEXT,
        /**
         * A value, Flow's {@code HasValue}, in its {@code value} property: the fields. No text: a TextField's DOM
         * text is its label. A DatePicker's or a TimePicker's is the text its {@code <input>} shows,
         * {@code "31.1.2000"}: its {@code value} property is ISO, the server's view, not the user's.
         */
        VALUE,
        /**
         * A DateTimePicker's: two texts, its date's and its time's as their {@code <input>}s show them,
         * {@code ["17.11.2026","10.00"]}. Its {@code value} property is ISO.
         */
        DATE_AND_TIME,
        /**
         * A Checkbox's or a Switch's value is whether it's ticked, its {@code checked} property; {@code value} is
         * {@code "on"}.
         */
        CHECKED,
        /**
         * One item, shown by its label: a ComboBox, a Select, a ListBox. Its {@code value} property is Flow's key for
         * the item, {@code "3"}, meaningless to the user.
         */
        SELECTED_ITEM,
        /** Items, shown by their labels: a MultiSelectComboBox, a CheckboxGroup, a MultiSelectListBox. */
        SELECTED_ITEMS,
        /** A number, a Slider's {@code 30}; or two, a RangeSlider's {@code [8,17]}. */
        NUMBER,
        /**
         * Nothing of its own: a CustomField's value is the fields inside it, each shown with its own. Its
         * {@code value} property is theirs joined by a tab, not Flow's value.
         */
        PARTS,
        /** A type this table doesn't list: either may be there. */
        UNKNOWN;

        boolean hasText() {
            return this == TEXT || this == UNKNOWN;
        }
    }

    /**
     * @param flowName the Flow class's simple name, {@code Button}, or {@code <tag>} for an element with none
     * @param actions  the tools that act on it beyond what every component allows; only tools this server has
     */
    record Type(String flowName, NameFrom nameFrom, Holds holds, List<String> actions) {}

    private static final Map<String, Type> TYPES = Map.ofEntries(
            Map.entry("vaadin-button", clickable("Button")),
            Map.entry("vaadin-drawer-toggle", clickable("DrawerToggle")),
            Map.entry("vaadin-side-nav-item", clickable("SideNavItem")),
            Map.entry("vaadin-breadcrumbs-item", clickable("BreadcrumbsItem")),
            Map.entry("vaadin-checkbox", labelled("Checkbox", Holds.CHECKED)),
            Map.entry("vaadin-switch", labelled("Switch", Holds.CHECKED)),
            Map.entry("vaadin-text-field", field("TextField")),
            Map.entry("vaadin-text-area", field("TextArea")),
            Map.entry("vaadin-password-field", field("PasswordField")),
            Map.entry("vaadin-email-field", field("EmailField")),
            Map.entry("vaadin-integer-field", field("IntegerField")),
            Map.entry("vaadin-number-field", field("NumberField")),
            Map.entry("vaadin-big-decimal-field", field("BigDecimalField")),
            Map.entry("vaadin-date-picker", field("DatePicker")),
            Map.entry("vaadin-time-picker", field("TimePicker")),
            Map.entry("vaadin-date-time-picker", labelled("DateTimePicker", Holds.DATE_AND_TIME)),
            Map.entry("vaadin-combo-box", labelled("ComboBox", Holds.SELECTED_ITEM)),
            Map.entry("vaadin-select", labelled("Select", Holds.SELECTED_ITEM)),
            Map.entry("vaadin-radio-group", labelled("RadioButtonGroup", Holds.SELECTED_ITEM)),
            Map.entry("vaadin-multi-select-combo-box", labelled("MultiSelectComboBox", Holds.SELECTED_ITEMS)),
            Map.entry("vaadin-checkbox-group", labelled("CheckboxGroup", Holds.SELECTED_ITEMS)),
            Map.entry("vaadin-list-box", labelled("ListBox", Holds.SELECTED_ITEM)),
            Map.entry("vaadin-slider", labelled("Slider", Holds.NUMBER)),
            Map.entry("vaadin-range-slider", labelled("RangeSlider", Holds.NUMBER)),
            Map.entry("vaadin-custom-field", labelled("CustomField", Holds.PARTS)),
            Map.entry("vaadin-dialog", titled("Dialog")),
            Map.entry("vaadin-confirm-dialog", titled("ConfirmDialog")),
            Map.entry("vaadin-login-overlay", titled("LoginOverlay")),
            Map.entry("vaadin-login-form", titled("LoginForm")),
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

    /**
     * Dialogs, by the slot each renders its title into: that element isn't shown, the title being the dialog's name.
     */
    static final Map<String, String> DIALOG_TITLE_SLOTS = Map.of("vaadin-dialog", "title",
            "vaadin-confirm-dialog", "header", "vaadin-login-overlay", "title");

    /**
     * A LoginForm and a LoginOverlay: the error each shows after the app's {@code setError(true)}, its text from the
     * component's {@code i18n} (D_login_from_i18n). A LoginForm's name, its heading, comes from its {@code i18n} too.
     */
    static final Set<String> LOGINS = Set.of("vaadin-login-form", "vaadin-login-overlay");

    /**
     * What {@code toggle_expand} expands and collapses, when it has items: those have a {@code has-children} attribute.
     */
    static final Set<String> EXPANDABLE = Set.of("vaadin-side-nav-item");

    /** A MultiSelectListBox: a {@code vaadin-list-box} with {@code multiple} set. */
    private static final Type MULTI_SELECT_LIST_BOX = labelled("MultiSelectListBox", Holds.SELECTED_ITEMS);

    /**
     * Fields: nothing inside one is the app's, so a snapshot shows none of it — a ComboBox's scroller, a
     * RadioButtonGroup's buttons, a Select's list. Not a CustomField: the fields inside it are the app's.
     */
    static final Set<String> FIELDS = TYPES.entrySet().stream()
            .filter(e -> e.getValue().holds() != Holds.TEXT && e.getValue().holds() != Holds.PARTS)
            .map(Map.Entry::getKey)
            .collect(Collectors.toUnmodifiableSet());

    /**
     * What a user acts on: a type with tools of its own, a link, a Grid. In a grid's cell, header or footer, one shows
     * in its short view, {@code Button "Edit" [ref=3]}; anything else as its text, a Span's {@code Frank Rossi}.
     */
    static final Set<String> ACTED_ON = Stream.concat(
                    TYPES.entrySet().stream().filter(e -> !e.getValue().actions().isEmpty()).map(Map.Entry::getKey),
                    Stream.of("a", "vaadin-grid"))
            .collect(Collectors.toUnmodifiableSet());

    /** Never in a snapshot, nor anything inside them: decoration, and the dev tools. */
    static final Set<String> SKIPPED = Set.of("vaadin-icon", "svg", "img", "vaadin-tooltip", "script", "style",
            "template", "noscript", "link", "meta", "vaadin-dev-tools", "vaadin-connection-indicator");

    /** The type of a component the browser has: its tag, and for a {@code vaadin-list-box}, its {@code multiple}. */
    static Type of(UnifiedElement element) {
        String tag = element.getTag();
        return of(tag, tag.equals("vaadin-list-box") && element.getPropertyBoolean("multiple"));
    }

    /** @param multiple whether a {@code vaadin-list-box} takes more than one item; ignored for any other tag */
    static Type of(String tag, boolean multiple) {
        if (multiple && tag.equals("vaadin-list-box")) {
            return MULTI_SELECT_LIST_BOX;
        }
        Type known = TYPES.get(tag);
        if (known != null) {
            return known;
        }
        if (tag.startsWith("vaadin-")) {
            return new Type(className(tag.substring("vaadin-".length())), NameFrom.LABEL_OR_TEXT, Holds.UNKNOWN, List.of());
        }
        return new Type("<" + tag + ">", NameFrom.LABEL_OR_TEXT, Holds.UNKNOWN, List.of());
    }

    /** A component named by its text, made to be clicked: a Button, a link in a SideNav or Breadcrumbs. */
    private static Type clickable(String flowName) {
        return new Type(flowName, NameFrom.TEXT, Holds.TEXT, List.of("click"));
    }

    private static Type field(String flowName) {
        return labelled(flowName, Holds.VALUE);
    }

    /** A field named by its label, holding something other than a text; a CustomField takes no value of its own. */
    private static Type labelled(String flowName, Holds holds) {
        return new Type(flowName, NameFrom.LABEL, holds, holds == Holds.PARTS ? List.of() : List.of("set_value"));
    }

    /** A component named by the heading it shows: a dialog, a LoginForm. */
    private static Type titled(String flowName) {
        return new Type(flowName, NameFrom.TITLE, Holds.TEXT, List.of());
    }

    private static Type html(String flowName) {
        return new Type(flowName, NameFrom.TEXT, Holds.TEXT, List.of());
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
