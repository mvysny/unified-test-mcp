package com.vaadin.unifiedtestmcp;

import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.vaadin.testing.unified.UnifiedElement;
import com.vaadin.testing.unified.UnifiedPage;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
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
 * with neither an id nor text of its own, stand aside: their children take their place. Any other custom element shows
 * as its tag, what it renders in its shadow root under it, a LitTemplate's components:
 *
 * <pre>
 * - &lt;search-bar&gt; [ref=4, id=search]
 *   - TextField [ref=5, placeholder="Search", value=""] actions: set_value
 * </pre>
 *
 * A FormItem shows, named by its label, what it labels under it (D_form_item_label); a field, of what the app put in
 * its prefix and suffix, the components a user acts on (D_field_affixes):
 *
 * <pre>
 * - FormItem "Comment" [ref=7]
 *   - TextField [ref=8, placeholder="Add comment", value=""] actions: set_value
 *     - Button "Send" [ref=9] actions: click
 * </pre>
 *
 * A component the user can't
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
 * of the page shown; {@code collapsed} a SideNavItem that has items, hidden. One with items has
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
 * A Grid shows a line per row, at most {@value #GRID_ROWS} of those in view (D_grid_as_rows):
 *
 * <pre>
 * - Grid [ref=1, id=people, rows=200, selection=single]
 *   Header: | Name ↑ | Age | Tags | Actions |
 *   Header: | TextField [ref=2, placeholder="Filter", value=""] | | | |
 *   - row 0 [selected]: Frank Rossi | 20 | Lyon \| 20s | Button "Edit" [ref=3]
 *   - rows 10–199 not shown: get_rows ref=1
 *   Footer: | 200 people | | | |
 * </pre>
 *
 * In a cell, a component a user acts on ({@link ComponentTypes#ACTED_ON}) shows in its short view, type, name and
 * bracket; anything else as its text, cut at {@value #CELL_CAP} characters; parts joined by {@code ; }
 * (D_component_views). {@link #full} shows one component with nothing cut; {@link #rows} reads a grid's rows beyond
 * the view (D_get_rows_scrolls).
 *
 * <h2>Implementation detail</h2>
 * One script, one round-trip: it waits for the page's animations to end and its grids to load, walks the flat tree
 * from {@code <body>}, or from the modal on top, the app's shadow roots in their hosts' places, never a Vaadin
 * component's (D_app_shadow_roots), stamps each component it shows with
 * {@link Refs#ATTRIBUTE} (keeping a stamp already there, so a component keeps its ref), and returns the tree. The
 * Unified API can't list a page's components (R_unified_no_enumeration), hence the escape hatch. A Grid's rows come
 * from its shadow DOM, its {@code <tr>}s, each cell's content the light-DOM element its {@code <slot>} shows
 * (R_vaadin_grid_dom).
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

    /** A Grid shows at most this many of its rows in view: an overview, the rest is {@code get_rows}'. */
    static final int GRID_ROWS = 10;

    /** A run of text in a grid cell longer than this is cut, with {@code …}, outside the full view. */
    private static final int CELL_CAP = 50;

    /**
     * Walks the page from {@code <body>} or the modal on top; from {@code root} alone, every row of a grid in view
     * and every component in a cell named ({@code full}); or reads a grid's rows {@code offset} on ({@code rows}),
     * stamping nothing.
     */
    private static final String SCRIPT = """
            async ({ next, layouts, skipped, fields, inline, titleSlots, logins, expandable, attribute, gridRows,
                     root, full, rows: rowsWanted }) => {
              // A server-side navigation changes the URL after the round-trip that an action waits for
              // (R_flow_navigation_url): with React, once Flow clears its navigation flag; without, in a setTimeout.
              // The Unified API's own actions wait for it; a click through the escape hatch (ShadowParts) doesn't
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
              // the app's own shadow roots, as a template's: not a Vaadin component's, which is its own, as the
              // Unified API's lookups leave it out (D_app_shadow_roots)
              const appShadow = e => e.shadowRoot && !/^(vaadin-|copilot-)/.test(e.localName) ? e.shadowRoot : null;
              // every element of the page, the app's shadow roots' too, each shadow root's right after its host
              const appElements = (root = document) => Array.from(root.querySelectorAll('*'))
                  .flatMap(e => appShadow(e) ? [e, ...appElements(appShadow(e))] : [e]);
              // what shows in an element's place, the flat tree: an app's shadow root's children rather than its
              // own; a <slot>'s, what is slotted into it, else its fallback
              const flatChildNodes = e => e.localName === 'slot' && e.assignedNodes().length > 0 ? e.assignedNodes()
                  : Array.from((appShadow(e) ?? e).childNodes);
              const flatChildren = e => flatChildNodes(e).filter(n => n.nodeType === Node.ELEMENT_NODE);
              // an element at the top of a shadow root is in its host
              const parentTagOf = e => (e.parentElement ?? e.getRootNode().host)?.localName ?? 'body';
              // a grid fetching rows shows them empty; the wait TestBench's GridElement does (R_testbench_grid_element)
              const loading = g => g._dataProviderController?.isLoading() || g.$connector?.hasRootRequestQueue?.()
                  || !!g.$?.items?.querySelector(':scope > tr[loading]:not([hidden])');
              const frame = () => Promise.race([new Promise(resolve => requestAnimationFrame(resolve)), tick(50)]);
              const settle = async g => {
                await tick(0);
                for (let waited = 0; loading(g) && waited < 5000; waited += 10) await tick(10);
                await frame();
              };
              for (const g of appElements().filter(e => e.localName === 'vaadin-grid')) await settle(g);
              // counted once the grids have settled, their cells holding components too; as the Unified API finds an
              // id, in the app's shadow roots too
              let idCounts = null;
              const idUnique = id => {
                if (idCounts === null) {
                  idCounts = new Map();
                  for (const e of appElements()) if (e.id) idCounts.set(e.id, (idCounts.get(e.id) ?? 0) + 1);
                }
                return idCounts.get(id) === 1;
              };
              const isVisible = %s;
              const INLINE = new Set(inline);
              const stamps = rowsWanted == null;
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
              // rendered in its shadow DOM from its i18n (D_login_from_i18n): the app's, which may set a part only,
              // merged over the component's defaults (R_vaadin_login_dom)
              const i18nOf = e => e.__effectiveI18n ?? e.i18n;
              const titleOf = e => e.localName === 'vaadin-login-form' ? str(i18nOf(e)?.form?.title)
                  : str(e.getAttribute('aria-label'))
                    ?? (e.localName in titleSlots
                        ? str(textOf(e.querySelector(`:scope > [slot="${titleSlots[e.localName]}"]`))) : null);
              const loginError = e => str([i18nOf(e)?.errorMessage?.title, i18nOf(e)?.errorMessage?.message]
                  .filter(str).join(': '));
              // as the overlay renders it: its i18n's header over its own property
              const loginDescription = e => i18nOf(e)?.header?.description != null
                  ? str(i18nOf(e).header.description) : str(e.description);
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
              // a Grid as rows (R_vaadin_grid_dom): each cell's content a light-DOM child slotted into its <td>
              const contentOf = td => td?.querySelector('slot')?.assignedElements()[0] ?? null;
              const cellsOf = tr => Array.from(tr?.children ?? [])
                  .filter(td => !td.hidden && !td.matches('[part~="details-cell"]'));
              const selectionOf = g => ({ true: 'multi', false: 'single' })[
                  g.$?.table?.getAttribute('aria-multiselectable')] ?? null;
              // a cell's runs of text, and in their places, in their short view, the components a user acts on; in
              // the full view every component
              const partsOf = content => {
                const parts = [];
                const text = t => {
                  t = t.replace(/\\s+/g, ' ').trim();
                  if (t === '') return;
                  const last = parts[parts.length - 1];
                  if (last?.text !== undefined) last.text += ' ' + t; else parts.push({ text: t });
                };
                const walk = (n, elementsOnly) => {
                  for (const c of flatChildNodes(n)) {
                    if (c.nodeType === Node.TEXT_NODE) {
                      if (!elementsOnly) text(c.textContent);
                      continue;
                    }
                    if (c.nodeType !== Node.ELEMENT_NODE) continue;
                    const tag = c.localName;
                    if (c.hidden || SKIPPED.has(tag) || tag.startsWith('copilot-')) continue;
                    const actedOn = INLINE.has(tag);
                    const nodes = actedOn || full ? visit(c, n.localName, true) : [];
                    nodes.forEach(node => parts.push({ node }));
                    if (actedOn || FIELDS.has(tag)) continue;
                    // a shown component's own text is its name already
                    if (getComputedStyle(c).display === 'contents' || isVisible(c)) walk(c, nodes.length > 0);
                  }
                };
                if (content) walk(content, false);
                return parts;
              };
              // what the app put in a field's prefix and suffix: the components a user acts on, a Send button
              // (D_field_affixes)
              const affixes = field => Array.from(field.children)
                  .filter(c => ['prefix', 'suffix'].includes(c.getAttribute('slot')))
                  .flatMap(c => actedOnIn(c, field.localName));
              const actedOnIn = (e, parentTag) => INLINE.has(e.localName) ? visit(e, parentTag)
                  : e.hidden || SKIPPED.has(e.localName) ? [] : flatChildren(e).flatMap(c => actedOnIn(c, e.localName));
              const rowOf = tr => ({ index: tr.index, selected: tr.hasAttribute('selected'),
                  cells: cellsOf(tr).map(td => partsOf(contentOf(td))) });
              const gridOf = g => {
                const columns = cellsOf(g.$.header.lastElementChild).length;
                // a header or footer row with a cell per column; a row of groups spans columns
                const lineOf = tr => {
                  const tds = cellsOf(tr);
                  if (tr.hidden || tds.length !== columns
                      || tds.some(td => Number(td.getAttribute('colspan') || 1) > 1)) return null;
                  const cells = tds.map(td => {
                    const content = contentOf(td);
                    const sorter = content?.querySelector('vaadin-grid-sorter');
                    return { parts: partsOf(content), sortable: !!sorter, sorted: sorter?.direction ?? null,
                      order: str(sorter?.shadowRoot?.querySelector('[part="order"]')?.textContent) };
                  });
                  return cells.some(c => c.parts.length > 0) ? cells : null;
                };
                const inView = Array.from(g.$.items.children)
                    .filter(tr => !tr.hidden && tr.index >= g._firstVisibleIndex && tr.index <= g._lastVisibleIndex
                        && tr.index < g.size)
                    .sort((a, b) => a.index - b.index);
                return {
                  header: Array.from(g.$.header.children).map(lineOf).filter(Boolean),
                  rows: (full ? inView : inView.slice(0, gridRows)).map(rowOf),
                  footer: Array.from(g.$.footer.children).map(lineOf).filter(Boolean).slice(0, 1)
                };
              };
              const visit = (e, parentTag, short = false) => {
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
                const children = () => short || tag === 'vaadin-grid' ? [] : FIELDS.has(tag) ? affixes(e)
                    : tag === 'vaadin-app-layout' ? regions(e) : flatChildren(e).flatMap(c => visit(c, tag));
                // a FormItem's is the text in its label slot (D_form_item_label)
                const label = tag === 'vaadin-form-item' ? str(textOf(e.querySelector(':scope > [slot=label]')))
                    : custom ? str(e.label) : null;
                // Flow's bootstrap wraps every view in <div id="outlet"><flow-container-root-…>: not the app's
                const bootstrap = tag.startsWith('flow-container-') || (tag === 'div' && e.id === 'outlet');
                // one the user can't see stands aside: what's inside may be seen, as a display: contents footer's
                const shown = !bootstrap && (custom ? !(LAYOUTS.has(tag) && !e.id && !label) : !!(e.id || text))
                    && isVisible(e);
                if (!shown) return children();
                let ref = null;
                if (stamps) {
                  ref = e.getAttribute(attribute);
                  if (ref === null) {
                    ref = String(next++);
                    e.setAttribute(attribute, ref);
                  }
                }
                const grid = tag === 'vaadin-grid';
                return [{
                  tag, ref: ref === null ? null : Number(ref), text: str(text),
                  id: str(e.id), idUnique: !!e.id && idUnique(e.id),
                  testId: str(e.getAttribute('data-testid')),
                  label,
                  title: tag in titleSlots || LOGINS.has(tag) ? titleOf(e) : null,
                  description: tag === 'vaadin-login-overlay' ? loginDescription(e) : null,
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
                  rows: grid ? e.size ?? 0 : null,
                  // no count of the selected: the browser has only those among the rows it loaded (R_vaadin_grid_dom)
                  selection: grid ? selectionOf(e) : null,
                  grid: grid && !short ? gridOf(e) : null,
                  children: children()
                }];
              };
              // get_rows: a page of rendered rows at a time, scrolled to as TestBench does (R_testbench_grid_element)
              if (rowsWanted != null) {
                const end = Math.min(rowsWanted.offset + rowsWanted.length, root.size);
                const read = [];
                for (let i = rowsWanted.offset; i < end;) {
                  root.scrollToIndex(i);
                  await settle(root);
                  const rendered = Array.from(root.$.items.children)
                      .filter(tr => !tr.hidden && tr.index >= i && tr.index < end).sort((a, b) => a.index - b.index);
                  const from = i;
                  for (const tr of rendered) {
                    if (tr.index !== i) break;
                    read.push(rowOf(tr));
                    i++;
                  }
                  if (i === from) throw new Error(`row ${i} didn't render after scrolling to it`);
                }
                root.scrollToIndex(rowsWanted.offset);
                await settle(root);
                return { next, size: root.size, rows: read };
              }
              if (root) {
                // visited before next is read: the visit advances it
                const nodes = visit(root, parentTagOf(root));
                return { next, nodes };
              }
              // a modal opened over another is after it in the DOM: Flow appends it to the body, or puts it inside
              // the modal that opened it
              const modals = appElements().filter(e => e.matches('[aria-modal=true]') && e.opened !== false && !e.hidden)
                  .reverse();
              const nodes = modals.length > 0 ? visit(modals[0], parentTagOf(modals[0]))
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
     * Snapshots the page and makes its refs the session's valid ones. A page an action started loading is waited for
     * first ({@link PageLoads}).
     *
     * @return the snapshot's text, starting with the browser's URL; {@link #NO_PAGE} before any navigation
     */
    static String take(UnifiedPage page, Refs refs) {
        Page browser = page.unwrap(Page.class);
        PageLoads.of(browser).await();
        if (isBlank(browser)) {
            return NO_PAGE + "\n";
        }
        Map<?, ?> result = evaluate(page, refs, null, false, null);
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
        render(nodes, 0, out, targets, false);
        refs.replace((String) result.get("url"), modals.isEmpty() ? null : modals.get(0), targets, next(result));
        return out.toString();
    }

    /**
     * The component the latest snapshot showed as {@code ref}, alone and in its full view: as in the snapshot, but a
     * Grid with every row in view and every component in its cells in its short view. Its refs join the session's,
     * the page's other refs staying valid.
     *
     * @throws com.github.mvysny.tinymcpserver.MCPErrorResponseException as {@link Refs#resolve(UnifiedPage, int)}
     */
    static String full(UnifiedPage page, Refs refs, int ref) {
        Map<?, ?> result = evaluate(page, refs, refs.resolve(page, ref).element(), true, null);
        StringBuilder out = new StringBuilder();
        Map<Integer, Refs.Target> targets = new LinkedHashMap<>();
        render((List<?>) result.get("nodes"), 0, out, targets, true);
        refs.add(targets, next(result));
        return out.toString();
    }

    /**
     * A Grid's rows {@code offset} to {@code offset + length - 1}, or to its last, a line each as its snapshot writes
     * them, without refs: reading them scrolls the grid a page at a time, and its cells then show other rows. Leaves
     * the grid scrolled to {@code offset}.
     *
     * @param grid   a {@code vaadin-grid}
     * @param offset one of its rows
     */
    static String rows(UnifiedPage page, Refs refs, UnifiedElement grid, int offset, int length) {
        Map<?, ?> result = evaluate(page, refs, grid, false, Map.of("offset", offset, "length", length));
        StringBuilder out = new StringBuilder();
        renderRows((List<?>) result.get("rows"), 0, out, new LinkedHashMap<>(), false);
        return out.toString();
    }

    /** Runs {@link #SCRIPT} from {@code root}, or the page if {@code null}; {@code rows} reads a grid's rows. */
    private static Map<?, ?> evaluate(UnifiedPage page, Refs refs, @Nullable UnifiedElement root, boolean full,
                                      @Nullable Map<String, Integer> rows) {
        ElementHandle handle = root == null ? null : root.unwrap(Locator.class).elementHandle();
        try {
            Map<String, Object> arg = new HashMap<>();
            arg.put("next", refs.next());
            arg.put("layouts", List.copyOf(ComponentTypes.LAYOUTS));
            arg.put("skipped", List.copyOf(ComponentTypes.SKIPPED));
            arg.put("fields", List.copyOf(ComponentTypes.FIELDS));
            arg.put("inline", List.copyOf(ComponentTypes.ACTED_ON));
            arg.put("titleSlots", ComponentTypes.DIALOG_TITLE_SLOTS);
            arg.put("logins", List.copyOf(ComponentTypes.LOGINS));
            arg.put("expandable", List.copyOf(ComponentTypes.EXPANDABLE));
            arg.put("attribute", Refs.ATTRIBUTE);
            arg.put("gridRows", GRID_ROWS);
            arg.put("root", handle);
            arg.put("full", full);
            arg.put("rows", rows);
            return (Map<?, ?>) page.unwrap(Page.class).evaluate(SCRIPT, arg);
        } finally {
            if (handle != null) {
                handle.dispose();
            }
        }
    }

    private static int next(Map<?, ?> result) {
        return ((Number) result.get("next")).intValue();
    }

    /** @param full whether a Grid shows in its full view: every row in view, every component in a cell named */
    private static void render(List<?> nodes, int depth, StringBuilder out, Map<Integer, Refs.Target> targets,
                               boolean full) {
        for (Object o : nodes) {
            Map<?, ?> node = (Map<?, ?>) o;
            if (node.get("region") != null) {
                renderRegion(node, depth, out, targets, full);
                continue;
            }
            out.append("  ".repeat(depth)).append("- ").append(shortView(node, targets));
            List<String> actions = new ArrayList<>(type(node).actions());
            if (Boolean.TRUE.equals(node.get("expandable"))) {
                actions.add("toggle_expand");
            }
            if (!actions.isEmpty()) {
                out.append(" actions: ").append(String.join(", ", actions));
            }
            out.append('\n');
            if (node.get("grid") != null) {
                renderGrid(node, depth + 1, out, targets, full);
            }
            render((List<?>) node.get("children"), depth + 1, out, targets, full);
        }
    }

    /**
     * A component's short view, its type, name and bracket, {@code Button "Edit" [ref=3]}; the bracket left out when
     * empty, a row read by {@code get_rows} stamping no ref.
     */
    private static String shortView(Map<?, ?> node, Map<Integer, Refs.Target> targets) {
        ComponentTypes.Type type = type(node);
        String id = (String) node.get("id");
        StringBuilder out = new StringBuilder(type.flowName());
        List<String> bracket = new ArrayList<>();
        if (node.get("ref") != null) {
            int ref = ((Number) node.get("ref")).intValue();
            targets.put(ref, new Refs.Target(type.flowName(), Boolean.TRUE.equals(node.get("idUnique")) ? id : null));
            bracket.add("ref=" + ref);
        }
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
        if (node.get("rows") != null) {
            bracket.add("rows=" + ((Number) node.get("rows")).intValue());
        }
        if (node.get("selection") != null) {
            bracket.add("selection=" + node.get("selection"));
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
        if (!bracket.isEmpty()) {
            out.append(" [").append(String.join(", ", bracket)).append(']');
        }
        return out.toString();
    }

    private static ComponentTypes.Type type(Map<?, ?> node) {
        return ComponentTypes.of((String) node.get("tag"), Boolean.TRUE.equals(node.get("multiple")));
    }

    /**
     * A Grid's lines under its own: {@code Header:} lines, its rows, those not shown, its {@code Footer:} line.
     *
     * <pre>
     *   Header: | Name ↑ | Age | Actions |
     *   - row 0 [selected]: Frank Rossi | 20 | Button "Edit" [ref=3]
     *   - rows 10–199 not shown: get_rows ref=1
     *   Footer: | 200 people | | |
     * </pre>
     */
    private static void renderGrid(Map<?, ?> node, int depth, StringBuilder out, Map<Integer, Refs.Target> targets,
                                   boolean full) {
        Map<?, ?> grid = (Map<?, ?>) node.get("grid");
        String indent = "  ".repeat(depth);
        for (Object line : (List<?>) grid.get("header")) {
            out.append(indent).append("Header: ").append(headerLine((List<?>) line, targets, full)).append('\n');
        }
        List<?> rows = (List<?>) grid.get("rows");
        renderRows(rows, depth, out, targets, full);
        String notShown = notShown(rows, ((Number) node.get("rows")).intValue());
        if (notShown != null) {
            out.append(indent).append("- ").append(notShown).append(" not shown: get_rows ref=")
                    .append(((Number) node.get("ref")).intValue()).append('\n');
        }
        for (Object line : (List<?>) grid.get("footer")) {
            out.append(indent).append("Footer: ").append(headerLine((List<?>) line, targets, full)).append('\n');
        }
    }

    /** {@code - row 0 [selected]: Frank Rossi | 20 | | Button "Edit" [ref=3]}, a line per row. */
    private static void renderRows(List<?> rows, int depth, StringBuilder out, Map<Integer, Refs.Target> targets,
                                   boolean full) {
        for (Object o : rows) {
            Map<?, ?> row = (Map<?, ?>) o;
            out.append("  ".repeat(depth)).append("- row ").append(((Number) row.get("index")).intValue());
            if (Boolean.TRUE.equals(row.get("selected"))) {
                out.append(" [selected]");
            }
            out.append(':');
            List<?> cells = (List<?>) row.get("cells");
            for (int i = 0; i < cells.size(); i++) {
                if (i > 0) {
                    out.append(" |");
                }
                String text = cell((List<?>) cells.get(i), targets, full);
                if (!text.isEmpty()) {
                    out.append(' ').append(text);
                }
            }
            out.append('\n');
        }
    }

    /**
     * {@code | Name ↑2 | Age ⇅ | |}: a header or footer row, a sorted column with its arrow and its order, one that
     * sorts with {@code ⇅}.
     */
    private static String headerLine(List<?> cells, Map<Integer, Refs.Target> targets, boolean full) {
        StringBuilder line = new StringBuilder("|");
        for (Object o : cells) {
            Map<?, ?> cell = (Map<?, ?>) o;
            String sorted = (String) cell.get("sorted");
            String arrow = sorted != null ? (sorted.equals("asc") ? " ↑" : " ↓")
                    + (cell.get("order") == null ? "" : cell.get("order"))
                    : Boolean.TRUE.equals(cell.get("sortable")) ? " ⇅" : "";
            String text = (cell((List<?>) cell.get("parts"), targets, full) + arrow).strip();
            line.append(text.isEmpty() ? " |" : " " + text + " |");
        }
        return line.toString();
    }

    /**
     * A cell's parts joined by {@code ; }: runs of text, cut at {@value #CELL_CAP} characters outside the full view,
     * and components in their short view; each {@code |} in it written {@code \|}, the cells' own separator.
     */
    private static String cell(List<?> parts, Map<Integer, Refs.Target> targets, boolean full) {
        List<String> shown = new ArrayList<>();
        for (Object o : parts) {
            Map<?, ?> part = (Map<?, ?>) o;
            String text = (String) part.get("text");
            String shownPart = text != null ? (full ? text : cap(text, CELL_CAP))
                    : shortView((Map<?, ?>) part.get("node"), targets);
            shown.add(shownPart.replace("|", "\\|"));
        }
        return String.join("; ", shown);
    }

    /**
     * {@code rows 0–49, 60–199}: the rows around those shown, {@code row 199} for one; {@code null} when every row is
     * shown.
     */
    private static @Nullable String notShown(List<?> shown, int size) {
        if (shown.size() == size) {
            return null;
        }
        List<int[]> ranges = new ArrayList<>();
        if (shown.isEmpty()) {
            ranges.add(new int[] {0, size - 1});
        } else {
            int first = ((Number) ((Map<?, ?>) shown.get(0)).get("index")).intValue();
            int last = ((Number) ((Map<?, ?>) shown.get(shown.size() - 1)).get("index")).intValue();
            if (first > 0) {
                ranges.add(new int[] {0, first - 1});
            }
            if (last < size - 1) {
                ranges.add(new int[] {last + 1, size - 1});
            }
        }
        boolean one = ranges.size() == 1 && ranges.get(0)[0] == ranges.get(0)[1];
        return (one ? "row " : "rows ") + String.join(", ", ranges.stream()
                .map(r -> r[0] == r[1] ? String.valueOf(r[0]) : r[0] + "–" + r[1]).toList());
    }

    /** {@code - Navbar:} with what it holds under it; a closed drawer as {@code - Drawer: closed; click …}. */
    private static void renderRegion(Map<?, ?> node, int depth, StringBuilder out, Map<Integer, Refs.Target> targets,
                                     boolean full) {
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
        render((List<?>) node.get("children"), depth + 1, out, targets, full);
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
