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

Here, a review would look like this: start `test-app`, register the built jar with a `claude`
started outside this checkout so it never sees the source or `AGENTS.md`, give it a task in
the user's words ("go to `second` and click back"), then ask it to critique the tools.

Candidates already visible, before any review:
- `get_text("no-such-id")` fails with the Unified API's "No component matches …". It doesn't say
  which ids are there, or that the component may be on another view (`navigate`).
- `navigate` to a path with no route reports success: Vaadin renders its "not found" view, and the
  agent only learns that when its next lookup times out.

`Q_when`: once per tool, before the commit that adds it? Or in batches, after a few tools land?
The review is manual and costs a session.

`Q_where_findings_go`: the lasting part of a finding is how the tool turned out (its
description, its error), and that lives in the code. Should a reviewed tool also get a `D_`
entry, at least when the review changed its shape?

`Q_automate`: could a test drive a blind agent? That needs a model in CI. Probably not, but a
script that sets up the blind session (a temp dir, the MCP registered, the task prompt) would
make the review cheap enough to do every time.
