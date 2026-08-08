#!/usr/bin/env bash
set -euo pipefail

# Provision extra Grafana Loki datasources from LOKI_QUERY_REGIONS.
# Writes monitoring/grafana/provisioning/datasources/loki-regions.yml. See docs/27-LOKI-MULTI-REGION-QUERY-GUIDE.md
#
# Usage:
#   LOKI_QUERY_REGIONS=us-east=http://loki-us:3100,eu-west=http://loki-eu:3100 \
#     ./scripts/write-grafana-loki-regions.sh

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_FILE="${ROOT_DIR}/monitoring/grafana/provisioning/datasources/loki-regions.yml"

if [[ -f "${ROOT_DIR}/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "${ROOT_DIR}/.env"
  set +a
fi

REGIONS_RAW="${LOKI_QUERY_REGIONS:-}"

umask 077
{
  echo "apiVersion: 1"
  echo "datasources:"
} >"$OUT_FILE"

if [[ -z "$REGIONS_RAW" ]]; then
  echo "Wrote ${OUT_FILE} (no extra regions — primary Loki datasource remains in loki.yml)"
  exit 0
fi

python3 - "$REGIONS_RAW" "$OUT_FILE" <<'PY'
import re
import sys

raw = sys.argv[1]
out_path = sys.argv[2]

entries = [e.strip() for e in raw.split(",") if e.strip()]
lines = open(out_path, "a", encoding="utf-8")

def slug(name: str) -> str:
    s = name.lower().strip().replace("_", "-").replace(" ", "-")
    s = re.sub(r"[^a-z0-9-]", "", s)
    return s or "region"

idx = 0
for entry in entries:
    if "=" in entry:
        name, url = entry.split("=", 1)
    else:
        idx += 1
        name = f"region-{idx}"
        url = entry
    name = name.strip()
    url = url.strip().rstrip("/")
    uid = f"loki-{slug(name)}"
    lines.write(f"  - name: Loki ({name})\n")
    lines.write(f"    uid: {uid}\n")
    lines.write("    type: loki\n")
    lines.write("    access: proxy\n")
    lines.write(f"    url: {url}\n")
    lines.write("    editable: false\n")

lines.close()
PY

echo "Wrote ${OUT_FILE}"
