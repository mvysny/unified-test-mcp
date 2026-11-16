---
name: blind-review
description: A blind-agent review of one MCP tool — an isolated Claude that knows only this MCP server does a tester's task through it, then critiques the tools; the transcript and the critique are triaged with the owner. Run by the owner as `/blind-review <tool>`, once per new tool after its tests pass and before the commit that adds it, and again after a tool's description or errors change.
disable-model-invocation: true
---

# Blind-agent review

How swing-mcp's tools got good: each new tool was tried by a Claude that had never seen the
project, knew nothing but the server's instructions and tool list, and drove a real app through
them; then it was asked what it would improve. The good suggestions fell into two kinds, and they
are what this review is for:

- **The agent doesn't have to guess.** What a tool expects, what it returns, which tool comes
  next: the description and the result say it, so it never has to try something to find out.
- **An error guides toward a fix.** It names the next step, not only what failed — swing-mcp's
  "… does not support swing_click. Call swing_snapshot or swing_get_cells to verify the list of
  actions".

Manual only: it needs a model, and a person to triage. CI never runs it.

## 1. Run it

`.claude/skills/blind-review/run.sh <tool>` runs the scenario `scenarios/<tool>.md` and prints the
scratch dir holding the transcripts. It takes minutes: run it in the background. No scenario for
the tool yet → write one first (below), and show it to the owner.

What it does, so a failure can be read:

1. Builds the jar (`:unified-test-mcp:shadowJar`).
2. Finds `test-app`. **Already running** (the owner's `./gradlew :test-app:run`): the JVM whose
   main class is `com.vaadin.unifiedtestmcp.testapp.Main`, its port from `ss -ltnp`; reused and
   left running — a scenario that needs a `test-app` change needs the owner to restart it first.
   **Not running**: started on a free port, handed over as `SERVER_PORT` (Vaadin Boot reads it),
   stopped when the script exits.
3. Writes an MCP config naming only this server into a scratch dir outside the checkout, so no
   project `AGENTS.md` loads.
4. **Run 1, the task**: `claude -p "<the scenario's ## Task>"` from that dir, its `{{url}}`
   replaced by the app's URL, isolated:
   - `--mcp-config … --strict-mcp-config` — this server only; not the owner's Playwright or
     Vaadin-docs MCPs, which would let it work around our tools;
   - `--tools ""` — no built-in tools: no Read of the source, no Bash `curl` of the app;
   - `--allowedTools "mcp__unified-test-mcp__*"` — the MCP calls run without a permission prompt;
   - `--disable-slash-commands` — none of the owner's skills;
   - `--output-format stream-json --verbose` — the transcript: every call, argument, result, error;
   - `--model sonnet` — the latest Sonnet; the alias, so it moves with each release.

   It still loads the owner's `~/.claude/CLAUDE.md`: `--bare` would skip it, but authenticates by
   `ANTHROPIC_API_KEY` only, not the subscription. Accepted: a real user of this server has a
   `CLAUDE.md` of their own too.
5. **Run 2, the critique**: `claude -p --resume <session>` with `critique.md`, so it critiques
   from what it just lived through, not from the tool list in the abstract. `critique.md` asks
   where it guessed, which error didn't name the next step, its top three changes — and to say
   "nothing" where nothing is wrong, since an agent asked for improvements invents some otherwise.

In the scratch dir: `task.txt` and `critique.txt` (one line per event: `SAYS`, `CALL`, `OK`,
`ERROR`, `END` with turns, time and cost), their raw `.jsonl`, and `ask.sh "<question>"`, which
puts a follow-up to the same blind agent — for a suggestion too vague to triage. test-app has to
be up for a follow-up that makes it call a tool.

## 2. Report to the owner

Two halves, kept apart:

- **What it did** — from `task.txt`, never from the agent's own account: the call sequence; every
  `ERROR` and what came next; every retry; every call whose argument it guessed (a ref it never
  saw, a path it tried twice); turns, time, cost; and whether it reached the scenario's
  "Done when". Check each "Watch for" item.
- **What it says** — the critique from `critique.txt`, verbatim.

## 3. Triage with the owner

Propose each suggestion as **keep** or **drop**, with a one-line reason; the owner decides, then
implement the keepers (with their tests, as for any tool change).

- **Keep** what removes a guess or a failed call the *transcript shows*, or makes an error name
  the next step. Behaviour beats opinion: a retry in the transcript outweighs a paragraph of
  critique.
- **Drop** renames and wording taste that change nothing it did; anything against a promise in
  `AGENTS.md` ("expose the CSS selector"); a new tool for something else — a good one becomes a
  file in `design/ideas/`.
- **A dropped suggestion that will come back** — the next blind agent will propose it again — gets
  its why-not written down, so it isn't argued over again.

Where findings go:

- Mostly nowhere but the code: the improved description, schema, result or error *is* the finding.
- A non-obvious reason for one tool's shape, or a short why-not for a dropped, recurring
  suggestion → that tool's javadoc.
- A decision spanning several tools (how every error names the next step, how results echo) → a
  `D_` entry in `design/decisions.md`.
- A weak spot the review found but nobody fixes yet → the scenario's "Watch for", so the next
  run checks it.

## Scenarios

`scenarios/<tool>.md`, one per tool, kept so a review can be re-run. Three sections:

- `## Task` — all the agent gets. A tester's words, naming what a tester sees (the app's URL as
  `{{url}}`, a view's path, a button's id or text), never a tool's name: whether it finds the
  right tool is half the test. A success path and at least one failure path (a wrong id, a
  missing route), so the errors get read too.
- `## Done when` — what the answers are, so the report can say whether it got there.
- `## Watch for` — the reviewer's: known weak spots, and the calls worth a look.
