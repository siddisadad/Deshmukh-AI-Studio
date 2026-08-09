# Multi-window burn-rate policies and per-tenant SLO

**Version:** v0.2.50-beta  
**Scope:** Per-organization SLO targets, HTTP metrics tagged by tenant, multi-window burn-rate alerts, and Grafana tenant SLO dashboard.

Extends platform SLOs ([42-SLO-ERROR-BUDGET-GUIDE.md](42-SLO-ERROR-BUDGET-GUIDE.md)).

---

## Per-tenant SLO settings

Columns on `organizations`:

| Column | Default | Description |
|--------|---------|-------------|
| `slo_availability_target` | 0.995 | Availability SLO (non-5xx ratio) |
| `slo_latency_target` | 0.95 | Latency SLO (fraction under threshold) |
| `slo_latency_threshold_seconds` | 2 | Latency threshold in seconds (exported to Prometheus) |

API:

| Endpoint | Role | Action |
|----------|------|--------|
| `GET /organizations/{id}/slo` | member | Read org SLO targets |
| `PUT /organizations/{id}/slo` | OWNER | Update targets |

Settings UI: `/settings/slo`

---

## Tenant HTTP metrics

`OrgSloRequestFilter` resolves `organization_id` from:

- `/organizations/{orgId}/…` paths
- `/projects/{projectId}/…` paths (project → org lookup)

`OrgSloMetricsConfiguration` adds `organization_id` as a low-cardinality Micrometer observation tag on `http.server.requests`.

Exported Prometheus gauges (refreshed every 60s):

- `aistudio_slo_org_availability_target{organization_id}`
- `aistudio_slo_org_latency_target{organization_id}`
- `aistudio_slo_org_latency_threshold_seconds{organization_id}`

---

## Recording rules

`monitoring/slo-recording-rules.yml` adds:

| Metric | Description |
|--------|-------------|
| `aistudio_slo:org_api_success_ratio:5m` | Per-tenant availability SLI |
| `aistudio_slo:org_api_availability_error_budget_remaining:30d` | Per-tenant availability budget |
| `aistudio_slo:org_api_error_burn_rate:5m/1h/6h` | Per-tenant error burn rates |
| `aistudio_slo:api_error_burn_rate:5m/1h/6h` | Platform burn rates |

Tenant latency SLI uses the 2s histogram bucket; custom `latencyThresholdSeconds` is exported for targets and UI.

---

## Multi-window burn-rate alerts

`monitoring/slo-alerts.yml`:

| Alert | Windows | Factor | Severity |
|-------|---------|--------|----------|
| `ApiSloAvailabilityBurnRatePage` | 5m + 1h | 14.4× | critical |
| `ApiSloAvailabilityBurnRateTicket` | 1h + 6h | 6× | warning |
| `OrgSloAvailabilityBurnRatePage` | 5m + 1h per tenant | 14.4× | critical |
| `OrgSloAvailabilityBurnRateTicket` | 1h + 6h per tenant | 6× | warning |
| `OrgSloAvailabilityBudgetLow` | 30d budget < 25% | — | warning |
| `OrgSloLatencyBudgetLow` | 30d budget < 25% | — | warning |

---

## Grafana

| Dashboard | UID | Description |
|-----------|-----|-------------|
| AI Studio SLO | `aistudio-slo` | Platform SLIs + multi-window burn panel |
| AI Studio SLO (tenant) | `aistudio-slo-tenant` | Per-org SLI, budgets, burn rates |

```bash
./scripts/write-grafana-slo-dashboard.sh
docker compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d
```

---

## Smoke test

```bash
export IMAGE_TAG=v0.2.50-beta
export METRICS_SCRAPE_TOKEN="$(openssl rand -hex 32)"
./scripts/write-prometheus-token.sh

# PUT /organizations/{id}/slo with custom targets
# Hit org-scoped API → scrape /actuator/prometheus → organization_id label present
curl -fsS "http://localhost:9090/api/v1/query?query=aistudio_slo_org_availability_target"
```

---

## Related

| Doc | Topic |
|-----|-------|
| [42-SLO-ERROR-BUDGET-GUIDE.md](42-SLO-ERROR-BUDGET-GUIDE.md) | Platform SLO baseline |
| [39-ALERTMANAGER-ONCALL-GUIDE.md](39-ALERTMANAGER-ONCALL-GUIDE.md) | Alert routing |
