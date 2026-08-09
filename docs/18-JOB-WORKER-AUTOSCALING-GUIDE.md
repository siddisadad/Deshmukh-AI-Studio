# Background job worker autoscaling (Docker Compose / GHCR staging)

Operators scale **dedicated worker containers** (`prod,worker` profile) independently of the API. Workers claim jobs from Postgres using `FOR UPDATE SKIP LOCKED` — safe for multiple replicas.

**Metrics:** `aistudio.jobs.queue.depth` (see Grafana panel *Background job queue depth*).  
**Alert:** `BackgroundJobQueueDepthHigh` when `pending > 50` for 15m (`monitoring/alerts.yml`).

---

## 1. When to scale

| Signal | Action |
|---|---|
| `pending` rising, `running` ≈ worker count | Scale **up** (`WORKER_REPLICAS`) |
| `pending` sustained > 50 | Alert fires — scale up or investigate stuck jobs |
| `running` jobs stuck > `JOB_STALE_LOCK_SECONDS` (900s) | Stale reclaim runs; check worker logs / DB locks |
| `pending` = 0, `running` = 0 for extended period | Optional scale **down** to save resources |

Job types: `KNOWLEDGE_REINDEX`, `DOCUMENT_GENERATE` — both CPU/IO heavy; size workers for peak reindex batches.

---

## 2. Manual scale (staging GHCR)

```bash
export IMAGE_TAG=v0.2.84-beta
export WORKER_REPLICAS=3
./scripts/staging-ghcr-deploy.sh
```

Deploy script pulls images, runs `docker compose up -d --scale worker=${WORKER_REPLICAS}`, and verifies each worker health.

### Scale hint script

```bash
# With monitoring stack running (Prometheus on :9090)
./scripts/worker-scale-hint.sh

# Or read metrics directly from API actuator
export METRICS_SCRAPE_TOKEN=...
export METRICS_URL=http://localhost:8080/actuator/prometheus
./scripts/worker-scale-hint.sh
```

Tunable env:

| Variable | Default | Meaning |
|---|---|---|
| `WORKER_TARGET_PENDING_PER_REPLICA` | `10` | Target pending jobs per worker when suggesting scale |
| `WORKER_REPLICAS_MAX` | `6` | Cap for suggested replicas |
| `WORKER_REPLICAS` | `1` | Current deploy setting (for comparison) |

Apply the suggested value with `WORKER_REPLICAS=N ./scripts/staging-ghcr-deploy.sh`.

---

## 3. Manual scale (production Compose)

```bash
docker compose \
  -f docker-compose.yml \
  -f docker-compose.prod.yml \
  -f docker-compose.worker.yml \
  -f docker-compose.worker-prod.yml \
  up -d --scale worker=3
```

API must keep `AISTUDIO_JOBS_WORKER_ENABLED=false` (compose overlays set this). Only **worker** containers poll the queue.

Verify:

```bash
docker compose ps worker
docker compose exec worker curl -fsS http://127.0.0.1:8080/actuator/health
```

---

## 4. Worker identity (optional)

Each replica defaults to a random `AISTUDIO_JOBS_WORKER_ID` (`worker-…`). For fixed ids in logs/metrics, set per-container env in an override file:

```yaml
# docker-compose.worker-ids.yml (example — use deploy labels or separate services for fixed ids)
services:
  worker:
    environment:
      AISTUDIO_JOBS_WORKER_ID: worker-1
```

With `--scale worker=N`, Compose duplicates env — ids collide unless you use separate service definitions or orchestrator (Kubernetes) pod names.

---

## 5. Tuning knobs

| Env | Default | Notes |
|---|---|---|
| `JOB_POLL_INTERVAL_MS` | `2000` | Worker poll frequency |
| `JOB_BATCH_SIZE` | `5` | Max jobs claimed per poll |
| `JOB_STALE_LOCK_SECONDS` | `900` | Reclaim stuck `RUNNING` locks |
| `JOB_MAX_ATTEMPTS` | `3` | Failed reclaim → `FAILED` status |

---

## 6. Monitoring checklist

1. Grafana → **Background job queue depth** — `pending`, `running`, `failed`
2. Alertmanager → `BackgroundJobQueueDepthHigh`
3. Loki → worker container logs: `{service="worker"}` (if labeled) or container name filter
4. After scale: confirm `running` ≤ `WORKER_REPLICAS` and `pending` trends down

---

## 7. Kubernetes / cloud-native autoscaling

See [61-K8S-HPA-WORKER-AUTOSCALING-GUIDE.md](61-K8S-HPA-WORKER-AUTOSCALING-GUIDE.md) for:

- `GET /api/v1/ops/jobs/queue` — pending depth + `suggestedReplicas` for HPA/KEDA
- `deploy/kubernetes/` — worker Deployment, Prometheus HPA, KEDA ScaledObject
- `scripts/scheduled-worker-autoscale.sh` — cron-friendly Compose scale apply

Future: per-job-type worker pools.

---

## Related

- [13-DEPLOYMENT-GUIDE.md](13-DEPLOYMENT-GUIDE.md) §8 — worker compose overlays
- [monitoring/README.md](../monitoring/README.md) — Prometheus + Grafana
- [14-STAGING-DOGFOOD-GUIDE.md](14-STAGING-DOGFOOD-GUIDE.md) — staging sign-off

---

## Document control

| Version | Date | Notes |
|---|---|---|
| 1.0 | 2026-08-08 | Playbook + `worker-scale-hint.sh` |
