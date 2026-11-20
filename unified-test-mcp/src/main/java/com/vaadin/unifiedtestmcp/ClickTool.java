package com.vaadin.unifiedtestmcp;

import com.github.mvysny.tinymcpserver.InputSchemaBuilder;
import com.github.mvysny.tinymcpserver.MCPProtocol;
import com.github.mvysny.tinymcpserver.ToolDescriptor;
import com.github.mvysny.tinymcpserver.ToolFunction;
import com.github.mvysny.tinymcpserver.ToolRequest;
import com.vaadin.testing.unified.UnifiedElement;
import com.vaadin.testing.unified.component.UnifiedGrid;

/**
 * {@code click(ref)}: clicks a component, then answers with the snapshot taken once the server has handled the
 * click.
 *
 * <pre>
 * click(ref: 3)   → Clicked ref=3
 *
 *                   URL: http://localhost:8080/form
 *                   - Span "Saved" [ref=8]
 *                   …
 * </pre>
 *
 * Any component takes a click; the snapshot lists {@code click} among the actions only of those made for it. A
 * SideNavItem is clicked on its link: an expanded one's centre is on its first item (D_nav_item_states).
 *
 * <p>{@code click(ref, row)} clicks a Grid's row, a row taking no ref of its own:
 *
 * <pre>
 * click(ref: 1, row: 150)   → Clicked row 150 of ref=1
 *
 *                             URL: http://localhost:8080/grid
 *                             - Grid [ref=1, rows=200, selection=single]
 *                               …
 *                               - row 150 [selected]: Alice Lund | 34 | Turku | Button "Edit" [ref=31]
 * </pre>
 *
 * The Unified API's {@code row(150).click()}: it scrolls the row into view and clicks a spot of it free of
 * components, so it selects the row, or opens what the app opens on a click, and no button in it is hit; a template's
 * card filling the cell is none, judged by what it holds (R_dramafinder_row_click).
 *
 * <p>Why not {@code click(ref, row, target: "Edit")}, a button in a row by its caption, as a blind review asked: every
 * component in a row in view has a ref of its own, and a second way of naming one would undo D_refs_not_ids. A row
 * out of view costs one {@code get_rows} first, whose snapshot has the refs of the rows it scrolled to.
 */
final class ClickTool implements ToolFunction {
    static final ToolDescriptor DESCRIPTOR = new ToolDescriptor("click",
            "Click a component, as a user would. Waits until it is visible and enabled, clicks it, waits until the "
                    + "server has handled the click, then returns the new snapshot of the page. With row, clicks "
                    + "that row of the Grid the ref names, by its number from the snapshot or get_rows (rows take "
                    + "no ref of their own): scrolls the grid to it and clicks a spot of it free of buttons and "
                    + "fields, which selects the row or opens what the app opens on a click.",
            new InputSchemaBuilder().requiredInteger("ref", SnapshotTool.REF_DESCRIPTION)
                    .optionalInteger("row", "A row of the Grid that ref names, numbered from 0 as the snapshot "
                            + "numbers them. Leave out to click the component itself")
                    .build());

    @Override
    public MCPProtocol.Content call(ToolRequest request) {
        int ref = request.arguments().getInt("ref");
        Integer row = request.arguments().getIntOrNull("row");
        if (row != null) {
            return MCPProtocol.Content.text(ToolCall.get((page, refs) -> {
                UnifiedGrid grid = Grids.resolve(page, refs, ref,
                        "row picks a row of a Grid. Leave row out to click the component itself.");
                Grids.checkRow(ref, row, grid.getRowCount());
                grid.row(row).click();
                return "Clicked row " + row + " of ref=" + ref + "\n\n" + Snapshot.take(page, refs);
            }));
        }
        String snapshot = ToolCall.get((page, refs) -> {
            UnifiedElement element = refs.resolve(page, ref).element();
            if (element.getTag().equals("vaadin-side-nav-item")) {
                ShadowParts.click(page, element, "link");
            } else {
                element.click();
            }
            return Snapshot.take(page, refs);
        });
        return MCPProtocol.Content.text("Clicked ref=" + ref + "\n\n" + snapshot);
    }
}
