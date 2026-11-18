package com.vaadin.unifiedtestmcp.testapp;

import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.masterdetaillayout.MasterDetailLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

/**
 * At {@code disclosure}: containers that show part of what they hold.
 *
 * <pre>
 * ▸ Shipping                Details, closed: Street [    ]
 * ▾ Billing                 Details, open: Account [    ]
 * Accordion: ▾ Personal: Name [    ]   ▸ Payment: Card number [    ]
 * Tabs: [Overview] Settings             bare Tabs; selecting one sets the status
 * TabSheet: [Info] Advanced              Info: "Version 1.0"; Advanced: Proxy [    ]
 * SplitLayout: Left pane | Right pane
 * MasterDetailLayout: [Show detail]      the detail, Detail [    ], appears on click
 * Status: Overview
 * </pre>
 */
@Route("disclosure")
public class DisclosureView extends VerticalLayout {
    public DisclosureView() {
        Span status = new Span("Overview");

        Details shipping = new Details("Shipping", new TextField("Street"));
        Details billing = new Details("Billing", new TextField("Account"));
        billing.setOpened(true);

        Accordion accordion = new Accordion();
        accordion.add("Personal", new TextField("Name"));
        accordion.add("Payment", new TextField("Card number"));

        Tabs tabs = new Tabs(new Tab("Overview"), new Tab("Settings"));
        tabs.addSelectedChangeListener(e -> status.setText(e.getSelectedTab().getLabel()));

        TabSheet sheet = new TabSheet();
        sheet.add("Info", new Span("Version 1.0"));
        sheet.add("Advanced", new TextField("Proxy"));

        SplitLayout split = new SplitLayout(new Span("Left pane"), new Span("Right pane"));

        MasterDetailLayout masterDetail = new MasterDetailLayout();
        masterDetail.setMaster(new Button("Show detail", e -> masterDetail.setDetail(new TextField("Detail"))));
        masterDetail.setHeight("150px");

        add(shipping, billing, accordion, tabs, sheet, split, masterDetail, status);
    }
}
