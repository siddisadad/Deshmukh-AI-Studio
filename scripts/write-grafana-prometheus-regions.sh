#!/usr/bin/env bash
set -euo pipefail

# Provision extra Grafana Prometheus datasources from PROMETHEUS_QUERY_REGIONS.
# Writes monitoring/grafana/provisioning/datasources/prometheus-regions.yml.
# See docs/33-FEDERATED-GRAFANA-LOKI-RULER-GUIDE.md
#
# Usage:
#   PROMETHEUS_QUERY_REGIONS=us-east=http://prom-us:9090,eu-west=http://prom-eu:9090 \
#     ./scripts/write-grafana-prometheus-regions.sh

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_FILE="${ROOT_DIR}/monitoring/grafana/provisioning/datasources/prometheus-regions.yml"

if [[ -f "${ROOT_DIR}/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "${ROOT_DIR}/.env"
  set +a
fi

REGIONS_RAW="${PROMETHEUS_QUERY_REGIONS:-}"

umask 077
{
  echo "apiVersion: 1"
  echo "datasources:"
} >"$OUT_FILE"

if [[ -z "$REGIONS_RAW" ]]; then
  echo "Wrote ${OUT_FILE} (no extra regions — primary Prometheus remains in prometheus.yml)"
  exit 0
fi

python3 - "$REGIONS_RAW" "$OUT_FILE" <<'PY'
import re
import sys

raw = sys.argv[1]
out_path = sys.argv[2]

entries = [e.strip() for e in raw.split(",") if e.strip()]

with open(out_path, "a", encoding="utf-8") as lines:
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
        slug = re.sub(r"[^a-z0-9-]", "", name.lower().strip().replace("_", "-").replace(" ", "-")) or "region"
        uid = f"prometheus-{slug}"
        lines.write(f"  - name: Prometheus ({name})\n")
        lines.write(f"    uid: {uid}\n")
        lines.write("    type: prometheus\n")
        lines.write("    access: proxy\n")
        lines.write(f"    url: {url}\n")
        lines.write("    editable: false\n")
PY

echo "Wrote ${OUT_FILE}"
