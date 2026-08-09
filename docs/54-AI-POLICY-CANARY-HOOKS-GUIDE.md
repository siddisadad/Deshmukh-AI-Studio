# AI policy canary promotion / rollback hooks

**Version:** v0.2.49-beta  
**Scope:** Automated canary promote/abort based on chat outcome metrics, optional webhook notifications, and scheduled evaluation.

Extends canary rollout ([53-AI-POLICY-CANARY-ROLLOUT-GUIDE.md](53-AI-POLICY-CANARY-ROLLOUT-GUIDE.md)).

---

## Hook settings

Stored on `organization_subscriptions`:

| Column | Default | Description |
|--------|---------|-------------|
| `ai_canary_auto_promote_enabled` | false | Auto-promote when thresholds pass |
| `ai_canary_auto_abort_enabled` | false | Auto-abort when error rate exceeds threshold |
| `ai_canary_hook_webhook_url` | null | Optional POST webhook on promote/abort |
| `ai_canary_min_samples` | 20 | Min canary outcomes before auto-abort |
| `ai_canary_abort_error_rate_percent` | 25 | Abort when canary error rate ≥ this |
| `ai_canary_promote_min_samples` | 50 | Min canary outcomes before auto-promote |
| `ai_canary_promote_max_error_rate_percent` | 5 | Promote when canary error rate ≤ this |

`GET /ai-policy` returns hook fields plus `canaryMetrics` counters.

---

## Outcome metrics

Table `org_ai_canary_outcomes` tracks per-org chat success/failure counts split by canary vs stable routing:

- `ConversationService` records an outcome after each chat request (sync or SSE stream)
- Routing uses `OrgAiRoutingContext.canaryRoute()` set during provider chain resolution

Counters reset after an automated or manual promote/abort.

---

## Evaluation

`OrgAiCanaryHookService.evaluateAndAct`:

1. Skip when no active canary (`canaryProviderChain` + `canaryPercent > 0`)
2. Auto-abort when enabled, samples ≥ `minSamples`, and canary error rate ≥ abort threshold
3. Auto-promote when enabled, samples ≥ `promoteMinSamples`, and canary error rate ≤ promote threshold
4. Fire webhook (`event: org_ai_canary_hook`) when configured
5. Reset outcome counters after promote/abort

Scheduled evaluation (optional):

```bash
AI_CANARY_HOOK_EVAL_ENABLED=true
AI_CANARY_HOOK_EVAL_INTERVAL_MS=300000
```

`OrgAiCanaryHookScheduler` scans orgs with active canary and at least one auto hook enabled.

---

## API

| Endpoint | Role | Action |
|----------|------|--------|
| `PUT /ai-policy/canary/hooks` | OWNER | Save hook toggles and thresholds |
| `POST /ai-policy/canary/evaluate` | OWNER | Evaluate metrics and apply hooks immediately |

Evaluate response: `{ action, reason, metrics }` where `action` is `NONE`, `PROMOTED`, or `ABORTED`.

---

## Settings UI

`/settings/ai-routing` **Canary automation hooks** section:

- Auto-promote / auto-abort toggles
- Webhook URL and threshold fields
- Live canary/stable outcome counters
- **Save hook settings** and **Evaluate now**

---

## Smoke test

```bash
export IMAGE_TAG=v0.2.49-beta

# Start canary → configure auto-abort with low minSamples
# Seed failures via chat or direct DB on org_ai_canary_outcomes
# POST canary/evaluate → action ABORTED, canary cleared
```

---

## Related

| Doc | Topic |
|-----|-------|
| [53-AI-POLICY-CANARY-ROLLOUT-GUIDE.md](53-AI-POLICY-CANARY-ROLLOUT-GUIDE.md) | Canary rollout |
| [36-AI-PROVIDER-HEALTH-CIRCUIT-GUIDE.md](36-AI-PROVIDER-HEALTH-CIRCUIT-GUIDE.md) | Provider health |
