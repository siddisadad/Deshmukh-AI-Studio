#!/usr/bin/env bash
set -euo pipefail

# Query multiple Loki endpoints and merge log lines by timestamp (real-time multi-region).
# See docs/27-LOKI-MULTI-REGION-QUERY-GUIDE.md
#
# Usage:
#   LOKI_QUERY_REGIONS=us-east=http://localhost:3100,eu-west=http://localhost:3101 \
#     ./scripts/query-loki-multi-region.sh
#   LOKI_QUERY_OUTPUT=summary ./scripts/query-loki-multi-region.sh

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if [[ -f .env ]]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi

QUERY="${LOKI_QUERY:-${LOKI_EXPORT_QUERY:-{service=\"api\"}}}"
HOURS="${LOKI_QUERY_HOURS:-${LOKI_EXPORT_HOURS:-1}}"
LIMIT="${LOKI_QUERY_LIMIT:-${LOKI_EXPORT_LIMIT:-2000}}"
OUTPUT="${LOKI_QUERY_OUTPUT:-ndjson}"
REGIONS_RAW="${LOKI_QUERY_REGIONS:-}"

if ! command -v python3 >/dev/null; then
  echo "python3 is required" >&2
  exit 1
fi

end_s=$(date +%s)
start_s=$((end_s - HOURS * 3600))
end_ns="${end_s}000000000"
start_ns="${start_s}000000000"

tmpdir="$(mktemp -d)"
trap 'rm -rf "$tmpdir"' EXIT

region_count=0

parse_regions() {
  local raw="$1"
  if [[ -z "$raw" ]]; then
    if [[ -n "${LOKI_URL:-}" ]]; then
      printf '%s\n' "local=${LOKI_URL}"
      return
    fi
    echo "Set LOKI_QUERY_REGIONS (name=url pairs) or LOKI_URL" >&2
    exit 1
  fi
  IFS=',' read -ra entries <<<"$raw"
  local idx=0
  for entry in "${entries[@]}"; do
    entry="${entry#"${entry%%[![:space:]]*}"}"
    entry="${entry%"${entry##*[![:space:]]}"}"
    if [[ -z "$entry" ]]; then
      continue
    fi
    if [[ "$entry" == *"="* ]]; then
      local name="${entry%%=*}"
      local url="${entry#*=}"
      name="${name#"${name%%[![:space:]]*}"}"
      name="${name%"${name##*[![:space:]]}"}"
      url="${url#"${url%%[![:space:]]*}"}"
      url="${url%"${url##*[![:space:]]}"}"
      printf '%s\n' "${name}=${url}"
    else
      idx=$((idx + 1))
      printf '%s\n' "region-${idx}=${entry}"
    fi
  done
}

while IFS= read -r pair; do
  region="${pair%%=*}"
  url="${pair#*=}"
  url="${url%/}"
  region_count=$((region_count + 1))
  out_json="${tmpdir}/${region}.json"
  if ! curl -fsS -G "${url}/loki/api/v1/query_range" \
    --data-urlencode "query=${QUERY}" \
    --data-urlencode "start=${start_ns}" \
    --data-urlencode "end=${end_ns}" \
    --data-urlencode "limit=${LIMIT}" \
    --data-urlencode "direction=forward" >"$out_json"; then
    echo "FAIL: Loki query for region ${region} at ${url}" >&2
    exit 1
  fi
  echo "OK: queried ${region} (${url})"
done < <(parse_regions "$REGIONS_RAW")

if [[ "$region_count" -eq 0 ]]; then
  echo "No Loki regions configured" >&2
  exit 1
fi

python3 - "$tmpdir" "$OUTPUT" <<'PY'
import json
import os
import sys

tmpdir = sys.argv[1]
output = sys.argv[2]
records = []

for name in sorted(os.listdir(tmpdir)):
    if not name.endswith(".json"):
        continue
    region = name[:-5]
    path = os.path.join(tmpdir, name)
    data = json.load(open(path))
    for stream in data.get("data", {}).get("result", []):
        labels = stream.get("stream", {})
        for ts, line in stream.get("values", []):
            records.append(
                {
                    "timestamp_ns": int(ts),
                    "region": region,
                    "labels": labels,
                    "line": line,
                }
            )

records.sort(key=lambda r: r["timestamp_ns"])

if output == "summary":
    counts = {}
    for r in records:
        counts[r["region"]] = counts.get(r["region"], 0) + 1
    print(f"total_lines={len(records)} regions={len(counts)}")
    for region, count in sorted(counts.items()):
        print(f"  {region}: {count}")
elif output == "table":
    for r in records:
        ts = r["timestamp_ns"]
        print(f"{ts}\t{r['region']}\t{r['line']}")
else:
    for r in records:
        print(json.dumps(r, ensure_ascii=False))
PY
