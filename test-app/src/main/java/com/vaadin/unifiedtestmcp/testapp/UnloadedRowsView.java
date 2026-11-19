package com.vaadin.unifiedtestmcp.testapp;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridMultiSelectionModel;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.router.Route;

import java.util.stream.Stream;

/**
 * At {@code unloaded-rows}: grids whose selection or size the server set beyond the rows the browser has loaded.
 *
 * <pre>
 * Preselected   200 people, single select, row 150 selected before the grid first shows
 *               [Select row 120]       selects row 120 on the server, out of view
 * Everyone      200 people, multi select
 *               [Select row 180]       selects row 180 on the server, out of view
 *               [Select all]           the server's selectAll(), no row loaded beyond the first page
 * Uncounted     a lazy grid of 1000 people whose data provider gives no count
 * Counted       the same 1000, with a count
 * </pre>
 */
@Route("unloaded-rows")
public class UnloadedRowsView extends VerticalLayout {
    private static final int LAZY_ROWS = 1000;

    public UnloadedRowsView() {
        Grid<GridView.Person> preselected = people("preselected");
        preselected.select(GridView.PEOPLE.get(150));
        Button selectOne = new Button("Select row 120", e -> preselected.select(GridView.PEOPLE.get(120)));

        Grid<GridView.Person> everyone = people("everyone");
        GridMultiSelectionModel<GridView.Person> model =
                (GridMultiSelectionModel<GridView.Person>) everyone.setSelectionMode(Grid.SelectionMode.MULTI);
        Button selectRow = new Button("Select row 180", e -> everyone.select(GridView.PEOPLE.get(180)));
        Button selectAll = new Button("Select all", e -> model.selectAll());

        Grid<GridView.Person> uncounted = grid("uncounted");
        uncounted.setItems(UnloadedRowsView::fetch);

        Grid<GridView.Person> counted = grid("counted");
        counted.setItems(UnloadedRowsView::fetch, query -> LAZY_ROWS);

        add(new H3("Preselected"), preselected, selectOne, new H3("Everyone"), everyone, selectRow, selectAll,
                new H3("Uncounted"), uncounted, new H3("Counted"), counted);
    }

    /** A grid of {@link GridView#PEOPLE}, all 200 in memory. */
    private static Grid<GridView.Person> people(String id) {
        Grid<GridView.Person> grid = grid(id);
        grid.setItems(GridView.PEOPLE);
        return grid;
    }

    /** Name and Age columns, no items yet. */
    private static Grid<GridView.Person> grid(String id) {
        Grid<GridView.Person> grid = new Grid<>();
        grid.setId(id);
        grid.addColumn(GridView.Person::name).setHeader("Name");
        grid.addColumn(GridView.Person::age).setHeader("Age");
        return grid;
    }

    /** People 0 to 999, a page at a time, as a database would hand them out. */
    private static Stream<GridView.Person> fetch(Query<GridView.Person, Void> query) {
        int end = Math.min(query.getOffset() + query.getLimit(), LAZY_ROWS);
        return Stream.iterate(query.getOffset(), i -> i < end, i -> i + 1)
                .map(i -> new GridView.Person("Person " + i, 20 + i % 45, "Turku"));
    }
}
