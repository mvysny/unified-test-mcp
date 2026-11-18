package com.vaadin.unifiedtestmcp.testapp;

import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.Route;

/**
 * At {@code display}: components that show something and take no input of their own.
 *
 * <pre>
 * (AS)                          Avatar "Alice Smith"
 * (AS)(BJ)(+2)                  AvatarGroup of four, two shown
 * New   Unread 5                Badges, the second with a number
 * ┌ Order #12 / Shipped ─┐      Card: title, subtitle, "3 items, 42 €" [Track]
 * Release notes …               Markdown: a heading, a list with bold, a link
 * [████░░░░░░]  [~~~~~~]        ProgressBar at 0.4, and an indeterminate one
 * </pre>
 */
@Route("display")
public class DisplayView extends VerticalLayout {
    public DisplayView() {
        Avatar avatar = new Avatar("Alice Smith");

        AvatarGroup group = new AvatarGroup(new AvatarGroup.AvatarGroupItem("Alice Smith"),
                new AvatarGroup.AvatarGroupItem("Bob Jones"), new AvatarGroup.AvatarGroupItem("Carol White"),
                new AvatarGroup.AvatarGroupItem("David Black"));
        group.setMaxItemsVisible(3);

        Card card = new Card();
        card.setTitle("Order #12");
        card.setSubtitle("Shipped");
        card.add(new Span("3 items, 42 €"), new Button("Track"));

        Markdown markdown = new Markdown("""
                ## Release notes

                - **Faster** startup
                - A [changelog](https://vaadin.com) link
                """);

        ProgressBar progress = new ProgressBar(0, 1, 0.4);
        ProgressBar busy = new ProgressBar();
        busy.setIndeterminate(true);

        add(avatar, group, new Badge("New"), new Badge("Unread", 5), card, markdown, progress, busy);
    }
}
