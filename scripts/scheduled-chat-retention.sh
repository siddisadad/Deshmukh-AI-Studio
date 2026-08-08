#!/usr/bin/env bash
set -euo pipefail

# Purge expired chat threads per project (cron-friendly). Skips legal-hold threads.
# Pair with project chat_retention_days policy and optional archive export first.
# CHAT_RETENTION_COMPLIANCE_EXPORT=true writes gzip JSON archives before purge
# CHAT_RETENTION_COMPLIANCE_DIR=./backups/compliance-purge
# CHAT_RETENTION_COMPLIANCE_S3_URI=s3://bucket/compliance-purge
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
compliance_export="${CHAT_RETENTION_COMPLIANCE_EXPORT:-false}"
compliance_dir="${CHAT_RETENTION_COMPLIANCE_DIR:-./backups/compliance-purge}"
mkdir -p "$compliance_dir"

while IFS= read -r project_id; do
  [[ -z "$project_id" ]] && continue
  count=0
  if [[ "$compliance_export" == "true" ]]; then
    stamp="$(date +%Y%m%d-%H%M%S)"
    archive_path="${compliance_dir}/compliance-purge-${project_id}-${stamp}.json.gz"
    header_file="$(mktemp)"
    http_code="$(curl -sS --max-time 120 -o "$archive_path" -D "$header_file" -w '%{http_code}' -X POST \
      "${API_BASE}/projects/${project_id}/conversations/retention-purge" \
      -H "Authorization: Bearer ${CHAT_RETENTION_ACCESS_TOKEN}" \
      -H 'Content-Type: application/json' \
      -d '{"complianceExport":true}')"
    if [[ "$http_code" != "200" ]]; then
      echo "FAIL: compliance purge export for project ${project_id} returned HTTP ${http_code}" >&2
      rm -f "$header_file"
      exit 1
    fi
    count="$(grep -i '^X-Purged-Count:' "$header_file" | tail -1 | awk '{print $2}' | tr -d '\r')"
    count="${count:-0}"
    rm -f "$header_file"
    if [[ -n "${CHAT_RETENTION_COMPLIANCE_S3_URI:-}" ]]; then
      if ! command -v aws >/dev/null; then
        echo "CHAT_RETENTION_COMPLIANCE_S3_URI is set but aws CLI was not found" >&2
        exit 1
      fi
      aws s3 cp "$archive_path" "${CHAT_RETENTION_COMPLIANCE_S3_URI%/}/$(basename "$archive_path")"
    fi
    if [[ "$count" -gt 0 ]]; then
      echo "Compliance archive ${archive_path} (${count} thread(s) purged)"
    fi
  else
    purge_json="$(curl -fsS --max-time 60 -X POST \
      "${API_BASE}/projects/${project_id}/conversations/retention-purge" \
      -H "Authorization: Bearer ${CHAT_RETENTION_ACCESS_TOKEN}")"
    count="$(json_get "${purge_json}" "['purgedCount']")"
    if [[ "$count" -gt 0 ]]; then
      echo "Purged ${count} thread(s) from project ${project_id}"
    fi
  fi
  total_purged=$((total_purged + count))
done <<<"$project_ids"

echo "OK: scheduled chat retention purge completed (${total_purged} total threads)"
