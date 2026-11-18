# Notification

Nearly every save shows one, and it's often the thing an agent checks for: "Saved", or an error.
It closes on its own after its duration, 5 s for `Notification.show`.

## Today

test-app's `notification` view: `Save` shows "Saved" for 5 s, `Fail` an error with a Close button
that stays until closed, `Delete` "Order deleted" with an Undo button for 10 s. After all three:

```
- NotificationContainer [ref=6]
  - NotificationCard "Saved" [ref=7]
  - NotificationCard [ref=9]
    - Span "Couldn't save: the server is down" [ref=10]
    - Button "Close" [ref=11] actions: click
  - NotificationCard [ref=13]
    - Span "Order deleted" [ref=14]
    - Button "Undo" [ref=15] actions: click
```

Works already: each shows in the answer of the `click` that opened it, the snapshot is taken right
away; `click` on Undo works; 6 s later "Saved" is gone. The `<vaadin-notification>` elements
themselves aren't listed: `display: none`, their content shown in the cards (`D_visible_only`).

Doesn't: the container is noise; nothing says which one is an
error; a notification that came and went between two calls leaves no trace. `UnifiedNotification`
has `getText()` and `isOpen()`.

## A shape to start from

```
- Notification "Saved" [ref=7]
- Notification [ref=9, theme=error]
  - Span "Couldn't save: the server is down" [ref=10]
  - Button "Close" [ref=11] actions: click
```

## Open questions

`Q_vanished`: a slow agent calls `snapshot` after "Saved" closed and never learns it was shown.
Remember the notifications opened since the last answer, as `console-errors.md` counts errors since
the last snapshot: `Notifications: "Saved" (closed)`. One mechanism for both?

`Q_where`: at the end of the snapshot where the DOM puts them, or at the top, after the header,
since they're news?

`Q_theme`: the theme variant, `error`, `success`, `warning`, `primary`, `contrast`, as the user
reads it from the colour. From the card's `theme` attribute?

`Q_over_modal`: a notification opened while a modal dialog is open: the snapshot shows only the
dialog (`D_modal_shows_alone`), but the notification is above the modal and takes clicks. Not tried.
