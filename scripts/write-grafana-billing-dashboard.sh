#!/usr/bin/env bash
set -euo pipefail

# Validates the billing usage Grafana dashboard JSON is present and well-formed.
# Dashboard is provisioned from monitoring/grafana/dashboards/aistudio-billing.json
# See docs/37-BILLING-USAGE-DASHBOARDS-GUIDE.md

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DASHBOARD="${ROOT_DIR}/monitoring/grafana/dashboards/aistudio-billing.json"

if [[ ! -f "$DASHBOARD" ]]; then
  echo "Missing dashboard: ${DASHBOARD}" >&2
  exit 1
fi

python3 - "$DASHBOARD" <<'PY'
import json
import sys

path = sys.argv[1]
with open(path) as f:
    doc = json.load(f)
assert doc.get("uid") == "aistudio-billing", "uid must be aistudio-billing"
assert doc.get("title"), "title required"
panels = doc.get("panels") or []
assert len(panels) >= 8, "expected panels including forecast"
print(f"OK: billing dashboard ({len(panels)} panels) at {path}")
PY
