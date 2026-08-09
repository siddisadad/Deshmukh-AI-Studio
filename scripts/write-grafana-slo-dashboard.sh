#!/usr/bin/env bash
set -euo pipefail

# Validates the SLO Grafana dashboard JSON is present and well-formed.
# Dashboard is provisioned from monitoring/grafana/dashboards/aistudio-slo.json
# See docs/42-SLO-ERROR-BUDGET-GUIDE.md

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DASHBOARD="${ROOT_DIR}/monitoring/grafana/dashboards/aistudio-slo.json"

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
assert doc.get("uid") == "aistudio-slo", "uid must be aistudio-slo"
assert doc.get("title"), "title required"
panels = doc.get("panels") or []
assert len(panels) >= 5, "expected SLO panels"
print(f"OK: SLO dashboard ({len(panels)} panels) at {path}")
PY

TENANT_DASHBOARD="${ROOT_DIR}/monitoring/grafana/dashboards/aistudio-slo-tenant.json"
if [[ ! -f "$TENANT_DASHBOARD" ]]; then
  echo "Missing dashboard: ${TENANT_DASHBOARD}" >&2
  exit 1
fi

python3 - "$TENANT_DASHBOARD" <<'PY'
import json
import sys

path = sys.argv[1]
with open(path) as f:
    doc = json.load(f)
assert doc.get("uid") == "aistudio-slo-tenant", "uid must be aistudio-slo-tenant"
panels = doc.get("panels") or []
assert len(panels) >= 4, "expected tenant SLO panels"
print(f"OK: tenant SLO dashboard ({len(panels)} panels) at {path}")
PY
