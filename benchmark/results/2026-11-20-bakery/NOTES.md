# 2026-11-20, Bakery: the first round

Each Bakery scenario through all three arms, seeds 1–3, the arms interleaved (`../../round.sh`),
63 runs. unified-test-mcp as of daf6dfc, with Dramafinder's local `2.2.0-shadow` (the row click of
parttio/dramafinder#311). `claude-sonnet-5-5`, Claude Code 2.1.329. `../../report.sh .` makes the
table.

Read the numbers with these in mind:

- **The times stand.** The host was quiet: at most 4.7 % of a run stalled on I/O, 11 % on CPU.
- **No infra outcomes.**
- **bakery-today-count's check misread three right answers.** It takes the first integer of the
  answer (`Answers.firstInteger`), and three agents explained before the number, against the
  task's "Answer with the number only": "Today's orders are rows 0–6. Rows 2 and 4 … **2**" read
  as 0, "… rows 1 and 2 … 2" as 1 (both ours), "Elaine Rowland (8:00 AM) … 2" as 8 (playwright's).
  The table counts them failed, as checked; the headline gives both. Taking the answer's last
  integer reads all three right and no other answer differently.

## Headline

| arm | passed, as checked | with the last integer read | cost per success | all runs' time |
|---|---|---|---|---|
| unified-test-mcp | 19 of 21 | 21 of 21 | $0.067 ($0.061) | 539 s |
| playwright | 18 of 21 | 19 of 21 | $0.147 ($0.139) | 972 s |
| chrome-devtools | 19 of 21 | 19 of 21 | $0.170 | 752 s |

Ours is the cheapest in six scenarios of seven (new-user: chrome-devtools $0.054, ours $0.063),
with the smallest context in all seven (12k–22k tokens at its largest turn, the competitors'
17k–64k), and the fastest in the three longest (new-order, edit-items, reschedule); in the other
four, within 7 s of the fastest.

## Findings

- **bakery-new-order: the competitors read a lazy ComboBox.** chrome-devtools on seeds 1 and 3
  and playwright on seed 3 gave up: the product dropdown listed its first 10 items, and the
  order's product wasn't among them, so they answered that the bakery doesn't sell it. Ours types
  the label with `set_value`, which the ComboBox filters by, and passed all three, at $0.088 to
  their $0.26–$0.33.
- **Ours spends ~20 s of new-order's ~36 s outside the model**: 31–40 s, the model's 11–19 s of
  it. The two product picks, each waiting 10 s on the Unified API's check of an item editor Bakery
  replaced (parttio/unified-testing-tool#377, `D_set_value_reads_back`), by the timing.
- **bakery-reschedule: one open row click does it.** Ours 14 turns, $0.076; chrome-devtools 36
  turns, $0.286. Playwright on seed 2 changed nothing: the search found two Lael Leblanc orders
  both due Dec 14, different phone numbers, and it asked which. The seed has two customers of one
  name; ours and chrome-devtools took the earlier one, the check's.
- **bakery-today-count seed 1 asks for Delivered**: Store, Delivered, 2. Ours read the check
  icons (`Icon "vaadin:check"`, `D_lone_icon`): "Today's Store orders showing the delivered check
  icon are rows 1 and 2". The data move with today's date, so a seed asks something else on
  another day.
- **The Crud scenarios** (locked-user, price-cap, new-user) are close between the arms: $0.046–
  $0.075 each, 17–30 s. Ours sees the inline editor without a `Modal:` line and passes anyway.
