#!/usr/bin/env bash
set -euo pipefail

# Validate staging-shaped Compose (docs/13-DEPLOYMENT-GUIDE.md §10) locally.
# Builds images (no GHCR), boots prod-profile API + edge UI, healthcheck, teardown.
#
# Usage:
#   ./scripts/staging-dry-run.sh
#   STAGING_UI_PORT=8091 STAGING_API_PORT=8092 ./scripts/staging-dry-run.sh

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is required for staging dry-run" >&2
  exit 1
fi

if ! docker compose version >/dev/null 2>&1; then
  echo "Docker Compose v2 is required" >&2
  exit 1
fi

export JWT_SECRET="${JWT_SECRET:-staging-dry-run-secret-key-at-least-32-bytes}"
export DB_PASSWORD="${DB_PASSWORD:-aistudio}"
export DB_NAME="${DB_NAME:-aistudio}"
export DB_USER="${DB_USER:-aistudio}"
# Prod profile requires HTTPS origins without localhost (ProductionCorsValidator).
export CORS_ORIGINS="${CORS_ORIGINS:-https://staging-dry-run.aistudio.test}"
export AI_PROVIDER="${AI_PROVIDER:-mock}"
export STAGING_UI_PORT="${STAGING_UI_PORT:-8091}"
export STAGING_API_PORT="${STAGING_API_PORT:-8092}"
export BILLING_APP_BASE_URL="${BILLING_APP_BASE_URL:-http://localhost:${STAGING_UI_PORT}}"
export SSO_APP_BASE_URL="${SSO_APP_BASE_URL:-http://localhost:${STAGING_UI_PORT}}"

COMPOSE=(
  docker compose
  -f docker-compose.yml
  -f docker-compose.staging.yml
  -f docker-compose.staging-local.yml
  -f docker-compose.worker.yml
  -f docker-compose.worker-prod.yml
)

cleanup() {
  echo "Tearing down staging dry-run stack…"
  "${COMPOSE[@]}" down -v --remove-orphans 2>/dev/null || true
}

trap cleanup EXIT

echo "Building and starting staging-shaped stack (UI :${STAGING_UI_PORT}, API :${STAGING_API_PORT})…"
"${COMPOSE[@]}" up -d --build postgres
"${COMPOSE[@]}" up -d --build api worker

echo "Waiting for API before edge UI…"
for i in $(seq 1 60); do
  if curl -fsS --max-time 5 "http://localhost:${STAGING_API_PORT}/actuator/health" >/dev/null 2>&1; then
    break
  fi
  if [ "$i" -eq 60 ]; then
    echo "API did not become healthy" >&2
    "${COMPOSE[@]}" logs --no-color api worker || true
    exit 1
  fi
  sleep 5
done

"${COMPOSE[@]}" up -d --build frontend

echo "Waiting for edge health…"
for i in $(seq 1 60); do
  if curl -fsS --max-time 5 "http://localhost:${STAGING_UI_PORT}/actuator/health" >/dev/null 2>&1 \
    && curl -fsS --max-time 5 -o /dev/null "http://localhost:${STAGING_UI_PORT}/" \
    && curl -fsS --max-time 5 "http://localhost:${STAGING_API_PORT}/actuator/health" >/dev/null 2>&1; then
    break
  fi
  if [ "$i" -eq 60 ]; then
    echo "Stack did not become healthy" >&2
    "${COMPOSE[@]}" logs --no-color || true
    exit 1
  fi
  sleep 5
done

"${ROOT_DIR}/scripts/healthcheck.sh" "http://localhost:${STAGING_UI_PORT}"
"${ROOT_DIR}/scripts/api-smoke.sh" "http://localhost:${STAGING_UI_PORT}"
API_URL="http://localhost:${STAGING_API_PORT}" \
  "${ROOT_DIR}/scripts/staging-provider-probes.sh" "http://localhost:${STAGING_UI_PORT}"

STAGING_SIGNOFF_SKIP_DOGFOOD=1 STAGING_SIGNOFF_REQUIRE_HTTPS=0 \
  "${ROOT_DIR}/scripts/staging-signoff.sh" "http://localhost:${STAGING_UI_PORT}"

echo "Checking worker health…"
for i in $(seq 1 30); do
  if "${COMPOSE[@]}" exec -T worker curl -fsS --max-time 5 http://127.0.0.1:8080/actuator/health >/dev/null 2>&1; then
    break
  fi
  if [ "$i" -eq 30 ]; then
    echo "Worker did not become healthy" >&2
    "${COMPOSE[@]}" logs --no-color worker || true
    exit 1
  fi
  sleep 2
done

echo "Staging dry-run passed (prod profile API, dedicated worker, GHCR-shaped ports, Flyway, API smoke)."
