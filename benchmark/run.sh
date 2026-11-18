#!/usr/bin/env bash
# One benchmark run: one scenario, one arm (MCP server), one seed. design/ideas/benchmark.md has
# the why of each step, "Harness".
#
#   run.sh <scenario> <arm> [seed] [results-dir]
#     scenario     a file in scenarios/, e.g. crm-count
#     arm          unified-test-mcp | playwright | chrome-devtools, from container/arms.json
#     seed         the app's benchmark.seed; default 1
#     results-dir  default results/<today>
#
# Starts the scenario's app from its fork's jar on free ports, sets the scenario up, has `claude -p`
# do its Task in a fresh container with only that arm's MCP server, checks the outcome, stops the
# app. Leaves <results-dir>/<scenario>.<arm>.s<seed>.<n>/ (the task, the raw transcript without
# its screenshots' bytes, the readable one, setup.json, check.json, summary.json, the app's log) and
# appends one line to <results-dir>/runs.jsonl. Builds a jar that is missing or older than its
# sources first.
#
# BENCHMARK_MODEL overrides the model (claude-sonnet-5-5); the token comes from
# ~/.config/unified-test-mcp-benchmark/oauth-token (`claude setup-token`).
set -euo pipefail
shopt -u patsub_replacement 2>/dev/null || true # an `&` in a parameter stays an `&`

here=$(cd "$(dirname "$0")" && pwd)
repo=$(git -C "$here" rev-parse --show-toplevel)
usage="usage: run.sh <scenario> <arm> [seed] [results-dir]"
scenario=${1:?$usage}
arm=${2:?$usage}
seed=${3:-1}
results=${4:-$here/results/$(date +%F)}
model=${BENCHMARK_MODEL:-claude-sonnet-5-5}
image=unified-test-mcp-benchmark
token_file=$HOME/.config/unified-test-mcp-benchmark/oauth-token
claude_bin=$(readlink -f "$(command -v claude)")
tz=$(timedatectl show -p Timezone --value)
log() { printf '[benchmark] %s\n' "$*" >&2; }
fail() { log "$*"; exit 1; }

[ -f "$here/scenarios/$scenario.md" ] || fail "no scenario $scenario; one of: $(ls "$here/scenarios" | sed 's/\.md$//' | tr '\n' ' ')"
jq -e --arg arm "$arm" '.mcpServers[$arm]' "$here/container/arms.json" >/dev/null \
  || fail "no arm $arm; one of: $(jq -r '.mcpServers | keys | join(" ")' "$here/container/arms.json")"
[ -r "$token_file" ] || fail "no token in $token_file; run \`claude setup-token\` and save it there"

mkdir -p "$results"
results=$(cd "$results" && pwd) # podman takes a relative -v source for a volume name
n=1
while [ -e "$results/$scenario.$arm.s$seed.$n" ]; do n=$((n + 1)); done
out="$results/$scenario.$arm.s$seed.$n"
mkdir "$out"

# --- the jars: built when missing or older than their sources ------------------------------------
stale() { [ ! -f "$1" ] || [ -n "$(find "${@:2}" -newer "$1" -type f -print -quit)" ]; }
case $scenario in
  crm-*) app=$repo/benchmark/apps/flow-crm-tutorial; build=(./mvnw -q -B package -DskipTests) ;;
  bakery-*) app=$repo/benchmark/apps/bakery-app-starter-flow-spring; build=(mvn -q -B package -DskipTests) ;;
  *) fail "no app for $scenario" ;;
esac
app_jar=$(ls "$app"/target/*.jar 2>/dev/null | grep -v '\.original$' | head -1 || true)
if stale "${app_jar:-/nonexistent}" "$app/src/main" "$app/pom.xml"; then
  log "building $(basename "$app")"
  (cd "$app" && "${build[@]}") >"$out/build.log" 2>&1 || fail "build failed: $out/build.log"
  app_jar=$(ls "$app"/target/*.jar | grep -v '\.original$' | head -1)
fi
server_jar=
if [ "$arm" = unified-test-mcp ]; then
  server_jar=$(ls "$repo"/unified-test-mcp/build/libs/unified-test-mcp-*.jar 2>/dev/null | grep -v -- '-plain\.jar$' | head -1 || true)
  if stale "${server_jar:-/nonexistent}" "$repo/unified-test-mcp/src/main" "$repo/unified-test-mcp/build.gradle.kts"; then
    log "building unified-test-mcp"
    (cd "$repo" && ./gradlew -q :unified-test-mcp:shadowJar) >"$out/build.log" 2>&1 || fail "build failed: $out/build.log"
    server_jar=$(ls "$repo"/unified-test-mcp/build/libs/unified-test-mcp-*.jar | grep -v -- '-plain\.jar$' | head -1)
  fi
fi

# --- the app: a fresh one, a fresh in-memory database --------------------------------------------
read -r port mport < <(python3 -c 'import socket
s = [socket.socket() for _ in range(2)]
for x in s: x.bind(("127.0.0.1", 0))
print(*[x.getsockname()[1] for x in s])')
log "$scenario / $arm / seed $seed: starting $(basename "$app") on $port"
java -Duser.language=en -Duser.country=US -jar "$app_jar" --server.port="$port" \
  --management.server.port="$mport" --benchmark.seed="$seed" >"$out/app.log" 2>&1 &
app_pid=$!
trap 'kill $app_pid 2>/dev/null || true; wait $app_pid 2>/dev/null || true' EXIT
for _ in $(seq 180); do
  [ "$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:$port/login")" = 200 ] && break
  kill -0 $app_pid 2>/dev/null || fail "the app died: $out/app.log"
  sleep 1
done
[ "$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:$port/login")" = 200 ] || fail "the app didn't answer: $out/app.log"
endpoint="http://localhost:$mport/actuator/benchmark/$scenario"
curl -sf -X POST -H 'Content-Type: application/json' -d '{"phase":"setup"}' "$endpoint" >"$out/setup.json" \
  || fail "setup failed: $(cat "$out/setup.json")"

# --- the task: the scenario's "## Task", its {{…}} filled ----------------------------------------
task=$(awk '/^## /{ on = ($0 == "## Task"); next } on' "$here/scenarios/$scenario.md" | sed '/./,$!d')
task=${task//\{\{url\}\}/http://localhost:$port}
while IFS=$'\t' read -r key value; do
  task=${task//\{\{$key\}\}/$value}
done < <(jq -r '.params | to_entries[] | [.key, .value] | @tsv' "$out/setup.json")
case $task in *'{{'*) fail "unfilled parameter in: $task" ;; esac
printf '%s\n' "$task" >"$out/task.txt"

# --- the agent: a fresh container, only this arm's MCP server, no built-in tools ------------------
jq --arg arm "$arm" '{mcpServers: {($arm): .mcpServers[$arm]}}' "$here/container/arms.json" >"$out/mcp.json"
mounts=(-v "$claude_bin:/usr/local/bin/claude:ro" -v "$out/mcp.json:/home/node/mcp.json:ro")
[ -z "$server_jar" ] || mounts+=(-v "$server_jar:/opt/unified-test-mcp.jar:ro")
log "the agent is at it"
# How much of the run the host spent stalled on I/O and on CPU (the kernel's pressure counters, in
# microseconds): a busy machine stretches the times, not the tokens.
stalled() { awk '/^some/ { sub("total=", "", $5); print $5 }' "/proc/pressure/$1" 2>/dev/null || echo 0; }
io_before=$(stalled io) cpu_before=$(stalled cpu)
started=$(date +%s)
status=0
CLAUDE_CODE_OAUTH_TOKEN=$(<"$token_file") timeout 1200 env XDG_DATA_HOME="$HOME/.local/share" \
  podman run --rm --network=host --shm-size=1g -e TZ="$tz" -e CLAUDE_CODE_OAUTH_TOKEN "${mounts[@]}" "$image" \
  claude -p "$task" --mcp-config /home/node/mcp.json --strict-mcp-config --tools "" \
    --allowedTools "mcp__${arm}__*" --disable-slash-commands --output-format stream-json --verbose \
    --model "$model" \
  >"$out/task.jsonl" 2>"$out/claude.stderr" || status=$?
wall=$(($(date +%s) - started))
percent() { awk -v a="$1" -v b="$2" -v w="$wall" 'BEGIN { printf "%.1f", w > 0 ? (b - a) / (w * 10000) : 0 }'; }
io_stall=$(percent "$io_before" "$(stalled io)") cpu_stall=$(percent "$cpu_before" "$(stalled cpu)")

# --- the check: the app decides, from its database and the agent's last words ---------------------
answer=$(jq -rs 'map(select(.type == "result")) | last | .result // ""' "$out/task.jsonl")
jq -n --arg answer "$answer" '{phase: "check", answer: $answer}' \
  | curl -s -X POST -H 'Content-Type: application/json' -d @- "$endpoint" >"$out/check.json"

jq -r --arg prefix "mcp__${arm}__" '
  def body: if type == "string" then . else map(if .type == "text" then .text else "[\(.type)]" end) | join("\n") end;
  def indent: gsub("\n"; "\n      ");
  if .type == "assistant" then .message.content[]
    | if .type == "text" then "SAYS  " + (.text | indent)
      elif .type == "tool_use" then "CALL  " + (.name | ltrimstr($prefix)) + " " + (.input | tojson)
      else empty end
  elif .type == "user" then (.message.content | if type == "array" then .[] else empty end)
    | select(.type == "tool_result")
    | (if .is_error then "ERROR " else "OK    " end) + (.content | body | indent)
  elif .type == "result" then
    "END   turns=\(.num_turns) duration=\(.duration_ms / 1000 | floor)s cost=$\(.total_cost_usd) is_error=\(.is_error)"
  else empty end' "$out/task.jsonl" >"$out/transcript.txt" || true

jq -cs --arg scenario "$scenario" --arg arm "$arm" --argjson seed "$seed" --arg model "$model" \
  --arg run "$(basename "$out")" --argjson wall "$wall" --argjson status "$status" \
  --argjson io_stall "$io_stall" --argjson cpu_stall "$cpu_stall" \
  --arg claude "$(basename "$claude_bin")" --arg head "$(git -C "$repo" describe --always --dirty)" \
  --slurpfile check "$out/check.json" --slurpfile setup "$out/setup.json" '
  (map(select(.type == "result")) | last) as $r
  | (map(select(.type == "system" and .subtype == "init")) | first) as $init
  | map(select(.type == "assistant")) as $said
  | [$said[].message.content[]? | select(.type == "tool_use")] as $calls
  | [.[] | select(.type == "user") | .message.content | if type == "array" then .[] else empty end
     | select(.type == "tool_result")] as $results
  | ($check[0] // {}) as $c
  | {run: $run, scenario: $scenario, arm: $arm, seed: $seed, model: $model, claude: $claude, head: $head,
     outcome: (if $r == null or ($r.is_error and (($r.result // "") | test("limit|overloaded|API Error"; "i")))
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
  "$out/task.jsonl" | tee -a "$results/runs.jsonl" >"$out/summary.json"

# A screenshot's base64 is most of a raw transcript's bytes, and committed results keep it out;
# the image block stays, so its count does too.
jq -c 'walk(if type == "object" and .type == "image" and (.source.data? | type) == "string"
  then .source.data = "<\(.source.data | length) base64 chars dropped>" else . end)' \
  "$out/task.jsonl" >"$out/task.jsonl.tmp" && mv "$out/task.jsonl.tmp" "$out/task.jsonl"

log "$(jq -r '"\(.outcome): \(.details // "")  (\(.turns) turns, \(.wall_s)s, $\(.cost_usd))"' "$out/summary.json")"
echo "$out"
