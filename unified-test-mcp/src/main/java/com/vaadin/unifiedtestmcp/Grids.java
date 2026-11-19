package com.vaadin.unifiedtestmcp;

import com.github.mvysny.tinymcpserver.MCPErrorResponseException;
import com.vaadin.testing.unified.UnifiedElement;
import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedGrid;

/**
 * A Grid a tool names by its ref, and one of its rows by its number from 0, as the snapshot shows them:
 *
 * <pre>{@code
 * UnifiedGrid grid = Grids.resolve(page, refs, 1, "click with row clicks a Grid's row; drop row to click it itself.");
 * Grids.checkRow(1, 250, grid.getRowCount());   // throws: ref 1 has 200 rows, 0–199: no row 250.
 * }</pre>
 */
final class Grids {
    private Grids() {}

    /**
     * @param why the sentence after {@code ref 5 is a Button, not a Grid:}, saying what the tool does with a Grid
     * @throws MCPErrorResponseException if the ref's component isn't a Grid, or as {@link Refs#resolve(UnifiedPage, int)}
     */
    static UnifiedGrid resolve(UnifiedPage page, Refs refs, int ref, String why) {
        UnifiedElement element = refs.resolve(page, ref).element();
        if (!element.getTag().equals("vaadin-grid")) {
            throw new MCPErrorResponseException("ref " + ref + " is a " + ComponentTypes.of(element).flowName()
                    + ", not a Grid: " + why);
        }
        return refs.resolve(page, ref, UnifiedGrid.class);
    }

    /** @throws MCPErrorResponseException if the grid of {@code size} rows has no row {@code row} */
    static void checkRow(int ref, int row, int size) {
        if (size == 0) {
            throw new MCPErrorResponseException("ref " + ref + " is a Grid with no rows.");
        }
        if (row < 0 || row >= size) {
            throw new MCPErrorResponseException("ref " + ref + " has " + size + " rows, 0–" + (size - 1) + ": no row "
                    + row + ".");
        }
    }
}
