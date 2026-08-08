#!/usr/bin/env bash
set -euo pipefail

# Reload regional Alertmanager instances after write-alertmanager-config.sh.
# Config must be mounted or synced to each regional Alertmanager before reload.
# See docs/39-ALERTMANAGER-ONCALL-GUIDE.md
#
# Usage:
#   ALERTMANAGER_QUERY_REGIONS=us-east=http://am-us:9093,eu-west=http://am-eu:9093 \
#     ./scripts/sync-alertmanager-regions.sh
#   ALERTMANAGER_URL=http://localhost:9093 ./scripts/sync-alertmanager-regions.sh

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CONFIG_FILE="${ROOT_DIR}/monitoring/alertmanager.generated.yml"

if [[ -f "${ROOT_DIR}/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "${ROOT_DIR}/.env"
  set +a
fi

if [[ ! -f "$CONFIG_FILE" ]]; then
  echo "Missing ${CONFIG_FILE} — run ./scripts/write-alertmanager-config.sh first" >&2
  exit 1
fi

reload_am() {
  local label="$1"
  local base_url="$2"
  base_url="${base_url%/}"

  local status
  status="$(curl -sS --max-time 15 -o /dev/null -w '%{http_code}' \
    "${base_url}/api/v2/status")"
  if [[ "$status" != "200" ]]; then
    echo "FAIL: Alertmanager ${label} status returned HTTP ${status}" >&2
    return 1
  fi

  status="$(curl -sS --max-time 15 -o /dev/null -w '%{http_code}' \
    -X POST "${base_url}/-/reload")"
  if [[ "$status" == "200" ]]; then
    echo "OK: Alertmanager ${label} reloaded (${base_url})"
  elif [[ "$status" == "404" ]]; then
    echo "WARN: Alertmanager ${label} lifecycle disabled (HTTP 404 on /-/reload) — config file must be updated out-of-band"
    echo "OK: Alertmanager ${label} reachable (${base_url})"
  else
    echo "FAIL: Alertmanager ${label} reload returned HTTP ${status}" >&2
    return 1
  fi
}

failed=0

if [[ -n "${ALERTMANAGER_URL:-}" ]]; then
  reload_am "primary" "${ALERTMANAGER_URL}" || failed=1
fi

REGIONS_RAW="${ALERTMANAGER_QUERY_REGIONS:-}"
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
    reload_am "$region" "${url%/}" || failed=1
  done
elif [[ -z "${ALERTMANAGER_URL:-}" ]]; then
  echo "Set ALERTMANAGER_QUERY_REGIONS or ALERTMANAGER_URL" >&2
  exit 1
fi

if [[ "$failed" -ne 0 ]]; then
  echo "FAIL: one or more Alertmanager syncs failed" >&2
  exit 1
fi

echo "OK: Alertmanager regional sync complete"
