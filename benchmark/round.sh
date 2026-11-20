#!/usr/bin/env bash
# A round: run.sh for every scenario given, each seed, the arms interleaved, serially
# (design/ideas/benchmark.md, "Serial runs, arms interleaved"); then report.sh's table.
#
#   round.sh <results-dir> "<seeds>" <scenario>...
#   round.sh results/2026-11-20-crm "1 2 3" crm-add-contact crm-count
#
# The arm going first rotates from one (scenario, seed) to the next. ARMS overrides the arms,
# default all three. Resumable: a (scenario, arm, seed) already judged in runs.jsonl (passed,
# failed or timeout) is skipped, so a round cut short goes on where it stopped. Two infra outcomes
# in a row (a rate limit, an API error) stop the round: run it again once the limit lifts.
set -euo pipefail
here=$(cd "$(dirname "$0")" && pwd)
usage='usage: round.sh <results-dir> "<seeds>" <scenario>...'
results=${1:?$usage}
seeds=${2:?$usage}
shift 2
[ $# -gt 0 ] || { echo "$usage" >&2; exit 1; }
read -r -a arms <<<"${ARMS:-unified-test-mcp playwright chrome-devtools}"
mkdir -p "$results"
judged() {
  [ -f "$results/runs.jsonl" ] && jq -e -s --arg s "$1" --arg a "$2" --argjson seed "$3" \
    'any(.[]; .scenario == $s and .arm == $a and .seed == $seed
         and (.outcome == "passed" or .outcome == "failed" or .outcome == "timeout"))' \
    "$results/runs.jsonl" >/dev/null
}
turn=0 infra=0
for scenario in "$@"; do
  for seed in $seeds; do
    for i in "${!arms[@]}"; do
      arm=${arms[$(((turn + i) % ${#arms[@]}))]}
      judged "$scenario" "$arm" "$seed" && continue
      "$here/run.sh" "$scenario" "$arm" "$seed" "$results" \
        || echo "[round] $scenario / $arm / seed $seed: run.sh failed" >&2
      if [ "$(tail -1 "$results/runs.jsonl" 2>/dev/null | jq -r .outcome)" = infra ]; then
        infra=$((infra + 1))
        [ "$infra" -lt 2 ] || { echo "[round] two infra outcomes in a row: stopped" >&2; exit 2; }
      else
        infra=0
      fi
    done
    turn=$((turn + 1))
  done
done
"$here/report.sh" "$results"
