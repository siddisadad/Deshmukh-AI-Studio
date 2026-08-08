#!/usr/bin/env bash
set -euo pipefail

# Staging dogfood checklist — automated gates + manual follow-ups (docs/13-DEPLOYMENT-GUIDE.md).
#
# Usage:
#   cp .env.example .env   # set HTTPS CORS_ORIGINS, DB_PASSWORD, JWT_SECRET, optional Stripe/OIDC
#   ./scripts/staging-dogfood.sh
#   ./scripts/staging-dogfood.sh https://staging.example.com
#
# Env:
#   API_URL — direct API base (default http://localhost:8080) for internal metrics check
#   METRICS_SCRAPE_TOKEN — when set, verifies /actuator/prometheus on API_URL

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
EDGE_URL="${1:-http://localhost:8088}"
API_URL="${API_URL:-http://localhost:8080}"

billing_provider="${BILLING_PROVIDER:-mock}"
sso_provider="${SSO_PROVIDER:-mock}"

if [[ -f "${ROOT_DIR}/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "${ROOT_DIR}/.env"
  set +a
  billing_provider="${BILLING_PROVIDER:-mock}"
  sso_provider="${SSO_PROVIDER:-mock}"
fi

echo "==> 1/6 validate-staging-env.sh"
"${ROOT_DIR}/scripts/validate-staging-env.sh"

echo "==> 2/6 healthcheck (edge)"
"${ROOT_DIR}/scripts/healthcheck.sh" "${EDGE_URL}"

echo "==> 3/6 post-deploy-smoke (edge)"
"${ROOT_DIR}/scripts/post-deploy-smoke.sh" "${EDGE_URL}"

echo "==> 4/7 api-smoke (authenticated journey)"
"${ROOT_DIR}/scripts/api-smoke.sh" "${EDGE_URL}"

echo "==> 5/7 staging-provider-probes (Stripe/OIDC/SAML/SMTP readiness)"
"${ROOT_DIR}/scripts/staging-provider-probes.sh" "${EDGE_URL}"

if [[ -n "${METRICS_SCRAPE_TOKEN:-}" ]]; then
  echo "==> 6/7 internal Prometheus metrics (${API_URL})"
  status="$(curl -sS --max-time 15 -o /dev/null -w '%{http_code}' \
    -H "Authorization: Bearer ${METRICS_SCRAPE_TOKEN}" \
    "${API_URL%/}/actuator/prometheus")"
  if [[ "$status" != "200" ]]; then
    echo "FAIL: expected 200 from internal /actuator/prometheus, got ${status}" >&2
    exit 1
  fi
  if ! curl -fsS --max-time 15 \
    -H "Authorization: Bearer ${METRICS_SCRAPE_TOKEN}" \
    "${API_URL%/}/actuator/prometheus" | grep -q 'http_server_requests'; then
    echo "FAIL: prometheus body missing http_server_requests metrics" >&2
    exit 1
  fi
  echo "Internal metrics OK"
else
  echo "==> 6/7 skipped — set METRICS_SCRAPE_TOKEN to verify internal Prometheus scrape"
fi

echo "==> 7/7 manual dogfood (operator)"
echo "  - Register or login; create org project; requirements + tasks + documents"
echo "  - AI chat: multi-thread + streaming (retry reconnect if mid-stream drop)"
echo "  - RAG: upload/index document and search project context"
if [[ "$billing_provider" == "stripe" ]]; then
  echo "  - Stripe: checkout (test card), portal, webhook delivery to /api/v1/billing/stripe/webhook"
else
  echo "  - Billing: mock provider (set BILLING_PROVIDER=stripe for real dogfood)"
fi
if [[ "$sso_provider" == "oidc" ]]; then
  echo "  - OIDC: SSO login start + callback on ${SSO_APP_BASE_URL:-<app-host>}/auth/sso/callback"
else
  echo "  - SSO: mock provider (set SSO_PROVIDER=oidc for real IdP dogfood)"
fi
mail_provider="${MAIL_PROVIDER:-logging}"
if [[ "$mail_provider" == "smtp" ]]; then
  echo "  - Mail: forgot-password email delivery via SMTP (${MAIL_HOST:-smtp})"
else
  echo "  - Mail: logging provider (set MAIL_PROVIDER=smtp for real email)"
fi
echo "  - Monitoring: optional docker-compose.monitoring.yml + Grafana dashboard"
echo ""
echo "OK: automated staging dogfood gates passed for edge ${EDGE_URL}"
