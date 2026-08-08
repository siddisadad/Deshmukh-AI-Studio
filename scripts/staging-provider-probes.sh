#!/usr/bin/env bash
set -euo pipefail

# Automated readiness probes for Stripe, OIDC, SAML, and SMTP before live staging dogfood.
# See docs/24-STAGING-PROVIDER-PROBES-GUIDE.md
#
# Usage:
#   ./scripts/staging-provider-probes.sh
#   ./scripts/staging-provider-probes.sh https://staging.example.com
#   API_URL=http://localhost:8080 ./scripts/staging-provider-probes.sh https://staging.example.com

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
EDGE_URL="${1:-http://localhost:8088}"
API_URL="${API_URL:-}"

billing_provider="${BILLING_PROVIDER:-mock}"
sso_provider="${SSO_PROVIDER:-mock}"
mail_provider="${MAIL_PROVIDER:-logging}"

if [[ -f "${ROOT_DIR}/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "${ROOT_DIR}/.env"
  set +a
  billing_provider="${BILLING_PROVIDER:-mock}"
  sso_provider="${SSO_PROVIDER:-mock}"
  mail_provider="${MAIL_PROVIDER:-logging}"
fi

if [[ -n "$API_URL" ]]; then
  API_BASE="${API_URL%/}/api/v1"
else
  API_BASE="${EDGE_URL%/}/api/v1"
fi

sso_app_base="${SSO_APP_BASE_URL:-${EDGE_URL%/}}"
redirect_uri="${sso_app_base%/}/auth/sso/callback"

json_get() {
  local json="$1"
  local key="$2"
  python3 -c "import json,sys; data=json.load(sys.stdin); print(data${key})" <<<"${json}"
}

probe_http_ok() {
  local label="$1"
  local url="$2"
  local extra_args=("${@:3}")
  local status
  status="$(curl -sS --max-time 20 -o /dev/null -w '%{http_code}' "${extra_args[@]}" "$url")"
  if [[ "$status" != "200" ]]; then
    echo "FAIL: ${label} — expected HTTP 200 from ${url}, got ${status}" >&2
    exit 1
  fi
  echo "OK: ${label}"
}

echo "==> Provider probes (edge ${EDGE_URL}, API ${API_BASE})"

echo "==> SSO providers list"
providers_json="$(curl -fsS --max-time 15 "${API_BASE}/auth/sso/providers")"
if ! printf '%s' "${providers_json}" | grep -q '"id"'; then
  echo "FAIL: /auth/sso/providers returned unexpected body: ${providers_json}" >&2
  exit 1
fi
echo "OK: SSO providers list"

echo "==> Billing plans (authenticated)"
probe_suffix="$(date +%s)-${RANDOM}"
probe_email="provider-probe-${probe_suffix}@example.com"
probe_password="Str0ngPass!ProviderProbe"
register_json="$(curl -fsS --max-time 20 -X POST "${API_BASE}/auth/register" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"${probe_email}\",\"password\":\"${probe_password}\",\"displayName\":\"Provider Probe\"}")"
probe_token="$(json_get "${register_json}" "['accessToken']")"
plans_json="$(curl -fsS --max-time 15 "${API_BASE}/billing/plans" \
  -H "Authorization: Bearer ${probe_token}")"
if ! printf '%s' "${plans_json}" | grep -q 'FREE'; then
  echo "FAIL: /billing/plans missing FREE plan: ${plans_json}" >&2
  exit 1
fi
echo "OK: billing plans"

probe_sso_start() {
  local provider_id="$1"
  local hint="${2:-}"
  local payload
  if [[ -n "$hint" ]]; then
    payload="{\"provider\":\"${provider_id}\",\"redirectUri\":\"${redirect_uri}\",\"loginHint\":\"${hint}\"}"
  else
    payload="{\"provider\":\"${provider_id}\",\"redirectUri\":\"${redirect_uri}\"}"
  fi
  local start_json
  start_json="$(curl -fsS --max-time 20 -X POST "${API_BASE}/auth/sso/start" \
    -H 'Content-Type: application/json' \
    -d "${payload}")"
  local auth_url
  auth_url="$(json_get "${start_json}" "['authorizationUrl']")"
  if [[ -z "$auth_url" ]]; then
    echo "FAIL: SSO start (${provider_id}) missing authorizationUrl: ${start_json}" >&2
    exit 1
  fi
  echo "OK: SSO start (${provider_id})"
}

if [[ "$billing_provider" == "stripe" ]]; then
  echo "==> Stripe API key + price IDs"
  for price_var in STRIPE_PRO_PRICE_ID STRIPE_TEAM_PRICE_ID; do
    price_id="${!price_var:-}"
    if [[ -z "$price_id" ]]; then
      echo "FAIL: ${price_var} is required when BILLING_PROVIDER=stripe" >&2
      exit 1
    fi
    status="$(curl -sS --max-time 20 -o /dev/null -w '%{http_code}' \
      -u "${STRIPE_API_KEY}:" "https://api.stripe.com/v1/prices/${price_id}")"
    if [[ "$status" != "200" ]]; then
      echo "FAIL: Stripe price ${price_id} (${price_var}) returned HTTP ${status}" >&2
      exit 1
    fi
    echo "OK: Stripe price ${price_var}"
  done
else
  echo "==> Billing provider mock (skip Stripe API probes)"
fi

if [[ "$sso_provider" == "oidc" ]]; then
  echo "==> OIDC issuer discovery"
  issuer="${OIDC_ISSUER_URI%/}"
  discovery_url="${issuer}/.well-known/openid-configuration"
  discovery_json="$(curl -fsS --max-time 20 "${discovery_url}")"
  discovered_issuer="$(json_get "${discovery_json}" "['issuer']")"
  if [[ "${discovered_issuer%/}" != "${issuer}" ]]; then
    echo "FAIL: OIDC discovery issuer mismatch: expected ${issuer}, got ${discovered_issuer}" >&2
    exit 1
  fi
  echo "OK: OIDC discovery (${discovery_url})"
  probe_sso_start "oidc" "probe-${RANDOM}@example.com"
elif [[ "$sso_provider" == "saml" ]]; then
  saml_stub="${SAML_STUB_MODE:-true}"
  if [[ "$saml_stub" == "false" ]]; then
    echo "==> SAML IdP metadata"
    probe_http_ok "SAML IdP metadata" "${SAML_METADATA_URL}"
    echo "==> SAML SP metadata"
    sp_xml="$(curl -fsS --max-time 20 "${API_BASE}/auth/sso/saml/metadata")"
    if ! printf '%s' "${sp_xml}" | grep -q 'EntityDescriptor'; then
      echo "FAIL: SP metadata XML missing EntityDescriptor" >&2
      exit 1
    fi
    if [[ -n "${SAML_ENTITY_ID:-}" ]] && ! printf '%s' "${sp_xml}" | grep -q "${SAML_ENTITY_ID}"; then
      echo "FAIL: SP metadata missing SAML_ENTITY_ID=${SAML_ENTITY_ID}" >&2
      exit 1
    fi
    echo "OK: SAML SP metadata"
    probe_sso_start "saml"
  else
    echo "==> SAML stub mode"
    probe_sso_start "saml"
  fi
else
  echo "==> SSO mock start"
  probe_sso_start "mock" "probe-${RANDOM}@example.com"
fi

if [[ "$mail_provider" == "smtp" ]]; then
  echo "==> SMTP TCP connect"
  mail_host="${MAIL_HOST:-}"
  mail_port="${MAIL_PORT:-587}"
  if [[ -z "$mail_host" ]]; then
    echo "FAIL: MAIL_HOST required when MAIL_PROVIDER=smtp" >&2
    exit 1
  fi
  if ! timeout 10 bash -c "echo >/dev/tcp/${mail_host}/${mail_port}" 2>/dev/null; then
    echo "FAIL: cannot reach SMTP ${mail_host}:${mail_port}" >&2
    exit 1
  fi
  echo "OK: SMTP ${mail_host}:${mail_port} reachable"
else
  echo "==> Mail provider ${mail_provider} (skip SMTP connect)"
fi

echo "OK: staging provider probes passed"
