## Task

The bakery app runs at {{url}}; log in as barista@vaadin.com, password barista. Place a new order for Grace Hopper, phone +1 555 0100: pickup at the Bakery, {{tomorrow}} at 11 AM; 2 × {{product_a}} and 1 × {{product_b}}, the latter with the note "no sugar".

## Done when

That order in the database: customer, phone, Bakery, due tomorrow 11:00, exactly those items, the note on product_b, state New.

## Watch for

- Due time, pickup location, product and quantity have no label, only a prefix icon or nothing; the item's note has the placeholder "Details".
- The item list grows a new empty row each time a product is picked.
- The time ComboBox shows "11:00 AM"; the DatePicker is in US format.
- Saving takes two steps: "Review order", then "Place order". Success is a 4 s notification "Order was created". An agent that stops at the review fails.
