#!/usr/bin/env bash
set -euo pipefail

# Uptime probe for staging/prod edge (nginx) or API directly.
# Usage:
#   ./scripts/healthcheck.sh
#   ./scripts/healthcheck.sh http://localhost:8088
#   ./scripts/healthcheck.sh https://staging.example.com

BASE_URL="${1:-http://localhost:8088}"
BASE_URL="${BASE_URL%/}"

fail() {
  echo "FAIL: $*" >&2
  exit 1
}

echo "Checking ${BASE_URL}/actuator/health"
health_json="$(curl -fsS --max-time 10 "${BASE_URL}/actuator/health")" || fail "health endpoint unreachable"
echo "${health_json}" | grep -q '"status"[[:space:]]*:[[:space:]]*"UP"' || fail "health status not UP: ${health_json}"

echo "Checking ${BASE_URL}/"
curl -fsS --max-time 10 -o /dev/null "${BASE_URL}/" || fail "frontend unreachable"

echo "OK: ${BASE_URL} healthy"
