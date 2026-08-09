#!/usr/bin/env bash
set -euo pipefail

# Send staged dunning reminders for past-due subscriptions (cron-friendly).
# Requires BILLING_USAGE_SYNC_TOKEN on API and BILLING_DUNNING_ENABLED=true.
#
# Usage:
#   export BILLING_USAGE_SYNC_TOKEN=...
#   ./scripts/scheduled-billing-dunning.sh
#   API_URL=http://localhost:8080 ./scripts/scheduled-billing-dunning.sh

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

response="$(curl -fsS --max-time 300 -X POST "${API_URL%/}/api/v1/billing/stripe/dunning/run" \
  -H "Authorization: Bearer ${TOKEN}")"

echo "$response"
echo "OK: Billing dunning run completed"
