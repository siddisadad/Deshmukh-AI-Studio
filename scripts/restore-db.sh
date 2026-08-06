#!/usr/bin/env bash
set -euo pipefail

# Restore Postgres into the Compose stack.
# Usage:
#   ./scripts/restore-db.sh ./backups/aistudio-YYYYMMDD-HHMMSS.sql.gz

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <backup.sql|backup.sql.gz>" >&2
  exit 1
fi

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKUP="$1"

if [[ ! -f "$BACKUP" ]]; then
  echo "Backup not found: $BACKUP" >&2
  exit 1
fi

COMPOSE=(docker compose -f "$ROOT_DIR/docker-compose.yml")
if docker compose -f "$ROOT_DIR/docker-compose.yml" -f "$ROOT_DIR/docker-compose.prod.yml" ps --status running postgres >/dev/null 2>&1; then
  COMPOSE=(docker compose -f "$ROOT_DIR/docker-compose.yml" -f "$ROOT_DIR/docker-compose.prod.yml")
fi

DB_USER="${DB_USER:-aistudio}"
DB_NAME="${DB_NAME:-aistudio}"

echo "Restoring $BACKUP into $DB_NAME (this replaces data)"
read -r -p "Type YES to continue: " confirm
if [[ "$confirm" != "YES" ]]; then
  echo "Aborted"
  exit 1
fi

if [[ "$BACKUP" == *.gz ]]; then
  gunzip -c "$BACKUP" | "${COMPOSE[@]}" exec -T postgres psql -U "$DB_USER" -d "$DB_NAME"
else
  cat "$BACKUP" | "${COMPOSE[@]}" exec -T postgres psql -U "$DB_USER" -d "$DB_NAME"
fi

echo "Restore complete"
