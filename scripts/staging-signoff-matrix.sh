#!/usr/bin/env bash
set -euo pipefail

# Run staging sign-off across multiple environments and produce a matrix report.
# See docs/43-STAGING-SIGNOFF-MATRIX-GUIDE.md
#
# Usage:
#   STAGING_SIGNOFF_ENVIRONMENTS=us=https://staging-us.example.com,eu=https://staging-eu.example.com \
#     ./scripts/staging-signoff-matrix.sh
#   ./scripts/staging-signoff-matrix.sh us=https://staging-us.example.com eu=https://staging-eu.example.com

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if [[ -f "${ROOT_DIR}/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "${ROOT_DIR}/.env"
  set +a
fi

matrix_raw="${STAGING_SIGNOFF_ENVIRONMENTS:-}"
if [[ $# -gt 0 ]]; then
  matrix_raw="$*"
  matrix_raw="${matrix_raw// /,}"
fi

if [[ -z "$matrix_raw" ]]; then
  echo "Set STAGING_SIGNOFF_ENVIRONMENTS or pass label=url pairs" >&2
  echo "Example: STAGING_SIGNOFF_ENVIRONMENTS=us=https://staging-us.example.com,eu=https://staging-eu.example.com" >&2
  exit 1
fi

timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
image_tag="${IMAGE_TAG:-unknown}"
base_report_dir="${STAGING_SIGNOFF_REPORT_DIR:-${ROOT_DIR}/reports/staging-signoff}"
matrix_dir="${base_report_dir}/matrix-${timestamp}"
matrix_json="${matrix_dir}/signoff-matrix-${timestamp}.json"
matrix_md="${matrix_dir}/signoff-matrix-${timestamp}.md"

include_dogfood="${STAGING_SIGNOFF_MATRIX_INCLUDE_DOGFOOD:-0}"
matrix_failed=0
env_pass_count=0
env_reports_ndjson="$(mktemp)"

IFS=',' read -ra entries <<<"$matrix_raw"
idx=0
for entry in "${entries[@]}"; do
  entry="${entry#"${entry%%[![:space:]]*}"}"
  entry="${entry%"${entry##*[![:space:]]}"}"
  if [[ -z "$entry" ]]; then
    continue
  fi
  if [[ "$entry" == *"="* ]]; then
    label="${entry%%=*}"
    url="${entry#*=}"
    label="${label#"${label%%[![:space:]]*}"}"
    label="${label%"${label##*[![:space:]]}"}"
    url="${url#"${url%%[![:space:]]*}"}"
    url="${url%"${url##*[![:space:]]}"}"
  else
    idx=$((idx + 1))
    label="env-${idx}"
    url="$entry"
    url="${url#"${url%%[![:space:]]*}"}"
    url="${url%"${url##*[![:space:]]}"}"
  fi
  url="${url%/}"

  echo "==> Sign-off matrix: ${label} (${url})"
  env_report_dir="${matrix_dir}/${label}"
  mkdir -p "${env_report_dir}"

  signoff_env=(
    STAGING_SIGNOFF_REPORT_DIR="${env_report_dir}"
    STAGING_SIGNOFF_LABEL="${label}"
    IMAGE_TAG="${image_tag}"
  )
  if [[ "$include_dogfood" != "1" ]]; then
    signoff_env+=(STAGING_SIGNOFF_SKIP_DOGFOOD=1)
  fi

  env_status="pass"
  env_detail="signoff exit 0"
  latest_json=""
  if ! env "${signoff_env[@]}" "${ROOT_DIR}/scripts/staging-signoff.sh" "${url}"; then
    env_status="fail"
    env_detail="staging-signoff.sh failed"
    matrix_failed=1
  else
    env_pass_count=$((env_pass_count + 1))
  fi

  latest_json="$(ls -1t "${env_report_dir}"/signoff-*.json 2>/dev/null | head -1 || true)"
  if [[ -n "$latest_json" && -f "$latest_json" ]]; then
    python3 -c "import json,sys; p=sys.argv[1]; print(json.dumps({'label':sys.argv[2],'host':sys.argv[3],'status':sys.argv[4],'detail':sys.argv[5],'reportJson':p,'report':json.load(open(p))}))" \
      "$latest_json" "$label" "$url" "$env_status" "$env_detail" >>"${env_reports_ndjson}"
  else
    python3 -c "import json,sys; print(json.dumps({'label':sys.argv[1],'host':sys.argv[2],'status':sys.argv[3],'detail':sys.argv[4]}))" \
      "$label" "$url" "$env_status" "$env_detail" >>"${env_reports_ndjson}"
  fi
done

mkdir -p "${matrix_dir}"
python3 - "${matrix_json}" "${matrix_md}" "${timestamp}" "${image_tag}" "${env_reports_ndjson}" <<'PY'
import json
import sys
from pathlib import Path

matrix_json, matrix_md, timestamp, image_tag, ndjson_path = sys.argv[1:6]
environments = []
with open(ndjson_path) as f:
    for line in f:
        line = line.strip()
        if line:
            environments.append(json.loads(line))

pass_count = sum(1 for e in environments if e.get("status") == "pass")
fail_count = sum(1 for e in environments if e.get("status") == "fail")
overall = "fail" if fail_count else "pass"

payload = {
    "timestamp": timestamp,
    "imageTag": image_tag,
    "summary": {
        "environments": len(environments),
        "pass": pass_count,
        "fail": fail_count,
        "overall": overall,
    },
    "environments": environments,
}
Path(matrix_json).write_text(json.dumps(payload, indent=2) + "\n")

lines = [
    "# Staging sign-off matrix report",
    "",
    "| Field | Value |",
    "|---|---|",
    f"| Timestamp | `{timestamp}` |",
    f"| IMAGE_TAG | `{image_tag}` |",
    f"| Overall | **{overall}** ({pass_count} pass / {fail_count} fail / {len(environments)} envs) |",
    "",
    "## Environment matrix",
    "",
    "| Environment | Host | Overall | Pass | Fail | Skip |",
    "|---|---|---|---:|---:|---:|",
]
for env in environments:
    report = env.get("report") or {}
    summary = report.get("summary") or {}
    overall_env = summary.get("overall") or env.get("status", "unknown")
    lines.append(
        f"| {env.get('label', '')} | `{env.get('host', '')}` | {overall_env} "
        f"| {summary.get('pass', '')} | {summary.get('fail', '')} | {summary.get('skip', '')} |"
    )
lines.extend(["", "## Per-environment reports", ""])
for env in environments:
    lines.append(f"- **{env.get('label', '')}** — {env.get('detail', '')}")
    if env.get("reportJson"):
        lines.append(f"  - JSON: `{env['reportJson']}`")
lines.append("")
Path(matrix_md).write_text("\n".join(lines))
PY

rm -f "${env_reports_ndjson}"

echo ""
echo "Sign-off matrix report:"
echo "  JSON: ${matrix_json}"
echo "  Markdown: ${matrix_md}"

if [[ -n "${STAGING_SIGNOFF_S3_URI:-}" ]]; then
  if ! command -v aws >/dev/null; then
    echo "STAGING_SIGNOFF_S3_URI is set but aws CLI was not found" >&2
    exit 1
  fi
  s3_prefix="${STAGING_SIGNOFF_S3_URI%/}/matrix-${timestamp}"
  aws s3 cp "$matrix_json" "${s3_prefix}/$(basename "$matrix_json")"
  aws s3 cp "$matrix_md" "${s3_prefix}/$(basename "$matrix_md")"
  echo "  S3: ${s3_prefix}/"
fi

if [[ "$matrix_failed" -ne 0 ]]; then
  echo "FAIL: one or more environments failed sign-off" >&2
  exit 1
fi

echo "OK: staging sign-off matrix passed (${env_pass_count} environments)"
