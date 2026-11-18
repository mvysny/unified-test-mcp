package com.vaadin.unifiedtestmcp.testapp;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

/**
 * At {@code notification}: notifications that close on a timer, stay until closed, and carry a button.
 *
 * <pre>
 * [Save]    shows "Saved" for 5 s, Notification.show's default
 * [Fail]    shows an error "Couldn't save: the server is down" [Close], open until closed
 * [Delete]  shows "Order deleted" [Undo] for 10 s; Undo sets the status
 * Status: Order kept
 * </pre>
 */
@Route("notification")
public class NotificationView extends VerticalLayout {
    public NotificationView() {
        Span status = new Span("Order kept");

        Button fail = new Button("Fail", e -> {
            Notification error = new Notification();
            error.addThemeVariants(NotificationVariant.LUMO_ERROR);
            error.add(new Span("Couldn't save: the server is down"), new Button("Close", c -> error.close()));
            error.setDuration(0);
            error.open();
        });

        Button delete = new Button("Delete", e -> {
            status.setText("Order deleted");
            Notification undo = new Notification();
            undo.add(new Span("Order deleted"), new Button("Undo", u -> {
                status.setText("Order restored");
                undo.close();
            }));
            undo.setDuration(10_000);
            undo.open();
        });

        add(new Button("Save", e -> Notification.show("Saved")), fail, delete, status);
    }
}
