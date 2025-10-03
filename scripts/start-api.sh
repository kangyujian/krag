#!/usr/bin/env bash
set -euo pipefail

# Krag API startup helper
# Usage:
#   scripts/start-api.sh [--port 8080] [--profile dev] [--kill] [--foreground]
#                        [--skip-build] [--log-file <path>] [--no-wait]
#
# Defaults:
#   --port 8080, background start, waits for readiness, logs to logs/krag-api.log

PORT=8080
PROFILE=""
KILL=0
FOREGROUND=0
SKIP_BUILD=0
WAIT_READY=1
LOG_FILE="logs/krag-api.log"

print_usage() {
  cat <<EOF
Usage: scripts/start-api.sh [options]
Options:
  --port <n>        Server port (default: 8080)
  --profile <name>  Spring profile to activate (e.g., dev)
  --kill            Free the port by killing existing processes on it
  --foreground      Run in foreground (no log file, no readiness wait)
  --skip-build      Do not build/repackage; fail if jar missing
  --log-file <path> Log file path when running in background (default: logs/krag-api.log)
  --no-wait         Do not wait for readiness in background mode
  -h, --help        Show this help
EOF
}

while [ $# -gt 0 ]; do
  case "$1" in
    --port)
      PORT=${2:-}
      shift 2 || true
      ;;
    --profile)
      PROFILE=${2:-}
      shift 2 || true
      ;;
    --kill)
      KILL=1
      shift
      ;;
    --foreground)
      FOREGROUND=1
      shift
      ;;
    --skip-build)
      SKIP_BUILD=1
      shift
      ;;
    --log-file)
      LOG_FILE=${2:-}
      shift 2 || true
      ;;
    --no-wait)
      WAIT_READY=0
      shift
      ;;
    -h|--help)
      print_usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      print_usage
      exit 1
      ;;
  esac
done

ROOT_DIR=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT_DIR"

# Build/repackage if jar is missing
JAR_PATH=$(ls -1 krag-api/target/krag-api-*.jar 2>/dev/null | head -n1 || true)
if [ -z "$JAR_PATH" ]; then
  if [ "$SKIP_BUILD" -eq 1 ]; then
    echo "[start-api] ERROR: krag-api jar not found and --skip-build provided" >&2
    exit 1
  fi
  echo "[start-api] Packaging krag-api executable jar..."
  (cd krag-api && mvn -q -DskipTests package spring-boot:repackage)
  JAR_PATH=$(ls -1 krag-api/target/krag-api-*.jar 2>/dev/null | head -n1 || true)
  if [ -z "$JAR_PATH" ]; then
    echo "[start-api] ERROR: krag-api jar not found after repackage" >&2
    exit 1
  fi
fi

# Handle port
PIDS=$(lsof -ti :"$PORT" || true)
if [ -n "$PIDS" ]; then
  if [ "$KILL" -eq 1 ]; then
    echo "[start-api] Killing processes on :$PORT -> $PIDS"
    for PID in $PIDS; do kill "$PID" || true; done
  else
    echo "[start-api] Port :$PORT is in use by: $PIDS"
    echo "[start-api] Use --kill to free the port or choose --port <n>"
    exit 1
  fi
fi

RUN_CMD=(java -jar "$JAR_PATH" "--server.port=$PORT")
if [ -n "$PROFILE" ]; then
  RUN_CMD+=("--spring.profiles.active=$PROFILE")
fi

if [ "$FOREGROUND" -eq 1 ]; then
  echo "[start-api] Starting in foreground on :$PORT"
  echo "[start-api] Command: ${RUN_CMD[*]}"
  exec "${RUN_CMD[@]}"
else
  mkdir -p "$(dirname "$LOG_FILE")" .run
  echo "[start-api] Starting in background on :$PORT, logging to $LOG_FILE"
  nohup "${RUN_CMD[@]}" >>"$LOG_FILE" 2>&1 &
  API_PID=$!
  echo "$API_PID" > .run/krag-api.pid
  echo "[start-api] PID: $API_PID"

  if [ "$WAIT_READY" -eq 1 ]; then
    echo "[start-api] Waiting for readiness..."
    ATTEMPTS=60
    until [ "$ATTEMPTS" -le 0 ]; do
      # Prefer log-based readiness signals
      if grep -q "Tomcat started on port $PORT" "$LOG_FILE" || grep -q "Started KragApplication" "$LOG_FILE"; then
        break
      fi
      CODE=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:$PORT/" || echo 000)
      if [ "$CODE" != "000" ]; then
        break
      fi
      sleep 1
      ATTEMPTS=$((ATTEMPTS-1))
    done
    if [ "$ATTEMPTS" -le 0 ]; then
      echo "[start-api] ERROR: API did not become ready; tail $LOG_FILE for details" >&2
      exit 1
    fi
    echo "[start-api] Ready on http://localhost:$PORT/"
  fi
fi

exit 0