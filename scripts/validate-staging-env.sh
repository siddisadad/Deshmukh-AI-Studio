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
  require SSO_APP_BASE_URL
fi

if [[ "$sso_provider" == "saml" ]]; then
  require SSO_APP_BASE_URL
  saml_stub="${SAML_STUB_MODE:-true}"
  if [[ "$saml_stub" == "false" ]]; then
    require SAML_METADATA_URL
    require SAML_ENTITY_ID
  fi
fi

mail_provider="${MAIL_PROVIDER:-logging}"
if [[ "$mail_provider" == "smtp" ]]; then
  require MAIL_HOST
  require MAIL_FROM
  if [[ -z "${MAIL_USERNAME:-}" && -z "${MAIL_USER:-}" ]]; then
    missing+=("MAIL_USERNAME or MAIL_USER")
  fi
  require MAIL_PASSWORD
fi

loki_store="${LOKI_OBJECT_STORE:-filesystem}"
if [[ "$loki_store" == "s3" ]]; then
  require LOKI_S3_BUCKET
  require LOKI_S3_REGION
  require LOKI_S3_ACCESS_KEY_ID
  require LOKI_S3_SECRET_ACCESS_KEY
fi

if [[ ${#missing[@]} -gt 0 ]]; then
  echo "Missing required environment variables:" >&2
  for v in "${missing[@]}"; do
    echo "  - $v" >&2
  done
  echo "See .env.example and docs/13-DEPLOYMENT-GUIDE.md" >&2
  exit 1
fi

# Staging/prod compose uses SPRING_PROFILES_ACTIVE=prod (ProductionCorsValidator).
IFS=',' read -ra cors_origins <<< "$CORS_ORIGINS"
for origin in "${cors_origins[@]}"; do
  trimmed="${origin#"${origin%%[![:space:]]*}"}"
  trimmed="${trimmed%"${trimmed##*[![:space:]]}"}"
  if [[ -z "$trimmed" ]]; then
    continue
  fi
  if [[ ! "$trimmed" =~ ^https:// ]]; then
    echo "CORS_ORIGINS entry must use HTTPS for staging/production: $trimmed" >&2
    echo "Use ./scripts/staging-dry-run.sh for local prod-profile validation with test HTTPS origins." >&2
    exit 1
  fi
  if [[ "$trimmed" =~ localhost ]] || [[ "$trimmed" =~ 127\. ]]; then
    echo "CORS_ORIGINS must not include localhost for staging/production: $trimmed" >&2
    echo "Set CORS_ORIGINS=https://<your-staging-host> (see docs/13-DEPLOYMENT-GUIDE.md)." >&2
    exit 1
  fi
done

echo "Staging/production env OK (BILLING_PROVIDER=${billing_provider}, SSO_PROVIDER=${sso_provider}, MAIL_PROVIDER=${mail_provider})."
