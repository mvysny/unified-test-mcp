package com.vaadin.unifiedtestmcp.testapp;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.combobox.dataview.ComboBoxListDataView;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.Route;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * At {@code choices}: a field of each kind whose value is an item, each showing it through an item label
 * generator — so the browser holds the item's key, and the user sees the generated label:
 *
 * <pre>
 * Country   [Czechia ▾]            (ComboBox, id country; its items are Country records)
 * Visited   [Finland] [Sweden]     (MultiSelectComboBox)
 * Home      ( ) Finland (•) Czechia ( ) Sweden     (RadioButtonGroup)
 * Favourite [Sweden ▾]             (Select)
 * Next      [        ▾]            (ComboBox, nothing selected)
 * Dream     [        ▾]            (ComboBox, nothing selected; a country typed in becomes an item, selected)
 * </pre>
 */
@Route("choices")
public class ChoicesView extends VerticalLayout {
    record Country(String code, String name) {}

    private static final Country FINLAND = new Country("fi", "Finland");
    private static final Country CZECHIA = new Country("cz", "Czechia");
    private static final Country SWEDEN = new Country("se", "Sweden");
    private static final List<Country> COUNTRIES = List.of(FINLAND, CZECHIA, SWEDEN);

    public ChoicesView() {
        ComboBox<Country> country = new ComboBox<>("Country", COUNTRIES);
        country.setItemLabelGenerator(Country::name);
        country.setValue(CZECHIA);
        country.setId("country");

        MultiSelectComboBox<Country> visited = new MultiSelectComboBox<>("Visited", COUNTRIES);
        visited.setItemLabelGenerator(Country::name);
        visited.setValue(new LinkedHashSet<>(List.of(FINLAND, SWEDEN))); // Set.of's order differs per run

        RadioButtonGroup<Country> home = new RadioButtonGroup<>("Home", COUNTRIES);
        home.setItemLabelGenerator(Country::name);
        home.setValue(CZECHIA);

        Select<Country> favourite = new Select<>("Favourite", e -> {});
        favourite.setItems(COUNTRIES);
        favourite.setItemLabelGenerator(Country::name);
        favourite.setValue(SWEDEN);

        ComboBox<Country> next = new ComboBox<>("Next", COUNTRIES);
        next.setItemLabelGenerator(Country::name);

        ComboBox<Country> dream = new ComboBox<>("Dream");
        dream.setItemLabelGenerator(Country::name);
        ComboBoxListDataView<Country> dreams = dream.setItems(new ArrayList<>(COUNTRIES));
        dream.addCustomValueSetListener(e -> {
            Country typed = new Country(e.getDetail().toLowerCase(Locale.ROOT), e.getDetail());
            dreams.addItem(typed);
            dream.setValue(typed);
        });

        add(country, visited, home, favourite, next, dream);
    }
}
