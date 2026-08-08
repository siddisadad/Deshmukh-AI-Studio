#!/usr/bin/env bash
set -euo pipefail

# Suggest WORKER_REPLICAS from background job queue depth (Prometheus or actuator scrape).
# Does not change running containers — operator applies scale via deploy script.
#
# Usage:
#   ./scripts/worker-scale-hint.sh
#   PROMETHEUS_URL=http://localhost:9090 ./scripts/worker-scale-hint.sh
#   METRICS_URL=http://localhost:8080/actuator/prometheus METRICS_SCRAPE_TOKEN=... ./scripts/worker-scale-hint.sh

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if [[ -f .env ]]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi

PROMETHEUS_URL="${PROMETHEUS_URL:-http://localhost:9090}"
METRICS_URL="${METRICS_URL:-http://localhost:8080/actuator/prometheus}"
MAX_REPLICAS="${WORKER_REPLICAS_MAX:-6}"
TARGET_PENDING_PER_WORKER="${WORKER_TARGET_PENDING_PER_REPLICA:-10}"

fetch_depth() {
  local status="$1"
  local query="aistudio_jobs_queue_depth{status=\"${status}\"}"

  if command -v python3 >/dev/null; then
    python3 - "$PROMETHEUS_URL" "$METRICS_URL" "$query" "$status" "${METRICS_SCRAPE_TOKEN:-}" <<'PY'
import json
import re
import sys
import urllib.parse
import urllib.request

prom_url, metrics_url, query, status, token = sys.argv[1:6]
line_pattern = re.compile(
    rf"^aistudio_jobs_queue_depth{{[^}}]*status=\"{status}\"[^}}]*}}\s+([0-9.eE+-]+)"
)

def fetch(url, bearer=None):
    req = urllib.request.Request(url)
    if bearer:
        req.add_header("Authorization", f"Bearer {bearer}")
    with urllib.request.urlopen(req, timeout=10) as resp:
        return resp.read().decode()

try:
    q = urllib.parse.urlencode({"query": query})
    url = f"{prom_url.rstrip('/')}/api/v1/query?{q}"
    payload = json.loads(fetch(url))
    results = payload.get("data", {}).get("result", [])
    if results:
        print(float(results[0]["value"][1]))
        sys.exit(0)
except Exception:
    pass

for line in fetch(metrics_url, token or None).splitlines():
    m = line_pattern.match(line.strip())
    if m:
        print(float(m.group(1)))
        sys.exit(0)

print(f"metric aistudio_jobs_queue_depth status={status} not found", file=sys.stderr)
sys.exit(1)
PY
  else
    echo "python3 is required to parse metrics" >&2
    exit 1
  fi
}

pending="$(fetch_depth pending)"
running="$(fetch_depth running)"
failed="$(fetch_depth failed)"

suggested=$(( (pending + TARGET_PENDING_PER_WORKER - 1) / TARGET_PENDING_PER_WORKER ))
if [ "$suggested" -lt 1 ]; then
  suggested=1
fi
if [ "$suggested" -gt "$MAX_REPLICAS" ]; then
  suggested="$MAX_REPLICAS"
fi

current="${WORKER_REPLICAS:-1}"

echo "Background job queue depth:"
echo "  pending=${pending} running=${running} failed=${failed}"
echo "Suggested WORKER_REPLICAS=${suggested} (target ~${TARGET_PENDING_PER_WORKER} pending per worker, max ${MAX_REPLICAS})"
echo "Current WORKER_REPLICAS=${current}"

if [ "$suggested" -gt "$current" ]; then
  echo "Action: scale up — WORKER_REPLICAS=${suggested} ./scripts/staging-ghcr-deploy.sh"
elif [ "$suggested" -lt "$current" ] && [ "${pending%.*}" -eq 0 ] && [ "${running%.*}" -eq 0 ]; then
  echo "Action: optional scale down — WORKER_REPLICAS=${suggested} ./scripts/staging-ghcr-deploy.sh"
else
  echo "Action: no change recommended"
fi

pending_int="${pending%.*}"
if [ "$pending_int" -gt 50 ]; then
  echo "Note: pending > 50 — BackgroundJobQueueDepthHigh alert may fire (monitoring/alerts.yml)"
fi
