package com.vaadin.unifiedtestmcp.testapp;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.router.Route;

/**
 * At {@code menus}: a MenuBar, a ContextMenu and a Popover; each menu item sets the status to its text.
 *
 * <pre>
 * [File ▾] [Edit ▾] [Help]     File: New, Open; Edit: Copy, Paste (disabled); Help is an item of its own
 * Right-click me               a ContextMenu: Rename, Delete
 * [Info]                       opens a Popover: "Opens on click, closes on Esc" [Got it]
 * Status: Nothing chosen
 * </pre>
 */
@Route("menus")
public class MenusView extends VerticalLayout {
    public MenusView() {
        Span status = new Span("Nothing chosen");

        MenuBar bar = new MenuBar();
        MenuItem file = bar.addItem("File");
        file.getSubMenu().addItem("New", e -> status.setText("New"));
        file.getSubMenu().addItem("Open", e -> status.setText("Open"));
        MenuItem edit = bar.addItem("Edit");
        edit.getSubMenu().addItem("Copy", e -> status.setText("Copy"));
        edit.getSubMenu().addItem("Paste", e -> status.setText("Paste")).setEnabled(false);
        bar.addItem("Help", e -> status.setText("Help"));

        Span target = new Span("Right-click me");
        ContextMenu menu = new ContextMenu(target);
        menu.addItem("Rename", e -> status.setText("Rename"));
        menu.addItem("Delete", e -> status.setText("Delete"));

        Button info = new Button("Info");
        Popover popover = new Popover();
        popover.add(new Span("Opens on click, closes on Esc"), new Button("Got it", e -> popover.close()));
        popover.setTarget(info);

        add(bar, target, info, status);
    }
}
