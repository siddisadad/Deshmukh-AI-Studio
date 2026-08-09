# Kubernetes HPA and cloud-native worker autoscaling

**Version:** v0.2.56-beta  
**Scope:** Job queue metrics API for external autoscalers, K8s HPA/KEDA manifests, and scheduled Compose autoscale cron.

Complements the Docker Compose playbook ([18-JOB-WORKER-AUTOSCALING-GUIDE.md](18-JOB-WORKER-AUTOSCALING-GUIDE.md)).

---

## Queue metrics API

Operator endpoint (same token as billing/SIEM ops):

| Method | Path | Auth |
|--------|------|------|
| `GET` | `/api/v1/ops/jobs/queue` | `BILLING_USAGE_SYNC_TOKEN` |

Example response:

```json
{
  "pending": 25,
  "running": 2,
  "failed": 0,
  "suggestedReplicas": 3,
  "targetPendingPerReplica": 10,
  "maxReplicas": 6
}
```

Env tuning (matches `worker-scale-hint.sh`):

| Variable | Default |
|----------|---------|
| `WORKER_TARGET_PENDING_PER_REPLICA` | `10` |
| `WORKER_REPLICAS_MAX` | `6` |

---

## Kubernetes manifests

Under `deploy/kubernetes/`:

| File | Purpose |
|------|---------|
| `worker-deployment.yaml` | Worker Deployment (`prod,worker` profile) |
| `worker-hpa-prometheus.yaml` | HPA on external metric `aistudio_jobs_queue_pending` |
| `worker-keda-scaledobject.yaml` | KEDA `metrics-api` trigger on `pending` field |

Prometheus recording rule: `monitoring/job-queue-autoscale-rules.yml`

### Prometheus adapter HPA

1. Install [prometheus-adapter](https://github.com/kubernetes-sigs/prometheus-adapter) with rule mapping `aistudio_jobs_queue_depth{status="pending"}` → external metric `aistudio_jobs_queue_pending`
2. `kubectl apply -f deploy/kubernetes/worker-deployment.yaml`
3. `kubectl apply -f deploy/kubernetes/worker-hpa-prometheus.yaml`

### KEDA

1. Install KEDA
2. Set `BILLING_USAGE_SYNC_TOKEN` for the scaler trigger (or restrict API network policy)
3. `kubectl apply -f deploy/kubernetes/worker-keda-scaledobject.yaml`

---

## Scheduled Compose autoscale

```bash
export BILLING_USAGE_SYNC_TOKEN=...
./scripts/scheduled-worker-autoscale.sh

# Apply scale on staging GHCR host:
WORKER_AUTOSCALE_APPLY=1 ./scripts/scheduled-worker-autoscale.sh
```

---

## Related

| Doc | Topic |
|-----|-------|
| [18-JOB-WORKER-AUTOSCALING-GUIDE.md](18-JOB-WORKER-AUTOSCALING-GUIDE.md) | Compose manual scale + `worker-scale-hint.sh` |
| [13-DEPLOYMENT-GUIDE.md](13-DEPLOYMENT-GUIDE.md) | Worker compose overlays |
