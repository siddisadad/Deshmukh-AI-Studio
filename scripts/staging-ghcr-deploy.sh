#!/usr/bin/env bash
set -euo pipefail

# Deploy staging-shaped stack from published GHCR images (docs/13-DEPLOYMENT-GUIDE.md §10).
#
# Prerequisites:
#   docker login ghcr.io   # PAT with read:packages if images are private
#   cp .env.example .env   # set JWT_SECRET, DB_PASSWORD, CORS_ORIGINS
#
# Usage:
#   ./scripts/staging-ghcr-deploy.sh
#   IMAGE_TAG=sha-abc123 STAGING_UI_PORT=8088 ./scripts/staging-ghcr-deploy.sh

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if [[ -f .env ]]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi

"${ROOT_DIR}/scripts/validate-staging-env.sh"

if ! command -v docker >/dev/null 2>&1 || ! docker compose version >/dev/null 2>&1; then
  echo "Docker Compose v2 is required" >&2
  exit 1
fi

export IMAGE_TAG="${IMAGE_TAG:-main}"
export STAGING_UI_PORT="${STAGING_UI_PORT:-8088}"
export STAGING_API_PORT="${STAGING_API_PORT:-8080}"

COMPOSE=(
  docker compose
  -f docker-compose.yml
  -f docker-compose.staging.yml
  -f docker-compose.worker.yml
  -f docker-compose.worker-prod.yml
  -f docker-compose.worker-staging.yml
)

echo "Pulling GHCR images (tag=${IMAGE_TAG})…"
"${COMPOSE[@]}" pull

echo "Starting staging stack (UI :${STAGING_UI_PORT}, API :${STAGING_API_PORT})…"
"${COMPOSE[@]}" up -d

echo "Waiting for health…"
for i in $(seq 1 60); do
  if curl -fsS --max-time 5 "http://localhost:${STAGING_UI_PORT}/actuator/health" >/dev/null 2>&1 \
    && curl -fsS --max-time 5 -o /dev/null "http://localhost:${STAGING_UI_PORT}/"; then
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

echo "Running post-deploy smoke…"
"${ROOT_DIR}/scripts/post-deploy-smoke.sh" "http://localhost:${STAGING_UI_PORT}"

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

echo "Staging GHCR deploy healthy (IMAGE_TAG=${IMAGE_TAG}, dedicated job worker)."
