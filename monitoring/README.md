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

Staging-shaped stack:

```bash
docker compose -f docker-compose.yml -f docker-compose.staging.yml -f docker-compose.monitoring.yml up -d
```

## Security

- `METRICS_SCRAPE_TOKEN` enables a static bearer for `/actuator/prometheus` only.
- Keep the token on the internal Docker network; never proxy metrics on nginx.
- Rotate the token if leaked.

## Alerts

`monitoring/alerts.yml` defines `ApiDown`, `ApiHighErrorRate`, and `ApiHighLatencyP95`. Prometheus forwards firing alerts to Alertmanager (`monitoring/alertmanager.yml`).

- Alertmanager UI: http://localhost:9093 (do not expose publicly without auth)
- Default receiver logs alerts in the UI only; add `webhook_configs` or `email_configs` in `alertmanager.yml` for paging in production
