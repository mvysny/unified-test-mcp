#!/usr/bin/env bash
# The mechanical half of a blind review (SKILL.md has the rest): builds the server's jar, finds or
# starts test-app, has an isolated `claude -p` do the scenario's task through this MCP server
# only, then critique it from the same session.
#
#   run.sh <scenario>    e.g. run.sh navigate, for scenarios/navigate.md
#
# Leaves everything in a fresh scratch dir outside the checkout, and prints its path last:
#   task.txt, critique.txt    each run's transcript, readable: SAYS / CALL / OK / ERROR / END lines
#   task.jsonl, critique.jsonl    the same, raw stream-json
#   ask.sh "<question>"       asks the blind agent a follow-up, in the same session
#   test-app.log              test-app's output, when this script started it
set -euo pipefail

skill_dir=$(cd "$(dirname "$0")" && pwd)
repo=$(git -C "$skill_dir" rev-parse --show-toplevel)
scenario="$skill_dir/scenarios/${1:?usage: run.sh <scenario>; one of: $(ls "$skill_dir/scenarios" | sed 's/\.md$//' | tr '\n' ' ')}.md"
[ -f "$scenario" ] || { echo "no scenario $scenario" >&2; exit 2; }
server=unified-test-mcp
out=$(mktemp -d "${TMPDIR:-/tmp}/blind-review-$1.XXXX")
mkdir "$out/work"
log() { printf '[blind-review] %s\n' "$*" >&2; }

# --- the jar ---------------------------------------------------------------------------------
log "building the jar"
(cd "$repo" && ./gradlew -q :unified-test-mcp:shadowJar)
jar=$(ls "$repo"/unified-test-mcp/build/libs/unified-test-mcp-*.jar | grep -v -- '-plain\.jar$' | head -1)

# --- test-app: reuse the owner's, or start one on a free port ----------------------------------
listening_ports() { ss -ltnpH | awk -v p="pid=$1," 'index($0, p) { n = split($4, a, ":"); print a[n] }'; }
answers() { [ "$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:$1/")" = 200 ]; }

port=
started=
# by process name, java: `pgrep -f` would match any shell whose command line mentions the class
app_pid=$(ps -C java -o pid=,args= | awk '$NF == "com.vaadin.unifiedtestmcp.testapp.Main" { print $1; exit }')
if [ -n "$app_pid" ]; then
  for p in $(listening_ports "$app_pid"); do
    if answers "$p"; then port=$p; break; fi
  done
  [ -n "$port" ] || { echo "test-app runs (pid $app_pid) but answers on no port it listens on" >&2; exit 1; }
  log "reusing the running test-app on port $port; it stays running"
else
  port=$(python3 -c 'import socket; s = socket.socket(); s.bind(("127.0.0.1", 0)); print(s.getsockname()[1])')
  log "starting test-app on port $port"
  # Vaadin Boot reads SERVER_PORT; Gradle hands the client's environment to the run task's JVM
  (cd "$repo" && SERVER_PORT=$port exec ./gradlew -q --console=plain :test-app:run) >"$out/test-app.log" 2>&1 &
  gradle_pid=$!
  started=1
  stop_app() {
    local pid
    pid=$(ss -ltnpH "sport = :$port" | grep -o 'pid=[0-9]*' | head -1 | cut -d= -f2 || true)
    [ -z "$pid" ] || kill "$pid" 2>/dev/null || true
    kill "$gradle_pid" 2>/dev/null || true
    log "stopped test-app"
  }
  trap stop_app EXIT
  for _ in $(seq 300); do
    answers "$port" && break
    kill -0 "$gradle_pid" 2>/dev/null || { tail -20 "$out/test-app.log" >&2; echo "test-app didn't start" >&2; exit 1; }
    sleep 1
  done
  answers "$port" || { echo "test-app didn't answer on port $port within 300 s" >&2; exit 1; }
fi

# --- the isolated agent ----------------------------------------------------------------------
jq -n --arg java "$(command -v java)" --arg jar "$jar" --arg name "$server" \
  '{mcpServers: {($name): {type: "stdio", command: $java, args: ["-jar", $jar]}}}' >"$out/mcp.json"

# The isolation; every flag's why is in SKILL.md, "Run 1".
flags=(--mcp-config "$out/mcp.json" --strict-mcp-config --tools "" --allowedTools "mcp__${server}__*"
  --disable-slash-commands --output-format stream-json --verbose --model sonnet)

# "## Task" is all the agent gets, {{url}} the app's; the scenario's other sections are the reviewer's.
task=$(awk '/^## /{ on = ($0 == "## Task"); next } on' "$scenario" | sed "s|{{url}}|http://localhost:$port|g")
[ -n "$(printf '%s' "$task" | tr -d '[:space:]')" ] || { echo "$scenario has no '## Task' section" >&2; exit 2; }

readable() {
  jq -r --arg prefix "mcp__${server}__" '
    def body: if type == "string" then . else map(select(.type == "text").text) | join("\n") end;
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
    else empty end'
}

cd "$out/work"
log "run 1, the task"
claude -p "$task" "${flags[@]}" >"$out/task.jsonl"
readable <"$out/task.jsonl" >"$out/task.txt"
session=$(jq -r 'select(.type == "system" and .subtype == "init") | .session_id' "$out/task.jsonl" | head -1)

log "run 2, the critique"
claude -p --resume "$session" "$(cat "$skill_dir/critique.md")" "${flags[@]}" >"$out/critique.jsonl"
readable <"$out/critique.jsonl" >"$out/critique.txt"

# A follow-up question needs the same session, cwd and MCP server; test-app has to be up for a
# question that makes it call a tool.
{
  printf '#!/usr/bin/env bash\n# ask.sh "<question>": a follow-up to the blind agent, in its own session\n'
  printf 'cd %q\n' "$out/work"
  printf 'claude -p --resume %q "$1"' "$session"
  printf ' %q' "${flags[@]}"
  printf ' | tee -a %q | jq -r '\''select(.type == "result").result'\''\n' "$out/ask.jsonl"
} >"$out/ask.sh"
chmod +x "$out/ask.sh"

if [ -n "$started" ]; then trap - EXIT; stop_app; fi
echo "$out"
