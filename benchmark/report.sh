#!/usr/bin/env bash
# A round's numbers, one row per scenario and arm, medians over its runs:
#
#   report.sh <results-dir>
#
#   scenario           arm                runs  passed  timeout  infra  time_s  api_s  turns  calls  errors  cost_usd  context  io%
#   crm-add-contact    playwright            3       3        0      0    34.0   22.6     21     20       1    0.0991    21900  2.1
#
# time_s is the agent's whole run, api_s the model's part of it; context is the largest single
# turn's input, in tokens; io% how much of the run the host spent stalled on I/O, `-` for a run
# from before run.sh measured it. A high io% stretches the times, not the tokens. A run the app
# couldn't judge (no result: a rate limit, an API error) counts as infra, not as failed; a run out
# of time as timeout, a failure. Neither has numbers for the medians.
set -euo pipefail
runs=${1:?usage: report.sh <results-dir>}/runs.jsonl
[ -f "$runs" ] || { echo "no $runs" >&2; exit 1; }

jq -rs '
  def median: sort | if length == 0 then null
    elif length % 2 == 1 then .[length / 2 | floor]
    else (.[length / 2 - 1] + .[length / 2]) / 2 end;
  def fmt($d): if . == null then "-" else (. * pow(10; $d) | round) / pow(10; $d) | tostring end;
  ["scenario", "arm", "runs", "passed", "timeout", "infra", "time_s", "api_s", "turns", "calls", "errors", "cost_usd", "context", "io%"],
  (group_by([.scenario, .arm])[]
   | (map(select(.outcome == "passed" or .outcome == "failed"))) as $judged
   | [.[0].scenario, .[0].arm, length, (map(select(.outcome == "passed")) | length),
      (map(select(.outcome == "timeout")) | length), (map(select(.outcome == "infra")) | length),
      ($judged | map(.duration_ms / 1000) | median | fmt(1)),
      ($judged | map(.duration_api_ms / 1000) | median | fmt(1)),
      ($judged | map(.turns) | median | fmt(0)),
      ($judged | map(.tool_calls) | median | fmt(0)),
      ($judged | map(.tool_errors) | median | fmt(0)),
      ($judged | map(.cost_usd) | median | fmt(4)),
      ($judged | map(.max_context) | median | fmt(0)),
      ($judged | map(.io_stall_pct // empty) | median | fmt(1))]
   | map(tostring))
  | @tsv' "$runs" | column -t -s $'\t'
