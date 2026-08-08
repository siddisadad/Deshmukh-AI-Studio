#!/usr/bin/env bash
set -euo pipefail

# Generate federated Grafana dashboard JSON for multi-region Loki + Prometheus.
# Writes monitoring/grafana/dashboards/aistudio-federated.json
# See docs/33-FEDERATED-GRAFANA-LOKI-RULER-GUIDE.md
#
# Usage:
#   LOKI_QUERY_REGIONS=us-east=http://loki-us:3100 ./scripts/write-grafana-federated-dashboard.sh
#   PROMETHEUS_QUERY_REGIONS=us-east=http://prom-us:9090 ./scripts/write-grafana-federated-dashboard.sh

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_FILE="${ROOT_DIR}/monitoring/grafana/dashboards/aistudio-federated.json"

if [[ -f "${ROOT_DIR}/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "${ROOT_DIR}/.env"
  set +a
fi

python3 - "${OUT_FILE}" "${LOKI_QUERY_REGIONS:-}" "${PROMETHEUS_QUERY_REGIONS:-}" <<'PY'
import json
import re
import sys

out_path = sys.argv[1]
loki_raw = sys.argv[2]
prom_raw = sys.argv[3]


def slug(name: str) -> str:
    s = name.lower().strip().replace("_", "-").replace(" ", "-")
    s = re.sub(r"[^a-z0-9-]", "", s)
    return s or "region"


def parse_regions(raw: str, prefix: str, default_name: str, default_uid: str):
    regions = []
    if not raw:
        regions.append((default_name, default_uid))
        return regions
    idx = 0
    for entry in [e.strip() for e in raw.split(",") if e.strip()]:
        if "=" in entry:
            name, _url = entry.split("=", 1)
        else:
            idx += 1
            name = f"region-{idx}"
        name = name.strip()
        regions.append((name, f"{prefix}-{slug(name)}"))
    return regions


loki_regions = parse_regions(loki_raw, "loki", "local", "Loki")
prom_regions = parse_regions(prom_raw, "prometheus", "local", "Prometheus")

panels = []
panel_id = 1
y = 0

for name, uid in prom_regions:
    panels.append({
        "datasource": {"type": "prometheus", "uid": uid},
        "fieldConfig": {
            "defaults": {
                "mappings": [
                    {"options": {"0": {"text": "DOWN"}, "1": {"text": "UP"}}, "type": "value"}
                ],
                "thresholds": {
                    "mode": "absolute",
                    "steps": [
                        {"color": "red", "value": None},
                        {"color": "green", "value": 1},
                    ],
                },
            },
            "overrides": [],
        },
        "gridPos": {"h": 4, "w": 6, "x": 0, "y": y},
        "id": panel_id,
        "options": {"colorMode": "background", "graphMode": "none", "reduceOptions": {"calcs": ["lastNotNull"]}},
        "title": f"API up ({name})",
        "type": "stat",
        "targets": [{"expr": "up{job=\"aistudio-api\"}", "legendFormat": name, "refId": "A"}],
    })
    panel_id += 1
    panels.append({
        "datasource": {"type": "prometheus", "uid": uid},
        "fieldConfig": {"defaults": {"unit": "reqps"}, "overrides": []},
        "gridPos": {"h": 8, "w": 18, "x": 6, "y": y},
        "id": panel_id,
        "options": {"legend": {"displayMode": "list", "placement": "bottom"}},
        "title": f"HTTP 5xx rate ({name})",
        "type": "timeseries",
        "targets": [{
            "expr": "sum(rate(http_server_requests_seconds_count{job=\"aistudio-api\",status=~\"5..\"}[5m]))",
            "legendFormat": "5xx",
            "refId": "A",
        }],
    })
    panel_id += 1
    y += 8

for name, uid in loki_regions:
    panels.append({
        "datasource": {"type": "loki", "uid": uid},
        "fieldConfig": {"defaults": {"unit": "ops"}, "overrides": []},
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": y},
        "id": panel_id,
        "options": {"legend": {"displayMode": "list", "placement": "bottom"}},
        "title": f"ERROR log rate ({name})",
        "type": "timeseries",
        "targets": [{
            "expr": "sum(rate({service=\"api\"} | json | level=\"ERROR\" [5m]))",
            "legendFormat": "ERROR",
            "refId": "A",
        }],
    })
    panel_id += 1
    panels.append({
        "datasource": {"type": "loki", "uid": uid},
        "fieldConfig": {"defaults": {"unit": "ops"}, "overrides": []},
        "gridPos": {"h": 8, "w": 12, "x": 12, "y": y},
        "id": panel_id,
        "options": {"legend": {"displayMode": "list", "placement": "bottom"}},
        "title": f"WARN log rate ({name})",
        "type": "timeseries",
        "targets": [{
            "expr": "sum(rate({service=\"api\"} | json | level=\"WARN\" [5m]))",
            "legendFormat": "WARN",
            "refId": "A",
        }],
    })
    panel_id += 1
    y += 8

dashboard = {
    "annotations": {"list": []},
    "editable": True,
    "fiscalYearStartMonth": 0,
    "graphTooltip": 0,
    "links": [],
    "panels": panels,
    "refresh": "30s",
    "schemaVersion": 39,
    "tags": ["aistudio", "federated", "multi-region"],
    "templating": {"list": []},
    "time": {"from": "now-6h", "to": "now"},
    "timepicker": {},
    "timezone": "browser",
    "title": "AI Studio Federated",
    "uid": "aistudio-federated",
    "version": 1,
}

with open(out_path, "w", encoding="utf-8") as f:
    json.dump(dashboard, f, indent=2)
    f.write("\n")
PY

echo "Wrote ${OUT_FILE}"
