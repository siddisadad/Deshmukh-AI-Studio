# Staging sign-off cron and release gate integration

**Version:** v0.2.54-beta  
**Scope:** Scheduled sign-off cron wrapper, release gate API, and pre-tag validation script.

Complements live-host sign-off ([31-STAGING-LIVE-SIGNOFF-GUIDE.md](31-STAGING-LIVE-SIGNOFF-GUIDE.md)), sign-off matrix ([43-STAGING-SIGNOFF-MATRIX-GUIDE.md](43-STAGING-SIGNOFF-MATRIX-GUIDE.md)), and S3 archival ([38-STAGING-SIGNOFF-S3-ARCHIVE-GUIDE.md](38-STAGING-SIGNOFF-S3-ARCHIVE-GUIDE.md)).

---

## Database (V31)

Table `staging_signoff_runs` stores submitted sign-off reports for release gate evaluation:

| Column | Purpose |
|--------|---------|
| `run_type` | `SINGLE` or `MATRIX` |
| `host` | Edge URL or `matrix` |
| `image_tag` | `IMAGE_TAG` from report |
| `overall` | `pass` or `fail` |
| `pass_count` / `fail_count` / `skip_count` | Summary counts |
| `report_json` | Full report payload |
| `s3_uri` | Optional S3 archive prefix |

---

## Scheduled cron

```bash
export IMAGE_TAG=v0.2.54-beta
export STAGING_SIGNOFF_URL=https://staging.yourdomain.com
export STAGING_SIGNOFF_S3_URI=s3://bucket/staging-signoff-reports  # optional
export BILLING_USAGE_SYNC_TOKEN=...
export OPS_SIGNOFF_SUBMIT_ENABLED=1  # default 1 — POST report to API after run

./scripts/scheduled-staging-signoff.sh
```

Matrix cron:

```bash
export STAGING_SIGNOFF_ENVIRONMENTS=us=https://staging-us.example.com,eu=https://staging-eu.example.com
export IMAGE_TAG=v0.2.54-beta
./scripts/scheduled-staging-signoff.sh
```

Runs `staging-signoff.sh` or `staging-signoff-matrix.sh`, then optionally submits the report via API.

Local latest pointers for offline gate checks:

- `reports/staging-signoff/latest-signoff.json`
- `reports/staging-signoff/latest-signoff-matrix.json`

---

## Release gate API (operator)

Auth: `BILLING_USAGE_SYNC_TOKEN`

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/ops/staging-signoff/submit` | Submit sign-off JSON report |
| `GET` | `/api/v1/ops/staging-signoff/runs` | List recent runs |
| `GET` | `/api/v1/ops/release-gate?imageTag={tag}` | Evaluate gate for tag |

Release gate opens when a **passing** report exists for the requested `imageTag` within `RELEASE_GATE_MAX_AGE_HOURS` (default 48).

---

## Pre-tag gate check

Before tagging a beta on `main`:

```bash
export IMAGE_TAG=v0.2.54-beta
export BILLING_USAGE_SYNC_TOKEN=...
./scripts/release-gate-check.sh
```

Without API token, validates local `latest-signoff.json` or `RELEASE_GATE_REPORT_JSON`.

---

## Environment

| Variable | Default | Purpose |
|----------|---------|---------|
| `RELEASE_GATE_MAX_AGE_HOURS` | `48` | Max age for passing sign-off |
| `OPS_SIGNOFF_SUBMIT_ENABLED` | `1` | Cron submits report to API |
| `STAGING_SIGNOFF_URL` | unset | Single-host cron target |
| `STAGING_SIGNOFF_ENVIRONMENTS` | unset | Matrix cron config |

---

## Release workflow

1. Deploy staging with candidate `IMAGE_TAG`
2. Cron or manual `scheduled-staging-signoff.sh` → pass + API submit
3. `release-gate-check.sh` → exit 0
4. Merge release PR, tag `v0.2.X-beta`

---

## Related

| Doc | Topic |
|-----|-------|
| [31-STAGING-LIVE-SIGNOFF-GUIDE.md](31-STAGING-LIVE-SIGNOFF-GUIDE.md) | Sign-off checks |
| [43-STAGING-SIGNOFF-MATRIX-GUIDE.md](43-STAGING-SIGNOFF-MATRIX-GUIDE.md) | Multi-environment matrix |
| [14-STAGING-DOGFOOD-GUIDE.md](14-STAGING-DOGFOOD-GUIDE.md) | Staging deploy playbook |
