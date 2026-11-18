package com.vaadin.unifiedtestmcp.testapp;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalDataProvider;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.router.Route;

import java.util.List;
import java.util.stream.IntStream;

/**
 * At {@code grid}: a Grid with more rows than it shows, and a TreeGrid.
 *
 * <pre>
 * People: Name ⇅ | Age ⇅ | City | Actions      200 rows, single select; each row's [Edit] sets the status
 * Departments: Department | People             a TreeGrid, multi select: Engineering ▸ Backend, Frontend; Sales ▸ Nordics
 * Status: Nothing selected
 * </pre>
 */
@Route("grid")
public class GridView extends VerticalLayout {
    record Person(String name, int age, String city) {}

    record Department(String name, int people, List<Department> parts) {}

    private static final List<String> FIRST = List.of("Alice", "Bob", "Carol", "David", "Eve", "Frank", "Grace",
            "Heidi", "Ivan", "Judy");
    private static final List<String> LAST = List.of("Novak", "Smith", "Virtanen", "Berg", "Rossi", "Kowalski",
            "Dubois", "Jansen", "Horvat", "Silva", "Meyer", "Lund", "Costa", "Weber", "Nagy", "Popescu", "Moreau",
            "Fischer", "Laine", "Ortiz");
    private static final List<String> CITIES = List.of("Turku", "Brno", "Oslo", "Lyon", "Porto", "Graz");

    static final List<Person> PEOPLE = IntStream.range(0, 200)
            .mapToObj(i -> new Person(FIRST.get(i % FIRST.size()) + " " + LAST.get(i / FIRST.size() % LAST.size()),
                    20 + i * 7 % 45, CITIES.get(i % CITIES.size())))
            .toList();

    private static final List<Department> DEPARTMENTS = List.of(
            new Department("Engineering", 12, List.of(new Department("Backend", 7, List.of()),
                    new Department("Frontend", 5, List.of()))),
            new Department("Sales", 4, List.of(new Department("Nordics", 4, List.of()))));

    public GridView() {
        Span status = new Span("Nothing selected");

        Grid<Person> people = new Grid<>();
        people.addColumn(Person::name).setHeader("Name").setSortable(true);
        people.addColumn(Person::age).setHeader("Age").setSortable(true);
        people.addColumn(Person::city).setHeader("City");
        people.addComponentColumn(person -> new Button("Edit", e -> status.setText("Editing " + person.name())))
                .setHeader("Actions");
        people.setItems(PEOPLE);
        people.asSingleSelect().addValueChangeListener(e ->
                status.setText(e.getValue() == null ? "Nothing selected" : "Selected " + e.getValue().name()));

        TreeGrid<Department> departments = new TreeGrid<>();
        departments.addHierarchyColumn(Department::name).setHeader("Department");
        departments.addColumn(Department::people).setHeader("People");
        departments.setDataProvider(new TreeDataProvider<>(
                new TreeData<Department>().addItems(DEPARTMENTS, Department::parts),
                HierarchicalDataProvider.HierarchyFormat.FLATTENED));
        departments.setSelectionMode(Grid.SelectionMode.MULTI);

        add(people, departments, status);
    }
}
