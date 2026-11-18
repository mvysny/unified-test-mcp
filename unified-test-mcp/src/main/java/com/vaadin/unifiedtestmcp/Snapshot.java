package com.vaadin.unifiedtestmcp;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.vaadin.testing.unified.UnifiedElement;
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
 * - TextField "Email" [ref=2, value="", helper="We never share it", required]
 * - Checkbox "Subscribe" [ref=3, checked]
 * - Button "Save" [ref=4] actions: click
 * - Button [ref=5, id=close, aria-label="Close", icon="vaadin:close"] actions: click
 * &lt;type&gt;  &lt;name&gt;  [ref, id, icon, path, value, the hints, the flags that are on]  &lt;the type's own tools&gt;
 * </pre>
 *
 * The quoted name is what {@link ComponentTypes} says names that type: a Button's text, a field's label. A component
 * with none has no quoted name, so an icon-only Button reads as one; its bracket shows its {@code aria-label=}, else
 * its {@code placeholder=}, for the agent to tell it by. A layout without an id, and plain HTML
 * with neither an id nor text of its own, stand aside: their children take their place. A component the user can't
 * see, by {@link #isVisible}, isn't shown: one the app hid, a closed dialog's, Details' or menu's content, a closed
 * drawer's, a collapsed SideNavItem's items. What's inside it shows only if the user sees that, as a Dialog footer's
 * buttons. Nor is the out-of-sight region where Vaadin announces to a screen reader what a component did.
 *
 * An AppLayout shows what it holds under its regions, the app's chrome apart from the view; a closed drawer as a line
 * naming what opens it (D_app_layout_regions). Navigation says where the user is, where each item links, and where
 * more is hidden:
 *
 * <pre>
 * - AppLayout [ref=1]
 *   - Navbar:
 *     - DrawerToggle [ref=2, aria-label="Toggle navigation panel"] actions: click
 *   - Drawer:
 *     - SideNav [ref=3]
 *       - SideNavItem "Inbox" [ref=4, path="nav", current] actions: click
 *       - SideNavItem "Archive" [ref=5, path="nav/archive", collapsed] actions: click, toggle_expand
 *   - Content:
 *     - H2 "Inbox" [ref=6]
 * </pre>
 * <pre>
 *   - Drawer: closed; click the DrawerToggle, ref=2, to open it
 * </pre>
 *
 * {@code path=} is where a SideNavItem or a BreadcrumbsItem links, as the app gave it; {@code current} marks the one
 * of the page shown; {@code collapsed} a SideNavItem whose items aren't shown. One with items has
 * {@code toggle_expand} among its actions, the one action listed by a component's state rather than its type
 * (D_nav_item_states).
 *
 * A dialog is named by its title: a Dialog's or a ConfirmDialog's header, a LoginOverlay's title, its bracket showing
 * its {@code description=}. A LoginForm is named by its heading, {@code LoginForm "Log in"}. While a modal dialog is
 * open, nothing under it takes input, so the snapshot shows the one on top alone, below a line naming the modal stack,
 * top first:
 *
 * <pre>
 * Modal: Dialog "Why?" over Dialog "Delete order?" over the page; only the dialog on top is shown: nothing under it takes input
 * - Dialog "Why?" [ref=9]
 *   - Span "Deleting can't be undone." [ref=10]
 *   - Button "OK" [ref=11] actions: click
 * </pre>
 *
 * {@code icon=} names the icon in front of the name, in the {@code prefix} slot or an icon-only Button's, as the app
 * gave it: {@code "vaadin:close"}, {@code "lumo:cross"}, a FontIcon's ligature or classes. A hint for the agent to
 * find "the X button" by, so an icon without a name, an SvgIcon, isn't shown (R_vaadin_icons).
 *
 * Every field has a {@code value=}, in {@code get_value}'s notation, {@link Json}: {@code ""} empty, {@code null}
 * nothing selected; a component without one isn't a field. A Slider's has {@code min=} and {@code max=} beside it.
 * A Checkbox's {@code checked} says its value; a CustomField has none, the fields inside it being shown with theirs.
 *
 * The hints, each cut at {@value #HINT_CAP} characters: {@code helper=}, the text below a field, which
 * {@code get_helper} reads uncut; {@code tooltip=}, which {@code get_tooltip} does.
 *
 * {@code disabled} and {@code readonly} are each component's own, as the browser has them: Flow disables the fields
 * inside a disabled layout too; a read-only CustomField's fields are read-only only if the app's subclass passes it
 * on (R_flow_field_states).
 *
 * A field that shows invalid has {@code invalid "At most 120"}, with its error message; a LoginForm or a LoginOverlay
 * that shows its error has {@code error "Incorrect username or password: Check that you have entered…"}, the error's
 * title and message as the user sees them.
 *
 * <h2>Implementation detail</h2>
 * One script, one round-trip: it waits for the page's animations to end, walks the light DOM from {@code <body>}, or
 * from the modal on top, stamps each component it shows with
 * {@link Refs#ATTRIBUTE} (keeping a stamp already there, so a component keeps its ref), and returns the tree. The
 * Unified API can't list a page's components (R_unified_no_enumeration), hence the escape hatch.
 */
final class Snapshot {
    private Snapshot() {}

    /**
     * Whether the user can see an element, as a JavaScript function of it: a snapshot shows a component only if so,
     * and {@link Refs} acts on one only if so. False for one under {@code display: none} or {@code visibility:
     * hidden}, which is how Vaadin hides a closed Details' content or a closed drawer (R_vaadin_hidden_in_dom); and
     * for a {@code display: contents} element, which has no box of its own though its children may.
     *
     * Blind to what's covered by another element, to opacity, size and clipping (D_visible_only).
     */
    static final String IS_VISIBLE = "e => e.checkVisibility({ visibilityProperty: true })";

    /** {@link #IS_VISIBLE} of the element in the browser. */
    static boolean isVisible(UnifiedElement element) {
        return Boolean.TRUE.equals(element.unwrap(Locator.class).evaluate(IS_VISIBLE));
    }

    /** A value longer than this is cut, with {@code …}; a name never is: it says which component this is. */
    private static final int VALUE_CAP = 80;

    /** A helper text or a tooltip longer than this is cut, with {@code …}: advice, so nothing is lost to it. */
    private static final int HINT_CAP = 120;

    private static final String SCRIPT = """
            async ({ next, layouts, skipped, fields, titleSlots, logins, expandable, attribute }) => {
              // A server-side navigation changes the URL after the round-trip that an action waits for
              // (R_flow_navigation_url): with React, once Flow clears its navigation flag; without, in a setTimeout.
              // Drop once Auto-Wait waits for it: parttio/unified-testing-tool#233
              const tick = ms => new Promise(resolve => setTimeout(resolve, ms));
              for (let waited = 0; window.Vaadin?.Flow?.navigation && waited < 2000; waited += 10) await tick(10);
              await tick(0);
              const LAYOUTS = new Set(layouts), SKIPPED = new Set(skipped), FIELDS = new Set(fields);
              // a component shows or hides at its animation's end, a drawer at its visibility transition's; a
              // Button's 1.4 s fade isn't waited for (D_visible_only). A component's own animations run in its
              // shadow root, which document.getAnimations() leaves out (R_vaadin_animations)
              const roots = [document];
              for (let i = 0; i < roots.length; i++) {
                for (const e of roots[i].querySelectorAll('*')) {
                  if (e.shadowRoot && !SKIPPED.has(e.localName) && !e.localName.startsWith('copilot-')) {
                    roots.push(e.shadowRoot);
                  }
                }
              }
              const showsOrHides = a => a.playState === 'running' && a.effect?.getComputedTiming().endTime !== Infinity
                  && (a.transitionProperty === undefined || ['visibility', 'display'].includes(a.transitionProperty));
              for (let waited = 0; roots.some(root => root.getAnimations().some(showsOrHides)) && waited < 1000;
                  waited += 10) await tick(10);
              const isVisible = %s;
              const LOGINS = new Set(logins);
              const SLIDERS = new Set(['vaadin-slider', 'vaadin-range-slider']);
              const NAV_ITEMS = new Set(['vaadin-side-nav-item', 'vaadin-breadcrumbs-item']);
              const EXPANDABLE = new Set(expandable);
              // the light-DOM children a Vaadin component renders for itself: its input, label, messages
              const OWN_PARTS = new Set(['input', 'textarea', 'label', 'error-message', 'helper', 'tooltip']);
              const str = v => typeof v === 'string' && v.trim() !== '' ? v : null;
              // what the user sees selected, as Dramafinder reads it: the value property is Flow's key for the item
              const itemLabel = (e, i) => typeof i === 'string' ? i : String(i[e.itemLabelPath || 'label'] ?? '');
              const textOf = e => (e?.textContent || '').trim();
              // a LoginOverlay has aria-labelledby, no aria-label; a LoginForm's heading and a login's error are
              // rendered in its shadow DOM from its i18n (D_login_from_i18n)
              const titleOf = e => e.localName === 'vaadin-login-form' ? str(e.i18n?.form?.title)
                  : str(e.getAttribute('aria-label'))
                    ?? (e.localName in titleSlots
                        ? str(textOf(e.querySelector(`:scope > [slot="${titleSlots[e.localName]}"]`))) : null);
              const loginError = e => str([e.i18n?.errorMessage?.title, e.i18n?.errorMessage?.message]
                  .filter(str).join(': '));
              const iconOf = e => {
                const icon = e.querySelector(':scope > vaadin-icon[slot=prefix]')
                    ?? (e.localName === 'vaadin-button' ? e.querySelector(':scope > vaadin-icon:not([slot])') : null);
                return icon ? str(icon.getAttribute('icon')) ?? str(icon.ligature) ?? str(icon.iconClass) : null;
              };
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
                  case 'vaadin-checkbox-group':
                    return Array.from(e.querySelectorAll(':scope > vaadin-checkbox[checked]')).map(textOf);
                  case 'vaadin-list-box':
                    return e.multiple ? (e.selectedValues || []).map(i => textOf(e.items[i]))
                        : e.items?.[e.selected] ? textOf(e.items[e.selected]) : null;
                  case 'vaadin-slider':
                  case 'vaadin-range-slider':
                    return e.value ?? null;
                  // the text the user sees, "31.1.2000": the value property is ISO
                  case 'vaadin-date-picker':
                  case 'vaadin-time-picker':
                    return e.inputElement?.value ?? '';
                  case 'vaadin-date-time-picker':
                    return ['date-picker', 'time-picker']
                        .map(slot => e.querySelector(`:scope > [slot=${slot}]`)?.inputElement?.value ?? '');
                  default:
                    return typeof e.value === 'string' ? e.value : null;
                }
              };
              const ownText = e => Array.from(e.childNodes).filter(n => n.nodeType === Node.TEXT_NODE)
                  .map(n => n.textContent).join(' ').replace(/\\s+/g, ' ').trim();
              // an AppLayout's children by their slot, the view in none (R_vaadin_app_layout_dom)
              const REGIONS = [['Navbar', slot => slot?.startsWith('navbar')], ['Drawer', slot => slot === 'drawer'],
                  ['Content', slot => slot === null]];
              const regions = layout => {
                const nodes = REGIONS.map(([region, holds]) => {
                  const members = Array.from(layout.children).filter(c => holds(c.getAttribute('slot')));
                  return region === 'Drawer' && members.length > 0 && !layout.drawerOpened ? { region, closed: true }
                      : { region, children: members.flatMap(c => visit(c, layout.localName)) };
                });
                // looked up once every region is visited: a toggle in the content has its ref only then
                const toggle = layout.querySelector('vaadin-drawer-toggle');
                const toggleRef = toggle && isVisible(toggle) ? toggle.getAttribute(attribute) : null;
                return nodes.filter(n => n.closed || n.children.length > 0)
                    .map(n => n.closed ? { ...n, toggleRef: toggleRef === null ? null : Number(toggleRef) } : n);
              };
              const visit = (e, parentTag) => {
                const tag = e.localName;
                if (e.hidden || SKIPPED.has(tag) || tag.startsWith('copilot-')) return [];
                // Vaadin's screen-reader announcements, "Czechia added to selection" (@vaadin/a11y-base announce.js)
                if (parentTag === 'body' && tag === 'div' && e.style.position === 'fixed'
                    && e.style.clip === 'rect(0px, 0px, 0px, 0px)') return [];
                if (parentTag.startsWith('vaadin-') && OWN_PARTS.has(e.getAttribute('slot'))) return [];
                if (titleSlots[parentTag] === e.getAttribute('slot')) return [];
                // told to close, a dialog may show a while yet, its exit animation not waited for if not begun
                if (tag in titleSlots && !e.opened) return [];
                const custom = tag.includes('-');
                const text = ownText(e);
                const children = () => FIELDS.has(tag) ? [] : tag === 'vaadin-app-layout' ? regions(e)
                    : Array.from(e.children).flatMap(c => visit(c, tag));
                // Flow's bootstrap wraps every view in <div id="outlet"><flow-container-root-…>: not the app's
                const bootstrap = tag.startsWith('flow-container-') || (tag === 'div' && e.id === 'outlet');
                // one the user can't see stands aside: what's inside may be seen, as a display: contents footer's
                const shown = !bootstrap && (custom ? !(LAYOUTS.has(tag) && !e.id) : !!(e.id || text)) && isVisible(e);
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
                  title: tag in titleSlots || LOGINS.has(tag) ? titleOf(e) : null,
                  description: tag === 'vaadin-login-overlay' ? str(e.description) : null,
                  ariaLabel: str(e.getAttribute('aria-label')),
                  placeholder: custom ? str(e.placeholder) : null,
                  icon: custom ? iconOf(e) : null,
                  // "" is a link too, to the app's root
                  path: NAV_ITEMS.has(tag) && typeof e.path === 'string' ? e.path : null,
                  value: custom ? valueOf(e) : null,
                  min: SLIDERS.has(tag) ? e.min ?? null : null,
                  max: SLIDERS.has(tag) ? e.max ?? null : null,
                  helper: custom ? str(textOf(e.querySelector(':scope > [slot=helper]'))) : null,
                  tooltip: custom ? str(e.querySelector(':scope > vaadin-tooltip[slot=tooltip]')?.text) : null,
                  disabled: e.disabled === true || e.hasAttribute('disabled'),
                  checked: custom && e.checked === true,
                  indeterminate: custom && e.indeterminate === true,
                  current: custom && e.current === true,
                  expandable: EXPANDABLE.has(tag) && e.hasAttribute('has-children'),
                  collapsed: EXPANDABLE.has(tag) && e.hasAttribute('has-children') && !e.expanded,
                  multiple: custom && e.multiple === true,
                  invalid: custom && e.invalid === true,
                  error: LOGINS.has(tag) && e.error === true,
                  errorMessage: !custom ? null : LOGINS.has(tag) ? loginError(e) : str(e.errorMessage),
                  required: custom && e.required === true,
                  readonly: custom && e.readonly === true,
                  children: children()
                }];
              };
              // a modal opened over another is after it in the DOM: Flow appends it to the body, or puts it inside
              // the modal that opened it
              const modals = Array.from(document.querySelectorAll('[aria-modal=true]'))
                  .filter(e => e.opened !== false && !e.hidden).reverse();
              const nodes = modals.length > 0 ? visit(modals[0], modals[0].parentElement.localName)
                  : Array.from(document.body.children).flatMap(c => visit(c, 'body'));
              return { url: location.href, title: document.title, next, nodes,
                  modals: modals.map(e => ({ tag: e.localName, title: titleOf(e) })) };
            }""".formatted(IS_VISIBLE);

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
                "titleSlots", ComponentTypes.DIALOG_TITLE_SLOTS, "logins", List.copyOf(ComponentTypes.LOGINS),
                "expandable", List.copyOf(ComponentTypes.EXPANDABLE), "attribute", Refs.ATTRIBUTE);
        Map<?, ?> result = (Map<?, ?>) browser.evaluate(SCRIPT, arg);
        StringBuilder out = new StringBuilder("URL: ").append(result.get("url")).append('\n');
        String title = (String) result.get("title");
        if (!title.isBlank()) {
            out.append("Title: ").append(title.strip()).append('\n');
        }
        List<String> modals = ((List<?>) result.get("modals")).stream()
                .map(m -> dialogName((Map<?, ?>) m)).toList();
        if (!modals.isEmpty()) {
            out.append("Modal: ").append(String.join(" over ", modals))
                    .append(" over the page; only the dialog on top is shown: nothing under it takes input\n");
        }
        Map<Integer, Refs.Target> targets = new LinkedHashMap<>();
        List<?> nodes = (List<?>) result.get("nodes");
        if (nodes.isEmpty()) {
            out.append("(no components)\n");
        }
        render(nodes, 0, out, targets);
        refs.replace((String) result.get("url"), modals.isEmpty() ? null : modals.get(0), targets,
                ((Number) result.get("next")).intValue());
        return out.toString();
    }

    private static void render(List<?> nodes, int depth, StringBuilder out, Map<Integer, Refs.Target> targets) {
        for (Object o : nodes) {
            Map<?, ?> node = (Map<?, ?>) o;
            if (node.get("region") != null) {
                renderRegion(node, depth, out, targets);
                continue;
            }
            ComponentTypes.Type type = ComponentTypes.of((String) node.get("tag"),
                    Boolean.TRUE.equals(node.get("multiple")));
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
            String name = name(node, type.nameFrom());
            if (name != null) {
                out.append(' ').append(quote(name));
            } else if (node.get("ariaLabel") != null) {
                bracket.add("aria-label=" + quote((String) node.get("ariaLabel")));
            } else if (node.get("placeholder") != null) {
                bracket.add("placeholder=" + quote((String) node.get("placeholder")));
            }
            if (node.get("description") != null) {
                bracket.add("description=" + quote((String) node.get("description")));
            }
            if (node.get("icon") != null) {
                bracket.add("icon=" + quote((String) node.get("icon")));
            }
            if (node.get("path") != null) {
                bracket.add("path=" + quote((String) node.get("path")));
            }
            String value = value(type.holds(), node.get("value"));
            if (value != null) {
                bracket.add("value=" + value);
            }
            for (String key : List.of("min", "max")) {
                if (node.get(key) != null) {
                    bracket.add(key + "=" + Json.of(node.get(key)));
                }
            }
            for (String key : List.of("helper", "tooltip")) {
                if (node.get(key) != null) {
                    bracket.add(key + "=" + quote(cap(collapse((String) node.get(key)), HINT_CAP)));
                }
            }
            flag(node, "checked", bracket);
            flag(node, "indeterminate", bracket);
            flag(node, "current", bracket);
            flag(node, "collapsed", bracket);
            flag(node, "disabled", bracket);
            flag(node, "readonly", bracket);
            flag(node, "required", bracket);
            flagWithMessage(node, "invalid", bracket);
            flagWithMessage(node, "error", bracket);
            out.append(" [").append(String.join(", ", bracket)).append(']');
            List<String> actions = new ArrayList<>(type.actions());
            if (Boolean.TRUE.equals(node.get("expandable"))) {
                actions.add("toggle_expand");
            }
            if (!actions.isEmpty()) {
                out.append(" actions: ").append(String.join(", ", actions));
            }
            out.append('\n');
            render((List<?>) node.get("children"), depth + 1, out, targets);
        }
    }

    /** {@code - Navbar:} with what it holds under it; a closed drawer as {@code - Drawer: closed; click …}. */
    private static void renderRegion(Map<?, ?> node, int depth, StringBuilder out, Map<Integer, Refs.Target> targets) {
        out.append("  ".repeat(depth)).append("- ").append(node.get("region")).append(':');
        if (Boolean.TRUE.equals(node.get("closed"))) {
            out.append(" closed");
            if (node.get("toggleRef") != null) {
                out.append("; click the DrawerToggle, ref=").append(((Number) node.get("toggleRef")).intValue())
                        .append(", to open it");
            }
            out.append('\n');
            return;
        }
        out.append('\n');
        render((List<?>) node.get("children"), depth + 1, out, targets);
    }

    /** The type's own naming property, its text or its label; {@code null} for none. */
    private static @Nullable String name(Map<?, ?> node, ComponentTypes.NameFrom nameFrom) {
        String label = (String) node.get("label");
        String text = (String) node.get("text");
        return switch (nameFrom) {
            case TEXT -> text;
            case LABEL -> label;
            case LABEL_OR_TEXT -> label != null ? label : text;
            case TITLE -> (String) node.get("title");
        };
    }

    /** {@code Dialog "Delete order?"}; {@code Dialog} for one without a title. */
    private static String dialogName(Map<?, ?> modal) {
        String type = ComponentTypes.of((String) modal.get("tag"), false).flowName();
        return modal.get("title") == null ? type : type + " " + quote((String) modal.get("title"));
    }

    private static void flag(Map<?, ?> node, String key, List<String> bracket) {
        if (Boolean.TRUE.equals(node.get(key))) {
            bracket.add(key);
        }
    }

    /** {@code invalid "At most 120"}: a state that shows an error, with its message; {@code invalid} without one. */
    private static void flagWithMessage(Map<?, ?> node, String key, List<String> bracket) {
        if (Boolean.TRUE.equals(node.get(key))) {
            Object message = node.get("errorMessage");
            bracket.add(message == null ? key : key + " " + quote((String) message));
        }
    }

    /**
     * {@code get_value}'s answer, cut at {@value #VALUE_CAP} characters; {@code null} for a component that isn't a
     * field, or one of a type {@link ComponentTypes} doesn't list without a value.
     */
    private static @Nullable String value(ComponentTypes.Holds holds, @Nullable Object value) {
        return switch (holds) {
            case TEXT, CHECKED, PARTS -> null;
            case VALUE -> Json.of(cap(value == null ? "" : (String) value, VALUE_CAP));
            case SELECTED_ITEM -> Json.of(value == null ? null : cap((String) value, VALUE_CAP));
            case SELECTED_ITEMS, NUMBER, DATE_AND_TIME -> cap(Json.of(value), VALUE_CAP);
            case UNKNOWN -> value == null || ((String) value).isBlank() ? null
                    : Json.of(cap((String) value, VALUE_CAP));
        };
    }

    private static String cap(String value, int cap) {
        return value.length() <= cap ? value : value.substring(0, cap) + "…";
    }

    private static String collapse(String s) {
        return s.strip().replaceAll("\\s+", " ");
    }

    /** Whitespace collapsed, {@code "} and {@code \} escaped, in quotes. */
    private static String quote(String s) {
        return '"' + collapse(s).replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }
}
