# Alertmanager on-call and cross-cluster routing

**Version:** v0.2.34-beta  
**Scope:** Generate Alertmanager receivers (Slack, PagerDuty, webhook) and route alerts by `severity` and `cluster` across regional stacks.

Complements federated observability ([33-FEDERATED-GRAFANA-LOKI-RULER-GUIDE.md](33-FEDERATED-GRAFANA-LOKI-RULER-GUIDE.md)) and staging ops playbooks ([14-STAGING-DOGFOOD-GUIDE.md](14-STAGING-DOGFOOD-GUIDE.md)).

---

## Scripts

| Script | Purpose |
|--------|---------|
| `write-alertmanager-config.sh` | Generate `monitoring/alertmanager.generated.yml` from env |
| `sync-alertmanager-regions.sh` | Reload regional Alertmanagers after config is deployed |

---

## Configuration

```bash
# On-call integrations (optional — omit for UI-only dev)
ALERTMANAGER_SLACK_WEBHOOK_URL=https://hooks.slack.com/services/...
ALERTMANAGER_SLACK_CHANNEL=#alerts          # optional
ALERTMANAGER_PAGERDUTY_ROUTING_KEY=...      # Events API v2 routing key
ALERTMANAGER_WEBHOOK_URL=https://oncall.example/hooks/alerts
ALERTMANAGER_CRITICAL_ONLY_PAGERDUTY=1      # default 1 — page only on severity=critical

# Cluster label on Prometheus metric alerts
PROMETHEUS_CLUSTER_NAME=us-east

# Regional Alertmanager reload (after shared config mount)
ALERTMANAGER_QUERY_REGIONS=us-east=http://am-us:9093,eu-west=http://am-eu:9093
```

Loki log alerts carry `cluster: primary` in `monitoring/loki-alerts.yml`. Override per region when fanning out ruler rules ([33-FEDERATED-GRAFANA-LOKI-RULER-GUIDE.md](33-FEDERATED-GRAFANA-LOKI-RULER-GUIDE.md)).

---

## Routing

Generated routes (when integrations are set):

1. **critical** → PagerDuty (when `ALERTMANAGER_PAGERDUTY_ROUTING_KEY` set and `ALERTMANAGER_CRITICAL_ONLY_PAGERDUTY=1`)
2. **critical** → Slack critical receiver (duplicate title prefix)
3. **warning|critical** → Slack + generic webhook
4. **cluster** label → `cluster-default` grouping route (keeps per-region alert batches separate)

Default receiver remains UI-only for dev when no integrations are configured.

---

## Local / staging

```bash
export METRICS_SCRAPE_TOKEN="$(openssl rand -hex 32)"
./scripts/write-prometheus-token.sh
./scripts/write-alertmanager-config.sh
docker compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d
```

Alertmanager mounts `alertmanager.generated.yml` with `--web.enable-lifecycle` for `/-/reload`.

---

## Multi-region reload

Deploy the same generated config to each regional Alertmanager (shared volume, GitOps, or image bake). Then:

```bash
./scripts/write-alertmanager-config.sh
export ALERTMANAGER_QUERY_REGIONS=us-east=http://am-us:9093,eu-west=http://am-eu:9093
./scripts/sync-alertmanager-regions.sh
```

Or single host:

```bash
ALERTMANAGER_URL=http://localhost:9093 ./scripts/sync-alertmanager-regions.sh
```

The sync script verifies `/api/v2/status` and POSTs `/-/reload`. When lifecycle is disabled it warns and still reports reachability.

---

## Smoke test

```bash
./scripts/write-alertmanager-config.sh
python3 -c "import yaml; yaml.safe_load(open('monitoring/alertmanager.generated.yml'))"
bash -n scripts/write-alertmanager-config.sh
bash -n scripts/sync-alertmanager-regions.sh
```

With a running Alertmanager:

```bash
ALERTMANAGER_URL=http://localhost:9093 ./scripts/sync-alertmanager-regions.sh
```

---

## Related

| Doc | Topic |
|-----|-------|
| [33-FEDERATED-GRAFANA-LOKI-RULER-GUIDE.md](33-FEDERATED-GRAFANA-LOKI-RULER-GUIDE.md) | Regional Loki/Prometheus + ruler fan-out |
| [monitoring/README.md](../monitoring/README.md) | Full monitoring stack setup |
| [18-JOB-WORKER-AUTOSCALING-GUIDE.md](18-JOB-WORKER-AUTOSCALING-GUIDE.md) | `BackgroundJobQueueDepthHigh` alert |
