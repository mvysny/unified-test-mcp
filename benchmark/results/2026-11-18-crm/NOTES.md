# 2026-11-18, CRM: the first round

The harness's shakedown: each CRM scenario through Playwright MCP 0.0.85 and chrome-devtools-mcp
1.13.0, seeds 1–3, the two arms taking turns on each seed; then unified-test-mcp once per scenario
as a probe. `claude-sonnet-5-5`, Claude Code 2.1.327. `../../report.sh .` makes the table.

Read the numbers with these in mind:

- **The times are unreliable.** The host was busy with other work, stalled on I/O ~13% and on CPU
  ~18% of the minute measured near the end. Turns, calls, tokens, cost and pass/fail are the
  agents' behaviour and stand. Measuring the stall per run (`io_stall_pct`) came in mid-round, and
  a bug in it lost the summaries of the last 8 runs: rebuilt by `summarize.sh` from their
  transcripts and checks, without `wall_s`, `exit`, the stalls and `head`.
- **The probe ran a work-in-progress unified-test-mcp**, built from a tree with another session's
  uncommitted snapshot changes: its Grid showed as a flat list of cells, only the ~38 rendered
  rows. Not a measurement of any commit.
- **crm-delete-closed.playwright.s3.1 timed out** at 20 minutes, with 13 contacts to delete
  (seeds 1 and 2 had 6 and 7).
- **The two wrong counts** (crm-count, playwright s3: 0 for 4; chrome-devtools s2: 2 for 11): the
  agent read the grid with a page script, counted the 35–36 rendered rows of 200, and stopped.
- Screenshots' base64 is dropped from `task.jsonl` (their count stays, `images`); the apps' logs
  aren't committed.
