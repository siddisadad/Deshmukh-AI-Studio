#!/usr/bin/env bash
set -euo pipefail

# Export Loki logs to gzipped NDJSON for off-site archive (cron-friendly).
# Optional upload to S3 when LOKI_ARCHIVE_S3_URI is set and aws CLI is available.
#
# Usage:
#   ./scripts/export-loki-logs.sh
#   LOKI_URL=http://localhost:3100 LOKI_EXPORT_HOURS=168 ./scripts/export-loki-logs.sh
#   LOKI_ARCHIVE_S3_URI=s3://my-bucket/loki-exports ./scripts/export-loki-logs.sh

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if [[ -f .env ]]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi

LOKI_URL="${LOKI_URL:-http://localhost:3100}"
QUERY="${LOKI_EXPORT_QUERY:-{service=\"api\"}}"
HOURS="${LOKI_EXPORT_HOURS:-24}"
LIMIT="${LOKI_EXPORT_LIMIT:-5000}"
OUT_DIR="${LOKI_EXPORT_DIR:-./backups/loki}"

if ! command -v python3 >/dev/null; then
  echo "python3 is required to parse Loki responses" >&2
  exit 1
fi

mkdir -p "$OUT_DIR"

end_s=$(date +%s)
start_s=$((end_s - HOURS * 3600))
end_ns="${end_s}000000000"
start_ns="${start_s}000000000"

tmp_json="$(mktemp)"
trap 'rm -f "$tmp_json"' EXIT

curl -fsS -G "${LOKI_URL%/}/loki/api/v1/query_range" \
  --data-urlencode "query=${QUERY}" \
  --data-urlencode "start=${start_ns}" \
  --data-urlencode "end=${end_ns}" \
  --data-urlencode "limit=${LIMIT}" \
  --data-urlencode "direction=forward" >"$tmp_json"

stamp="$(date +%Y%m%d-%H%M%S)"
filename="loki-export-${stamp}.ndjson.gz"
out_path="${OUT_DIR}/${filename}"

python3 - "$tmp_json" <<'PY' | gzip >"$out_path"
import json
import sys

data = json.load(open(sys.argv[1]))
for stream in data.get("data", {}).get("result", []):
    labels = stream.get("stream", {})
    for ts, line in stream.get("values", []):
        record = {"timestamp_ns": ts, "labels": labels, "line": line}
        print(json.dumps(record, ensure_ascii=False))
PY

line_count="$(gzip -dc "$out_path" | wc -l | tr -d ' ')"
echo "Exported ${line_count} log lines to ${out_path}"

if [[ -n "${LOKI_ARCHIVE_S3_URI:-}" ]]; then
  if ! command -v aws >/dev/null; then
    echo "LOKI_ARCHIVE_S3_URI is set but aws CLI was not found" >&2
    exit 1
  fi
  dest="${LOKI_ARCHIVE_S3_URI%/}/${filename}"
  aws s3 cp "$out_path" "$dest"
  echo "Uploaded to ${dest}"
fi
