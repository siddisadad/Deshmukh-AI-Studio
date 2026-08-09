#!/usr/bin/env bash
set -euo pipefail

# Scheduled worker autoscale — reads queue metrics API and optionally applies WORKER_REPLICAS.
# See docs/61-K8S-HPA-WORKER-AUTOSCALING-GUIDE.md
#
# Usage:
#   export BILLING_USAGE_SYNC_TOKEN=...
#   ./scripts/scheduled-worker-autoscale.sh
#   WORKER_AUTOSCALE_APPLY=1 ./scripts/scheduled-worker-autoscale.sh

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if [[ -f .env ]]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi

API_URL="${API_URL:-http://localhost:8080}"
TOKEN="${BILLING_USAGE_SYNC_TOKEN:-}"

if [[ -z "$TOKEN" ]]; then
  echo "BILLING_USAGE_SYNC_TOKEN is required" >&2
  exit 1
fi

response="$(curl -fsS --max-time 30 \
  "${API_URL%/}/api/v1/ops/jobs/queue" \
  -H "Authorization: Bearer ${TOKEN}")"

echo "$response"

suggested="$(python3 -c "import json,sys; print(json.load(sys.stdin)['suggestedReplicas'])" <<<"$response")"
current="${WORKER_REPLICAS:-1}"
apply="${WORKER_AUTOSCALE_APPLY:-0}"

echo "Suggested WORKER_REPLICAS=${suggested} (current=${current})"

if [[ "$apply" == "1" && "$suggested" != "$current" ]]; then
  echo "Applying scale via staging-ghcr-deploy.sh"
  export WORKER_REPLICAS="$suggested"
  "${ROOT_DIR}/scripts/staging-ghcr-deploy.sh"
else
  echo "No apply (set WORKER_AUTOSCALE_APPLY=1 to scale Compose staging)"
fi

echo "OK: worker autoscale check completed"
