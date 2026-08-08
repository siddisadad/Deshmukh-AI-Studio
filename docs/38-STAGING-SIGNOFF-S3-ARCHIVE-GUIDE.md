# Staging sign-off report archival in S3

**Version:** v0.2.33-beta  
**Scope:** Off-site upload of live-host sign-off JSON/Markdown reports after each run.

Complements sign-off automation ([31-STAGING-LIVE-SIGNOFF-GUIDE.md](31-STAGING-LIVE-SIGNOFF-GUIDE.md)) and long-term archive playbooks ([21-OBSERVABILITY-LONG-TERM-ARCHIVE-GUIDE.md](21-OBSERVABILITY-LONG-TERM-ARCHIVE-GUIDE.md)).

---

## Configuration

```bash
STAGING_SIGNOFF_S3_URI=s3://my-bucket/staging-signoff-reports
```

Requires AWS CLI (`aws`) with credentials that can `s3:PutObject` on the bucket prefix.

Reports are uploaded **after every run** (pass or fail) so failed sign-offs are retained for debugging.

---

## S3 layout

Each run uploads to:

```text
s3://my-bucket/staging-signoff-reports/20260808T223000Z/signoff-20260808T223000Z.json
s3://my-bucket/staging-signoff-reports/20260808T223000Z/signoff-20260808T223000Z.md
```

The timestamp folder matches the report `timestamp` field in JSON.

---

## Usage

```bash
export IMAGE_TAG=v0.2.33-beta
export STAGING_SIGNOFF_S3_URI=s3://compliance-bucket/staging-signoff-reports
./scripts/staging-signoff.sh https://staging.yourdomain.com
```

Or with full dogfood:

```bash
export STAGING_SIGNOFF=1
export STAGING_SIGNOFF_S3_URI=s3://compliance-bucket/staging-signoff-reports
./scripts/staging-dogfood.sh https://staging.yourdomain.com
```

Pair with Glacier lifecycle on the bucket prefix for long-term retention ([21-OBSERVABILITY-LONG-TERM-ARCHIVE-GUIDE.md](21-OBSERVABILITY-LONG-TERM-ARCHIVE-GUIDE.md)).

---

## Smoke test

```bash
# LocalStack or dev bucket
export STAGING_SIGNOFF_SKIP_DOGFOOD=1
export STAGING_SIGNOFF_REQUIRE_HTTPS=0
export STAGING_SIGNOFF_S3_URI=s3://test-bucket/signoff
./scripts/staging-signoff.sh http://localhost:8088
aws s3 ls s3://test-bucket/signoff/ --recursive
```

---

## Related

| Doc | Topic |
|-----|-------|
| [31-STAGING-LIVE-SIGNOFF-GUIDE.md](31-STAGING-LIVE-SIGNOFF-GUIDE.md) | Sign-off checks + local reports |
| [14-STAGING-DOGFOOD-GUIDE.md](14-STAGING-DOGFOOD-GUIDE.md) | Staging deploy playbook |
| [21-OBSERVABILITY-LONG-TERM-ARCHIVE-GUIDE.md](21-OBSERVABILITY-LONG-TERM-ARCHIVE-GUIDE.md) | Glacier lifecycle + cross-region |
