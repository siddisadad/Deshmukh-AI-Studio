#!/usr/bin/env bash
set -euo pipefail

# Enable S3 cross-region replication (CRR) from source bucket to DR bucket.
# One-way async replication for archive prefixes; requires versioning on both buckets.
#
# Usage:
#   SOURCE_S3_BUCKET=my-logs SOURCE_S3_REGION=us-east-1 \
#   DEST_S3_BUCKET=my-logs-dr DEST_S3_REGION=us-west-2 \
#   S3_REPLICATION_ROLE_ARN=arn:aws:iam::123456789012:role/s3-crr-role \
#   ./scripts/enable-s3-cross-region-replication.sh

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if [[ -f .env ]]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi

SOURCE_BUCKET="${SOURCE_S3_BUCKET:-${LOKI_S3_BUCKET:-${ARCHIVE_S3_BUCKET:-}}}"
SOURCE_REGION="${SOURCE_S3_REGION:-${LOKI_S3_REGION:-${AWS_REGION:-us-east-1}}}"
DEST_BUCKET="${DEST_S3_BUCKET:-}"
DEST_REGION="${DEST_S3_REGION:-}"
ROLE_ARN="${S3_REPLICATION_ROLE_ARN:-}"
PREFIXES="${ARCHIVE_S3_PREFIXES:-${ARCHIVE_S3_PREFIX:-loki-exports/,chat-archives/}}"

if [[ -z "$SOURCE_BUCKET" || -z "$DEST_BUCKET" || -z "$DEST_REGION" || -z "$ROLE_ARN" ]]; then
  echo "Required: SOURCE_S3_BUCKET (or LOKI_S3_BUCKET), DEST_S3_BUCKET, DEST_S3_REGION, S3_REPLICATION_ROLE_ARN" >&2
  exit 1
fi

if ! command -v aws >/dev/null; then
  echo "aws CLI is required" >&2
  exit 1
fi

if ! command -v python3 >/dev/null; then
  echo "python3 is required" >&2
  exit 1
fi

echo "==> Enabling versioning on source and destination buckets"
aws s3api put-bucket-versioning --bucket "$SOURCE_BUCKET" --region "$SOURCE_REGION" \
  --versioning-configuration Status=Enabled
aws s3api put-bucket-versioning --bucket "$DEST_BUCKET" --region "$DEST_REGION" \
  --versioning-configuration Status=Enabled

tmp_config="$(mktemp)"
trap 'rm -f "$tmp_config"' EXIT

python3 - "$PREFIXES" "$DEST_BUCKET" "$DEST_REGION" "$ROLE_ARN" >"$tmp_config" <<'PY'
import json
import sys

prefixes_raw, dest_bucket, dest_region, role_arn = sys.argv[1:5]
prefixes = [p.strip() for p in prefixes_raw.split(",") if p.strip()]
rules = []
for idx, prefix in enumerate(prefixes):
    rules.append({
        "ID": f"crr-{idx}-{prefix.replace('/', '-')}",
        "Status": "Enabled",
        "Priority": idx + 1,
        "Filter": {"Prefix": prefix},
        "DeleteMarkerReplication": {"Status": "Disabled"},
        "Destination": {
            "Bucket": f"arn:aws:s3:::{dest_bucket}",
            "StorageClass": "STANDARD",
        },
    })

config = {"Role": role_arn, "Rules": rules}
print(json.dumps(config, indent=2))
PY

echo "==> Applying replication configuration on s3://${SOURCE_BUCKET}"
aws s3api put-bucket-replication \
  --bucket "$SOURCE_BUCKET" \
  --region "$SOURCE_REGION" \
  --replication-configuration "file://${tmp_config}"

echo "OK: CRR enabled ${SOURCE_BUCKET} (${SOURCE_REGION}) -> ${DEST_BUCKET} (${DEST_REGION}) for prefixes ${PREFIXES}"
