#!/usr/bin/env bash
set -euo pipefail

# Minimal authenticated API journey smoke (register → project create → /me).
# Works against nginx edge (/api/v1 proxy) or direct API when API_URL is set.
#
# Usage:
#   ./scripts/api-smoke.sh
#   ./scripts/api-smoke.sh https://staging.example.com
#   API_URL=http://localhost:8080 ./scripts/api-smoke.sh

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
EDGE_URL="${1:-http://localhost:8088}"

if [[ -n "${API_URL:-}" ]]; then
  API_BASE="${API_URL%/}/api/v1"
else
  API_BASE="${EDGE_URL%/}/api/v1"
fi

suffix="$(date +%s)-$RANDOM"
email="api-smoke-${suffix}@example.com"
password="Str0ngPass!ApiSmoke"

json_get() {
  local json="$1"
  local key="$2"
  python3 -c "import json,sys; data=json.load(sys.stdin); print(data${key})" <<<"${json}"
}

echo "==> Register ${email}"
register_json="$(curl -fsS --max-time 20 -X POST "${API_BASE}/auth/register" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"${email}\",\"password\":\"${password}\",\"displayName\":\"API Smoke\"}")"

access_token="$(json_get "${register_json}" "['accessToken']")"
org_id="$(json_get "${register_json}" "['organization']['id']")"

echo "==> GET /me"
me_json="$(curl -fsS --max-time 15 "${API_BASE}/me" \
  -H "Authorization: Bearer ${access_token}")"
if ! printf '%s' "${me_json}" | grep -q "${email}"; then
  echo "FAIL: /me response missing registered email" >&2
  exit 1
fi

echo "==> Create project"
project_json="$(curl -fsS --max-time 20 -X POST "${API_BASE}/organizations/${org_id}/projects" \
  -H "Authorization: Bearer ${access_token}" \
  -H 'Content-Type: application/json' \
  -d '{"name":"API Smoke Project","projectKey":"SMK","description":"api-smoke.sh"}')"
project_id="$(json_get "${project_json}" "['id']")"

echo "==> List projects"
list_json="$(curl -fsS --max-time 15 "${API_BASE}/organizations/${org_id}/projects" \
  -H "Authorization: Bearer ${access_token}")"
if ! printf '%s' "${list_json}" | grep -q "${project_id}"; then
  echo "FAIL: project list missing ${project_id}" >&2
  exit 1
fi

echo "OK: API smoke passed (${API_BASE}, project ${project_id})"
