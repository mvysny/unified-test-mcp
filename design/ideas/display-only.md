# Display-only components: Avatar, AvatarGroup, Badge, Card, Markdown, ProgressBar

Components that show something and take no input. Each needs only a snapshot line that says what
the user sees: a `ComponentTypes` row and a tweak in the snapshot script, no tool.

## Today

test-app's `display` view:

```
- Avatar [ref=1, aria-label="Alice Smith (AS)"]
- AvatarGroup [ref=2, aria-label="Currently 4 active users"]
  - Avatar [ref=8, aria-label="AS", tooltip="Alice Smith"]
  - Avatar [ref=9, aria-label="BJ", tooltip="Bob Jones"]
  - Avatar [ref=10, aria-label="+2", tooltip="Carol White David Black"]
- Badge "New" [ref=11]
- Badge "Unread" [ref=12]
- Card [ref=13]
  - Span "Shipped" [ref=14]
  - Span "3 items, 42 €" [ref=15]
  - Button "Track" [ref=16] actions: click
  - Div "Order #12" [ref=17, id=card-title-vaadin-card-21]
- Markdown [ref=18]
  - H2 "Release notes" [ref=19]
  - <li> "startup" [ref=20]
    - <strong> "Faster" [ref=21]
  - <li> "A link" [ref=22]
    - Anchor "changelog" [ref=23]
- ProgressBar [ref=24]
- ProgressBar [ref=25, indeterminate]
```

`get_text` on the Markdown: `Release notes Faster startup A changelog link`. `get_value` on the
first ProgressBar: `"0.4"`, a string.

## Per component

- **Avatar**: named by its `aria-label`, "Alice Smith (AS)"; the name alone, "Alice Smith", from
  its `name` property?
- **AvatarGroup**: the closed overflow menu's avatars aren't listed (`D_visible_only`); the visible
  ones are named by their initials, the full name in `tooltip=`. A group as its names,
  `AvatarGroup "Alice Smith, Bob Jones, +2"`?
- **Badge**: "Unread" lacks its number 5, which the web component renders from its `number`
  property. `Badge "Unread" [number=5]`?
- **Card**: the title is a trailing Div with a generated id, the subtitle a Span first. Name the
  Card by its title, as a Dialog by its header, the subtitle as `helper=`?
- **Markdown**: inline formatting splits text across lines, and `<li>` and `<strong>` show by
  tag. The Markdown's source, its `content` property, as one value: exact, and cheap in tokens?
  Its links would then be no refs; a Markdown with links needing a click?
- **ProgressBar**: its value isn't on its line. `value=0.4`, as a Slider's; `get_value` a number,
  not a string. A percentage, as the user reads the bar?

## Open questions

`Q_values_as_shown`: `D_values_as_shown` says a value is what the user sees. A ProgressBar shows a
bar, no number; 0.4 or 40 %?

`Q_markdown_source`: Markdown's source is exact and short, but is it what the user sees?
