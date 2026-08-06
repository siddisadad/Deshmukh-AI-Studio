#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/backend"

if ! python3 -m venv /tmp/.venv-probe 2>/dev/null; then
  if command -v sudo >/dev/null 2>&1; then
    sudo DEBIAN_FRONTEND=noninteractive apt-get update -qq
    sudo DEBIAN_FRONTEND=noninteractive apt-get install -y python3.12-venv
  else
    echo "python3-venv is required but could not be installed" >&2
    exit 1
  fi
else
  rm -rf /tmp/.venv-probe
fi

if [ ! -d .venv ]; then
  python3 -m venv .venv
fi

.venv/bin/pip install -r requirements.txt
