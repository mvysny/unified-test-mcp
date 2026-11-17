package com.vaadin.unifiedtestmcp;

import com.github.mvysny.tinymcpserver.MCPErrorResponseException;
import com.vaadin.testing.unified.Findable;
import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedContainer;
import com.vaadin.testing.unified.query.Query;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.TreeSet;

/**
 * One MCP session's refs: the components its latest snapshot named, and how to find each again.
 *
 * <pre>{@code
 * Snapshot.take(page, refs);                   // replaces the refs with the ones it shows
 * refs.resolve(page, 7).element().click();     // the component the snapshot showed as ref=7
 * }</pre>
 *
 * A component with an id unique on the page is found by it, so Flow re-creating its element doesn't break the
 * ref; any other by its stamped {@value #ATTRIBUTE} attribute. Numbers come from a counter that never goes back,
 * not from the page, so a ref from before a page load never names a component after it.
 *
 * <p>Browser-thread-confined.
 */
final class Refs {
    /** The attribute a snapshot stamps on each component it shows, the ref as its value. */
    static final String ATTRIBUTE = "data-mcp-ref";

    /**
     * @param flowName  as the snapshot showed it, {@code Button}
     * @param uniqueId  the component's id, if no other element on the page had it; {@code null} otherwise
     */
    record Target(String flowName, @Nullable String uniqueId) {}

    private @Nullable Map<Integer, Target> latest;
    /** The URL the latest snapshot was of. */
    private String latestUrl = "";
    private int next = 1;

    /** The number the next newly stamped component gets. */
    int next() {
        return next;
    }

    /**
     * Called by a snapshot of the page at {@code url}: these are now the valid refs, and {@code next} the next
     * number to stamp.
     */
    void replace(String url, Map<Integer, Target> refs, int next) {
        latestUrl = url;
        latest = Map.copyOf(refs);
        this.next = next;
    }

    /**
     * The component the latest snapshot showed as {@code ref}, checked to be still on the page.
     *
     * @throws MCPErrorResponseException if there's no snapshot yet, the ref isn't in the latest one, or its
     *         component is gone; each names the next step
     */
    UnifiedContainer resolve(UnifiedPage page, int ref) {
        return resolve(page, ref, UnifiedContainer.class);
    }

    /**
     * The component the latest snapshot showed as {@code ref}, as the Unified API wrapper of its type, for what
     * only that wrapper reads:
     *
     * <pre>{@code
     * refs.resolve(page, 5, UnifiedComboBox.class).getSelectedItem()   // => "Czechia"
     * }</pre>
     *
     * @param type the wrapper of the component's own type; another type finds nothing, and so throws as for a
     *             component that's gone
     * @throws MCPErrorResponseException as {@link #resolve(UnifiedPage, int)}
     */
    <W, Q extends Query<W, Q>> W resolve(UnifiedPage page, int ref, Class<? extends Findable<Q>> type) {
        if (latest == null) {
            throw new MCPErrorResponseException("No refs yet: call snapshot if a page is open, or navigate to the app's URL;"
                    + " then use a ref from the snapshot either returns.");
        }
        Target target = latest.get(ref);
        if (target == null) {
            throw new MCPErrorResponseException("ref " + ref + " isn't in the latest snapshot, of " + latestUrl
                    + (latest.isEmpty() ? ", which showed no components" : "; its refs are " + ranges())
                    + ". A ref from another page needs that page again: navigate to it. Else call snapshot to see"
                    + " the components on this page now.");
        }
        Q query = page.find(type);
        query = target.uniqueId() != null
                ? query.withId(target.uniqueId())
                : query.withAttribute(ATTRIBUTE, String.valueOf(ref));
        if (!query.exists()) {
            throw new MCPErrorResponseException("ref " + ref + " (" + target.flowName()
                    + ") is gone from the page: the app removed it, or re-created it. Call snapshot to see the"
                    + " components on the page now.");
        }
        return query.single();
    }

    /** {@code 1-4, 7, 9-12}. */
    private String ranges() {
        StringBuilder out = new StringBuilder();
        Integer start = null;
        Integer prev = null;
        for (int ref : new TreeSet<>(latest.keySet())) {
            if (prev != null && ref == prev + 1) {
                prev = ref;
                continue;
            }
            appendRange(out, start, prev);
            start = ref;
            prev = ref;
        }
        appendRange(out, start, prev);
        return out.toString();
    }

    private static void appendRange(StringBuilder out, @Nullable Integer start, @Nullable Integer end) {
        if (start == null) {
            return;
        }
        if (!out.isEmpty()) {
            out.append(", ");
        }
        out.append(start).append(start.equals(end) ? "" : "-" + end);
    }
}
