#!/usr/bin/env bash
set -euo pipefail

# Fast workspace health gate (no Docker required).
# CI runs this in the environment-config job; agents can run before pushing.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

echo "==> validate-environment-json.py"
python3 scripts/validate-environment-json.py

echo "==> shell script syntax (bash -n)"
for script in scripts/*.sh .cursor/*.sh; do
  bash -n "$script"
done

echo "==> frontend lint + build"
(
  cd frontend
  npm ci --silent
  npm run lint
  npm run build
)

echo "==> backend unit tests (excludes *IT integration tests)"
(
  cd backend
  mvn -B -q test -Dtest='!**/*IT'
)

echo "OK: workspace validation passed"
