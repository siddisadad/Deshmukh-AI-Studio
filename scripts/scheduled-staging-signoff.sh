#!/usr/bin/env bash
set -euo pipefail

# Scheduled staging sign-off — cron-friendly wrapper for single-host or matrix runs.
# Optionally submits the report to the API release-gate store.
# See docs/59-STAGING-SIGNOFF-RELEASE-GATE-GUIDE.md
#
# Usage (single host):
#   export STAGING_SIGNOFF_URL=https://staging.example.com
#   export IMAGE_TAG=v0.2.54-beta
#   ./scripts/scheduled-staging-signoff.sh
#
# Usage (matrix):
#   export STAGING_SIGNOFF_ENVIRONMENTS=us=https://staging-us.example.com,eu=https://staging-eu.example.com
#   export IMAGE_TAG=v0.2.54-beta
#   ./scripts/scheduled-staging-signoff.sh

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if [[ -f .env ]]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi

image_tag="${IMAGE_TAG:-}"
if [[ -z "$image_tag" ]]; then
  echo "IMAGE_TAG is required" >&2
  exit 1
fi

matrix_raw="${STAGING_SIGNOFF_ENVIRONMENTS:-}"
signoff_url="${STAGING_SIGNOFF_URL:-}"

if [[ -z "$matrix_raw" && -z "$signoff_url" ]]; then
  echo "Set STAGING_SIGNOFF_URL or STAGING_SIGNOFF_ENVIRONMENTS" >&2
  exit 1
fi

submit_enabled="${OPS_SIGNOFF_SUBMIT_ENABLED:-1}"
api_url="${API_URL:-http://localhost:8080}"
token="${BILLING_USAGE_SYNC_TOKEN:-}"
s3_uri="${STAGING_SIGNOFF_S3_URI:-}"

if [[ -n "$matrix_raw" ]]; then
  echo "==> Scheduled staging sign-off matrix (IMAGE_TAG=${image_tag})"
  "${ROOT_DIR}/scripts/staging-signoff-matrix.sh"
  report_json="$(ls -1t reports/staging-signoff/matrix-*/signoff-matrix-*.json 2>/dev/null | head -1 || true)"
else
  echo "==> Scheduled staging sign-off (IMAGE_TAG=${image_tag})"
  "${ROOT_DIR}/scripts/staging-signoff.sh" "${signoff_url%/}"
  report_dir="${STAGING_SIGNOFF_REPORT_DIR:-${ROOT_DIR}/reports/staging-signoff}"
  report_json="$(ls -1t "${report_dir}"/signoff-*.json 2>/dev/null | grep -v signoff-matrix | head -1 || true)"
fi

if [[ "$submit_enabled" == "1" && -n "$token" && -n "$report_json" && -f "$report_json" ]]; then
  echo "==> Submitting sign-off report to API"
  payload="$(python3 -c "import json,sys; print(json.dumps({'reportJson': open(sys.argv[1]).read(), 's3Uri': sys.argv[2] or None}))" \
    "$report_json" "$s3_uri")"
  curl -fsS --max-time 60 -X POST "${api_url%/}/api/v1/ops/staging-signoff/submit" \
    -H "Authorization: Bearer ${token}" \
    -H "Content-Type: application/json" \
    -d "$payload"
  echo ""
fi

echo "OK: scheduled staging sign-off completed"
