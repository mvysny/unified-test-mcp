package com.vaadin.unifiedtestmcp.testapp;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;

import java.util.Locale;

/**
 * Every UI in {@code fi-FI}, whatever the browser's language: a DatePicker shows {@code 31.1.2000}, a TimePicker
 * {@code 7.30}, a BigDecimalField {@code 9,90}, on any machine. None of them can be mistaken for ISO.
 */
public class FinnishLocale implements VaadinServiceInitListener {
    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(e -> e.getUI().setLocale(Locale.of("fi", "FI")));
    }
}
