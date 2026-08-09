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
org_id="$(json_get "${register_json}" "['organization']['id']")"

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

if [[ -n "${LOKI_QUERY_REGIONS:-}" ]]; then
  echo "==> Loki multi-region readiness"
  IFS=',' read -ra loki_entries <<<"${LOKI_QUERY_REGIONS}"
  loki_idx=0
  for entry in "${loki_entries[@]}"; do
    entry="${entry#"${entry%%[![:space:]]*}"}"
    entry="${entry%"${entry##*[![:space:]]}"}"
    if [[ -z "$entry" ]]; then
      continue
    fi
    if [[ "$entry" == *"="* ]]; then
      loki_region="${entry%%=*}"
      loki_url="${entry#*=}"
      loki_region="${loki_region#"${loki_region%%[![:space:]]*}"}"
      loki_region="${loki_region%"${loki_region##*[![:space:]]}"}"
      loki_url="${loki_url#"${loki_url%%[![:space:]]*}"}"
      loki_url="${loki_url%"${loki_url##*[![:space:]]}"}"
    else
      loki_idx=$((loki_idx + 1))
      loki_region="region-${loki_idx}"
      loki_url="$entry"
      loki_url="${loki_url#"${loki_url%%[![:space:]]*}"}"
      loki_url="${loki_url%"${loki_url##*[![:space:]]}"}"
    fi
    loki_url="${loki_url%/}"
    probe_http_ok "Loki ${loki_region} ready" "${loki_url}/ready"
  done
else
  echo "==> Loki multi-region (skip — LOKI_QUERY_REGIONS not set)"
fi

ai_provider="${AI_PROVIDER:-mock}"
if [[ "$ai_provider" == "mock" ]]; then
  echo "==> AI provider mock (skip live API probes)"
else
  echo "==> AI provider health (probe=true)"
  health_json="$(curl -fsS --max-time 45 "${API_BASE}/assistants/provider-health?probe=true" \
    -H "Authorization: Bearer ${probe_token}")"
  if ! printf '%s' "${health_json}" | grep -q '"id"'; then
    echo "FAIL: /assistants/provider-health returned unexpected body: ${health_json}" >&2
    exit 1
  fi
  python3 - "${health_json}" <<'PY'
import json, sys
data = json.loads(sys.argv[1])
providers = data.get("providers") or []
if not providers:
    raise SystemExit("FAIL: no providers in health response")
for p in providers:
    pid = p.get("id")
    if p.get("probeStatus") != "up":
        raise SystemExit(f"FAIL: AI provider {pid} probeStatus={p.get('probeStatus')}")
print(f"OK: AI provider probes ({len(providers)} configured)")
PY
fi

git_provider="${GIT_METADATA_PROVIDER:-mock}"
if [[ "$git_provider" == "mock" ]]; then
  echo "==> Git metadata mock (skip live git API probes)"
else
  echo "==> Git host API tokens"
  if [[ -n "${GITHUB_SYNC_TOKEN:-}" ]]; then
    github_base="${GITHUB_API_BASE_URL:-https://api.github.com}"
    github_base="${github_base%/}"
    status="$(curl -sS --max-time 20 -o /dev/null -w '%{http_code}' \
      -H "Authorization: Bearer ${GITHUB_SYNC_TOKEN}" \
      -H "Accept: application/vnd.github+json" \
      "${github_base}/user")"
    if [[ "$status" != "200" ]]; then
      echo "FAIL: GitHub token probe returned HTTP ${status}" >&2
      exit 1
    fi
    echo "OK: GitHub API token"
  else
    echo "==> GitHub token not set (skip)"
  fi
  if [[ -n "${GITLAB_SYNC_TOKEN:-}" ]]; then
    gitlab_base="${GITLAB_API_BASE_URL:-https://gitlab.com/api/v4}"
    gitlab_base="${gitlab_base%/}"
    status="$(curl -sS --max-time 20 -o /dev/null -w '%{http_code}' \
      -H "PRIVATE-TOKEN: ${GITLAB_SYNC_TOKEN}" \
      "${gitlab_base}/user")"
    if [[ "$status" != "200" ]]; then
      echo "FAIL: GitLab token probe returned HTTP ${status}" >&2
      exit 1
    fi
    echo "OK: GitLab API token"
  else
    echo "==> GitLab token not set (skip)"
  fi
  if [[ -n "${BITBUCKET_SYNC_TOKEN:-}" ]]; then
    bitbucket_base="${BITBUCKET_API_BASE_URL:-https://api.bitbucket.org/2.0}"
    bitbucket_base="${bitbucket_base%/}"
    status="$(curl -sS --max-time 20 -o /dev/null -w '%{http_code}' \
      -H "Authorization: Bearer ${BITBUCKET_SYNC_TOKEN}" \
      "${bitbucket_base}/user")"
    if [[ "$status" != "200" ]]; then
      echo "FAIL: Bitbucket token probe returned HTTP ${status}" >&2
      exit 1
    fi
    echo "OK: Bitbucket API token"
  else
    echo "==> Bitbucket token not set (skip)"
  fi

  echo "==> Org git credential API probe"
  api_probe_provider=""
  if [[ -n "${GITHUB_SYNC_TOKEN:-}" ]]; then
    api_probe_provider="github"
  elif [[ -n "${GITLAB_SYNC_TOKEN:-}" ]]; then
    api_probe_provider="gitlab"
  elif [[ -n "${BITBUCKET_SYNC_TOKEN:-}" ]]; then
    api_probe_provider="bitbucket"
  fi
  if [[ -n "$api_probe_provider" ]]; then
    test_json="$(curl -fsS --max-time 20 -X POST \
      "${API_BASE}/organizations/${org_id}/git-credentials/${api_probe_provider}/test" \
      -H "Authorization: Bearer ${probe_token}")"
    if ! printf '%s' "${test_json}" | grep -q '"ok":true'; then
      echo "FAIL: git credential test (${api_probe_provider}) returned: ${test_json}" >&2
      exit 1
    fi
    echo "OK: org git credential API probe (${api_probe_provider})"
  else
    echo "==> No git tokens configured (skip API probe)"
  fi
fi

echo "OK: staging provider probes passed"
