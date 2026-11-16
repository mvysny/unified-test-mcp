# Blind-agent review: every new tool tried by a Claude that knows only the MCP

How swing-mcp's tools got good: each new tool was tried by a Claude that had never seen the
project. It knew nothing but the MCP server's instructions and tool list, and drove a real app
through them. Then that harness was asked what it thought of the API and what it would improve.
The good suggestions got implemented. Most of them fell into two kinds:

- **The agent doesn't have to guess.** What a tool expects, what it returns, which tool comes
  next: the description and the result say it. The agent shouldn't have to try something to find
  out.
- **An error message guides toward a fix.** It names the next step, not only what failed:
  swing-mcp's "… does not support swing_click. Call swing_snapshot or swing_get_cells to verify
  the list of actions".

## The plan: a repo skill, run by hand

A project skill, `.claude/skills/blind-review/`, invoked by the owner as `/blind-review <tool>`.
Manual only: CI never runs it (it needs a model and a person to triage).

**When:** once per tool, after its implementation is done and its tests pass, before the commit
that adds it. Its scenario stays, so a review can be re-run after a tool's description or errors
change.

**What the skill does:**

1. Builds the jar (`:unified-test-mcp:shadowJar`) and starts `test-app` in the background.
2. Writes an MCP config naming only this server, into a scratch dir outside the checkout.
3. **Run 1, the task.** `claude -p "<scenario>"` from that scratch dir, isolated:
   - `--mcp-config <cfg> --strict-mcp-config` — this server only; not the owner's Playwright or
     Vaadin-docs MCPs, which would let it work around our tools;
   - `--tools ""` — no built-in tools: no Read of the source, no Bash `curl` of the app;
   - `--allowedTools "mcp__<server>__*"` — the MCP calls run without a permission prompt;
   - `--disable-slash-commands` — none of the owner's skills;
   - `--output-format stream-json --verbose` — the transcript: every call, argument, result, error;
   - `--model sonnet` — the latest Sonnet, the alias kept so it moves with each release.

   It still loads the owner's `~/.claude/CLAUDE.md` (`--bare` would skip it but authenticates by
   `ANTHROPIC_API_KEY` only, not the subscription). Accepted: a real user of this server has a
   `CLAUDE.md` of their own too.
4. **Run 2, the critique.** `claude -p --resume <session id> "<critique prompt>"`, so it critiques
   from what it just lived through, not from the tool list in the abstract.
5. **Report**, two halves kept apart:
   - *What it did* — from the transcript, not from its own account: the call sequence, every tool
     error, every retry, every call with a guessed argument, the turns it took.
   - *What it says* — the critique, verbatim.
6. **Triage with the owner** — each suggestion proposed as keep / drop, with a reason (below).
   The owner decides; then implement the keepers.

**Scenarios** — `.claude/skills/blind-review/scenarios/<tool>.md`, one per tool:
- A task in a tester's words, naming what a tester sees (a view's path, a button's id or
  text), never a tool's name: whether it finds the right tool is half the test.
- A success path and at least one failure path (a wrong id, a missing route), so the errors get
  read too.
- What "done" looks like, so the report can say whether it got there.

**The critique prompt** — fixed, in the skill, asking concretely:
- where it had to guess (an argument, what a result meant, which tool next);
- which error didn't tell it what to do next;
- what it would change in a description, a parameter, a result or an error — its top three;
- "Say 'nothing' where nothing is wrong": an agent asked for improvements invents some otherwise.

**Triage rules** (the bikeshed filter):
- **Keep** what removes a guess or a failed call the *transcript shows*, or makes an error name
  the next step. Behaviour beats opinion: a retry in the transcript outweighs a paragraph of
  critique.
- **Drop** renames and wording taste that change nothing it did; anything against a promise in
  `AGENTS.md` ("expose the CSS selector"); a new tool for something else (→ an idea file, if
  it's good).
- **A dropped suggestion that will come back** — the next blind agent will propose it again — gets
  its why-not written down, so it isn't argued over again (below).

**Where findings go:**
- Mostly nowhere but the code: the improved description, schema, result or error *is* the
  finding.
- A non-obvious reason for one tool's shape → that tool's javadoc; a short why-not for a dropped,
  recurring suggestion → the same place.
- A decision spanning several tools (how every error names the next step, how results echo) →
  a `D_` entry.

## Open questions

`Q_app_port`: `test-app` serves on 8080, which `./gradlew :test-app:run` by the owner may already
hold. A free port per review (Vaadin Boot reads one from the environment, to verify), or reuse an
app already up?

`Q_whole_toolset`: some problems only show across tools (which tool comes next, consistent
results). A periodic review of the whole set, with a scenario spanning several tools, besides the
per-tool one?

Candidate already visible, before any review: `navigate` to a path with no route still reports
success. The snapshot it returns shows Vaadin's "not found" view, so the agent can tell, but
nothing says so.
