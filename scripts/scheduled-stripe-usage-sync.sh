#!/usr/bin/env bash
set -euo pipefail

# Report seat + AI overage usage to Stripe metered subscription items (cron-friendly).
# Requires BILLING_USAGE_SYNC_TOKEN on API and BILLING_PROVIDER=stripe.
#
# Usage:
#   export BILLING_USAGE_SYNC_TOKEN=...
#   ./scripts/scheduled-stripe-usage-sync.sh
#   API_URL=http://localhost:8080 ./scripts/scheduled-stripe-usage-sync.sh

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

response="$(curl -fsS --max-time 120 -X POST "${API_URL%/}/api/v1/billing/stripe/sync-metered-usage" \
  -H "Authorization: Bearer ${TOKEN}")"

echo "$response"

if python3 -c "import json,sys; d=json.load(sys.stdin); sys.exit(0 if d.get('failed',0)==0 else 1)" <<<"${response}"; then
  echo "OK: Stripe metered usage sync completed"
else
  echo "FAIL: Stripe metered usage sync reported failures" >&2
  exit 1
fi
