package com.vaadin.unifiedtestmcp.testapp;

import com.vaadin.flow.component.ModalityMode;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

/**
 * At {@code dialog}: dialogs, modal and not, one opening another, and a ConfirmDialog.
 *
 * <pre>
 * [Delete]   opens a modal "Delete order?": Reason [    ]  [Why?]  [Cancel] [Delete]
 *                                            [Why?] opens a second modal on top: [OK]
 * [Notes]    opens a non-modal "Notes": [Close]; the page stays usable. Added to the view, so it's in the DOM
 *            while closed too
 * [Discard]  opens a ConfirmDialog "Discard changes?": [Cancel] [Discard]
 * Status: Not deleted
 * </pre>
 */
@Route("dialog")
public class DialogView extends VerticalLayout {
    public DialogView() {
        Span status = new Span("Not deleted");

        Dialog why = new Dialog("Why?");
        why.add(new Span("Deleting can't be undone."));
        why.getFooter().add(new Button("OK", e -> why.close()));

        Dialog delete = new Dialog("Delete order?");
        delete.add(new TextField("Reason"), new Button("Why?", e -> why.open()));
        delete.getFooter().add(new Button("Cancel", e -> delete.close()), new Button("Delete", e -> {
            status.setText("Deleted");
            delete.close();
        }));

        Dialog notes = new Dialog("Notes");
        notes.setModality(ModalityMode.MODELESS);
        notes.add(new Span("Remember to call back."));
        notes.getFooter().add(new Button("Close", e -> notes.close()));

        ConfirmDialog discard = new ConfirmDialog("Discard changes?", "Unsaved changes will be lost.", "Discard",
                e -> status.setText("Discarded"));
        discard.setCancelable(true);

        add(new Button("Delete", e -> delete.open()), new Button("Notes", e -> notes.open()),
                new Button("Discard", e -> discard.open()), status, notes);
    }
}
