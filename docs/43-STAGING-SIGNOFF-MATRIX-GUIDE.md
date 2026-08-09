# Multi-environment staging sign-off matrix

**Version:** v0.2.38-beta  
**Scope:** Run live-host sign-off across multiple staging environments and produce a combined matrix report.

Complements single-host sign-off ([31-STAGING-LIVE-SIGNOFF-GUIDE.md](31-STAGING-LIVE-SIGNOFF-GUIDE.md)) and S3 archival ([38-STAGING-SIGNOFF-S3-ARCHIVE-GUIDE.md](38-STAGING-SIGNOFF-S3-ARCHIVE-GUIDE.md)).

---

## Configuration

```bash
STAGING_SIGNOFF_ENVIRONMENTS=us-east=https://staging-us.example.com,eu-west=https://staging-eu.example.com
export IMAGE_TAG=v0.2.38-beta
```

Optional:

| Variable | Default | Purpose |
|----------|---------|---------|
| `STAGING_SIGNOFF_MATRIX_INCLUDE_DOGFOOD` | `0` | Run full dogfood per environment (slow) |
| `STAGING_SIGNOFF_REPORT_DIR` | `./reports/staging-signoff` | Base report directory |
| `STAGING_SIGNOFF_S3_URI` | unset | Upload matrix summary JSON/Markdown to S3 |
| `STAGING_SIGNOFF_LABEL` | unset | Per-env label (set automatically by matrix) |

---

## Usage

```bash
./scripts/staging-signoff-matrix.sh

# Or CLI pairs:
./scripts/staging-signoff-matrix.sh \
  us-east=https://staging-us.example.com \
  eu-west=https://staging-eu.example.com
```

By default each environment runs `staging-signoff.sh` with `STAGING_SIGNOFF_SKIP_DOGFOOD=1` (sign-off probes only). Set `STAGING_SIGNOFF_MATRIX_INCLUDE_DOGFOOD=1` to include full dogfood gates per host.

---

## Reports

Matrix output under `reports/staging-signoff/matrix-<timestamp>/`:

| File | Content |
|------|---------|
| `signoff-matrix-<timestamp>.json` | Combined summary + per-environment embedded reports |
| `signoff-matrix-<timestamp>.md` | Operator matrix table |
| `<label>/signoff-*.json` | Per-environment sign-off reports |

Each per-environment report includes an `environment` field when run via the matrix.

---

## S3 archival

When `STAGING_SIGNOFF_S3_URI` is set, matrix JSON/Markdown upload to:

```text
s3://bucket/prefix/matrix-<timestamp>/signoff-matrix-<timestamp>.json
```

Per-environment reports remain on disk (or sync separately).

---

## Smoke test

```bash
export STAGING_SIGNOFF_SKIP_DOGFOOD=1
export STAGING_SIGNOFF_REQUIRE_HTTPS=0
export STAGING_SIGNOFF_ENVIRONMENTS=local=http://127.0.0.1:9
./scripts/staging-signoff-matrix.sh
# Expect fail (host unreachable) — validates parsing + report generation
bash -n scripts/staging-signoff-matrix.sh
```

---

## Related

| Doc | Topic |
|-----|-------|
| [31-STAGING-LIVE-SIGNOFF-GUIDE.md](31-STAGING-LIVE-SIGNOFF-GUIDE.md) | Single-host sign-off |
| [14-STAGING-DOGFOOD-GUIDE.md](14-STAGING-DOGFOOD-GUIDE.md) | Staging deploy playbook |
