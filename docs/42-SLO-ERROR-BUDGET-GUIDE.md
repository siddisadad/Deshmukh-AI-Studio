# SLO dashboards and error budget alerts

**Version:** v0.2.37-beta  
**Scope:** Prometheus recording rules, error-budget alerts, and Grafana SLO dashboard for API availability and latency.

Complements API monitoring ([monitoring/README.md](../monitoring/README.md)) and Alertmanager routing ([39-ALERTMANAGER-ONCALL-GUIDE.md](39-ALERTMANAGER-ONCALL-GUIDE.md)).

---

## SLO targets

| SLO | Target | SLI |
|-----|--------|-----|
| **Availability** | 99.5% over 30d | Non-5xx HTTP responses |
| **Latency** | 95% under 2s over 30d | `http_server_requests_seconds_bucket{le="2"}` |

---

## Recording rules

`monitoring/slo-recording-rules.yml` (evaluated every 1m):

| Metric | Description |
|--------|-------------|
| `aistudio_slo:api_success_ratio:5m` | 5m rolling success ratio |
| `aistudio_slo:api_latency_sli:5m` | 5m rolling fraction under 2s |
| `aistudio_slo:api_availability:30d` | 30d average availability SLI |
| `aistudio_slo:api_latency_sli:30d` | 30d average latency SLI |
| `aistudio_slo:api_availability_error_budget_remaining:30d` | Availability error budget remaining (0–1) |
| `aistudio_slo:api_latency_error_budget_remaining:30d` | Latency error budget remaining (0–1) |

Requires Prometheus retention ≥ 30d for full-window accuracy (default dev stack may show partial windows).

---

## Alerts

`monitoring/slo-alerts.yml`:

| Alert | Condition |
|-------|-----------|
| `ApiSloAvailabilityBudgetLow` | Availability budget < 25% for 1h |
| `ApiSloAvailabilityBudgetCritical` | Availability budget < 10% for 30m |
| `ApiSloLatencyBudgetLow` | Latency budget < 25% for 1h |
| `ApiSloAvailabilityBurnRateHigh` | 1h 5xx rate > 14.4× SLO allowance (~2-day exhaust) |

Routes through Alertmanager when on-call integrations are configured.

---

## Grafana

**Title:** AI Studio SLO  
**UID:** `aistudio-slo`

```bash
./scripts/write-grafana-slo-dashboard.sh
docker compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d
```

Panels: 30d SLIs, error budget remaining, rolling 5m SLI vs targets.

---

## Smoke test

```bash
export IMAGE_TAG=v0.2.37-beta
export METRICS_SCRAPE_TOKEN="$(openssl rand -hex 32)"
./scripts/write-prometheus-token.sh
./scripts/write-alertmanager-config.sh
docker compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d

curl -fsS "http://localhost:9090/api/v1/query?query=aistudio_slo:api_success_ratio:5m"
./scripts/write-grafana-slo-dashboard.sh
```

---

## Related

| Doc | Topic |
|-----|-------|
| [monitoring/README.md](../monitoring/README.md) | Stack setup |
| [41-BILLING-ANOMALY-FORECAST-GUIDE.md](41-BILLING-ANOMALY-FORECAST-GUIDE.md) | Billing anomaly alerts |
