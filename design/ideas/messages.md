# Messages: MessageList and MessageInput

A chat: a list of messages, each with its author and time, and an input that sends one.

## Today

test-app's `messages` view: two messages, Alice's and Bob's, and a MessageInput that adds its
message to the list.

```
- MessageList [ref=1]
  - Message "Is the order shipped?" [ref=2]
    - Avatar [ref=3, aria-label="Alice (A)"]
  - Message "Yes, this morning." [ref=4]
    - Avatar [ref=5, aria-label="Bob (B)"]
- MessageInput [ref=6]
  - MessageInputButton "Send" [ref=7, disabled]
```

Works already: `set_value "Thanks!"` on the MessageInput, through the generic element: its line
shows `value="Thanks!"` and Send turns enabled. It doesn't send; `click` on Send, not tried.

Doesn't: the author and the time aren't shown, the author only inside the avatar's `aria-label`.
No `actions:` on the MessageInput or its Send button. No Unified API wrapper.

## A shape to start from

```
- MessageList [ref=1]
  - Message "Is the order shipped?" [ref=2, from="Alice", time="9:00"]
  - Message "Yes, this morning." [ref=4, from="Bob", time="9:05"]
- MessageInput [ref=6, value=""] actions: set_value
  - Button "Send" [ref=7, disabled] actions: click
```

## Open questions

`Q_send`: `set_value` and then `click` on Send, as a user does; does Enter in the input send too,
and should `set_value` press it?

`Q_long_lists`: a chat of hundreds of messages, as a Grid's rows: at most N in view, the rest by a
tool (`D_grid_as_rows`)? The last N, as the user sees the bottom of it?

`Q_markdown`: a MessageList in markdown mode renders each message's HTML; its text as the name
still?
