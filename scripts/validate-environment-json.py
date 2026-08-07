#!/usr/bin/env python3
"""Validate .cursor/environment.json structure for Cloud Agents."""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ENV_PATH = ROOT / ".cursor" / "environment.json"

REQUIRED_TERMINALS = {"api", "ui", "prototype-api", "prototype-ui"}
REQUIRED_PORTS = {8080, 5173, 8000, 5500}


def main() -> int:
    if not ENV_PATH.is_file():
        print(f"Missing {ENV_PATH}", file=sys.stderr)
        return 1

    with ENV_PATH.open(encoding="utf-8") as f:
        data = json.load(f)

    errors: list[str] = []

    for key in ("install", "start", "terminals", "ports"):
        if key not in data:
            errors.append(f"missing required key: {key}")

    install = data.get("install")
    start = data.get("start")
    if not isinstance(install, str) or not install.strip():
        errors.append("install must be a non-empty string")
    if not isinstance(start, str) or not start.strip():
        errors.append("start must be a non-empty string")

    terminals = data.get("terminals")
    if isinstance(terminals, list):
        names = {t.get("name") for t in terminals if isinstance(t, dict)}
        missing = REQUIRED_TERMINALS - names
        if missing:
            errors.append(f"terminals missing names: {sorted(missing)}")
        for t in terminals:
            if not isinstance(t, dict) or not str(t.get("command", "")).strip():
                errors.append("each terminal needs a non-empty command")
    else:
        errors.append("terminals must be an array")

    ports = data.get("ports")
    if isinstance(ports, list):
        port_nums = {p.get("port") for p in ports if isinstance(p, dict)}
        missing_ports = REQUIRED_PORTS - port_nums
        if missing_ports:
            errors.append(f"ports missing: {sorted(missing_ports)}")
    else:
        errors.append("ports must be an array")

    for script in (".cursor/install.sh", ".cursor/start.sh"):
        path = ROOT / script
        if not path.is_file():
            errors.append(f"missing script: {script}")
        elif not path.stat().st_mode & 0o111:
            errors.append(f"script not executable: {script}")

    if errors:
        for err in errors:
            print(f"ERROR: {err}", file=sys.stderr)
        return 1

    print(f"OK: {ENV_PATH} valid ({len(terminals)} terminals, {len(ports)} ports)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
