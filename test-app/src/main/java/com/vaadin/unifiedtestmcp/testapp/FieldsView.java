package com.vaadin.unifiedtestmcp.testapp;

import com.vaadin.flow.component.HasEnabled;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.checkbox.Switch;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.listbox.MultiSelectListBox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.slider.DecimalSlider;
import com.vaadin.flow.component.slider.IntegerRangeSlider;
import com.vaadin.flow.component.slider.IntegerRangeSliderValue;
import com.vaadin.flow.component.slider.IntegerSlider;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * At {@code fields}: one of each Vaadin field in {@code vaadin-core}, each holding a value, inside a layout with the id
 * {@code fields}. The parameter puts all of them in one state:
 *
 * <pre>
 * fields                   enabled, editable
 * fields/disabled          each field disabled
 * fields/readonly          each field read-only
 * fields/disabled-parent   the layout disabled, the fields left as they are
 * </pre>
 */
@Route("fields")
public class FieldsView extends VerticalLayout implements HasUrlParameter<String> {
    private final VerticalLayout fields = new VerticalLayout();

    public FieldsView() {
        fields.setId("fields");

        TextArea notes = new TextArea("Notes");
        notes.setValue("Line one\nLine two");

        IntegerField age = new IntegerField("Age");
        age.setValue(42);

        NumberField weight = new NumberField("Weight");
        weight.setValue(72.5);

        BigDecimalField price = new BigDecimalField("Price");
        price.setValue(new BigDecimal("9.90"));

        CheckboxGroup<String> toppings = new CheckboxGroup<>("Toppings", "Cheese", "Ham", "Olives");
        toppings.setValue(new LinkedHashSet<>(List.of("Cheese", "Olives")));

        ListBox<String> size = new ListBox<>();
        size.setItems("Small", "Medium", "Large");
        size.setValue("Medium");
        size.setAriaLabel("Size");

        MultiSelectListBox<String> days = new MultiSelectListBox<>();
        days.setItems("Mon", "Tue", "Wed");
        days.setValue(new LinkedHashSet<>(List.of("Mon", "Wed")));
        days.setAriaLabel("Days");

        IntegerSlider volume = new IntegerSlider("Volume", 0, 100);
        volume.setValue(30);

        DecimalSlider ratio = new DecimalSlider("Ratio", 0, 1);
        ratio.setStep(0.1);
        ratio.setValue(0.5);

        IntegerRangeSlider hours = new IntegerRangeSlider("Hours", 0, 24);
        hours.setValue(new IntegerRangeSliderValue(8, 17));

        PasswordField password = new PasswordField("Password");
        password.setValue("secret");

        EmailField email = new EmailField("Email");
        email.setValue("alice@example.com");

        fields.add(new TextField("Text", "Alice", ""), notes, password, email, age, weight, price,
                new DatePicker("Born", LocalDate.of(2000, 1, 31)), new TimePicker("Alarm", LocalTime.of(7, 30)),
                new DateTimePicker("Meeting", LocalDateTime.of(2026, 11, 17, 10, 0)),
                new Checkbox("Agree", true), new Switch("Dark mode", true), toppings, size, days,
                volume, ratio, hours, new PhoneField());
        add(fields);
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter @Nullable String state) {
        switch (state == null ? "" : state) {
            case "disabled" -> fields.getChildren().forEach(c -> ((HasEnabled) c).setEnabled(false));
            case "readonly" -> fields.getChildren().forEach(c -> ((HasValue<?, ?>) c).setReadOnly(true));
            case "disabled-parent" -> fields.setEnabled(false);
            default -> {}
        }
    }

    /** A CustomField of two TextFields, its value the two joined: {@code "+420 123456"}. */
    static class PhoneField extends CustomField<String> {
        private final TextField prefix = new TextField("Prefix", "+420", "");
        private final TextField number = new TextField("Number", "123456", "");

        PhoneField() {
            setLabel("Phone");
            add(prefix, number);
        }

        @Override
        protected String generateModelValue() {
            return prefix.getValue() + " " + number.getValue();
        }

        @Override
        protected void setPresentationValue(String value) {
            String[] parts = value.split(" ", 2);
            prefix.setValue(parts[0]);
            number.setValue(parts.length > 1 ? parts[1] : "");
        }
    }
}
