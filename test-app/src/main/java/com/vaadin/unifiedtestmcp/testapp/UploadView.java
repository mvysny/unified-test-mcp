package com.vaadin.unifiedtestmcp.testapp;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.server.streams.UploadMetadata;

import java.util.ArrayList;
import java.util.List;

/**
 * At {@code upload}: an Upload that keeps files in memory; once they're all in, the status names them.
 *
 * <pre>
 * [Upload Files...]  Drop files here
 * Status: Nothing received          then e.g. "Received notes.txt (12 bytes)"
 * </pre>
 */
@Route("upload")
public class UploadView extends VerticalLayout {
    public UploadView() {
        Span status = new Span("Nothing received");
        // the handler runs on the upload's request; the status is set once the browser says all are done
        List<UploadMetadata> received = new ArrayList<>();
        Upload upload = new Upload(UploadHandler.inMemory((metadata, data) -> {
            synchronized (received) {
                received.add(metadata);
            }
        }));
        upload.addAllFinishedListener(e -> {
            synchronized (received) {
                status.setText("Received " + String.join(", ", received.stream()
                        .map(m -> m.fileName() + " (" + m.contentLength() + " bytes)").toList()));
                received.clear();
            }
        });

        add(upload, status);
    }
}
