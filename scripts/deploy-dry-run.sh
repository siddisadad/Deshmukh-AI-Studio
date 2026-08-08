#!/usr/bin/env bash
set -euo pipefail

# Validate production-shaped Compose (docs/13-DEPLOYMENT-GUIDE.md §8) on a clean machine.
# Builds images, boots prod profile, runs healthcheck, tears down.
#
# Usage:
#   ./scripts/deploy-dry-run.sh
#   DRY_RUN_UI_PORT=8090 ./scripts/deploy-dry-run.sh

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is required for deploy dry-run" >&2
  exit 1
fi

if ! docker compose version >/dev/null 2>&1; then
  echo "Docker Compose v2 is required" >&2
  exit 1
fi

export JWT_SECRET="${JWT_SECRET:-deploy-dry-run-secret-key-at-least-32-bytes}"
export DB_PASSWORD="${DB_PASSWORD:-aistudio}"
export DB_NAME="${DB_NAME:-aistudio}"
export DB_USER="${DB_USER:-aistudio}"
# Prod profile requires HTTPS origins without localhost (ProductionCorsValidator).
export CORS_ORIGINS="${CORS_ORIGINS:-https://dry-run.aistudio.test}"
export AI_PROVIDER="${AI_PROVIDER:-mock}"
export DRY_RUN_UI_PORT="${DRY_RUN_UI_PORT:-8090}"

COMPOSE=(
  docker compose
  -f docker-compose.yml
  -f docker-compose.prod.yml
  -f docker-compose.dry-run.yml
  -f docker-compose.worker.yml
  -f docker-compose.worker-prod.yml
)

cleanup() {
  echo "Tearing down dry-run stack…"
  "${COMPOSE[@]}" down -v --remove-orphans 2>/dev/null || true
}

trap cleanup EXIT

echo "Building and starting prod-shaped stack (UI on :${DRY_RUN_UI_PORT})…"
"${COMPOSE[@]}" up -d --build

echo "Waiting for health…"
for i in $(seq 1 60); do
  if curl -fsS --max-time 5 "http://localhost:${DRY_RUN_UI_PORT}/actuator/health" >/dev/null 2>&1 \
    && curl -fsS --max-time 5 -o /dev/null "http://localhost:${DRY_RUN_UI_PORT}/"; then
    break
  fi
  if [ "$i" -eq 60 ]; then
    echo "Stack did not become healthy" >&2
    "${COMPOSE[@]}" logs --no-color || true
    exit 1
  fi
  sleep 5
done

"${ROOT_DIR}/scripts/healthcheck.sh" "http://localhost:${DRY_RUN_UI_PORT}"

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

echo "Deploy dry-run passed (prod profile, edge nginx, Flyway on startup, dedicated job worker)."
