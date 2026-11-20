# 2026-11-20, CRM: the second round

Each CRM scenario through all three arms, seeds 1–3, the arms interleaved (`../../round.sh`),
45 runs. unified-test-mcp as of 2382ed4: the grid as a row a line, `get_rows`, `click(ref, row)`.
`claude-sonnet-5-5`, Claude Code 2.1.329. `../../report.sh .` makes the table.

Read the numbers with these in mind:

- **The times stand this round.** The host was quiet: at most 0.5 % of a run stalled on I/O,
  10 % on CPU. The first round's times didn't; its competitors' numbers came from Claude Code
  2.1.327 besides, so this round runs them again rather than reusing them.
- **No infra outcomes**: no run hit a rate limit or an API error.

## Headline

| arm | passed | cost per success | all runs' time |
|---|---|---|---|
| unified-test-mcp | 15 of 15 | $0.102 | 668 s |
| playwright | 13 of 15 | $0.125 | 1158 s |
| chrome-devtools | 12 of 15 | $0.236 | 1106 s |

Ours is the cheapest and the fastest in four scenarios of five, with the smallest context
(15.7k–19.9k tokens at its largest turn, where the first round's probe reached 51k).

## Findings

- **The five failures are the competitors'**, all in the grid scenarios, as in the first round:
  crm-count answered from the rows a page script found (4 for 7, 2 for 11, 0 for 4), and
  crm-delete-closed left 4 and 5 contacts in place, answering 3 and 2 for 7. Both competitors
  read the grid with page scripts (`browser_evaluate`, `browser_run_code_unsafe`,
  `evaluate_script`), around the UI; ours has no such tool, and read it with `get_rows`.
- **crm-count**: ours 6 turns, 12 s, where the first round's probe took 210 turns and $1.23.
- **crm-delete-closed is ours the dearest**: 37 turns, $0.282, about chrome-devtools' $0.291 and
  over playwright's $0.118 (which passed 2 of 3). Its agent reads all 190 rows in one `get_rows`,
  then clicks each contact's row and the form's Delete, 13 contacts on seed 3: two calls a contact,
  each answering with the page's snapshot, the form's fields included. The context grows to 46k
  over them.
- **Three of playwright's runs took 106–109 s, ~95 s of it the model's**, at the turns of its
  other runs (add-contact seeds 2 and 3, invalid-email seed 1, back to back), where its other
  runs of those scenarios took 36–42 s: the model's latency for those minutes, by the timing.
  add-contact's median carries it.
