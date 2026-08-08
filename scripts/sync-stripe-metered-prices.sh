#!/usr/bin/env bash
set -euo pipefail

# Validate Stripe metered price IDs for seat and AI overage billing.
# See docs/32-STRIPE-METERED-PRICES-SYNC-GUIDE.md
#
# Usage:
#   source .env   # STRIPE_API_KEY + metered price env vars
#   ./scripts/sync-stripe-metered-prices.sh

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if [[ -f "${ROOT_DIR}/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "${ROOT_DIR}/.env"
  set +a
fi

if [[ -z "${STRIPE_API_KEY:-}" ]]; then
  echo "STRIPE_API_KEY is required" >&2
  exit 1
fi

probe_price() {
  local label="$1"
  local price_id="$2"
  local expect_metered="${3:-}"
  if [[ -z "$price_id" ]]; then
    echo "SKIP: ${label} (not set)"
    return 0
  fi
  local body status
  body="$(curl -sS --max-time 20 -u "${STRIPE_API_KEY}:" "https://api.stripe.com/v1/prices/${price_id}")"
  status="$(python3 -c "import json,sys; print(json.load(sys.stdin).get('active', False))" <<<"${body}")"
  if [[ "$status" != "True" ]]; then
    echo "FAIL: ${label} price ${price_id} is not active" >&2
    exit 1
  fi
  if [[ -n "$expect_metered" ]]; then
  recurring="$(python3 -c "import json,sys; d=json.load(sys.stdin); print((d.get('recurring') or {}).get('usage_type',''))" <<<"${body}")"
    if [[ "$recurring" != "metered" ]]; then
      echo "FAIL: ${label} price ${price_id} recurring.usage_type=${recurring:-none} (expected metered)" >&2
      exit 1
    fi
  fi
  echo "OK: ${label} (${price_id})"
}

echo "==> Stripe metered price validation"
probe_price "STRIPE_PRO_PRICE_ID" "${STRIPE_PRO_PRICE_ID:-}" ""
probe_price "STRIPE_TEAM_PRICE_ID" "${STRIPE_TEAM_PRICE_ID:-}" ""
probe_price "STRIPE_PRO_SEAT_METERED_PRICE_ID" "${STRIPE_PRO_SEAT_METERED_PRICE_ID:-}" metered
probe_price "STRIPE_TEAM_SEAT_METERED_PRICE_ID" "${STRIPE_TEAM_SEAT_METERED_PRICE_ID:-}" metered
probe_price "STRIPE_PRO_AI_OVERAGE_METERED_PRICE_ID" "${STRIPE_PRO_AI_OVERAGE_METERED_PRICE_ID:-}" metered
probe_price "STRIPE_TEAM_AI_OVERAGE_METERED_PRICE_ID" "${STRIPE_TEAM_AI_OVERAGE_METERED_PRICE_ID:-}" metered
echo "OK: Stripe metered prices validated"
