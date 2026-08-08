#!/usr/bin/env bash
set -euo pipefail

# Apply S3 lifecycle rules for long-term log/chat archive tiering (Glacier / Deep Archive).
# Does not modify Loki hot chunks unless ARCHIVE_S3_PREFIX targets them — default is export prefixes.
#
# Usage:
#   ARCHIVE_S3_BUCKET=my-logs ./scripts/apply-s3-archive-lifecycle.sh
#   LOKI_S3_BUCKET=my-logs ARCHIVE_S3_PREFIX=loki-exports/ ./scripts/apply-s3-archive-lifecycle.sh
#   ARCHIVE_S3_PREFIXES=loki-exports/,chat-archives/ ARCHIVE_GLACIER_DAYS=90 ./scripts/apply-s3-archive-lifecycle.sh

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if [[ -f .env ]]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi

BUCKET="${ARCHIVE_S3_BUCKET:-${LOKI_S3_BUCKET:-}}"
if [[ -z "$BUCKET" ]]; then
  echo "Set ARCHIVE_S3_BUCKET or LOKI_S3_BUCKET" >&2
  exit 1
fi

if ! command -v aws >/dev/null; then
  echo "aws CLI is required" >&2
  exit 1
fi

if ! command -v python3 >/dev/null; then
  echo "python3 is required to build lifecycle JSON" >&2
  exit 1
fi

PREFIXES="${ARCHIVE_S3_PREFIXES:-${ARCHIVE_S3_PREFIX:-loki-exports/,chat-archives/}}"
GLACIER_DAYS="${ARCHIVE_GLACIER_DAYS:-90}"
DEEP_DAYS="${ARCHIVE_DEEP_ARCHIVE_DAYS:-365}"
EXPIRE_DAYS="${ARCHIVE_EXPIRE_DAYS:-0}"
STORAGE_CLASS="${ARCHIVE_GLACIER_STORAGE_CLASS:-GLACIER}"

tmp_config="$(mktemp)"
trap 'rm -f "$tmp_config"' EXIT

python3 - "$PREFIXES" "$GLACIER_DAYS" "$DEEP_DAYS" "$EXPIRE_DAYS" "$STORAGE_CLASS" >"$tmp_config" <<'PY'
import json
import sys

prefixes_raw, glacier_days, deep_days, expire_days, storage_class = sys.argv[1:6]
glacier_days = int(glacier_days)
deep_days = int(deep_days)
expire_days = int(expire_days)
prefixes = [p.strip() for p in prefixes_raw.split(",") if p.strip()]

rules = []
for idx, prefix in enumerate(prefixes):
    transitions = []
    if glacier_days > 0:
        transitions.append({"Days": glacier_days, "StorageClass": storage_class})
    if deep_days > 0 and deep_days > glacier_days:
        transitions.append({"Days": deep_days, "StorageClass": "DEEP_ARCHIVE"})
    rule = {
        "ID": f"archive-tiering-{idx}-{prefix.replace('/', '-')}",
        "Filter": {"Prefix": prefix},
        "Status": "Enabled",
    }
    if transitions:
        rule["Transitions"] = transitions
    if expire_days > 0:
        rule["Expiration"] = {"Days": expire_days}
    rules.append(rule)

print(json.dumps({"Rules": rules}, indent=2))
PY

echo "Applying lifecycle to s3://${BUCKET} (prefixes=${PREFIXES})"
aws s3api put-bucket-lifecycle-configuration \
  --bucket "$BUCKET" \
  --lifecycle-configuration "file://${tmp_config}"

echo "OK: lifecycle applied (Glacier after ${GLACIER_DAYS}d, Deep Archive after ${DEEP_DAYS}d)"
