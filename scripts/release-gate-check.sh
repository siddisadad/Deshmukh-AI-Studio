#!/usr/bin/env bash
set -euo pipefail

# Release gate check — validates passing sign-off for IMAGE_TAG before tagging/deploy.
# Uses API when BILLING_USAGE_SYNC_TOKEN is set; otherwise reads local latest report JSON.
# See docs/59-STAGING-SIGNOFF-RELEASE-GATE-GUIDE.md
#
# Usage:
#   export IMAGE_TAG=v0.2.54-beta
#   export BILLING_USAGE_SYNC_TOKEN=...
#   ./scripts/release-gate-check.sh
#
# Local report fallback:
#   RELEASE_GATE_REPORT_JSON=reports/staging-signoff/latest-signoff.json ./scripts/release-gate-check.sh

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

api_url="${API_URL:-http://localhost:8080}"
token="${BILLING_USAGE_SYNC_TOKEN:-}"
report_json="${RELEASE_GATE_REPORT_JSON:-}"

if [[ -n "$token" ]]; then
  response="$(curl -fsS --max-time 30 \
    "${api_url%/}/api/v1/ops/release-gate?imageTag=${image_tag}" \
    -H "Authorization: Bearer ${token}")"
  allowed="$(python3 -c "import json,sys; print(json.load(sys.stdin)['allowed'])" <<<"$response")"
  reason="$(python3 -c "import json,sys; print(json.load(sys.stdin)['reason'])" <<<"$response")"
  if [[ "$allowed" == "True" ]]; then
    echo "OK: release gate open for ${image_tag} — ${reason}"
    exit 0
  fi
  echo "FAIL: release gate blocked for ${image_tag} — ${reason}" >&2
  exit 1
fi

if [[ -z "$report_json" ]]; then
  report_dir="${STAGING_SIGNOFF_REPORT_DIR:-${ROOT_DIR}/reports/staging-signoff}"
  if [[ -f "${report_dir}/latest-signoff-matrix.json" ]]; then
    report_json="${report_dir}/latest-signoff-matrix.json"
  elif [[ -f "${report_dir}/latest-signoff.json" ]]; then
    report_json="${report_dir}/latest-signoff.json"
  fi
fi

if [[ -z "$report_json" || ! -f "$report_json" ]]; then
  echo "Set BILLING_USAGE_SYNC_TOKEN or provide RELEASE_GATE_REPORT_JSON / local latest-signoff.json" >&2
  exit 1
fi

python3 - "$report_json" "$image_tag" <<'PY'
import json
import sys

path, expected_tag = sys.argv[1:3]
data = json.load(open(path))
tag = data.get("imageTag", "unknown")
summary = data.get("summary") or {}
overall = summary.get("overall", "fail")
if tag != expected_tag:
    print(f"FAIL: report imageTag={tag} does not match IMAGE_TAG={expected_tag}", file=sys.stderr)
    sys.exit(1)
if overall != "pass":
    print(f"FAIL: sign-off overall={overall}", file=sys.stderr)
    sys.exit(1)
print(f"OK: local sign-off report passes for {expected_tag}")
PY
