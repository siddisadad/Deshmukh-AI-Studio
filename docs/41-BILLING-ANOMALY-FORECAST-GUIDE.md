# Billing anomaly alerts and cost forecasting

**Version:** v0.2.36-beta  
**Scope:** Prometheus billing alert rules, linear month-end forecasts, and Grafana forecast panels.

Complements usage dashboards ([37-BILLING-USAGE-DASHBOARDS-GUIDE.md](37-BILLING-USAGE-DASHBOARDS-GUIDE.md)) and seat metering ([28-BILLING-SEAT-USAGE-METERING-GUIDE.md](28-BILLING-SEAT-USAGE-METERING-GUIDE.md)).

---

## New Prometheus metrics

| Metric | Type | Description |
|--------|------|-------------|
| `aistudio_billing_ai_overage_forecast_period` | Gauge | Linear forecast of overage actions at month-end |
| `aistudio_billing_estimated_overage_cents_period` | Gauge | MTD overage cost in cents (plan rates × overage counts) |
| `aistudio_billing_estimated_overage_cents_forecast_period` | Gauge | Linear forecast of overage cost at month-end |

Forecast formula: `MTD × days_in_month / day_of_month` (UTC calendar month).

---

## Alert rules

`monitoring/billing-alerts.yml` (loaded by Prometheus alongside `alerts.yml`):

| Alert | Trigger |
|-------|---------|
| `BillingAiOverageRateHigh` | Overage action rate > 0.25/s for 10m |
| `BillingAiOverageMtdHigh` | MTD overage actions > 500 for 1h |
| `BillingAiOverageForecastHigh` | Month-end overage forecast > 2000 for 30m |
| `BillingEstimatedOverageCentsForecastHigh` | Forecast cost > $100 for 30m |
| `BillingAiOverageAnomalySpike` | Today's overage > 3× 7-day average (min baseline 10) |

Alerts route through Alertmanager ([39-ALERTMANAGER-ONCALL-GUIDE.md](39-ALERTMANAGER-ONCALL-GUIDE.md)).

Tune thresholds in `billing-alerts.yml` for your platform scale.

---

## Grafana

The **AI Studio Billing Usage** dashboard (`uid: aistudio-billing`) adds:

- Overage month-end forecast (actions)
- Estimated overage cost MTD and forecast (USD)
- MTD vs forecast timeseries

```bash
./scripts/write-grafana-billing-dashboard.sh
docker compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d
```

---

## Smoke test

```bash
export IMAGE_TAG=v0.2.36-beta
export METRICS_SCRAPE_TOKEN="$(openssl rand -hex 32)"
./scripts/write-prometheus-token.sh
./scripts/write-alertmanager-config.sh
docker compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d

curl -fsS "http://localhost:9090/api/v1/query?query=aistudio_billing_ai_overage_forecast_period" \
  -H "Authorization: Bearer $(cat monitoring/.prometheus-token)"
```

Generate PRO overage usage via chat, then confirm gauges and Grafana forecast panels update.

---

## Related

| Doc | Topic |
|-----|-------|
| [37-BILLING-USAGE-DASHBOARDS-GUIDE.md](37-BILLING-USAGE-DASHBOARDS-GUIDE.md) | Base billing metrics |
| [39-ALERTMANAGER-ONCALL-GUIDE.md](39-ALERTMANAGER-ONCALL-GUIDE.md) | On-call routing |
