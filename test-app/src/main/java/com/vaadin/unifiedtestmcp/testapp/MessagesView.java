package com.vaadin.unifiedtestmcp.testapp;

import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * At {@code messages}: a MessageList of two messages, and a MessageInput whose message the list gets from "Me".
 *
 * <pre>
 * Alice  9:00   Is the order shipped?
 * Bob    9:05   Yes, this morning.
 * [Message          ] [Send]
 * </pre>
 */
@Route("messages")
public class MessagesView extends VerticalLayout {
    public MessagesView() {
        List<MessageListItem> messages = new ArrayList<>(List.of(
                new MessageListItem("Is the order shipped?", Instant.parse("2026-11-18T09:00:00Z"), "Alice"),
                new MessageListItem("Yes, this morning.", Instant.parse("2026-11-18T09:05:00Z"), "Bob")));
        MessageList list = new MessageList(messages);
        MessageInput input = new MessageInput(e -> {
            messages.add(new MessageListItem(e.getValue(), Instant.now(), "Me"));
            list.setItems(messages);
        });

        add(list, input);
    }
}
