#!/usr/bin/env bash
set -euo pipefail

# Post-deploy smoke checks after staging/prod compose is up (docs/13-DEPLOYMENT-GUIDE.md §11).
#
# Usage:
#   ./scripts/post-deploy-smoke.sh
#   ./scripts/post-deploy-smoke.sh https://staging.example.com

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BASE_URL="${1:-http://localhost:8088}"

echo "==> Edge health + SPA"
"${ROOT_DIR}/scripts/healthcheck.sh" "$BASE_URL"

echo "==> Actuator info (public)"
info_json="$(curl -fsS --max-time 10 "${BASE_URL%/}/actuator/info")"
if ! printf '%s' "${info_json}" | grep -q '"app"'; then
  echo "FAIL: actuator/info missing app metadata: ${info_json}" >&2
  exit 1
fi

echo "==> Prometheus metrics (must not be public on edge)"
status="$(curl -sS --max-time 10 -o /dev/null -w '%{http_code}' "${BASE_URL%/}/actuator/prometheus")"
if [[ "$status" == "200" ]]; then
  echo "FAIL: /actuator/prometheus is reachable without auth on the edge (expected 401/403/404)" >&2
  exit 1
fi
echo "Prometheus edge status ${status} (OK — scrape api:8080 internally with auth)"

echo "OK: post-deploy smoke passed for ${BASE_URL}"
