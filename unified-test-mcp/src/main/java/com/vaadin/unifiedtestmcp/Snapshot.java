package com.vaadin.unifiedtestmcp;

import com.microsoft.playwright.Page;
import com.vaadin.testing.unified.UnifiedPage;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Vaadin components on the page as text, one line each, indented by nesting:
 *
 * <pre>
 * Path: /form
 * - TextField "Name" [ref=1, id=name, value="Alice"]
 * - Checkbox "Subscribe" [ref=2, checked]
 * - Button "Save" [ref=3] actions: click
 * - Button "Close" [ref=4, id=close, aria-label] actions: click
 * &lt;type&gt;  &lt;name&gt;  [ref, id, value, the flags that are on]  &lt;the type's own tools&gt;
 * </pre>
 *
 * The name is what {@link ComponentTypes} says names that type: a Button's text, a field's label; with neither,
 * the {@code aria-label}, then the placeholder, the bracket saying which. A layout without an id, and plain HTML
 * with neither an id nor text of its own, stand aside: their children take their place.
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
            ({ next, layouts, skipped, attribute }) => {
              const LAYOUTS = new Set(layouts), SKIPPED = new Set(skipped);
              // the light-DOM children a Vaadin component renders for itself: its input, label, messages
              const OWN_PARTS = new Set(['input', 'textarea', 'label', 'error-message', 'helper', 'tooltip']);
              const str = v => typeof v === 'string' && v.trim() !== '' ? v : null;
              const ownText = e => Array.from(e.childNodes).filter(n => n.nodeType === Node.TEXT_NODE)
                  .map(n => n.textContent).join(' ').replace(/\\s+/g, ' ').trim();
              const visit = (e, parentTag) => {
                const tag = e.localName;
                if (e.hidden || SKIPPED.has(tag) || tag.startsWith('copilot-')) return [];
                if (parentTag.startsWith('vaadin-') && OWN_PARTS.has(e.getAttribute('slot'))) return [];
                const custom = tag.includes('-');
                const text = ownText(e);
                const children = () => Array.from(e.children).flatMap(c => visit(c, tag));
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
                  value: custom ? str(e.value) : null,
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
              return { path: location.pathname + location.search, next, nodes };
            }""";

    /**
     * Snapshots the page and makes its refs the session's valid ones.
     *
     * @return the snapshot's text, starting with the browser's path
     */
    static String take(UnifiedPage page, Refs refs) {
        Map<String, Object> arg = Map.of("next", refs.next(), "layouts", List.copyOf(ComponentTypes.LAYOUTS),
                "skipped", List.copyOf(ComponentTypes.SKIPPED), "attribute", Refs.ATTRIBUTE);
        Map<?, ?> result = (Map<?, ?>) page.unwrap(Page.class).evaluate(SCRIPT, arg);
        StringBuilder out = new StringBuilder("Path: ").append(result.get("path")).append('\n');
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
            if (type.showValue() && node.get("value") != null) {
                bracket.add("value=" + quote(cap((String) node.get("value"))));
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

    private static String cap(String value) {
        return value.length() <= VALUE_CAP ? value : value.substring(0, VALUE_CAP) + "…";
    }

    /** Whitespace collapsed, {@code "} and {@code \} escaped, in quotes. */
    private static String quote(String s) {
        return '"' + s.strip().replaceAll("\\s+", " ").replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }
}
