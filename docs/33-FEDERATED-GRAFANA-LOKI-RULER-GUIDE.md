# Federated Grafana dashboards and Loki ruler fan-out

**Version:** v0.2.28-beta  
**Scope:** Multi-region Grafana dashboards and synchronized Loki ruler alert rules.

Complements multi-region Loki query ([27-LOKI-MULTI-REGION-QUERY-GUIDE.md](27-LOKI-MULTI-REGION-QUERY-GUIDE.md)) and monitoring stack ([monitoring/README.md](../monitoring/README.md)).

---

## Overview

| Tool | Purpose |
|------|---------|
| `write-grafana-prometheus-regions.sh` | Extra Grafana Prometheus datasources from `PROMETHEUS_QUERY_REGIONS` |
| `write-grafana-federated-dashboard.sh` | **AI Studio Federated** dashboard — per-region API up, 5xx, ERROR/WARN logs |
| `sync-loki-ruler-regions.sh` | POST `loki-alerts.yml` to each regional Loki ruler |

Run after `write-grafana-loki-regions.sh` when both Loki and Prometheus regions are configured.

---

## Configuration

```bash
# Loki regions (existing)
LOKI_QUERY_REGIONS=us-east=http://loki-us-east.internal:3100,eu-west=http://loki-eu-west.internal:3100

# Optional federated Prometheus regions
PROMETHEUS_QUERY_REGIONS=us-east=http://prom-us-east.internal:9090,eu-west=http://prom-eu-west.internal:9090

# Ruler fan-out namespace (default fake — matches local compose mount)
LOKI_RULER_NAMESPACE=fake
```

---

## One-shot provisioning

```bash
export LOKI_QUERY_REGIONS=us-east=http://loki-us:3100,eu-west=http://loki-eu:3100
export PROMETHEUS_QUERY_REGIONS=us-east=http://prom-us:9090,eu-west=http://prom-eu:9090

./scripts/write-grafana-loki-regions.sh
./scripts/write-grafana-prometheus-regions.sh
./scripts/write-grafana-federated-dashboard.sh
./scripts/sync-loki-ruler-regions.sh

export METRICS_SCRAPE_TOKEN="$(openssl rand -hex 32)"
./scripts/write-prometheus-token.sh
docker compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d
```

Grafana → **AI Studio** folder → dashboard **AI Studio Federated** (`uid: aistudio-federated`).

---

## Loki ruler fan-out

Local compose mounts `monitoring/loki-alerts.yml` into the primary Loki container. Remote regions need the same rules for `ApiErrorLogsHigh`, `ApiWarnLogsHigh`, and `ApiLogsMissing`.

```bash
# Primary + all LOKI_QUERY_REGIONS endpoints
./scripts/sync-loki-ruler-regions.sh

# Primary only
LOKI_URL=http://localhost:3100 ./scripts/sync-loki-ruler-regions.sh
```

Cron example (after regional Loki deploy):

```bash
0 */6 * * * cd /opt/aistudio && ./scripts/sync-loki-ruler-regions.sh
```

Rules POST to `POST /loki/api/v1/rules/{namespace}` with `Content-Type: application/yaml`.

---

## Federated dashboard panels

Per Prometheus region (or local default):

- API scrape target `up`
- HTTP 5xx rate

Per Loki region (or local default):

- ERROR log rate (`level="ERROR"` JSON logs)
- WARN log rate

When `LOKI_QUERY_REGIONS` / `PROMETHEUS_QUERY_REGIONS` are unset, the dashboard uses primary **Loki** and **Prometheus** datasources (single-region dev).

---

## Smoke test

```bash
export IMAGE_TAG=v0.2.28-beta
docker compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d
./scripts/write-grafana-federated-dashboard.sh
./scripts/sync-loki-ruler-regions.sh   # LOKI_URL=http://localhost:3100
# Grafana :3000 → AI Studio Federated
```

---

## Related

| Doc | Topic |
|-----|-------|
| [27-LOKI-MULTI-REGION-QUERY-GUIDE.md](27-LOKI-MULTI-REGION-QUERY-GUIDE.md) | CLI merged LogQL |
| [17-LOG-ARCHIVE-GUIDE.md](17-LOG-ARCHIVE-GUIDE.md) | Loki export cron |
| [monitoring/README.md](../monitoring/README.md) | Stack setup |
