package com.vaadin.unifiedtestmcp;

import com.github.mvysny.tinymcpserver.MCPErrorResponseException;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.vaadin.testing.unified.UnifiedElement;
import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedGrid;
import org.jspecify.annotations.Nullable;
import org.vaadin.addons.dramafinder.AutoWait;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A Grid a tool names by its ref, and one of its rows by its number from 0, as the snapshot shows them:
 *
 * <pre>{@code
 * UnifiedGrid grid = Grids.resolve(page, refs, 1, "click with row clicks a Grid's row; drop row to click it itself.");
 * Grids.checkRow(1, 250, grid.getRowCount());   // throws: ref 1 has 200 rows, 0–199: no row 250.
 * Grids.sort(page, grid, 1, "Age", "desc");      // its sorter clicked until it shows descending
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

    /**
     * Clicks the sorter of the column captioned {@code caption} until it shows {@code direction}, as a user does: a
     * sorter goes none, ascending, descending, none, a click each (R_vaadin_grid_dom). Waits for the server after each
     * click, as the Unified API does.
     *
     * <h4>Implementation detail</h4>
     * The sorter, a light-DOM {@code <vaadin-grid-sorter>} holding the caption, clicked through the escape hatch: the
     * Unified API's {@code column(caption).sort(…)} fails on a grid with a header group row (R_unified_grid_headers).
     *
     * @param direction {@code "asc"}, {@code "desc"}, or {@code null} for not sorting by it
     * @throws MCPErrorResponseException if no column of the grid by that caption sorts, naming those that do
     */
    static void sort(UnifiedPage page, UnifiedGrid grid, int ref, String caption, @Nullable String direction) {
        Locator sorters = grid.element().unwrap(Locator.class).locator("vaadin-grid-sorter");
        List<String> captions = sorters.allTextContents().stream().map(String::strip).toList();
        int found = Collections.frequency(captions, caption);
        if (found != 1) {
            throw new MCPErrorResponseException(found == 0
                    ? "ref " + ref + " has no column \"" + caption + "\" that sorts. "
                            + (captions.isEmpty() ? "None of its columns sorts."
                            : "Those that do: " + String.join(", ", captions) + "; the snapshot marks them ⇅, ↑ or ↓.")
                    : "ref " + ref + " has " + found + " columns \"" + caption + "\" that sort: sort can't tell them"
                            + " apart.");
        }
        Locator sorter = sorters.nth(captions.indexOf(caption));
        for (int clicks = 0; clicks < 3 && !Objects.equals(sorter.evaluate("s => s.direction"), direction); clicks++) {
            sorter.click();
            AutoWait.afterAction(page.unwrap(Page.class));
        }
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
