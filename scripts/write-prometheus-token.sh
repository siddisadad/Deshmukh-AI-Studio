#!/usr/bin/env bash
set -euo pipefail

# Write Prometheus bearer token file from METRICS_SCRAPE_TOKEN (monitoring/README.md).
#
# Usage:
#   export METRICS_SCRAPE_TOKEN="$(openssl rand -hex 32)"
#   ./scripts/write-prometheus-token.sh

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TOKEN_FILE="${ROOT_DIR}/monitoring/.prometheus-token"

if [[ -z "${METRICS_SCRAPE_TOKEN:-}" ]]; then
  echo "METRICS_SCRAPE_TOKEN is not set" >&2
  echo "Example: export METRICS_SCRAPE_TOKEN=\"\$(openssl rand -hex 32)\"" >&2
  exit 1
fi

umask 077
printf '%s' "${METRICS_SCRAPE_TOKEN}" >"${TOKEN_FILE}"
echo "Wrote ${TOKEN_FILE}"
