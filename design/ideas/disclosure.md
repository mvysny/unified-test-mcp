# Disclosure: Details, Accordion, Tabs, TabSheet, SplitLayout, MasterDetailLayout

Containers that show part of what they hold. The question for each: does the snapshot show the
hidden part, and how does it say what's open or selected.

## Today

test-app's `disclosure` view:

```
- Details [ref=1]
  - Div [ref=2, id=content-vaadin-details-18]
    - TextField "Street" [ref=3, value=""] actions: set_value
  - DetailsSummary [ref=4, id=summary-vaadin-details-19]
    - Span "Shipping" [ref=5]
- Details [ref=6]                      (Billing, open: the same lines)
- Accordion [ref=11]
  - AccordionPanel [ref=12]
    - Div [ref=13, id=content-vaadin-accordion-panel-28]
      - TextField "Name" [ref=14, value=""] actions: set_value
    - AccordionHeading [ref=15, id=summary-vaadin-accordion-panel-29]
      - Span "Personal" [ref=16]
  - AccordionPanel [ref=17]            (Payment, closed: the same lines)
- Tabs [ref=22]
  - Tab "Overview" [ref=23, value="Overview"]
  - Tab "Settings" [ref=24, value="Settings"]
- Tabsheet [ref=25]
  - Tabs [ref=26]
    - Tab "Info" [ref=27, id=tabsheet-tab-8c7914ab-…, value="Info"]
    - Tab "Advanced" [ref=28, id=tabsheet-tab-f0ddb4fa-…, value="Advanced"]
  - Span "Version 1.0" [ref=29, id=tabsheet-panel-42]
- SplitLayout [ref=30]
  - Span "Left pane" [ref=31]
  - Span "Right pane" [ref=32]
- MasterDetailLayout [ref=33]
  - Button "Show detail" [ref=34] actions: click
```

Works already: `click` on a Tab selects it; on a TabSheet's tab it swaps the content shown, only
the selected tab's is in the DOM (`R_vaadin_hidden_in_dom`); MasterDetailLayout's detail appears
under it once set. `click` on the "Shipping" Span toggles the Details, but the answer shows no
change.

Doesn't: a closed Details' or panel's content shows like an open one's, its fields offering
`set_value`; nothing says open, closed or selected; the summary comes after the content, the DOM's
order; the generated ids and `value=` repeating the name are noise (`snapshot-cross-cutting.md`).
`UnifiedDetails` has `open()`, `close()`, `isOpen()`, `getSummaryText()`; `UnifiedTabs` and
`UnifiedTabSheet` `selectTab(label)` and `getSelectedTab()`. Nothing for Accordion.

## A shape to start from

```
- Details "Shipping" [ref=1, closed] actions: click
- Details "Billing" [ref=6, open] actions: click
  - TextField "Account" [ref=8, value=""] actions: set_value
- Tabs [ref=22]
  - Tab "Overview" [ref=23, selected] actions: click
  - Tab "Settings" [ref=24] actions: click
```

## Open questions

`Q_closed_content`: leave a closed Details' content out, as `D_modal_shows_alone` leaves out what
takes no input? What does `set_value` on the closed "Street" do today: Playwright waits for it to
be visible, so a 10 s timeout? Not tried.

`Q_named_by_summary`: a Details or a panel named by its summary text, the summary line dropped,
and `click` on the Details' ref toggles it.

`Q_tabsheet`: a TabSheet as its tabs and the selected one's content, without the inner `Tabs` line.

`Q_split_and_master_detail`: SplitLayout and MasterDetailLayout as layouts, standing aside like
VerticalLayout? Their splitter and overlay mode take no input an agent needs.
