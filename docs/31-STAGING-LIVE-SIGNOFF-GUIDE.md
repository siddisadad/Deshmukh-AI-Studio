# Staging live-host sign-off automation

**Version:** v0.2.26-beta  
**Scope:** Automated live-host sign-off after deploy — extends dogfood gates with HTTPS, security headers, billing/SSO path probes, SSE stream smoke, and a JSON/Markdown report.

Complements [14-STAGING-DOGFOOD-GUIDE.md](14-STAGING-DOGFOOD-GUIDE.md) (operator playbook) and [24-STAGING-PROVIDER-PROBES-GUIDE.md](24-STAGING-PROVIDER-PROBES-GUIDE.md) (provider readiness).

---

## Quick start

After deploy on a staging host:

```bash
export IMAGE_TAG=v0.2.26-beta
./scripts/staging-signoff.sh https://staging.yourdomain.com
```

Reports are written to `reports/staging-signoff/` (override with `STAGING_SIGNOFF_REPORT_DIR`).

Full dogfood + sign-off in one command:

```bash
STAGING_SIGNOFF=1 ./scripts/staging-dogfood.sh https://staging.yourdomain.com
```

---

## What runs

| Phase | Source | Checks |
|-------|--------|--------|
| Dogfood gates | `staging-dogfood.sh` (automated steps 1–6) | env validation, health, smoke, API journey, provider probes, optional internal metrics |
| HTTPS / TLS | `staging-signoff.sh` | TLS reachability; optional HSTS on `/actuator/health` |
| Security headers | `staging-signoff.sh` | `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy` on SPA + health |
| Actuator leak | `staging-signoff.sh` | `/actuator/prometheus` must not return 200 on edge |
| Billing paths | `staging-signoff.sh` | org overview, usage history; Stripe checkout URL when `BILLING_PROVIDER=stripe` |
| SSO | `staging-signoff.sh` | public `/auth/sso/providers` list |
| SSE stream | `staging-signoff.sh` | mock/native stream `delta` + `done` events (disable with `STAGING_SIGNOFF_STREAM=0`) |

Manual UX checklist (§9 in dogfood guide) remains for human operators — RAG upload, Kanban, network reconnect UX, etc.

---

## Environment variables

| Variable | Default | Purpose |
|----------|---------|---------|
| `STAGING_SIGNOFF_REPORT_DIR` | `./reports/staging-signoff` | Report output directory |
| `STAGING_SIGNOFF_SKIP_DOGFOOD` | unset | Skip dogfood when invoked from dogfood step 7 |
| `STAGING_SIGNOFF_REQUIRE_HTTPS` | `1` when URL is `https://` | Fail if edge is not HTTPS |
| `STAGING_SIGNOFF_STREAM` | `1` | Run SSE chat stream probe |
| `STAGING_SIGNOFF` / `STAGING_DOGFOOD_FULL_SIGNOFF` | unset | Dogfood step 7 runs full sign-off instead of manual echo |
| `IMAGE_TAG` | `unknown` | Recorded in report |
| `API_URL` | edge proxy | Direct API for probes/metrics when set |
| `METRICS_SCRAPE_TOKEN` | unset | Enables dogfood internal Prometheus check |

---

## Sign-off report

Each run writes:

- `signoff-YYYYMMDDTHHMMSSZ.json` — machine-readable checks + summary
- `signoff-YYYYMMDDTHHMMSSZ.md` — operator table for release notes

Example JSON fields:

```json
{
  "timestamp": "20260808T213000Z",
  "host": "https://staging.yourdomain.com",
  "imageTag": "v0.2.26-beta",
  "providers": { "billing": "stripe", "sso": "oidc", "mail": "smtp" },
  "summary": { "pass": 14, "fail": 0, "skip": 2, "overall": "pass" },
  "checks": [ { "name": "dogfood-automated", "status": "pass", "detail": "..." } ]
}
```

Record the Markdown report path in the sign-off table ([14-STAGING-DOGFOOD-GUIDE.md](14-STAGING-DOGFOOD-GUIDE.md) §10) before tagging the next beta.

---

## Local dry-run

`staging-dry-run.sh` runs sign-off extras (skip dogfood duplicate, no HTTPS requirement) against the local staging-shaped stack:

```bash
./scripts/staging-dry-run.sh
```

---

## Troubleshooting

| Failure | Likely fix |
|---------|------------|
| `security-headers-spa` | Confirm nginx edge config (`nginx/conf.d/aistudio.conf`) |
| `https-tls` | DNS, TLS cert, or set `STAGING_SIGNOFF_REQUIRE_HTTPS=0` for HTTP dev |
| `hsts-header` skip | Expected on HTTP or non-prod profile; enable prod + HTTPS API |
| `sse-stream-chat` | Check `AI_PROVIDER`, API logs, edge proxy `proxy_buffering off` |
| `billing-stripe-checkout` | Stripe keys, price IDs, `BILLING_APP_BASE_URL` |

---

## Related

| Doc | Topic |
|-----|-------|
| [14-STAGING-DOGFOOD-GUIDE.md](14-STAGING-DOGFOOD-GUIDE.md) | Staging deploy + manual checklist |
| [24-STAGING-PROVIDER-PROBES-GUIDE.md](24-STAGING-PROVIDER-PROBES-GUIDE.md) | Stripe/OIDC/SAML/SMTP probes |
| [13-DEPLOYMENT-GUIDE.md](13-DEPLOYMENT-GUIDE.md) | Compose deploy + GHCR |
