#!/usr/bin/env bash
set -euo pipefail

# Compare internal MTD revenue estimates vs Stripe paid invoices (cron-friendly).
# Requires BILLING_USAGE_SYNC_TOKEN on API, BILLING_PROVIDER=stripe, and
# BILLING_RECONCILIATION_ENABLED=true.
#
# Usage:
#   export BILLING_USAGE_SYNC_TOKEN=...
#   ./scripts/scheduled-stripe-reconciliation.sh
#   API_URL=http://localhost:8080 ./scripts/scheduled-stripe-reconciliation.sh

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

response="$(curl -fsS --max-time 300 -X POST "${API_URL%/}/api/v1/billing/stripe/reconcile" \
  -H "Authorization: Bearer ${TOKEN}")"

echo "$response"

if python3 -c "import json,sys; d=json.load(sys.stdin); sys.exit(0 if d.get('mismatched',0)==0 else 1)" <<<"${response}"; then
  echo "OK: Stripe revenue reconciliation completed"
else
  echo "FAIL: Stripe revenue reconciliation reported mismatches" >&2
  exit 1
fi
