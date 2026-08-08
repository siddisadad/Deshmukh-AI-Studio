#!/usr/bin/env bash
set -euo pipefail

# Export all visible project conversation threads to gzipped archives (cron-friendly).
# Optional upload to S3 when CHAT_ARCHIVE_S3_URI is set and aws CLI is available.
#
# Usage:
#   export CHAT_ARCHIVE_ACCESS_TOKEN=... CHAT_ARCHIVE_ORG_ID=...
#   ./scripts/scheduled-chat-archive.sh
#
#   CHAT_ARCHIVE_EMAIL=archiver@company.com CHAT_ARCHIVE_PASSWORD=... ./scripts/scheduled-chat-archive.sh
#   CHAT_ARCHIVE_S3_URI=s3://my-bucket/chat-archives ./scripts/scheduled-chat-archive.sh

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

FORMAT="${CHAT_ARCHIVE_FORMAT:-json}"
OUT_DIR="${CHAT_ARCHIVE_DIR:-./backups/chat}"
PROJECT_STATUS="${CHAT_ARCHIVE_PROJECT_STATUS:-ACTIVE}"
ASSISTANT_ROLE="${CHAT_ARCHIVE_ASSISTANT_ROLE:-}"

if [[ "$FORMAT" != "json" && "$FORMAT" != "markdown" ]]; then
  echo "CHAT_ARCHIVE_FORMAT must be json or markdown (got: ${FORMAT})" >&2
  exit 1
fi

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
  if [[ -n "${CHAT_ARCHIVE_ACCESS_TOKEN:-}" ]]; then
    return 0
  fi
  if [[ -z "${CHAT_ARCHIVE_EMAIL:-}" || -z "${CHAT_ARCHIVE_PASSWORD:-}" ]]; then
    echo "Set CHAT_ARCHIVE_ACCESS_TOKEN or CHAT_ARCHIVE_EMAIL + CHAT_ARCHIVE_PASSWORD" >&2
    exit 1
  fi
  local login_json
  login_json="$(curl -fsS --max-time 30 -X POST "${API_BASE}/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"email\":\"${CHAT_ARCHIVE_EMAIL}\",\"password\":\"${CHAT_ARCHIVE_PASSWORD}\"}\")"
  CHAT_ARCHIVE_ACCESS_TOKEN="$(json_get "${login_json}" "['accessToken']")"
  if [[ -z "${CHAT_ARCHIVE_ORG_ID:-}" ]]; then
    CHAT_ARCHIVE_ORG_ID="$(json_get "${login_json}" "['organization']['id']")"
  fi
}

resolve_org_id() {
  if [[ -n "${CHAT_ARCHIVE_ORG_ID:-}" ]]; then
    return 0
  fi
  local me_json
  me_json="$(curl -fsS --max-time 20 "${API_BASE}/me" \
    -H "Authorization: Bearer ${CHAT_ARCHIVE_ACCESS_TOKEN}")"
  CHAT_ARCHIVE_ORG_ID="$(python3 -c "
import json, sys
orgs = json.load(sys.stdin).get('organizations') or []
if not orgs:
    sys.exit('No organizations on /me — set CHAT_ARCHIVE_ORG_ID')
print(orgs[0]['id'])
" <<<"${me_json}")"
}

mkdir -p "$OUT_DIR"
stamp="$(date +%Y%m%d-%H%M%S)"
run_dir="${OUT_DIR}/${stamp}"
mkdir -p "$run_dir"

resolve_token
resolve_org_id

echo "==> Listing projects (org ${CHAT_ARCHIVE_ORG_ID}, status=${PROJECT_STATUS})"
projects_json="$(curl -fsS --max-time 30 \
  "${API_BASE}/organizations/${CHAT_ARCHIVE_ORG_ID}/projects?status=${PROJECT_STATUS}" \
  -H "Authorization: Bearer ${CHAT_ARCHIVE_ACCESS_TOKEN}")"

project_ids="$(python3 -c "
import json, sys
projects = json.load(sys.stdin)
if not isinstance(projects, list):
    projects = projects.get('content', projects.get('items', []))
for p in projects:
    print(p['id'])
" <<<"${projects_json}")"

if [[ -z "${project_ids}" ]]; then
  echo "No projects to archive"
  exit 0
fi

exported=0
manifest_entries=()

while IFS= read -r project_id; do
  [[ -z "$project_id" ]] && continue
  query=""
  if [[ -n "$ASSISTANT_ROLE" ]]; then
    query="?format=${FORMAT}&assistantRole=${ASSISTANT_ROLE}"
  else
    query="?format=${FORMAT}"
  fi
  url="${API_BASE}/projects/${project_id}/conversations/export${query}"
  tmp_file="$(mktemp)"
  headers_file="$(mktemp)"

  http_code="$(curl -sS --max-time 120 -o "$tmp_file" -D "$headers_file" -w '%{http_code}' \
    -H "Authorization: Bearer ${CHAT_ARCHIVE_ACCESS_TOKEN}" "$url")"

  if [[ "$http_code" != "200" ]]; then
    echo "WARN: export failed for project ${project_id} (HTTP ${http_code})" >&2
    rm -f "$tmp_file" "$headers_file"
    continue
  fi

  filename="$(grep -i '^content-disposition:' "$headers_file" | sed -n 's/.*filename="\([^"]*\)".*/\1/p' | head -1)"
  if [[ -z "$filename" ]]; then
    ext="json"
    if [[ "$FORMAT" == "markdown" ]]; then
      ext="md"
    fi
    filename="project-${project_id}-threads.${ext}"
  fi

  out_gz="${run_dir}/${filename}.gz"
  gzip -c "$tmp_file" >"$out_gz"
  rm -f "$tmp_file" "$headers_file"

  size_bytes="$(wc -c <"$out_gz" | tr -d ' ')"
  echo "Exported project ${project_id} -> ${out_gz} (${size_bytes} bytes gzipped)"
  manifest_entries+=("${project_id}|${filename}|${size_bytes}")
  exported=$((exported + 1))

  if [[ -n "${CHAT_ARCHIVE_S3_URI:-}" ]]; then
    if ! command -v aws >/dev/null; then
      echo "CHAT_ARCHIVE_S3_URI is set but aws CLI was not found" >&2
      exit 1
    fi
    dest="${CHAT_ARCHIVE_S3_URI%/}/${stamp}/${filename}.gz"
    aws s3 cp "$out_gz" "$dest"
    echo "Uploaded to ${dest}"
  fi
done <<<"${project_ids}"

manifest_path="${run_dir}/manifest.json"
entries_file="$(mktemp)"
printf '%s\n' "${manifest_entries[@]}" >"$entries_file"
python3 - "$manifest_path" "$stamp" "$CHAT_ARCHIVE_ORG_ID" "$FORMAT" "$exported" "$entries_file" <<'PY'
import json
import sys

path, stamp, org_id, fmt, count, entries_path = sys.argv[1:7]
entries = []
with open(entries_path) as f:
    for line in f:
        line = line.strip()
        if not line:
            continue
        pid, name, size = line.split("|", 2)
        entries.append({"projectId": pid, "filename": name, "sizeBytesGz": int(size)})

doc = {
    "exportedAt": stamp,
    "organizationId": org_id,
    "format": fmt,
    "projectCount": int(count),
    "archives": entries,
}
with open(path, "w") as f:
    json.dump(doc, f, indent=2)
    f.write("\n")
PY
rm -f "$entries_file"

echo "Done: ${exported} project archive(s) in ${run_dir}"
manifest_gz="${run_dir}/manifest.json.gz"
gzip -c "$manifest_path" >"$manifest_gz"

if [[ -n "${CHAT_ARCHIVE_S3_URI:-}" ]]; then
  dest="${CHAT_ARCHIVE_S3_URI%/}/${stamp}/manifest.json.gz"
  aws s3 cp "$manifest_gz" "$dest"
  echo "Uploaded manifest to ${dest}"
fi
