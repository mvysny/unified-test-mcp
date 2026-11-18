package com.vaadin.unifiedtestmcp;

import com.github.mvysny.tinymcpserver.InputSchemaBuilder;
import com.github.mvysny.tinymcpserver.MCPErrorResponseException;
import com.github.mvysny.tinymcpserver.MCPProtocol;
import com.github.mvysny.tinymcpserver.ToolDescriptor;
import com.github.mvysny.tinymcpserver.ToolFunction;
import com.github.mvysny.tinymcpserver.ToolRequest;
import com.vaadin.testing.unified.UnifiedElement;

/**
 * {@code toggle_expand(ref)}: expands a collapsed SideNavItem or collapses an expanded one, by a click on its arrow,
 * then answers with the snapshot.
 *
 * <pre>
 * toggle_expand(ref: 7)   → Expanded ref=7
 *
 *                           URL: http://localhost:8080/nav
 *                           …
 *                               - SideNavItem "Archive" [ref=7, path="nav/archive"] actions: click, toggle_expand
 *                                 - SideNavItem "2025" [ref=13, path="nav/archive/2025"] actions: click
 * toggle_expand(ref: 6)   → error: ref 6 is a SideNavItem without items under it: nothing to expand. …
 * </pre>
 *
 * Unlike {@code click}, it doesn't open the item's page (D_nav_item_states). A toggle, as the arrow is: the answer
 * says which way it went. Refused before anything is touched: a component that doesn't expand, an item without
 * items, a disabled one.
 */
final class ToggleExpandTool implements ToolFunction {
    static final ToolDescriptor DESCRIPTOR = new ToolDescriptor("toggle_expand",
            "Expand or collapse a SideNavItem that has items under it, as a click on its arrow does: expands a "
                    + "collapsed one (marked collapsed in the snapshot), collapses an expanded one (its items listed "
                    + "under it). Unlike click, it doesn't open the item's page. Waits until the server has handled "
                    + "it, then says which way it went and returns the new snapshot of the page. The snapshot lists "
                    + "toggle_expand in the actions of the components it fits.",
            new InputSchemaBuilder().requiredInteger("ref", SnapshotTool.REF_DESCRIPTION).build());

    @Override
    public MCPProtocol.Content call(ToolRequest request) {
        int ref = request.arguments().getInt("ref");
        return MCPProtocol.Content.text(ToolCall.get((page, refs) -> {
            UnifiedElement element = refs.resolve(page, ref).element();
            String flowName = ComponentTypes.of(element).flowName();
            if (!ComponentTypes.EXPANDABLE.contains(element.getTag())) {
                throw new MCPErrorResponseException("ref " + ref + " is a " + flowName + ": it doesn't expand or"
                        + " collapse. toggle_expand takes a SideNavItem with items under it; the snapshot lists"
                        + " toggle_expand in its actions.");
            }
            if (element.getAttribute("has-children") == null) {
                throw new MCPErrorResponseException("ref " + ref + " is a " + flowName + " without items under it:"
                        + " nothing to expand." + (element.getAttribute("path") != null ? " Call click to open its page."
                        : ""));
            }
            if (Boolean.TRUE.equals(element.getPropertyBoolean("disabled"))) {
                throw new MCPErrorResponseException("ref " + ref + " is a disabled " + flowName
                        + ": a user can't expand or collapse it.");
            }
            ShadowParts.click(page, element, "toggle-button");
            boolean expanded = Boolean.TRUE.equals(element.getPropertyBoolean("expanded"));
            return (expanded ? "Expanded" : "Collapsed") + " ref=" + ref + "\n\n" + Snapshot.take(page, refs);
        }));
    }
}
