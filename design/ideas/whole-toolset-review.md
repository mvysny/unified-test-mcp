# Whole-toolset review: one big scenario across every tool

The `blind-review` skill tries one tool at a time. Some problems only show across tools: which
tool comes next, whether results echo the same way, whether one tool's error points at another
that exists, whether refs carry from one result into the next call. A per-tool scenario never
walks that far.

The idea: one big scenario, a system test of the entire MCP, run through the same
`/blind-review` skill and isolation as a per-tool one — a tester's task through all of
`test-app`'s views, touching every tool, with failure paths along the way. Run periodically, not
per tool: after a few tools land, or after a change to something every tool shares (`Snapshot`,
`Refs`, how errors are worded).

Later, once there are enough tools for it to be worth designing.

## Open questions

`Q_scenario_shape`: one long task (closer to a real tester's session, but one early failure
hides the rest), or a few chained tasks in one session?

`Q_grows_with_tools`: the scenario has to touch every tool, so each new tool edits it. Who
notices when one is missing — the skill, comparing the transcript's tool names with the server's
tool list?

`Q_also_junit`: the same walk as a deterministic test in `ToolsTest` (a fixed call sequence, no
model) would catch a regression across tools in CI. Worth it beside the per-tool tests, or
duplication?
