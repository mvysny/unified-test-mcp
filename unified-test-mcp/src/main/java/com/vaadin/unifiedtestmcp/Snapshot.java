package com.vaadin.unifiedtestmcp;

import com.microsoft.playwright.Page;
import com.vaadin.testing.unified.UnifiedPage;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The page's URL and title, if it has one, then its Vaadin components as text, one line each, indented by nesting:
 *
 * <pre>
 * URL: http://localhost:8080/form
 * Title: Form
 * - TextField "Name" [ref=1, id=name, value="Alice"]
 * - TextField "Email" [ref=2, value="", required]
 * - Checkbox "Subscribe" [ref=3, checked]
 * - Button "Save" [ref=4] actions: click
 * - Button "Close" [ref=5, id=close, aria-label] actions: click
 * &lt;type&gt;  &lt;name&gt;  [ref, id, value, the flags that are on]  &lt;the type's own tools&gt;
 * </pre>
 *
 * The name is what {@link ComponentTypes} says names that type: a Button's text, a field's label; with neither,
 * the {@code aria-label}, then the placeholder, the bracket saying which. A layout without an id, and plain HTML
 * with neither an id nor text of its own, stand aside: their children take their place. Every field has a
 * {@code value=}, in {@code get_value}'s notation, {@link Json}: {@code ""} empty, {@code null} nothing selected;
 * a component without one isn't a field. A Checkbox's {@code checked} says its value.
 *
 * <h2>Implementation detail</h2>
 * One script, one round-trip: it walks the light DOM from {@code <body>}, stamps each component it shows with
 * {@link Refs#ATTRIBUTE} (keeping a stamp already there, so a component keeps its ref), and returns the tree. The
 * Unified API can't list a page's components (R_unified_no_enumeration), hence the escape hatch.
 */
final class Snapshot {
    private Snapshot() {}

    /** A value longer than this is cut, with {@code …}; a name never is: it says which component this is. */
    private static final int VALUE_CAP = 80;

    private static final String SCRIPT = """
            async ({ next, layouts, skipped, fields, attribute }) => {
              // A server-side navigation changes the URL after the round-trip that an action waits for
              // (R_flow_navigation_url): with React, once Flow clears its navigation flag; without, in a setTimeout.
              const tick = ms => new Promise(resolve => setTimeout(resolve, ms));
              for (let waited = 0; window.Vaadin?.Flow?.navigation && waited < 2000; waited += 10) await tick(10);
              await tick(0);
              const LAYOUTS = new Set(layouts), SKIPPED = new Set(skipped), FIELDS = new Set(fields);
              // the light-DOM children a Vaadin component renders for itself: its input, label, messages
              const OWN_PARTS = new Set(['input', 'textarea', 'label', 'error-message', 'helper', 'tooltip']);
              const str = v => typeof v === 'string' && v.trim() !== '' ? v : null;
              // what the user sees selected, as Dramafinder reads it: the value property is Flow's key for the item
              const itemLabel = (e, i) => typeof i === 'string' ? i : String(i[e.itemLabelPath || 'label'] ?? '');
              const valueOf = e => {
                switch (e.localName) {
                  case 'vaadin-combo-box':
                    return e.selectedItem == null ? null : str(itemLabel(e, e.selectedItem));
                  case 'vaadin-multi-select-combo-box':
                    return (e.selectedItems || []).map(i => itemLabel(e, i));
                  case 'vaadin-radio-group': {
                    const checked = e.querySelector(':scope > vaadin-radio-button[checked] > label');
                    return checked ? str(checked.textContent) : null;
                  }
                  case 'vaadin-select':
                    return str(e.querySelector(':scope > vaadin-select-value-button')?.textContent);
                  default:
                    return typeof e.value === 'string' ? e.value : null;
                }
              };
              const ownText = e => Array.from(e.childNodes).filter(n => n.nodeType === Node.TEXT_NODE)
                  .map(n => n.textContent).join(' ').replace(/\\s+/g, ' ').trim();
              const visit = (e, parentTag) => {
                const tag = e.localName;
                if (e.hidden || SKIPPED.has(tag) || tag.startsWith('copilot-')) return [];
                if (parentTag.startsWith('vaadin-') && OWN_PARTS.has(e.getAttribute('slot'))) return [];
                const custom = tag.includes('-');
                const text = ownText(e);
                const children = () => FIELDS.has(tag) ? [] : Array.from(e.children).flatMap(c => visit(c, tag));
                // Flow's bootstrap wraps every view in <div id="outlet"><flow-container-root-…>: not the app's
                const bootstrap = tag.startsWith('flow-container-') || (tag === 'div' && e.id === 'outlet');
                const shown = !bootstrap && (custom ? !(LAYOUTS.has(tag) && !e.id) : !!(e.id || text));
                if (!shown) return children();
                let ref = e.getAttribute(attribute);
                if (ref === null) {
                  ref = String(next++);
                  e.setAttribute(attribute, ref);
                }
                return [{
                  tag, ref: Number(ref), text: str(text),
                  id: str(e.id), idUnique: !!e.id && document.querySelectorAll('#' + CSS.escape(e.id)).length === 1,
                  testId: str(e.getAttribute('data-testid')),
                  label: custom ? str(e.label) : null,
                  ariaLabel: str(e.getAttribute('aria-label')),
                  placeholder: custom ? str(e.placeholder) : null,
                  value: custom ? valueOf(e) : null,
                  disabled: e.disabled === true || e.hasAttribute('disabled'),
                  checked: custom && e.checked === true,
                  invalid: custom && e.invalid === true,
                  errorMessage: custom ? str(e.errorMessage) : null,
                  required: custom && e.required === true,
                  readonly: custom && e.readonly === true,
                  children: children()
                }];
              };
              const nodes = Array.from(document.body.children).flatMap(c => visit(c, 'body'));
              return { url: location.href, title: document.title, next, nodes };
            }""";

    /** What a snapshot of the browser's empty first page says, instead of listing nothing. */
    static final String NO_PAGE = "No page open yet: call navigate with the app's URL.";

    /** Whether the browser is still on its empty first page, before any navigation. */
    static boolean isBlank(Page browser) {
        return browser.url().equals("about:blank");
    }

    /**
     * Snapshots the page and makes its refs the session's valid ones.
     *
     * @return the snapshot's text, starting with the browser's URL; {@link #NO_PAGE} before any navigation
     */
    static String take(UnifiedPage page, Refs refs) {
        Page browser = page.unwrap(Page.class);
        if (isBlank(browser)) {
            return NO_PAGE + "\n";
        }
        Map<String, Object> arg = Map.of("next", refs.next(), "layouts", List.copyOf(ComponentTypes.LAYOUTS),
                "skipped", List.copyOf(ComponentTypes.SKIPPED), "fields", List.copyOf(ComponentTypes.FIELDS),
                "attribute", Refs.ATTRIBUTE);
        Map<?, ?> result = (Map<?, ?>) browser.evaluate(SCRIPT, arg);
        StringBuilder out = new StringBuilder("URL: ").append(result.get("url")).append('\n');
        String title = (String) result.get("title");
        if (!title.isBlank()) {
            out.append("Title: ").append(title.strip()).append('\n');
        }
        Map<Integer, Refs.Target> targets = new LinkedHashMap<>();
        List<?> nodes = (List<?>) result.get("nodes");
        if (nodes.isEmpty()) {
            out.append("(no components)\n");
        }
        render(nodes, 0, out, targets);
        refs.replace(targets, ((Number) result.get("next")).intValue());
        return out.toString();
    }

    private static void render(List<?> nodes, int depth, StringBuilder out, Map<Integer, Refs.Target> targets) {
        for (Object o : nodes) {
            Map<?, ?> node = (Map<?, ?>) o;
            ComponentTypes.Type type = ComponentTypes.of((String) node.get("tag"));
            int ref = ((Number) node.get("ref")).intValue();
            String id = (String) node.get("id");
            targets.put(ref, new Refs.Target(type.flowName(),
                    Boolean.TRUE.equals(node.get("idUnique")) ? id : null));

            out.append("  ".repeat(depth)).append("- ").append(type.flowName());
            List<String> bracket = new ArrayList<>();
            bracket.add("ref=" + ref);
            if (id != null) {
                bracket.add("id=" + id);
            }
            if (node.get("testId") != null) {
                bracket.add("testid=" + node.get("testId"));
            }
            Name name = name(node, type.nameFrom());
            if (name != null) {
                out.append(' ').append(quote(name.text()));
                if (name.from() != null) {
                    bracket.add(name.from());
                }
            }
            String value = value(type.holds(), node.get("value"));
            if (value != null) {
                bracket.add("value=" + value);
            }
            flag(node, "disabled", bracket);
            flag(node, "checked", bracket);
            flag(node, "required", bracket);
            flag(node, "readonly", bracket);
            if (Boolean.TRUE.equals(node.get("invalid"))) {
                Object message = node.get("errorMessage");
                bracket.add(message == null ? "invalid" : "invalid " + quote((String) message));
            }
            out.append(" [").append(String.join(", ", bracket)).append(']');
            if (!type.actions().isEmpty()) {
                out.append(" actions: ").append(String.join(", ", type.actions()));
            }
            out.append('\n');
            render((List<?>) node.get("children"), depth + 1, out, targets);
        }
    }

    /**
     * @param from where the name came from when it isn't the type's own naming property, {@code aria-label} or
     *             {@code placeholder}; {@code null} when it is
     */
    private record Name(String text, @Nullable String from) {}

    private static @Nullable Name name(Map<?, ?> node, ComponentTypes.NameFrom nameFrom) {
        String label = (String) node.get("label");
        String text = (String) node.get("text");
        String own = switch (nameFrom) {
            case TEXT -> text;
            case LABEL -> label;
            case LABEL_OR_TEXT -> label != null ? label : text;
        };
        if (own != null) {
            return new Name(own, null);
        }
        if (node.get("ariaLabel") != null) {
            return new Name((String) node.get("ariaLabel"), "aria-label");
        }
        if (node.get("placeholder") != null) {
            return new Name((String) node.get("placeholder"), "placeholder");
        }
        return null;
    }

    private static void flag(Map<?, ?> node, String key, List<String> bracket) {
        if (Boolean.TRUE.equals(node.get(key))) {
            bracket.add(key);
        }
    }

    /**
     * {@code get_value}'s answer, cut at {@value #VALUE_CAP} characters; {@code null} for a component that isn't a
     * field, or one of a type {@link ComponentTypes} doesn't list without a value.
     */
    private static @Nullable String value(ComponentTypes.Holds holds, @Nullable Object value) {
        return switch (holds) {
            case TEXT, CHECKED -> null;
            case VALUE -> Json.of(cap(value == null ? "" : (String) value));
            case SELECTED_ITEM -> Json.of(value == null ? null : cap((String) value));
            case SELECTED_ITEMS -> cap(Json.of(value));
            case UNKNOWN -> value == null || ((String) value).isBlank() ? null : Json.of(cap((String) value));
        };
    }

    private static String cap(String value) {
        return value.length() <= VALUE_CAP ? value : value.substring(0, VALUE_CAP) + "…";
    }

    /** Whitespace collapsed, {@code "} and {@code \} escaped, in quotes. */
    private static String quote(String s) {
        return '"' + s.strip().replaceAll("\\s+", " ").replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }
}
