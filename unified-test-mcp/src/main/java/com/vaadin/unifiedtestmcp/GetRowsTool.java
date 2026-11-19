package com.vaadin.unifiedtestmcp;

import com.github.mvysny.tinymcpserver.InputSchemaBuilder;
import com.github.mvysny.tinymcpserver.MCPErrorResponseException;
import com.github.mvysny.tinymcpserver.MCPProtocol;
import com.github.mvysny.tinymcpserver.ToolDescriptor;
import com.github.mvysny.tinymcpserver.ToolFunction;
import com.github.mvysny.tinymcpserver.ToolRequest;
import com.vaadin.testing.unified.component.UnifiedGrid;

/**
 * {@code get_rows(ref, offset, length)}: a Grid's rows from {@code offset} on, beyond the 10 the snapshot shows, as
 * the snapshot writes them; then the snapshot, the grid scrolled to {@code offset}.
 *
 * <pre>
 * get_rows(ref: 1, offset: 150, length: 2)   → Rows 150–151 of 200 in ref=1, the grid now scrolled to row 150:
 *                                              - row 150: Alice Lund | 34 | Turku | Button "Edit"
 *                                              - row 151: Bob Lund | 41 | Brno | Button "Edit"
 *
 *                                              URL: http://localhost:8080/grid
 *                                              …
 * </pre>
 *
 * The rows read take no refs: reading scrolls the grid a page at a time, and its cells then show other rows. The
 * snapshot after them has the refs of the rows in view, {@code offset} first.
 */
final class GetRowsTool implements ToolFunction {
    /** Rows one call reads at most: a long read scrolls the grid page by page. */
    static final int MAX_LENGTH = 500;

    static final ToolDescriptor DESCRIPTOR = new ToolDescriptor("get_rows",
            "Read a Grid's rows, those beyond the up to 10 the snapshot shows: from row offset, numbered from 0 as "
                    + "in the snapshot, length rows or up to the last, at most " + MAX_LENGTH + " a call. One line "
                    + "each, as the snapshot writes them: `- row 150 [selected]: Alice Lund | 34 | Button \"Edit\"`, "
                    + "cells joined by ` | `, a component a user acts on with its type, anything else as its text. "
                    + "Scrolls the grid to read them, as a user would, and leaves it scrolled to offset; then returns "
                    + "the new snapshot, whose rows in view carry their components' refs. The rows read here don't: "
                    + "once scrolled on, the grid shows other rows in those cells.",
            new InputSchemaBuilder().requiredInteger("ref", "The Grid's ref, from the latest snapshot")
                    .requiredInteger("offset", "The first row to read, numbered from 0").withMinimum(0)
                    .requiredInteger("length", "How many rows to read, at most " + MAX_LENGTH).withMinimum(1)
                    .withMaximum(MAX_LENGTH)
                    .build());

    @Override
    public MCPProtocol.Content call(ToolRequest request) {
        int ref = request.arguments().getInt("ref");
        int offset = request.arguments().getInt("offset");
        int length = request.arguments().getInt("length");
        if (length < 1 || length > MAX_LENGTH) {
            throw new MCPErrorResponseException("length " + length + ": get_rows reads 1 to " + MAX_LENGTH
                    + " rows a call; read more by calling it again from the next offset.");
        }
        return MCPProtocol.Content.text(ToolCall.get((page, refs) -> {
            UnifiedGrid grid = Grids.resolve(page, refs, ref, "get_rows reads a Grid's rows.");
            int size = grid.getRowCount();
            Grids.checkRow(ref, offset, size);
            String rows = Snapshot.rows(page, refs, grid.element(), offset, length);
            int last = Math.min(offset + length, size) - 1;
            return "Rows " + (offset == last ? String.valueOf(offset) : offset + "–" + last) + " of " + size
                    + " in ref=" + ref + ", the grid now scrolled to row " + offset + ":\n" + rows + "\n"
                    + Snapshot.take(page, refs);
        }));
    }
}
