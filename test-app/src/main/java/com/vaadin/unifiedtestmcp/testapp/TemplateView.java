package com.vaadin.unifiedtestmcp.testapp;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * At {@code template}: the app's components inside shadow roots, where a LitTemplate's {@code @Id} fields are; Bakery's
 * storefront in miniature. Each {@code <tag>} has an open shadow root, attached from Java
 * ({@link Element#attachShadow()}): a LitTemplate's DOM without the npm build its {@code @JsModule} would need
 * (R_flow_shadow_root).
 *
 * <pre>
 * &lt;order-view&gt;                     the route
 *   Orders                          an H2
 *   &lt;search-bar id=search&gt;          a template in the template
 *     [Search]  [New order]         filters the orders by customer; opens the dialog
 *   Grid of &lt;order-card&gt;s          each card's text in its shadow root, its status in a span with id=status
 *   Dialog "New order"              its content an &lt;order-editor&gt;:
 *     Customer [    ]
 *     &lt;order-item-editor&gt; ×2       [Product ▾] [1]: the ids products and amount in each
 *     &lt;buttons-bar&gt;                 [Cancel] [Save]: Flow's light DOM, in its slots left and right
 *   No order saved                  a span with id=status, the view's light DOM in its shadow root's slot
 * </pre>
 */
@Route("template")
@Tag("order-view")
public class TemplateView extends Component {
    record Item(String product, int amount) {}

    record Order(String customer, String status, List<Item> items) {}

    private static final List<String> PRODUCTS = List.of("Strawberry Bun", "Vanilla Cracker", "Blueberry Cheesecake");

    public TemplateView() {
        Span status = new Span("No order saved");
        status.setId("status");
        getElement().appendChild(status.getElement());

        Grid<Order> grid = new Grid<>();
        grid.setId("grid");
        grid.addComponentColumn(OrderCard::new).setHeader("Order");
        GridListDataView<Order> orders = grid.setItems(new ArrayList<>(List.of(
                new Order("Grace Hopper", "Pending", List.of(new Item("Strawberry Bun", 2))),
                new Order("Alan Turing", "Ready", List.of(new Item("Vanilla Cracker", 1))),
                new Order("Ada Lovelace", "Delivered", List.of(new Item("Blueberry Cheesecake", 1))))));

        Dialog dialog = new Dialog("New order");
        dialog.add(new OrderEditor(order -> {
            orders.addItem(order);
            status.setText("Saved order for " + order.customer() + ": " + OrderCard.items(order));
            dialog.close();
        }, dialog::close));

        SearchBar search = new SearchBar("New order", text -> orders.setFilter(order ->
                order.customer().toLowerCase(Locale.ROOT).contains(text.toLowerCase(Locale.ROOT))), dialog::open);
        search.setId("search");

        getElement().attachShadow().appendChild(new H2("Orders").getElement(), search.getElement(),
                grid.getElement(), dialog.getElement(), new Element("slot"));
    }

    /** A search field and an action button, as Bakery's {@code <search-bar>}. */
    @Tag("search-bar")
    static class SearchBar extends Component {
        SearchBar(String action, Consumer<String> onSearch, Runnable onAction) {
            TextField field = new TextField();
            field.setPlaceholder("Search");
            field.addValueChangeListener(e -> onSearch.accept(e.getValue()));
            getElement().attachShadow().appendChild(field.getElement(),
                    new Button(action, e -> onAction.run()).getElement());
        }
    }

    /** An order's customer, status and items, all in its shadow root. */
    @Tag("order-card")
    static class OrderCard extends Component {
        OrderCard(Order order) {
            Span status = new Span(order.status());
            status.setId("status");
            getElement().attachShadow().appendChild(new Span(order.customer()).getElement(), status.getElement(),
                    new Span(items(order)).getElement());
        }

        /** {@code 2 × Strawberry Bun, 1 × Vanilla Cracker}. */
        static String items(Order order) {
            return order.items().stream().map(item -> item.amount() + " × " + item.product())
                    .collect(Collectors.joining(", "));
        }
    }

    /** A new order's customer, two items and the buttons that save it or cancel. */
    @Tag("order-editor")
    static class OrderEditor extends Component {
        OrderEditor(Consumer<Order> onSave, Runnable onCancel) {
            TextField customer = new TextField("Customer");
            customer.setId("customer");
            List<OrderItemEditor> items = List.of(new OrderItemEditor(), new OrderItemEditor());
            Div itemsContainer = new Div(items.toArray(Component[]::new));
            itemsContainer.setId("items");
            Button save = new Button("Save", e -> onSave.accept(new Order(customer.getValue(), "New",
                    items.stream().map(OrderItemEditor::item).flatMap(Optional::stream).toList())));
            getElement().attachShadow().appendChild(customer.getElement(), itemsContainer.getElement(),
                    new ButtonsBar(new Button("Cancel", e -> onCancel.run()), save).getElement());
        }
    }

    /** A product and its amount; the same ids in every one, as a template's. */
    @Tag("order-item-editor")
    static class OrderItemEditor extends Component {
        private final ComboBox<String> product = new ComboBox<>();
        private final IntegerField amount = new IntegerField();

        OrderItemEditor() {
            product.setId("products");
            product.setPlaceholder("Product");
            product.setItems(PRODUCTS);
            amount.setId("amount");
            amount.setValue(1);
            getElement().attachShadow().appendChild(product.getElement(), amount.getElement());
        }

        /** The item, if a product is chosen. */
        Optional<Item> item() {
            return Optional.ofNullable(product.getValue())
                    .map(chosen -> new Item(chosen, amount.getValue() == null ? 1 : amount.getValue()));
        }
    }

    /** Two components, each put by its {@code slot} attribute into a {@code <slot>} of the bar's shadow root. */
    @Tag("buttons-bar")
    static class ButtonsBar extends Component {
        ButtonsBar(Component left, Component right) {
            getElement().attachShadow().appendChild(slot("left"), slot("right"));
            left.getElement().setAttribute("slot", "left");
            right.getElement().setAttribute("slot", "right");
            getElement().appendChild(left.getElement(), right.getElement());
        }

        private static Element slot(String name) {
            return new Element("slot").setAttribute("name", name);
        }
    }
}
