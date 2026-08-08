# Usage-based billing dashboards

**Version:** v0.2.32-beta  
**Scope:** Grafana dashboard and Prometheus metrics for seat metering and AI overage.

Complements seat metering ([28-BILLING-SEAT-USAGE-METERING-GUIDE.md](28-BILLING-SEAT-USAGE-METERING-GUIDE.md)) and Stripe metered sync ([32-STRIPE-METERED-PRICES-SYNC-GUIDE.md](32-STRIPE-METERED-PRICES-SYNC-GUIDE.md)).

---

## Prometheus metrics

| Metric | Type | Description |
|--------|------|-------------|
| `aistudio_billing_ai_actions_total{type="included"}` | Counter | Included-quota AI actions consumed |
| `aistudio_billing_ai_actions_total{type="overage"}` | Counter | Overage-metered AI actions |
| `aistudio_billing_ai_actions_today` | Gauge | Sum of included actions today (all orgs, UTC) |
| `aistudio_billing_ai_overage_today` | Gauge | Sum of overage actions today |
| `aistudio_billing_ai_overage_period` | Gauge | Overage actions MTD (UTC calendar month) |
| `aistudio_billing_ai_overage_forecast_period` | Gauge | Linear month-end overage action forecast |
| `aistudio_billing_estimated_overage_cents_period` | Gauge | MTD overage cost in cents (plan rates) |
| `aistudio_billing_estimated_overage_cents_forecast_period` | Gauge | Linear month-end overage cost forecast (cents) |
| `aistudio_billing_seats_active` | Gauge | Total memberships (seat proxy) |

Counters increment in `BillingService` on each consumed action. Gauges refresh from `ai_usage_daily` on scrape.

---

## Grafana dashboard

**Title:** AI Studio Billing Usage  
**UID:** `aistudio-billing`

Panels: today's included/overage actions, overage MTD, forecast panels, active seats, action rates, cumulative counters.

Anomaly alerts and cost forecasting: [41-BILLING-ANOMALY-FORECAST-GUIDE.md](41-BILLING-ANOMALY-FORECAST-GUIDE.md).

Provisioned automatically from `monitoring/grafana/dashboards/` when monitoring compose is up.

```bash
./scripts/write-grafana-billing-dashboard.sh   # validate JSON
docker compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d
```

Grafana → **AI Studio** folder (or default dashboards) → **AI Studio Billing Usage**.

---

## Smoke test

```bash
export IMAGE_TAG=v0.2.32-beta
export METRICS_SCRAPE_TOKEN="$(openssl rand -hex 32)"
./scripts/write-prometheus-token.sh
docker compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d

# Generate AI usage (chat messages on PRO plan org)
curl -fsS "http://localhost:9090/api/v1/query?query=aistudio_billing_ai_actions_total" \
  -H "Authorization: Bearer $(cat monitoring/prometheus-token.txt)"
```

---

## Related

| Doc | Topic |
|-----|-------|
| [28-BILLING-SEAT-USAGE-METERING-GUIDE.md](28-BILLING-SEAT-USAGE-METERING-GUIDE.md) | In-app metering |
| [22-BILLING-USAGE-INVOICES-GUIDE.md](22-BILLING-USAGE-INVOICES-GUIDE.md) | Usage history API |
| [monitoring/README.md](../monitoring/README.md) | Prometheus + Grafana stack |
