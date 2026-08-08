#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/prototype/backend"

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

if [ -f "$ROOT/frontend/package.json" ]; then
  if ! command -v npm >/dev/null 2>&1; then
    echo "npm is required for the React frontend" >&2
    exit 1
  fi
  (cd "$ROOT/frontend" && npm ci)
fi

if [ -f "$ROOT/e2e/package.json" ]; then
  (cd "$ROOT/e2e" && npm ci)
fi

if [ -f "$ROOT/backend/pom.xml" ]; then
  if ! command -v mvn >/dev/null 2>&1; then
    if command -v sudo >/dev/null 2>&1; then
      sudo DEBIAN_FRONTEND=noninteractive apt-get update -qq
      sudo DEBIAN_FRONTEND=noninteractive apt-get install -y maven
    else
      echo "maven is required for the Spring Boot API" >&2
      exit 1
    fi
  fi
  (cd "$ROOT/backend" && mvn -B -q dependency:go-offline -DskipTests)
fi

if [ -f "$ROOT/docker-compose.yml" ]; then
  if ! command -v docker >/dev/null 2>&1 || ! docker compose version >/dev/null 2>&1; then
    if command -v sudo >/dev/null 2>&1; then
      sudo DEBIAN_FRONTEND=noninteractive apt-get update -qq
      sudo DEBIAN_FRONTEND=noninteractive apt-get install -y docker.io docker-compose-v2 || true
    fi
  fi
  if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
    echo "Docker Compose available — start.sh can boot Postgres (pgvector) for local API dev"
    if docker info >/dev/null 2>&1; then
      (cd "$ROOT" && docker compose pull postgres 2>/dev/null || true)
    else
      echo "Docker daemon not running — Postgres via compose unavailable until Docker starts"
    fi
  fi
fi
