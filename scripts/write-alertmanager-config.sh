#!/usr/bin/env bash
set -euo pipefail

# Generate Alertmanager config with on-call receivers and cross-cluster routes.
# Writes monitoring/alertmanager.generated.yml.
# See docs/39-ALERTMANAGER-ONCALL-GUIDE.md
#
# Usage:
#   ALERTMANAGER_SLACK_WEBHOOK_URL=https://hooks.slack.com/... \
#   ALERTMANAGER_PAGERDUTY_ROUTING_KEY=... \
#     ./scripts/write-alertmanager-config.sh

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_FILE="${ROOT_DIR}/monitoring/alertmanager.generated.yml"

if [[ -f "${ROOT_DIR}/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "${ROOT_DIR}/.env"
  set +a
fi

umask 077

python3 - "$OUT_FILE" <<'PY'
import os
import sys

out_path = sys.argv[1]

slack_url = (os.environ.get("ALERTMANAGER_SLACK_WEBHOOK_URL") or "").strip()
slack_channel = (os.environ.get("ALERTMANAGER_SLACK_CHANNEL") or "").strip()
pagerduty_key = (os.environ.get("ALERTMANAGER_PAGERDUTY_ROUTING_KEY") or "").strip()
webhook_url = (os.environ.get("ALERTMANAGER_WEBHOOK_URL") or "").strip()
critical_only_pd = (os.environ.get("ALERTMANAGER_CRITICAL_ONLY_PAGERDUTY") or "1").strip().lower()
critical_only_pd = critical_only_pd in ("1", "true", "yes", "on")
cluster_name = (os.environ.get("ALERTMANAGER_CLUSTER_NAME") or os.environ.get("PROMETHEUS_CLUSTER_NAME") or "local").strip()

def yaml_quote(value: str) -> str:
    if not value:
        return "''"
    if all(c.isalnum() or c in "-_." for c in value):
        return value
    escaped = value.replace("\\", "\\\\").replace('"', '\\"')
    return f'"{escaped}"'

lines: list[str] = []
lines.append("global:")
lines.append("  resolve_timeout: 5m")
lines.append("")
lines.append("route:")
lines.append("  receiver: default")
lines.append("  group_by: ['alertname', 'severity', 'cluster']")
lines.append("  group_wait: 30s")
lines.append("  group_interval: 5m")
lines.append("  repeat_interval: 4h")
lines.append("  routes:")

route_idx = 0

if pagerduty_key and critical_only_pd:
    lines.append("    - matchers:")
    lines.append("        - severity=\"critical\"")
    lines.append("      receiver: pagerduty")
    lines.append("      continue: true")
    route_idx += 1

if slack_url:
    if critical_only_pd:
        lines.append("    - matchers:")
        lines.append("        - severity=\"critical\"")
        lines.append("      receiver: slack-critical")
        lines.append("      continue: true")
        route_idx += 1
    lines.append("    - matchers:")
    lines.append("        - severity=~\"warning|critical\"")
    lines.append("      receiver: slack")
    lines.append("      continue: true")
    route_idx += 1

if webhook_url:
    lines.append("    - matchers:")
    lines.append("        - severity=~\"warning|critical\"")
    lines.append("      receiver: webhook")
    lines.append("      continue: true")
    route_idx += 1

lines.append("    - matchers:")
lines.append("        - cluster=~\".+\"")
lines.append("      receiver: cluster-default")
lines.append("      group_by: ['alertname', 'severity', 'cluster']")

if route_idx == 0 and not slack_url and not pagerduty_key and not webhook_url:
    # No integrations — drop cluster route to keep config minimal
    lines = lines[:lines.index("  routes:") + 1]
    lines.append("    # Add matchers above when on-call integrations are configured.")

lines.append("")
lines.append("receivers:")
lines.append("  - name: default")
lines.append("    # UI-only fallback — configure Slack/PagerDuty/webhook env vars for paging.")
lines.append("  - name: cluster-default")
lines.append("    # Regional grouping route; notifications use continue receivers above.")

if slack_url:
    lines.append("  - name: slack")
    lines.append("    slack_configs:")
    lines.append("      - api_url: " + yaml_quote(slack_url))
    lines.append("        send_resolved: true")
    if slack_channel:
        lines.append("        channel: " + yaml_quote(slack_channel))
    lines.append("        title: '{{ template \"slack.default.title\" . }}'")
    lines.append("        text: '{{ template \"slack.default.text\" . }}'")
    if critical_only_pd:
        lines.append("  - name: slack-critical")
        lines.append("    slack_configs:")
        lines.append("      - api_url: " + yaml_quote(slack_url))
        lines.append("        send_resolved: true")
        if slack_channel:
            lines.append("        channel: " + yaml_quote(slack_channel))
        lines.append("        title: 'CRITICAL {{ template \"slack.default.title\" . }}'")
        lines.append("        text: '{{ template \"slack.default.text\" . }}'")

if pagerduty_key:
    lines.append("  - name: pagerduty")
    lines.append("    pagerduty_configs:")
    lines.append("      - routing_key: " + yaml_quote(pagerduty_key))
    lines.append("        send_resolved: true")
    lines.append("        description: '{{ template \"pagerduty.default.description\" . }}'")

if webhook_url:
    lines.append("  - name: webhook")
    lines.append("    webhook_configs:")
    lines.append("      - url: " + yaml_quote(webhook_url))
    lines.append("        send_resolved: true")

with open(out_path, "w", encoding="utf-8") as fh:
    fh.write("\n".join(lines) + "\n")

integrations = []
if slack_url:
    integrations.append("slack")
if pagerduty_key:
    integrations.append("pagerduty")
if webhook_url:
    integrations.append("webhook")
summary = ", ".join(integrations) if integrations else "default (UI only)"
print(f"Wrote {out_path} (cluster={cluster_name}, receivers: {summary})")
PY
