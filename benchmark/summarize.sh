#!/usr/bin/env bash
# One run's summary.json, from what its folder keeps: the raw transcript, setup.json, check.json.
#
#   summarize.sh <run-dir> [wall_s] [exit] [io_stall_pct] [cpu_stall_pct] [head]
#
# run.sh calls it after each run. Called by hand it rebuilds a lost summary, `null` for what only
# the run itself measured. Prints the summary line, for runs.jsonl.
#
# outcome: passed / failed by the app's check; timeout when the agent ran out of run.sh's time;
# infra when there's no result to judge (a rate limit, an API error).
set -euo pipefail
dir=${1:?usage: summarize.sh <run-dir> [wall_s] [exit] [io_stall_pct] [cpu_stall_pct] [head]}
IFS=. read -r scenario arm seed _ <<<"$(basename "$dir")"

jq -cs --arg run "$(basename "$dir")" --arg scenario "$scenario" --arg arm "$arm" \
  --argjson seed "${seed#s}" --argjson wall "${2:-null}" --argjson status "${3:-null}" \
  --argjson io_stall "${4:-null}" --argjson cpu_stall "${5:-null}" --arg head "${6:-}" \
  --slurpfile check "$dir/check.json" --slurpfile setup "$dir/setup.json" '
  (map(select(.type == "result")) | last) as $r
  | (map(select(.type == "system" and .subtype == "init")) | first) as $init
  | map(select(.type == "assistant")) as $said
  | [$said[].message.content[]? | select(.type == "tool_use")] as $calls
  | [.[] | select(.type == "user") | .message.content | if type == "array" then .[] else empty end
     | select(.type == "tool_result")] as $results
  | ($check[0] // {}) as $c
  | {run: $run, scenario: $scenario, arm: $arm, seed: $seed, model: $init.model,
     claude: $init.claude_code_version, head: (if $head == "" then null else $head end),
     outcome: (if $status == 124 then "timeout"
               elif $r == null or ($r.is_error and (($r.result // "") | test("limit|overloaded|API Error"; "i")))
               then "infra" elif $c.passed then "passed" else "failed" end),
     details: ($c.details // $c.error // null), attempts: ($c.attempts // null),
     expected: ($setup[0].expected // null), answer: ($r.result // null),
     wall_s: $wall, exit: $status, io_stall_pct: $io_stall, cpu_stall_pct: $cpu_stall,
     duration_ms: $r.duration_ms, duration_api_ms: $r.duration_api_ms, turns: $r.num_turns,
     cost_usd: $r.total_cost_usd, usage: $r.usage,
     max_context: ([$said[].message.usage | (.input_tokens // 0) + (.cache_creation_input_tokens // 0)
                    + (.cache_read_input_tokens // 0)] | max),
     tool_calls: ($calls | length),
     tool_errors: ([$results[] | select(.is_error)] | length),
     calls: ($calls | group_by(.name) | map({key: .[0].name, value: length}) | from_entries),
     result_chars: ([$results[].content | if type == "string" then length
                     else (map(.text // "" | length) | add // 0) end] | add // 0),
     images: ([$results[].content | if type == "array" then map(select(.type == "image")) | length else 0 end]
              | add // 0),
     tools_loaded: ($init.tools // [] | length),
     mcp_tools_loaded: ($init.tools // [] | map(select(startswith("mcp__"))) | length)}' \
  "$dir/task.jsonl" | tee "$dir/summary.json"
