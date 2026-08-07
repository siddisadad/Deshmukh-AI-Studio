#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker not available; start PostgreSQL manually (see README docker compose up)" >&2
  exit 0
fi

cd "$ROOT"
docker compose up -d postgres

for _ in $(seq 1 30); do
  if docker compose exec -T postgres pg_isready -U "${DB_USER:-aistudio}" -d "${DB_NAME:-aistudio}" >/dev/null 2>&1; then
    echo "PostgreSQL is ready"
    exit 0
  fi
  sleep 2
done

echo "PostgreSQL did not become ready within 60s" >&2
exit 1
