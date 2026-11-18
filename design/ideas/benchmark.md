# Benchmark: this server against Playwright MCP and Chrome DevTools MCP

The requirement: show how much faster (wall-clock) and leaner (tokens, cost) an agent reaches a
goal in a Vaadin app through this server than through Playwright MCP or Chrome DevTools MCP.

The same apps, scenarios and harness double as the **whole-toolset review**: a system test of
every tool together. Some problems only show across tools, and a per-tool `/blind-review`
scenario never walks that far: which tool comes next, whether results echo the same way, whether
one tool's error points at another that exists, whether refs carry from one result into the next
call. Our arm, run once, then critiqued through `--resume` as `run.sh` does: the critique comes
after the measured session, so it doesn't touch the numbers. The benchmark adds what blind-review
lacks, a baseline: "Playwright MCP did it in 6 calls, we needed 14" is a finding without any
critique.

## Decided

- **Apps.** Bakery (`vaadin/bakery-app-starter-flow-spring`) is the main one: a lazy Grid of
  orders, an order editor with ComboBoxes, Date/TimePicker and a list of order items, confirm
  dialogs, notifications, users/products admin. Its dashboard is Vaadin Charts (commercial): no
  scenario touches it. The CRM tutorial (`vaadin/flow-crm-tutorial`) is the simple, neutral
  baseline, Vaadin's own tutorial app. Both Unlicense, both on Vaadin 25.3.5 as of 2026-11. Forked
  to `mvysny/` for the benchmark's hooks (seeded data, a setup/check endpoint, attempt counts; each
  fork's README, "Benchmark hooks"), and pinned here as submodules in `benchmark/apps/`: the commit
  a measurement ran against is recorded. `test-app` stays the testbed for the tools' own tests and
  `/blind-review`: a public claim measured on an app built for these tools convinces nobody.
- **Only the owner runs it**, to prove the project viable: commercial components (Bakery's Crud,
  both dashboards' Charts) and the owner's Vaadin license are fine.
- **Some scenarios we don't expect to win** — a plain button and text field. All wins reads as
  marketing.
- **Black box.** The agent sees the running app only (`--tools ""`), no sources. With sources the
  time goes into reading code, the arms converge, and the rule-discovery scenarios stop working.
  A track where a developer verifies their own change, sources visible, may come later.
- **Model.** Sonnet 5.5 for the headline: most users' daily model, cheap enough for a large N, and
  a weaker model shows tool quality — Opus reasons its way around a clumsy toolset and hides the
  gap. Opus 5.5 on a subset; if the gap shrinks there, that's a finding too. Haiku 5.5 as a
  stretch. The exact model id, never the alias `run.sh` uses.
- **Timing.** Today's tools (click, set_value, the get_ tools, navigate, snapshot) can't do most
  scenarios. There's no Grid, and the snapshot walks light DOM only, so it sees nothing inside
  Bakery's Lit templates. Write the scenarios now, run the two competitors now for baselines and
  transcripts; every scenario we can't do yet is what the prototype implements next. The draft and
  the table of what each scenario needs: `benchmark/scenarios.md`, beside this file.

## Scenarios

Goal-shaped, never tool-shaped, in a tester's words; one file each in `benchmark/scenarios/`, like
blind-review's: `## Task` (all the agent gets), `## Done when`, `## Watch for`; the oracle is the
fork's check. No agent may get lucky: each must gather the same information, and no correct guess
exists without looking at the app.

1. **Values given.** The goal lists every value. The bare cost of operating the UI.
2. **Values looked up.** They exist only in the app: "move customer X's next order one day later"
   means reading a lazy Grid; no guess.
3. **Rules the app states.** An arbitrary constraint (a SKU pattern, a range 17–23) in the helper
   text or the validation error. Reading the helper first or submitting to read the error are both
   fair; the difference is what's measured — how cheaply a toolset shows that text. Not
   real-world rules (email, phone): a model gets those right first time, so they measure nothing.
4. **Seeded data, a new seed each round, the same for the three arms.** These apps are public and
   likely in the training data; the seed stops an agent from knowing a record.

The fork counts submit attempts per form, so luck shows in the data, not in an argument.

- **Shape.** Short, independent scenarios for the measured runs: one failure doesn't hide the
  rest, and each gets its own numbers. The same files chained into one session for the
  whole-toolset review, where refs carry over and the next tool has to be found.
- **Coverage.** The scenarios name goals, so a new tool doesn't edit them. The aggregator diffs
  the server's `tools/list` against the tools the transcripts called: a tool no scenario calls is
  either unneeded or undiscoverable.
- **Oracle.** "Done when" is checked against the app's state after the run (a DB query or an
  endpoint in the fork), never the agent's word. For a question, its final answer is matched. A
  fixed-sequence replay in `ToolsTest` maybe later; the per-tool failure paths (wrong id, missing
  route, disabled button) stay in blind-review.

## Harness

One podman image (`env XDG_DATA_HOME=/home/mavi/.local/share podman …`), a fresh
`podman run --rm` per run, the arm an argument — not an image per arm. A fresh container is an
identical start: empty `~/.claude`, fresh browser profile, no Chromium left over from the run
before.

- **Inside** (`benchmark/container/Containerfile`): `node:26-slim` (Debian trixie), with from apt
  `chromium` (Ubuntu's is a snap shim, useless in a container), `openjdk-21-jre-headless`,
  `tzdata`; from npm, pinned in `package-lock.json`, `@playwright/mcp` and `chrome-devtools-mcp`.
  Mounted read-only: the `claude` binary at a fixed version (`~/.local/share/claude/versions/<v>`,
  a glibc ELF, so not Alpine; `DISABLE_AUTOUPDATER=1`), and this server's jar from the latest
  build — it changes with every prototype step, the competitors don't.
- **The arms** (`arms.json`, `claude --mcp-config`'s format; `smoke.mjs` proves each one opens a
  page in the image): one Chromium for the three, Debian's, headless, 1280×720 (the viewport
  decides how many Grid rows exist in the DOM), a fresh profile, no sandbox, otherwise each
  server's defaults. Nothing downloads a browser per run. `--shm-size=1g`, the image's non-root
  `node` user, `TZ` the host's, the app's JVM in `en-US` (Bakery's dates and times are
  US-formatted).
- **Auth: the owner's subscription, not an API key.** A `claude setup-token` token in
  `~/.config/unified-test-mcp-benchmark/oauth-token` (mode 600), handed in as
  `CLAUDE_CODE_OAUTH_TOKEN`; `-p` needs no first-run state in the fresh HOME. Not the mounted
  `~/.claude/.credentials.json`: a refresh inside the container could rotate the host's login away.
- **The call** (`benchmark/run.sh <scenario> <arm> [seed]`, one run). An `mcp.json` naming only
  that arm; `--strict-mcp-config --tools "" --allowedTools "mcp__<arm>__*"
  --disable-slash-commands --output-format stream-json --verbose --model claude-sonnet-5-5`, a
  20-minute timeout. Each run's transcript, setup and check land in a folder per round in
  `benchmark/results/`, one summary line in its `runs.jsonl`; `report.sh` makes the table.
- **Results are committed, public**: public apps, public servers, a public repo, so anyone can
  check a number against its transcript. Not the screenshots' base64 (most of a raw transcript's
  bytes; their count stays), nor the apps' startup logs (noise with local paths).
- **No tool deferral.** With `--tools ""` there's no ToolSearch: every MCP tool loads up front
  (the init event lists all 25 of Playwright MCP's), so each arm's whole tool list is paid on every
  turn — 6.8k characters ours (8 tools), 21k Playwright MCP (25), 32k chrome-devtools (33), from
  `smoke.mjs`. Recorded per run as `mcp_tools_loaded`.
- **Isolation.** The container's HOME has no global `CLAUDE.md`, skills, hooks, plugins, memory or
  earlier sessions. Overwriting `~/.claude/CLAUDE.md` for the duration was the alternative: it
  leaves hooks, skills and `settings.json` plugins in, and takes the owner's instructions away from
  every other Claude session on the machine while the benchmark runs. Org-managed settings still
  apply — the same for all arms.
- **The apps** run on the host, in production mode: dev mode puts `<vaadin-dev-tools>` in the DOM
  the competitors snapshot, and Vite into the first load. The container reaches them by
  `--network=host`. Restarted for every run, `--benchmark.seed` the round's: H2 in memory, so a
  fresh start is a fresh database. Ready when `GET /login` answers on the app port: 9–15 s, no
  health endpoint. `--management.server.port` turns on `POST /actuator/benchmark/<scenario>`,
  `{"phase":"setup"}` then `{"phase":"check","answer":…}`; an explicit port, never 0 (Bakery's
  login exemption matches the request's local port). "Today" is the app JVM's, so the container's
  `TZ` must match it.
- **Serial runs, arms interleaved** round-robin within each round, so drift in API latency hits
  every arm alike. Parallel runs would share the app's state and the CPU.
- Blind-review's `run.sh` can move onto the same image and drop its accepted `CLAUDE.md` leak.

## Metrics

- **Success**, by the oracle, first. **Cost per success** = all runs' cost / successes, so a flaky
  arm can't look cheap.
- **Wall-clock**, `duration_ms`, split into model time (`duration_api_ms`) and the rest, roughly
  tool time. The container's start is outside it.
- **Tokens**: `total_cost_usd` (reported on a subscription too), raw input / cache write / cache
  read / output, and the **largest single-turn context** from the per-message usage — a lean
  toolset keeps it flat, DOM snapshots grow it.
- **Diagnostics**: turns, tool calls, tool errors, result size per tool. They explain the headline.
- Median and spread over N runs.

## Open questions

`Q_n`: N per scenario and arm. Subscription rate limits (the 5-hour windows): the harness has to
tell a rate-limited run from an agent's failure and re-run it.

`Q_one_chromium_fair`: Playwright MCP pins its own Chromium build (Playwright 1.66 alpha); it opens
pages on Debian's Chromium 156 in the smoke test, but does it work as well? If not, each arm gets
its own pinned build, and the report says so.
