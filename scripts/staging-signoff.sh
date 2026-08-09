#!/usr/bin/env bash
set -euo pipefail

# Full live-host sign-off automation — dogfood gates + extended probes + report.
# See docs/31-STAGING-LIVE-SIGNOFF-GUIDE.md
#
# Usage:
#   ./scripts/staging-signoff.sh https://staging.example.com
#   STAGING_SIGNOFF_SKIP_DOGFOOD=1 ./scripts/staging-signoff.sh https://staging.example.com
#
# Env:
#   STAGING_SIGNOFF_REPORT_DIR — report output directory (default ./reports/staging-signoff)
#   STAGING_SIGNOFF_S3_URI — optional s3://bucket/prefix for off-site report archive
#   STAGING_SIGNOFF_SKIP_DOGFOOD — skip staging-dogfood.sh (when invoked from dogfood step 7)
#   STAGING_SIGNOFF_REQUIRE_HTTPS — fail when edge URL is not https (default 1 on prod-shaped hosts)
#   STAGING_SIGNOFF_STREAM — run SSE chat stream probe (default 1)
#   STAGING_SIGNOFF_LABEL — optional environment label (recorded in report; used by sign-off matrix)
#   IMAGE_TAG — recorded in sign-off report
#   API_URL — direct API for metrics / probes when set

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
EDGE_URL="${1:-http://localhost:8088}"
EDGE_URL="${EDGE_URL%/}"

billing_provider="${BILLING_PROVIDER:-mock}"
sso_provider="${SSO_PROVIDER:-mock}"
mail_provider="${MAIL_PROVIDER:-logging}"
image_tag="${IMAGE_TAG:-unknown}"
api_url="${API_URL:-}"
signoff_label="${STAGING_SIGNOFF_LABEL:-}"

if [[ -f "${ROOT_DIR}/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "${ROOT_DIR}/.env"
  set +a
  billing_provider="${BILLING_PROVIDER:-mock}"
  sso_provider="${SSO_PROVIDER:-mock}"
  mail_provider="${MAIL_PROVIDER:-logging}"
  image_tag="${IMAGE_TAG:-unknown}"
  api_url="${API_URL:-}"
  signoff_label="${STAGING_SIGNOFF_LABEL:-}"
fi

if [[ -n "$api_url" ]]; then
  API_BASE="${api_url%/}/api/v1"
else
  API_BASE="${EDGE_URL}/api/v1"
fi

report_dir="${STAGING_SIGNOFF_REPORT_DIR:-${ROOT_DIR}/reports/staging-signoff}"
timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
report_json="${report_dir}/signoff-${timestamp}.json"
report_md="${report_dir}/signoff-${timestamp}.md"
checks_ndjson="$(mktemp)"
signoff_failed=0

require_https="${STAGING_SIGNOFF_REQUIRE_HTTPS:-}"
if [[ -z "$require_https" ]]; then
  if [[ "${EDGE_URL}" == https://* ]]; then
    require_https=1
  else
    require_https=0
  fi
fi

stream_probe="${STAGING_SIGNOFF_STREAM:-1}"

json_get() {
  local json="$1"
  local key="$2"
  python3 -c "import json,sys; data=json.load(sys.stdin); print(data${key})" <<<"${json}"
}

record_check() {
  local name="$1"
  local status="$2"
  local detail="${3:-}"
  python3 -c "import json,sys; print(json.dumps({'name':sys.argv[1],'status':sys.argv[2],'detail':sys.argv[3]}))" \
    "$name" "$status" "$detail" >>"${checks_ndjson}"
  if [[ "$status" == "fail" ]]; then
    signoff_failed=1
    echo "FAIL: ${name} — ${detail}" >&2
  elif [[ "$status" == "pass" ]]; then
    echo "OK: ${name}"
  else
    echo "SKIP: ${name} — ${detail}"
  fi
}

header_value() {
  local headers="$1"
  local name="$2"
  python3 -c "
import sys
name = sys.argv[1].lower()
for line in sys.stdin:
    if ':' in line:
        k, v = line.split(':', 1)
        if k.strip().lower() == name:
            print(v.strip())
            break
" "$name" <<<"${headers}"
}

check_security_headers() {
  local label="$1"
  local url="$2"
  local headers
  headers="$(curl -sSI --max-time 15 "${url}")" || {
    record_check "${label}" "fail" "headers unreachable at ${url}"
    return
  }
  local xcto xfo rp
  xcto="$(header_value "${headers}" "X-Content-Type-Options")"
  xfo="$(header_value "${headers}" "X-Frame-Options")"
  rp="$(header_value "${headers}" "Referrer-Policy")"
  if [[ "$xcto" != "nosniff" ]]; then
    record_check "${label}" "fail" "X-Content-Type-Options=${xcto:-missing}"
    return
  fi
  if [[ "$xfo" != "DENY" ]]; then
    record_check "${label}" "fail" "X-Frame-Options=${xfo:-missing}"
    return
  fi
  if [[ "$rp" != "strict-origin-when-cross-origin" ]]; then
    record_check "${label}" "fail" "Referrer-Policy=${rp:-missing}"
    return
  fi
  record_check "${label}" "pass" "${url}"
}

echo "==> Staging live-host sign-off for ${EDGE_URL}"

if [[ "${STAGING_SIGNOFF_SKIP_DOGFOOD:-}" != "1" ]]; then
  echo "==> Automated dogfood gates"
  if STAGING_DOGFOOD_SKIP_MANUAL=1 "${ROOT_DIR}/scripts/staging-dogfood.sh" "${EDGE_URL}"; then
    record_check "dogfood-automated" "pass" "staging-dogfood.sh exit 0"
  else
    record_check "dogfood-automated" "fail" "staging-dogfood.sh failed"
  fi
else
  record_check "dogfood-automated" "skip" "STAGING_SIGNOFF_SKIP_DOGFOOD=1"
fi

echo "==> HTTPS / TLS"
if [[ "${EDGE_URL}" == https://* ]]; then
  tls_status="$(curl -sS --max-time 20 -o /dev/null -w '%{http_code}' "${EDGE_URL}/")"
  if [[ "$tls_status" == "200" ]]; then
    record_check "https-tls" "pass" "TLS OK (${tls_status})"
  else
    record_check "https-tls" "fail" "expected HTTP 200 over TLS, got ${tls_status}"
  fi
  health_headers="$(curl -sSI --max-time 15 "${EDGE_URL}/actuator/health")"
  hsts="$(header_value "${health_headers}" "Strict-Transport-Security")"
  if [[ -n "$hsts" ]]; then
    record_check "hsts-header" "pass" "${hsts}"
  else
    record_check "hsts-header" "skip" "Strict-Transport-Security not set (enable prod + HTTPS API)"
  fi
elif [[ "$require_https" == "1" ]]; then
  record_check "https-tls" "fail" "edge URL is not HTTPS"
else
  record_check "https-tls" "skip" "non-HTTPS edge (${EDGE_URL})"
fi

echo "==> Security headers"
check_security_headers "security-headers-spa" "${EDGE_URL}/"
check_security_headers "security-headers-health" "${EDGE_URL}/actuator/health"

echo "==> Public actuator leak"
prom_status="$(curl -sS --max-time 15 -o /dev/null -w '%{http_code}' "${EDGE_URL}/actuator/prometheus")"
if [[ "$prom_status" == "200" ]]; then
  prom_sample="$(curl -sS --max-time 15 "${EDGE_URL}/actuator/prometheus" | head -c 120)"
  if printf '%s' "${prom_sample}" | grep -qiE '<html|<!doctype'; then
    record_check "actuator-prometheus-edge" "pass" "SPA fallback only (not real metrics)"
  elif printf '%s' "${prom_sample}" | grep -q 'http_server_requests'; then
    record_check "actuator-prometheus-edge" "fail" "public /actuator/prometheus exposes metrics"
  else
    record_check "actuator-prometheus-edge" "fail" "unexpected 200 body on /actuator/prometheus"
  fi
else
  record_check "actuator-prometheus-edge" "pass" "status ${prom_status}"
fi

echo "==> Authenticated journey probes"
probe_suffix="$(date +%s)-${RANDOM}"
probe_email="signoff-${probe_suffix}@example.com"
probe_password="Str0ngPass!Signoff"
register_json="$(curl -fsS --max-time 25 -X POST "${API_BASE}/auth/register" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"${probe_email}\",\"password\":\"${probe_password}\",\"displayName\":\"Signoff Probe\"}")"
probe_token="$(json_get "${register_json}" "['accessToken']")"
org_id="$(json_get "${register_json}" "['organization']['id']")"

project_json="$(curl -fsS --max-time 25 -X POST "${API_BASE}/organizations/${org_id}/projects" \
  -H "Authorization: Bearer ${probe_token}" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Signoff Project","projectKey":"SGN","description":"staging-signoff.sh"}')"
project_id="$(json_get "${project_json}" "['id']")"
record_check "register-project" "pass" "project ${project_id}"

assistants_json="$(curl -fsS --max-time 15 "${API_BASE}/assistants" \
  -H "Authorization: Bearer ${probe_token}")"
assistant_count="$(python3 -c "import json,sys; data=json.load(sys.stdin); print(len(data['assistants']))" <<<"${assistants_json}")"
if [[ "${assistant_count:-0}" -ge 4 ]]; then
  record_check "assistants-catalog" "pass" "${assistant_count} assistants"
else
  record_check "assistants-catalog" "fail" "expected >=4 assistants, got ${assistant_count}"
fi

overview_json="$(curl -fsS --max-time 15 "${API_BASE}/organizations/${org_id}/billing" \
  -H "Authorization: Bearer ${probe_token}")"
if printf '%s' "${overview_json}" | grep -q 'FREE'; then
  record_check "billing-overview" "pass" "org billing overview"
else
  record_check "billing-overview" "fail" "missing FREE plan in overview"
fi

usage_json="$(curl -fsS --max-time 15 "${API_BASE}/organizations/${org_id}/billing/usage" \
  -H "Authorization: Bearer ${probe_token}")"
if printf '%s' "${usage_json}" | grep -q '\['; then
  record_check "billing-usage" "pass" "usage history endpoint"
else
  record_check "billing-usage" "fail" "unexpected usage response"
fi

if [[ "$billing_provider" == "stripe" ]]; then
  checkout_json="$(curl -sS --max-time 25 -X POST "${API_BASE}/organizations/${org_id}/billing/checkout" \
    -H "Authorization: Bearer ${probe_token}" \
    -H 'Content-Type: application/json' \
    -d '{"planCode":"PRO"}')"
  checkout_url="$(json_get "${checkout_json}" "['checkoutUrl']" 2>/dev/null || true)"
  if [[ -n "${checkout_url:-}" ]] && [[ "${checkout_url}" == https://* ]]; then
    record_check "billing-stripe-checkout" "pass" "checkout URL issued"
  else
    record_check "billing-stripe-checkout" "fail" "missing Stripe checkout URL"
  fi
else
  record_check "billing-stripe-checkout" "skip" "BILLING_PROVIDER=${billing_provider}"
fi

sso_providers="$(curl -fsS --max-time 15 "${API_BASE}/auth/sso/providers")"
if printf '%s' "${sso_providers}" | grep -q '"id"'; then
  record_check "sso-providers" "pass" "public SSO provider list"
else
  record_check "sso-providers" "fail" "invalid SSO providers response"
fi

if [[ "$stream_probe" == "1" ]]; then
  thread_json="$(curl -fsS --max-time 25 -X POST "${API_BASE}/projects/${project_id}/conversations" \
    -H "Authorization: Bearer ${probe_token}" \
    -H 'Content-Type: application/json' \
    -d '{"assistantRole":"DEVELOPER","title":"Signoff stream"}')"
  thread_id="$(json_get "${thread_json}" "['id']")"
  set +e
  stream_body="$(curl -sS --max-time 120 -N \
    -X POST "${API_BASE}/conversations/${thread_id}/messages/stream" \
    -H "Authorization: Bearer ${probe_token}" \
    -H 'Content-Type: application/json' \
    -H 'Accept: text/event-stream' \
    -d '{"content":"Sign-off stream probe: reply with one short greeting"}' 2>/dev/null)"
  set -e
  if [[ -n "$stream_body" ]] \
    && printf '%s' "$stream_body" | grep -q 'event:delta' \
    && printf '%s' "$stream_body" | grep -q 'event:done'; then
    record_check "sse-stream-chat" "pass" "delta + done events"
  else
    record_check "sse-stream-chat" "fail" "SSE stream missing delta/done events"
  fi
else
  record_check "sse-stream-chat" "skip" "STAGING_SIGNOFF_STREAM=0"
fi

mkdir -p "${report_dir}"
python3 - "${report_json}" "${report_md}" "${timestamp}" "${EDGE_URL}" "${image_tag}" \
  "${billing_provider}" "${sso_provider}" "${mail_provider}" "${checks_ndjson}" "${signoff_label}" <<'PY'
import json
import sys
from pathlib import Path

report_json, report_md, timestamp, edge_url, image_tag, billing, sso, mail, checks_path, label = sys.argv[1:11]
checks = []
with open(checks_path) as f:
    for line in f:
        line = line.strip()
        if line:
            checks.append(json.loads(line))

passed = sum(1 for c in checks if c["status"] == "pass")
failed = sum(1 for c in checks if c["status"] == "fail")
skipped = sum(1 for c in checks if c["status"] == "skip")
overall = "fail" if failed else "pass"

payload = {
    "timestamp": timestamp,
    "host": edge_url,
    "imageTag": image_tag,
    "providers": {
        "billing": billing,
        "sso": sso,
        "mail": mail,
    },
    "summary": {"pass": passed, "fail": failed, "skip": skipped, "overall": overall},
    "checks": checks,
}
if label:
    payload["environment"] = label

Path(report_json).write_text(json.dumps(payload, indent=2) + "\n")

lines = [
    "# Staging live-host sign-off report",
    "",
    f"| Field | Value |",
    f"|---|---|",
    f"| Timestamp | `{timestamp}` |",
    f"| Host | `{edge_url}` |",
]
if label:
    lines.append(f"| Environment | `{label}` |")
lines.extend([
    f"| IMAGE_TAG | `{image_tag}` |",
    f"| Providers | billing={billing}, sso={sso}, mail={mail} |",
    f"| Overall | **{overall}** ({passed} pass / {failed} fail / {skipped} skip) |",
    "",
    "## Checks",
    "",
])
for c in checks:
    lines.append(f"- [{c['status']}] **{c['name']}** — {c['detail']}")
lines.append("")
Path(report_md).write_text("\n".join(lines))
PY

cp -f "${report_json}" "${report_dir}/latest-signoff.json"

rm -f "${checks_ndjson}"

echo ""
echo "Sign-off report:"
echo "  JSON: ${report_json}"
echo "  Markdown: ${report_md}"

if [[ -n "${STAGING_SIGNOFF_S3_URI:-}" ]]; then
  if ! command -v aws >/dev/null; then
    echo "STAGING_SIGNOFF_S3_URI is set but aws CLI was not found" >&2
    exit 1
  fi
  s3_prefix="${STAGING_SIGNOFF_S3_URI%/}/${timestamp}"
  aws s3 cp "$report_json" "${s3_prefix}/$(basename "$report_json")"
  aws s3 cp "$report_md" "${s3_prefix}/$(basename "$report_md")"
  echo "  S3: ${s3_prefix}/"
fi

if [[ "$signoff_failed" -ne 0 ]]; then
  echo "FAIL: staging sign-off checks failed for ${EDGE_URL}" >&2
  exit 1
fi

echo "OK: staging live-host sign-off passed for ${EDGE_URL}"
