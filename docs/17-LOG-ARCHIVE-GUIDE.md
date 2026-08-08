# Log archive and S3-backed Loki

Off-site retention for API JSON logs shipped by Promtail → Loki. Complements local compactor retention (`LOKI_RETENTION_PERIOD`).

**MVP scope:** filesystem Loki for docker-compose dev; optional **S3 object store** for production; **export script** for cron-based NDJSON archives.

---

## 1. Export logs off-site (cron)

`scripts/export-loki-logs.sh` queries Loki and writes **gzipped NDJSON** (one JSON object per log line).

```bash
# Default: last 24h of {service="api"} from local Loki
./scripts/export-loki-logs.sh

# Custom window and output directory
export LOKI_URL=http://localhost:3100
export LOKI_EXPORT_HOURS=168          # 7 days
export LOKI_EXPORT_QUERY='{service="api"} | json | level="ERROR"'
export LOKI_EXPORT_DIR=./backups/loki
./scripts/export-loki-logs.sh
```

**Optional S3 upload** (requires AWS CLI + credentials):

```bash
export LOKI_ARCHIVE_S3_URI=s3://my-company-aistudio-logs/loki-exports
./scripts/export-loki-logs.sh
```

**Cron example** (daily 02:15 UTC, keep 90 days locally then lifecycle on bucket):

```cron
15 2 * * * cd /opt/aistudio && LOKI_URL=http://127.0.0.1:3100 LOKI_ARCHIVE_S3_URI=s3://bucket/loki ./scripts/export-loki-logs.sh
```

Output format per line:

```json
{"timestamp_ns":"...","labels":{"service":"api","..."},"line":"..."}
```

---

## 2. Loki S3 object store (production)

Use when logs must survive host/volume loss. Chunks and delete markers live in S3; local volume holds TSDB shipper cache only.

### 2.1 Environment variables

```bash
LOKI_OBJECT_STORE=s3
LOKI_S3_BUCKET=my-aistudio-logs
LOKI_S3_REGION=us-east-1
LOKI_S3_ACCESS_KEY_ID=...
LOKI_S3_SECRET_ACCESS_KEY=...
# MinIO / path-style S3-compatible:
# LOKI_S3_ENDPOINT=http://minio:9000
# LOKI_S3_FORCE_PATH_STYLE=true
# LOKI_S3_INSECURE=true
LOKI_RETENTION_PERIOD=720h
```

Run `./scripts/validate-staging-env.sh` when `LOKI_OBJECT_STORE=s3`.

### 2.2 Compose

```bash
export METRICS_SCRAPE_TOKEN="$(openssl rand -hex 32)"
./scripts/write-prometheus-token.sh

docker compose -f docker-compose.yml -f docker-compose.monitoring.yml \
  -f docker-compose.monitoring-s3.yml up -d
```

Config file: `monitoring/loki-config-s3.yml` (schema `tsdb` + `object_store: s3`).

### 2.3 Bucket policy

- Restrict bucket to Loki IAM user / role (read/write on prefix).
- Enable versioning or lifecycle transition to Glacier for long-term archive.
- Do not expose the bucket publicly.

---

## 3. Local dev (filesystem)

Default stack keeps chunks on the `loki-data` volume (`monitoring/loki-config.yml`). Use export script before retention deletes old logs if you need off-site copies.

---

## 4. Related docs

- [monitoring/README.md](../monitoring/README.md) — Promtail, retention, alerts
- [13-DEPLOYMENT-GUIDE.md](13-DEPLOYMENT-GUIDE.md) — monitoring overlay on staging/prod
- [19-CHAT-ARCHIVE-SYNC-GUIDE.md](19-CHAT-ARCHIVE-SYNC-GUIDE.md) — conversation thread off-site archive

---

## Document control

| Version | Date | Notes |
|---|---|---|
| 1.0 | 2026-08-08 | S3 Loki config + export script |
