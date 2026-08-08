#!/usr/bin/env bash
set -euo pipefail

# Fan out Loki ruler alert rules to each regional Loki endpoint.
# See docs/33-FEDERATED-GRAFANA-LOKI-RULER-GUIDE.md
#
# Usage:
#   LOKI_QUERY_REGIONS=us-east=http://loki-us:3100,eu-west=http://loki-eu:3100 \
#     ./scripts/sync-loki-ruler-regions.sh
#   LOKI_URL=http://localhost:3100 ./scripts/sync-loki-ruler-regions.sh

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RULES_FILE="${ROOT_DIR}/monitoring/loki-alerts.yml"
NAMESPACE="${LOKI_RULER_NAMESPACE:-fake}"

if [[ -f "${ROOT_DIR}/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "${ROOT_DIR}/.env"
  set +a
fi

if [[ ! -f "$RULES_FILE" ]]; then
  echo "Missing ${RULES_FILE}" >&2
  exit 1
fi

sync_ruler() {
  local label="$1"
  local base_url="$2"
  base_url="${base_url%/}"
  local status
  status="$(curl -sS --max-time 30 -o /dev/null -w '%{http_code}' \
    -X POST "${base_url}/loki/api/v1/rules/${NAMESPACE}" \
    -H 'Content-Type: application/yaml' \
    --data-binary "@${RULES_FILE}")"
  if [[ "$status" == "202" || "$status" == "200" ]]; then
    echo "OK: Loki ruler ${label} (${base_url})"
  else
    echo "FAIL: Loki ruler ${label} returned HTTP ${status}" >&2
    return 1
  fi
}

failed=0

if [[ -n "${LOKI_URL:-}" ]]; then
  sync_ruler "primary" "${LOKI_URL}" || failed=1
fi

REGIONS_RAW="${LOKI_QUERY_REGIONS:-}"
if [[ -n "$REGIONS_RAW" ]]; then
  IFS=',' read -ra entries <<<"$REGIONS_RAW"
  idx=0
  for entry in "${entries[@]}"; do
    entry="${entry#"${entry%%[![:space:]]*}"}"
    entry="${entry%"${entry##*[![:space:]]}"}"
    if [[ -z "$entry" ]]; then
      continue
    fi
    if [[ "$entry" == *"="* ]]; then
      region="${entry%%=*}"
      url="${entry#*=}"
      region="${region#"${region%%[![:space:]]*}"}"
      region="${region%"${region##*[![:space:]]}"}"
      url="${url#"${url%%[![:space:]]*}"}"
      url="${url%"${url##*[![:space:]]}"}"
    else
      idx=$((idx + 1))
      region="region-${idx}"
      url="$entry"
      url="${url#"${url%%[![:space:]]*}"}"
      url="${url%"${url##*[![:space:]]}"}"
    fi
    sync_ruler "$region" "${url%/}" || failed=1
  done
elif [[ -z "${LOKI_URL:-}" ]]; then
  echo "Set LOKI_QUERY_REGIONS or LOKI_URL" >&2
  exit 1
fi

if [[ "$failed" -ne 0 ]]; then
  echo "FAIL: one or more Loki ruler syncs failed" >&2
  exit 1
fi

echo "OK: Loki ruler rules synced (namespace ${NAMESPACE})"
