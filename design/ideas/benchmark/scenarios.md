# Benchmark scenarios, a draft

Read from the apps' code at `vaadin/flow-crm-tutorial` 7db838e and
`vaadin/bakery-app-starter-flow-spring` 59c7c46 (both 2026-11-03), cloned into `benchmark/apps/`.

`{{…}}` is filled per round by the fork's setup endpoint from the seeded data, which also records
the expected outcome; its check endpoint decides "Done when". The Task is all the agent gets.

## What our server lacks, by scenario

| Capability | Scenarios that need it |
|---|---|
| **Components inside a Lit template's shadow root.** `Snapshot` walks `e.children`, light DOM only; Bakery's storefront, order editor and search bar are `LitTemplate`s with `@Id` fields, so today the snapshot sees nothing inside them. `idUnique` asks `document.querySelectorAll`, which doesn't reach them either. | every Bakery storefront scenario |
| **Grid**: read rows, rows beyond the rendered ones, open a row, renderer-made content (Bakery's order cards) | crm-move-contact, crm-count, crm-delete-closed, bakery-reschedule, bakery-today-count, bakery-edit-items |
| **Crud** (`vaadin-crud`, commercial): the edit column's icon buttons, the editor, its Save / Delete | bakery-price-cap, bakery-new-user, bakery-locked-user |
| **Notifications** (Bakery's last 4 s) | bakery-new-order, bakery-locked-user |
| **Links and tabs**: CRM's drawer `RouterLink`s, Bakery's menu `Tabs` | any scenario whose agent navigates by clicking instead of by URL |
| **A field without a label**, named by its prefix icon or placeholder | bakery-new-order, bakery-edit-items |
| Login: the CRM's `vaadin-login-form` shows already (`LoginForm "Log in"` with its two fields); Bakery's `vaadin-login-overlay` — check | all |

What the competitors stumble on, from a first look in the browser (2026-11-18):

- Playwright MCP's `browser_fill_form` fails on a Vaadin ComboBox ("Element is not a `<select>`
  element"); typing into it and pressing Enter works. Its `browser_click` on a CSS target needs
  `vaadin-button`, not `button`.
- Playwright MCP snapshots right after `browser_navigate` before Vaadin has rendered: the login
  page showed only "Online". Ours waits for the page to settle.
- Its snapshot of the CRM's list holds ~38 of the 200 rows, as a `treegrid`.

## CRM (log in as `user` / `password`, shown on the login page)

### crm-add-contact — values given
**Task:** The CRM app runs at {{url}}; log in as `user`, password `password`. Add a new contact: Ada
Lovelace, email ada@analytical.example, working at {{company}}, status Contacted. Tell me when it's
saved.
**Done when:** one new contact with exactly these values.
**Watch for:** Save stays disabled until the form is valid; two ComboBoxes. Expected a draw: a
scenario we don't expect to win.

### crm-move-contact — values looked up
**Task:** … {{contact_a}} has moved to the company {{contact_b}} works for, and has become a
customer. Update {{contact_a}}'s record.
**Done when:** contact_a's company is contact_b's, status Customer, nothing else changed.
**Watch for:** contact_b's company is read from the grid (the filter matches names); clicking
contact_a's row opens the form.

### crm-count — a question
**Task:** … How many contacts at {{company}} have the status {{status}}? Answer with the number
only.
**Done when:** the answer equals the count in the DB.
**Watch for:** the filter matches names only, and the Status and Company columns don't sort, so
every row has to be read; the fork seeds ~200 contacts, more than the grid renders at once. DOM
tools have to scroll a virtual grid. The largest gap expected.

### crm-delete-closed — looked up, a repeated action
**Task:** … Clean up {{company}}: delete every contact there whose status is Closed (lost). Leave
everyone else alone. Tell me how many you deleted.
**Done when:** none left at that company with that status, every other contact intact, the answer
equals the number.
**Watch for:** no confirm dialog; one row at a time. Every action answers with a snapshot
(`D_action_returns_snapshot`), so the cost per action multiplies — the "Saving agent tokens"
promise against that rule.

### crm-invalid-email — a failure, read back
**Task:** … Try to add a contact Jane Doe, email `jane.doe@`, at {{company}}, status Imported lead.
If the app won't accept it, don't fix it: tell me which field it objects to and how it shows that,
and leave nothing saved.
**Done when:** the answer names the Email field; no Jane Doe in the DB.
**Watch for:**
- The app shows no text: the EmailField's own check runs before the bean validator's "must be a
  well-formed email address" and has an empty error message, so the field only turns invalid
  (seen in the browser, 2026-11-18). A toolset has to show `invalid` without a message. A required
  field left empty does say "must not be empty".
- Save is disabled while the form is invalid. An agent that clicks it anyway: our `click` waits
  10 s for a disabled button (`Q_timeout` in `beyond-buttons.md`).

## Bakery (barista: `barista@vaadin.com` / `barista`; admin: `admin@vaadin.com` / `admin`, shown on the login overlay)

### bakery-new-order — values given
**Task:** The bakery app runs at {{url}}; log in as barista@vaadin.com, password barista. Place a
new order for Grace Hopper, phone +1 555 0100: pickup at the Bakery, {{tomorrow}} at 11 AM; 2 ×
{{product_a}} and 1 × {{product_b}}, the latter with the note "no sugar".
**Done when:** that order in the DB: customer, phone, Bakery, due tomorrow 11:00, exactly those
items, the note on product_b, state New.
**Watch for:**
- Due time, pickup location, product and quantity have no label, only a prefix icon or nothing;
  the item's note has the placeholder "Details".
- The item list grows a new empty row each time a product is picked.
- The time ComboBox shows "11:00 AM"; the DatePicker is in US format.
- Saving takes two steps: "Review order", then "Place order". Success is a 4 s notification "Order
  was created". An agent that stops at the review fails the oracle.

### bakery-reschedule — looked up
**Task:** … Customer {{customer}} called: their next order should be ready one day later, same
time. Make the change, and leave a comment on the order: "moved by phone".
**Done when:** that order (the setup records which: the earliest from today on) is due a day later,
same time, with a history item "moved by phone"; no other order changed.
**Watch for:** the storefront's search matches customer names; the order cards sit under group
headers (Today, This week, Upcoming); a card opens a details dialog; the date changes in the editor
("Edit order", then review and save), the comment only in the details view.

### bakery-today-count — a question
**Task:** … How many orders due today, to be picked up at the {{location}}, have the status
{{status}}? Answer with the number only.
**Done when:** the answer equals the count in the DB.
**Watch for:** the order cards under "Today" in a lazy grid; the status is a badge on each card.

### bakery-edit-items — looked up, a dynamic list
**Task:** … In {{customer}}'s next order, drop the {{product_x}}, and add one more of each remaining
product.
**Done when:** product_x gone, every other quantity one higher, nothing else changed.
**Watch for:** each item's delete button is an icon only (×); the quantity is an unlabeled
IntegerField with step buttons, at most 15 (the setup picks an order below that); the total
updates.

### bakery-price-cap — a rule the app states, an error every agent hits
**Task:** … Log in as admin@vaadin.com, password admin. Add a product "Benchmark Brioche" at 1200
dollars. If the app won't take that price, use the highest price it accepts.
**Done when:** the product exists at 1000.00.
**Watch for:** every arm gets the same error, "price must be between 0 and 1000"; how cheaply it is
read is the measure. The Crud editor opens from "New product"; "Unit price" has a `$` prefix and a
pattern.

### bakery-new-user — a rule the app states
**Task:** … As admin, create an account for the new baker Linus Torvalds, linus@bakery.example,
with the password `penguin`. If the app rejects that password, choose one it accepts and tell me
which.
**Done when:** the user exists with role baker and those names, and the password in the answer
matches the stored hash.
**Watch for:** the rule, "need 6 or more chars, mixing digits, lowercase and uppercase letters",
shows only as the field's error after an attempt, so no arm can guess it ahead. Role is a ComboBox
of lowercase values.

### bakery-locked-user — a failure, read back
**Task:** … As admin, change Mary Ocon's role to barista. If the app doesn't allow it, tell me what
it says.
**Done when:** the answer contains "User has been locked and cannot be modified or deleted"; Mary
unchanged.
**Watch for:** the refusal is a notification: transient or persistent — check. An arm that reads
it late may miss it.

## The whole-toolset session

An app's scenarios, one after another in one session, for the review run only: refs carried over,
navigation between views, which tool comes next. Each as its own `--resume` prompt, or one long
task?
