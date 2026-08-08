# Monitoring (Prometheus + Grafana + Loki)

Optional overlay for local or staging observability. Scrapes Spring Boot Actuator metrics from the API on the Docker network and ships API container JSON logs to Loki — not exposed on the public nginx edge.

## Prerequisites

1. Generate a scrape secret (32+ random bytes) and export it:

```bash
export METRICS_SCRAPE_TOKEN="$(openssl rand -hex 32)"
```

2. Write the Prometheus bearer token file (gitignored):

```bash
./scripts/write-prometheus-token.sh
```

3. Optional multi-region observability (`LOKI_QUERY_REGIONS`, `PROMETHEUS_QUERY_REGIONS`):

```bash
./scripts/write-grafana-loki-regions.sh
./scripts/write-grafana-prometheus-regions.sh
./scripts/write-grafana-federated-dashboard.sh
./scripts/write-grafana-billing-dashboard.sh
./scripts/sync-loki-ruler-regions.sh   # remote Loki rulers
```

See [docs/33-FEDERATED-GRAFANA-LOKI-RULER-GUIDE.md](../docs/33-FEDERATED-GRAFANA-LOKI-RULER-GUIDE.md).

## Start stack

With the default dev compose:

```bash
docker compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d
```

- Grafana: http://localhost:3000 (default `admin` / `GRAFANA_ADMIN_PASSWORD` or `admin`)
- Prometheus: http://localhost:9090 (internal ops; do not expose publicly)
- Alertmanager: http://localhost:9093 (internal ops; do not expose publicly)
- Loki: http://localhost:3100 (internal ops; query via Grafana Explore)

## Logs (Loki + Promtail)

Promtail discovers the Compose `api` service via Docker and pushes stdout to Loki. The API `prod` profile emits JSON logs with `requestId` in MDC.

In Grafana → **Explore** → datasource **Loki**:

```logql
{service="api"} | json | line_format "{{.message}}"
```

Filter by request id when debugging:

```logql
{service="api"} | json | requestId="your-request-id"
```

### Retention

Loki compactor deletes logs older than `LOKI_RETENTION_PERIOD` (default **720h / 30 days**). Requires `compactor.retention_enabled` and a 24h TSDB index period (configured in `loki-config.yml`). Compactor marker files live on the `loki-data` volume (`/loki/compactor`).

```bash
export LOKI_RETENTION_PERIOD=2160h   # 90 days
docker compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d
```

Deletion is asynchronous (after compaction + `retention_delete_delay`). Align `max_query_lookback` with retention so Grafana Explore cannot query beyond retained data.

### Off-site archive / S3 backend

- **Export (cron):** `scripts/export-loki-logs.sh` → gzipped NDJSON; optional `LOKI_ARCHIVE_S3_URI` upload
- **S3 object store:** `monitoring/loki-config-s3.yml` + `docker-compose.monitoring-s3.yml` overlay
- Playbook: [docs/17-LOG-ARCHIVE-GUIDE.md](../docs/17-LOG-ARCHIVE-GUIDE.md)
- **Long-term tiering / DR:** [docs/21-OBSERVABILITY-LONG-TERM-ARCHIVE-GUIDE.md](../docs/21-OBSERVABILITY-LONG-TERM-ARCHIVE-GUIDE.md)
- **Multi-region live query:** [docs/27-LOKI-MULTI-REGION-QUERY-GUIDE.md](../docs/27-LOKI-MULTI-REGION-QUERY-GUIDE.md) — `scripts/query-loki-multi-region.sh`, `write-grafana-loki-regions.sh`
- **Federated dashboards / ruler fan-out:** [docs/33-FEDERATED-GRAFANA-LOKI-RULER-GUIDE.md](../docs/33-FEDERATED-GRAFANA-LOKI-RULER-GUIDE.md)

Staging-shaped stack:

```bash
docker compose -f docker-compose.yml -f docker-compose.staging.yml -f docker-compose.monitoring.yml up -d
```

## Security

- `METRICS_SCRAPE_TOKEN` enables a static bearer for `/actuator/prometheus` only.
- Keep the token on the internal Docker network; never proxy metrics on nginx.
- Rotate the token if leaked.

## Alerts

**Metrics (Prometheus):** `monitoring/alerts.yml` defines `ApiDown`, `ApiHighErrorRate`, and `ApiHighLatencyP95`.

**Logs (Loki ruler):** `monitoring/loki-alerts.yml` defines `ApiErrorLogsHigh`, `ApiWarnLogsHigh`, and `ApiLogsMissing` (prod JSON logs with `level` field).

Both forward to Alertmanager (`monitoring/alertmanager.yml`).

- Alertmanager UI: http://localhost:9093 (do not expose publicly without auth)
- Default receiver logs alerts in the UI only; add `webhook_configs` or `email_configs` in `alertmanager.yml` for paging in production
