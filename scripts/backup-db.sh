#!/usr/bin/env bash
set -euo pipefail

# Backup Postgres from the Compose stack.
# Usage:
#   ./scripts/backup-db.sh
#   ./scripts/backup-db.sh ./backups

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="${1:-$ROOT_DIR/backups}"
STAMP="$(date +%Y%m%d-%H%M%S)"
FILE="$OUT_DIR/aistudio-$STAMP.sql.gz"

mkdir -p "$OUT_DIR"

COMPOSE=(docker compose)
if [[ -f "$ROOT_DIR/docker-compose.prod.yml" ]]; then
  # Prefer prod overlay when present; still works with base-only.
  if docker compose -f "$ROOT_DIR/docker-compose.yml" -f "$ROOT_DIR/docker-compose.prod.yml" ps --status running postgres >/dev/null 2>&1; then
    COMPOSE=(docker compose -f "$ROOT_DIR/docker-compose.yml" -f "$ROOT_DIR/docker-compose.prod.yml")
  else
    COMPOSE=(docker compose -f "$ROOT_DIR/docker-compose.yml")
  fi
fi

DB_USER="${DB_USER:-aistudio}"
DB_NAME="${DB_NAME:-aistudio}"

echo "Writing $FILE"
"${COMPOSE[@]}" exec -T postgres pg_dump -U "$DB_USER" "$DB_NAME" | gzip > "$FILE"
echo "Backup complete: $FILE"
