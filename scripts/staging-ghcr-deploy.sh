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

if ! command -v docker >/dev/null 2>&1 || ! docker compose version >/dev/null 2>&1; then
  echo "Docker Compose v2 is required" >&2
  exit 1
fi

export IMAGE_TAG="${IMAGE_TAG:-main}"
export STAGING_UI_PORT="${STAGING_UI_PORT:-8088}"
export STAGING_API_PORT="${STAGING_API_PORT:-8080}"

if [[ -z "${JWT_SECRET:-}" ]]; then
  echo "JWT_SECRET is required (set in .env or export)" >&2
  exit 1
fi
if [[ -z "${CORS_ORIGINS:-}" ]]; then
  echo "CORS_ORIGINS is required for prod profile (HTTPS origins)" >&2
  exit 1
fi

COMPOSE=(
  docker compose
  -f docker-compose.yml
  -f docker-compose.staging.yml
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

echo "Staging GHCR deploy healthy (IMAGE_TAG=${IMAGE_TAG})."
