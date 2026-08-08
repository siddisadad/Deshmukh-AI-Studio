# Long-term observability archive (Glacier + cross-region)

Tier **off-site export archives** (Loki NDJSON, chat thread gzips) from S3 Standard to **Glacier** / **Deep Archive**, and optionally **replicate** to a DR region. Complements hot Loki retention ([17-LOG-ARCHIVE-GUIDE.md](17-LOG-ARCHIVE-GUIDE.md)) and chat sync ([19-CHAT-ARCHIVE-SYNC-GUIDE.md](19-CHAT-ARCHIVE-SYNC-GUIDE.md)).

**MVP scope:** lifecycle + CRR **scripts and IAM examples** — operators run against AWS (or S3-compatible with reduced feature support). Loki live chunks stay on Standard unless you explicitly lifecycle those prefixes.

---

## 1. Archive tiers

| Tier | Typical use | Access |
|---|---|---|
| **S3 Standard** | 0–90 days — recent exports, restore tests | Immediate |
| **Glacier** (`GLACIER` or `GLACIER_IR`) | 90d–1y — compliance, incident lookback | Minutes–hours |
| **S3 Deep Archive** | 1y+ — long retention, rare access | 12–48 hours |

Hot **Loki query path** (`LOKI_RETENTION_PERIOD`, default 30d) is separate from export archive tiering.

---

## 2. Apply Glacier lifecycle (exports)

`scripts/apply-s3-archive-lifecycle.sh` sets prefix-scoped lifecycle rules via AWS CLI.

```bash
export ARCHIVE_S3_BUCKET=my-aistudio-logs
export ARCHIVE_S3_PREFIXES=loki-exports/,chat-archives/
export ARCHIVE_GLACIER_DAYS=90
export ARCHIVE_DEEP_ARCHIVE_DAYS=365
# Optional: delete after 7 years
# export ARCHIVE_EXPIRE_DAYS=2555
# Glacier Instant Retrieval vs Flexible (default GLACIER)
# export ARCHIVE_GLACIER_STORAGE_CLASS=GLACIER_IR

./scripts/apply-s3-archive-lifecycle.sh
```

Or reuse the Loki bucket:

```bash
export LOKI_S3_BUCKET=my-aistudio-logs
./scripts/apply-s3-archive-lifecycle.sh
```

**Example static policy** (reference): `monitoring/s3-lifecycle-archive-example.json`

### Restore for audit

```bash
aws s3api restore-object \
  --bucket my-aistudio-logs \
  --key loki-exports/20260808-021500/loki-export.ndjson.gz \
  --restore-request '{"Days":7,"GlacierJobParameters":{"Tier":"Standard"}}'
```

---

## 3. Cross-region replication (DR)

Replicate **export prefixes** to a secondary region bucket (async CRR).

### 3.1 IAM role

1. Create role `s3-crr-role` trusted by `s3.amazonaws.com`.
2. Attach policy from `monitoring/s3-replication-role-policy-example.json` (replace `SOURCE_BUCKET_NAME` / `DEST_BUCKET_NAME`).

### 3.2 Enable replication

```bash
export SOURCE_S3_BUCKET=my-aistudio-logs
export SOURCE_S3_REGION=us-east-1
export DEST_S3_BUCKET=my-aistudio-logs-dr
export DEST_S3_REGION=us-west-2
export S3_REPLICATION_ROLE_ARN=arn:aws:iam::123456789012:role/s3-crr-role
export ARCHIVE_S3_PREFIXES=loki-exports/,chat-archives/

./scripts/enable-s3-cross-region-replication.sh
```

Script enables versioning on both buckets and applies prefix-filtered replication rules.

### 3.3 DR notes

| Topic | Guidance |
|---|---|
| **RPO** | CRR is asynchronous — typically minutes; not a live failover for Loki queries |
| **RTO** | Restore exports from DR bucket; re-run Grafana/Loki only if you rebuild hot store |
| **Cost** | Replication + storage in second region; lifecycle rules should be applied on **both** buckets |
| **MinIO / non-AWS** | CRR and Glacier tiers may be unavailable — use export cron + cross-site copy instead |

---

## 4. End-to-end operator checklist

1. **Hot logs:** Loki retention (`LOKI_RETENTION_PERIOD`) + alerts ([monitoring/README.md](../monitoring/README.md)).
2. **Daily export:** `scripts/export-loki-logs.sh` + `LOKI_ARCHIVE_S3_URI` ([17-LOG-ARCHIVE-GUIDE.md](17-LOG-ARCHIVE-GUIDE.md)).
3. **Chat archive:** `scripts/scheduled-chat-archive.sh` + `CHAT_ARCHIVE_S3_URI` ([19-CHAT-ARCHIVE-SYNC-GUIDE.md](19-CHAT-ARCHIVE-SYNC-GUIDE.md)).
4. **Tiering:** `./scripts/apply-s3-archive-lifecycle.sh` (this guide).
5. **DR:** IAM role + `./scripts/enable-s3-cross-region-replication.sh`.
6. **Validate:** `./scripts/validate-staging-env.sh` when `LOKI_OBJECT_STORE=s3`.

---

## 5. Environment variables

| Variable | Default | Purpose |
|---|---|---|
| `ARCHIVE_S3_BUCKET` | — | Bucket for lifecycle (or `LOKI_S3_BUCKET`) |
| `ARCHIVE_S3_PREFIXES` | `loki-exports/,chat-archives/` | Prefix filters |
| `ARCHIVE_GLACIER_DAYS` | `90` | Days before Glacier transition |
| `ARCHIVE_DEEP_ARCHIVE_DAYS` | `365` | Days before Deep Archive |
| `ARCHIVE_EXPIRE_DAYS` | `0` | Optional expiration (0 = none) |
| `ARCHIVE_GLACIER_STORAGE_CLASS` | `GLACIER` | `GLACIER` or `GLACIER_IR` |
| `SOURCE_S3_BUCKET` | `LOKI_S3_BUCKET` | CRR source |
| `DEST_S3_BUCKET` | — | CRR destination |
| `DEST_S3_REGION` | — | DR region |
| `S3_REPLICATION_ROLE_ARN` | — | IAM role for CRR |

---

## 6. Related

| Doc | Topic |
|---|---|
| [17-LOG-ARCHIVE-GUIDE.md](17-LOG-ARCHIVE-GUIDE.md) | Loki export + S3 backend |
| [19-CHAT-ARCHIVE-SYNC-GUIDE.md](19-CHAT-ARCHIVE-SYNC-GUIDE.md) | Chat archive cron |
| [13-DEPLOYMENT-GUIDE.md](13-DEPLOYMENT-GUIDE.md) | Production deploy |

---

## Document control

| Version | Date | Notes |
|---|---|---|
| 1.0 | 2026-08-08 | Glacier lifecycle + CRR scripts and playbook |
