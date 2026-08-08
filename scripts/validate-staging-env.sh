#!/usr/bin/env bash
set -euo pipefail

# Validate .env / shell exports before staging or production compose deploy.
# Sources .env from repo root when present.
#
# Usage:
#   ./scripts/validate-staging-env.sh
#   source .env && ./scripts/validate-staging-env.sh

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if [[ -f .env ]]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi

missing=()

require() {
  local name="$1"
  if [[ -z "${!name:-}" ]]; then
    missing+=("$name")
  fi
}

require JWT_SECRET
require CORS_ORIGINS
require DB_PASSWORD

billing_provider="${BILLING_PROVIDER:-mock}"
sso_provider="${SSO_PROVIDER:-mock}"

if [[ "$billing_provider" == "stripe" ]]; then
  require STRIPE_API_KEY
  require STRIPE_WEBHOOK_SECRET
  require STRIPE_PRO_PRICE_ID
  require STRIPE_TEAM_PRICE_ID
fi

if [[ "$sso_provider" == "oidc" ]]; then
  require OIDC_ISSUER_URI
  require OIDC_CLIENT_ID
  require OIDC_CLIENT_SECRET
fi

if [[ ${#missing[@]} -gt 0 ]]; then
  echo "Missing required environment variables:" >&2
  for v in "${missing[@]}"; do
    echo "  - $v" >&2
  done
  echo "See .env.example and docs/13-DEPLOYMENT-GUIDE.md" >&2
  exit 1
fi

echo "Staging/production env OK (BILLING_PROVIDER=${billing_provider}, SSO_PROVIDER=${sso_provider})."
