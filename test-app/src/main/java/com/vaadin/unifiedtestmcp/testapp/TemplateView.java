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
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ShadowRoot;
import com.vaadin.flow.router.Route;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
 *     [Search]  [New order]         filters the orders by customer as typing pauses, as Bakery's; opens the dialog
 *   Grid of &lt;order-card&gt;s          each card's text in its shadow root, its status an &lt;order-status-badge
 *                                   id=status&gt;, Delivered a check icon alone; a heading on the first card, a
 *                                   long run of items on the second; a card covers its cell, padding included,
 *                                   as Bakery's
 *   Dialog "New order"              its content an &lt;order-editor&gt;:
 *     Customer [    ]
 *     &lt;order-item-editor&gt; …        [Product ▾] [1]: the ids products and amount in each; an empty one
 *                                   last, a product picked in it re-creating them all, as Bakery's
 *     &lt;buttons-bar&gt;                 [Cancel] [Save]: Flow's light DOM, in its slots left and right
 *   No order saved                  a span with id=status, the view's light DOM in its shadow root's slot
 * </pre>
 */
@Route("template")
@Tag("order-view")
public class TemplateView extends Component {
    record Item(String product, int amount) {}

    /** @param heading what its card opens with, as Bakery's first card of "This week"; {@code null} for none */
    record Order(String customer, String status, List<Item> items, @Nullable String heading) {
        Order(String customer, String status, List<Item> items) {
            this(customer, status, items, null);
        }
    }

    private static final List<String> PRODUCTS = List.of("Strawberry Bun", "Vanilla Cracker", "Blueberry Cheesecake");

    public TemplateView() {
        Span status = new Span("No order saved");
        status.setId("status");
        getElement().appendChild(status.getElement());

        Grid<Order> grid = new Grid<>();
        grid.setId("grid");
        grid.addComponentColumn(OrderCard::new).setHeader("Order");
        GridListDataView<Order> orders = grid.setItems(new ArrayList<>(List.of(
                new Order("Grace Hopper", "Pending", List.of(new Item("Strawberry Bun", 2)),
                        "This week: Fri, Nov 20 – Sun, Nov 22"),
                new Order("Alan Turing", "Ready", List.of(new Item("Vanilla Cracker", 1),
                        new Item("Blueberry Cheesecake", 3), new Item("Strawberry Bun", 2))),
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

    /** A search field and an action button, as Bakery's {@code <search-bar>}, which debounces its search too. */
    @Tag("search-bar")
    static class SearchBar extends Component {
        SearchBar(String action, Consumer<String> onSearch, Runnable onAction) {
            TextField field = new TextField();
            field.setPlaceholder("Search");
            field.setValueChangeMode(ValueChangeMode.LAZY);
            field.addValueChangeListener(e -> onSearch.accept(e.getValue()));
            getElement().attachShadow().appendChild(field.getElement(),
                    new Button(action, e -> onAction.run()).getElement());
        }
    }

    /**
     * An order's heading if any, customer, status and items, all in its shadow root; covering its cell, padding
     * included.
     */
    @Tag("order-card")
    static class OrderCard extends Component {
        OrderCard(Order order) {
            ShadowRoot shadow = getElement().attachShadow();
            if (order.heading() != null) {
                shadow.appendChild(new Span(order.heading()).getElement());
            }
            shadow.appendChild(new Span(order.customer()).getElement(), new StatusBadge(order.status()).getElement(),
                    new Span(items(order)).getElement());
            getElement().getStyle().set("display", "block").set("margin", "-50px").set("padding", "50px");
        }

        /** {@code 2 × Strawberry Bun, 1 × Vanilla Cracker}. */
        static String items(Order order) {
            return order.items().stream().map(item -> item.amount() + " × " + item.product())
                    .collect(Collectors.joining(", "));
        }
    }

    /** A status as its text, but Delivered as a check icon alone, its text hidden, as Bakery's badge. */
    @Tag("order-status-badge")
    static class StatusBadge extends Component {
        StatusBadge(String status) {
            Span text = new Span(status);
            Icon check = VaadinIcon.CHECK.create();
            (status.equals("Delivered") ? text : check).getStyle().set("display", "none");
            setId("status");
            getElement().attachShadow().appendChild(new Div(text, check).getElement());
        }
    }

    /**
     * A new order's customer, its items and the buttons that save it or cancel. As Bakery's, an empty item editor
     * comes last, and a product picked in it re-creates every item's editor, a new empty one after them.
     */
    @Tag("order-editor")
    static class OrderEditor extends Component {
        private final List<Item> items = new ArrayList<>();
        private final Div itemsContainer = new Div();

        OrderEditor(Consumer<Order> onSave, Runnable onCancel) {
            TextField customer = new TextField("Customer");
            customer.setId("customer");
            itemsContainer.setId("items");
            showItems();
            Button save = new Button("Save",
                    e -> onSave.accept(new Order(customer.getValue(), "New", List.copyOf(items))));
            getElement().attachShadow().appendChild(customer.getElement(), itemsContainer.getElement(),
                    new ButtonsBar(new Button("Cancel", e -> onCancel.run()), save).getElement());
        }

        private void showItems() {
            itemsContainer.removeAll();
            for (int i = 0; i < items.size(); i++) {
                int index = i;
                itemsContainer.add(new OrderItemEditor(items.get(i), item -> items.set(index, item)));
            }
            itemsContainer.add(new OrderItemEditor(null, item -> {
                items.add(item);
                showItems();
            }));
        }
    }

    /** A product and its amount; the same ids in every one, as a template's. */
    @Tag("order-item-editor")
    static class OrderItemEditor extends Component {
        /**
         * @param item     {@code null} for an empty editor
         * @param onChange gets the item on each change of its product or amount, once it has a product
         */
        OrderItemEditor(@Nullable Item item, Consumer<Item> onChange) {
            ComboBox<String> product = new ComboBox<>();
            product.setId("products");
            product.setPlaceholder("Product");
            product.setItems(PRODUCTS);
            IntegerField amount = new IntegerField();
            amount.setId("amount");
            product.setValue(item == null ? null : item.product());
            amount.setValue(item == null ? 1 : item.amount());
            Runnable changed = () -> {
                if (product.getValue() != null) {
                    onChange.accept(new Item(product.getValue(), amount.getValue() == null ? 1 : amount.getValue()));
                }
            };
            product.addValueChangeListener(e -> changed.run());
            amount.addValueChangeListener(e -> changed.run());
            getElement().attachShadow().appendChild(product.getElement(), amount.getElement());
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
