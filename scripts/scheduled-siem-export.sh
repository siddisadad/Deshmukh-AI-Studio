#!/usr/bin/env bash
set -euo pipefail

# Export pending thread DLP events to org SIEM connectors (cron-friendly).
# Requires BILLING_USAGE_SYNC_TOKEN on API and enabled SIEM connectors per org.
#
# Usage:
#   export BILLING_USAGE_SYNC_TOKEN=...
#   ./scripts/scheduled-siem-export.sh
#   API_URL=http://localhost:8080 ./scripts/scheduled-siem-export.sh

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

response="$(curl -fsS --max-time 300 -X POST "${API_URL%/}/api/v1/exports/siem/run" \
  -H "Authorization: Bearer ${TOKEN}")"

echo "$response"
echo "OK: SIEM export run completed"
