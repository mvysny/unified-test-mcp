package com.vaadin.unifiedtestmcp;

import com.github.mvysny.tinymcpserver.InputSchemaBuilder;
import com.github.mvysny.tinymcpserver.MCPProtocol;
import com.github.mvysny.tinymcpserver.ToolDescriptor;
import com.github.mvysny.tinymcpserver.ToolFunction;
import com.github.mvysny.tinymcpserver.ToolRequest;
import com.vaadin.testing.unified.component.UnifiedGrid;

import java.util.HashMap;
import java.util.Map;

/**
 * {@code sort(ref, column, direction)}: sorts a Grid by a column, by clicks on its sorter, then answers with the
 * snapshot.
 *
 * <pre>
 * sort(ref: 1, column: "Age", direction: "descending")   → Sorted ref=1 by Age, descending
 *
 *                                                          URL: http://localhost:8080/grid
 *                                                          - Grid [ref=1, rows=200, selection=single]
 *                                                            Header: | Name ⇅ | Age ↓ | City |
 *                                                            …
 * </pre>
 *
 * Clicks the sorter until it shows the direction asked for, as a user does ({@link Grids#sort}). On a grid sorting by
 * several columns, the column comes first, as Vaadin's click puts it; one sorted that way already but not first is
 * clicked round to come first, so "sort by Age" always makes Age the grid's first sort.
 *
 * <p>Why a direction, not a toggle as {@code toggle_expand} is: a sorter has three states, and descending takes two
 * clicks the agent would have to count from the state it saw.
 */
final class SortTool implements ToolFunction {
    /** The sorter's {@code direction} for each; none is {@code null}. */
    private static final Map<String, String> DIRECTIONS = new HashMap<>(Map.of("ascending", "asc",
            "descending", "desc"));

    static final ToolDescriptor DESCRIPTOR = new ToolDescriptor("sort",
            "Sort a Grid's rows by one of its columns, as clicks on the column's header do: ascending, descending, "
                    + "or none to stop sorting by it. The snapshot's Header: line marks a column that sorts with ⇅, "
                    + "a sorted one with ↑ or ↓; on a grid sorting by several columns, with its order too, `Age ↑1`. "
                    + "The column you sort comes first, its order 1, the others after it: to sort by Age, then by "
                    + "Name within an age, sort Name, then Age. A column sorted that way already but not first is "
                    + "clicked round to come first. Waits until the server has sorted, then returns the new snapshot, "
                    + "saying first when the column was so already and nothing was clicked.",
            new InputSchemaBuilder().requiredInteger("ref", "The Grid's ref, from the latest snapshot")
                    .requiredString("column", "The column's caption as its Header: cell shows it, without the "
                            + "marks: Age, not Age ↑")
                    .requiredString("direction", "ascending, descending, or none to stop sorting by it")
                    .withEnum("ascending", "descending", "none")
                    .build());

    @Override
    public MCPProtocol.Content call(ToolRequest request) {
        int ref = request.arguments().getInt("ref");
        String column = request.arguments().getString("column");
        String direction = request.arguments().getString("direction");
        return MCPProtocol.Content.text(ToolCall.get((page, refs) -> {
            UnifiedGrid grid = Grids.resolve(page, refs, ref, "sort sorts a Grid's rows by one of its columns.");
            boolean clicked = Grids.sort(page, grid, ref, column, DIRECTIONS.get(direction));
            String what = direction.equals("none")
                    ? (clicked ? "Stopped sorting ref=" + ref + " by " + column
                            : "ref=" + ref + " didn't sort by " + column + ": nothing clicked")
                    : (clicked ? "Sorted ref=" + ref + " by " + column + ", " + direction
                            : "ref=" + ref + " was sorted by " + column + ", " + direction + ", first already: nothing"
                                    + " clicked");
            return what + "\n\n" + Snapshot.take(page, refs);
        }));
    }
}
