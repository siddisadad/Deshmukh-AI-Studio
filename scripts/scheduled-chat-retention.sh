#!/usr/bin/env bash
set -euo pipefail

# Purge expired chat threads per project (cron-friendly). Skips legal-hold threads.
# Pair with project chat_retention_days policy and optional archive export first.
#
# Usage:
#   export CHAT_RETENTION_ACCESS_TOKEN=... CHAT_RETENTION_ORG_ID=...
#   ./scripts/scheduled-chat-retention.sh
#
#   CHAT_RETENTION_EMAIL=ops@company.com CHAT_RETENTION_PASSWORD=... ./scripts/scheduled-chat-retention.sh

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if [[ -f .env ]]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi

EDGE_URL="${EDGE_URL:-http://localhost:8088}"
if [[ -n "${API_URL:-}" ]]; then
  API_BASE="${API_URL%/}/api/v1"
else
  API_BASE="${EDGE_URL%/}/api/v1"
fi

PROJECT_STATUS="${CHAT_RETENTION_PROJECT_STATUS:-ACTIVE}"

if ! command -v python3 >/dev/null; then
  echo "python3 is required to parse API responses" >&2
  exit 1
fi

json_get() {
  local json="$1"
  local key="$2"
  python3 -c "import json,sys; data=json.load(sys.stdin); print(data${key})" <<<"${json}"
}

resolve_token() {
  if [[ -n "${CHAT_RETENTION_ACCESS_TOKEN:-}" ]]; then
    return 0
  fi
  if [[ -z "${CHAT_RETENTION_EMAIL:-}" || -z "${CHAT_RETENTION_PASSWORD:-}" ]]; then
    echo "Set CHAT_RETENTION_ACCESS_TOKEN or CHAT_RETENTION_EMAIL + CHAT_RETENTION_PASSWORD" >&2
    exit 1
  fi
  local login_json payload
  payload="$(CHAT_RETENTION_EMAIL="$CHAT_RETENTION_EMAIL" CHAT_RETENTION_PASSWORD="$CHAT_RETENTION_PASSWORD" python3 -c 'import json, os; print(json.dumps({"email": os.environ["CHAT_RETENTION_EMAIL"], "password": os.environ["CHAT_RETENTION_PASSWORD"]}))')"
  login_json="$(curl -fsS --max-time 30 -X POST "${API_BASE}/auth/login" \
    -H 'Content-Type: application/json' \
    -d "$payload")"
  CHAT_RETENTION_ACCESS_TOKEN="$(json_get "${login_json}" "['accessToken']")"
  if [[ -z "${CHAT_RETENTION_ORG_ID:-}" ]]; then
    CHAT_RETENTION_ORG_ID="$(json_get "${login_json}" "['organization']['id']")"
  fi
}

resolve_org_id() {
  if [[ -n "${CHAT_RETENTION_ORG_ID:-}" ]]; then
    return 0
  fi
  local me_json
  me_json="$(curl -fsS --max-time 20 "${API_BASE}/me" \
    -H "Authorization: Bearer ${CHAT_RETENTION_ACCESS_TOKEN}")"
  CHAT_RETENTION_ORG_ID="$(printf '%s' "$me_json" | python3 -c 'import json,sys; orgs=json.load(sys.stdin).get("organizations") or []; sys.exit("No organizations on /me — set CHAT_RETENTION_ORG_ID") if not orgs else None; print(orgs[0]["id"])')"
}

resolve_token
resolve_org_id

projects_json="$(curl -fsS --max-time 30 \
  "${API_BASE}/organizations/${CHAT_RETENTION_ORG_ID}/projects?status=${PROJECT_STATUS}" \
  -H "Authorization: Bearer ${CHAT_RETENTION_ACCESS_TOKEN}")"

project_ids="$(printf '%s' "$projects_json" | python3 -c 'import json,sys; print("\n".join(p["id"] for p in json.load(sys.stdin)))')"

total_purged=0
while IFS= read -r project_id; do
  [[ -z "$project_id" ]] && continue
  purge_json="$(curl -fsS --max-time 60 -X POST \
    "${API_BASE}/projects/${project_id}/conversations/retention-purge" \
    -H "Authorization: Bearer ${CHAT_RETENTION_ACCESS_TOKEN}")"
  count="$(json_get "${purge_json}" "['purgedCount']")"
  total_purged=$((total_purged + count))
  if [[ "$count" -gt 0 ]]; then
    echo "Purged ${count} thread(s) from project ${project_id}"
  fi
done <<<"$project_ids"

echo "OK: scheduled chat retention purge completed (${total_purged} total threads)"
