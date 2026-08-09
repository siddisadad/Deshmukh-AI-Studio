# Cost-aware AI routing and provider quotas

**Version:** v0.2.40-beta  
**Scope:** Prefer cheaper providers in multi-provider chains and enforce per-provider daily request quotas.

Complements adaptive latency routing ([40-ADAPTIVE-AI-ROUTING-GUIDE.md](40-ADAPTIVE-AI-ROUTING-GUIDE.md)) and multi-provider failover ([34-MULTI-PROVIDER-ROUTING-GUIDE.md](34-MULTI-PROVIDER-ROUTING-GUIDE.md)).

---

## Overview

| Component | Role |
|-----------|------|
| `AiProviderCostTierRegistry` | Relative cost tiers (lower = cheaper); defaults mock=1, openai=5, anthropic=8 |
| `AiProviderQuotaTracker` | In-process UTC daily quotas per provider |
| `RoutingAiProvider` | Skips quota-exhausted providers; reorders chain by cost tier when enabled |

When both cost-aware and adaptive routing are enabled, ordering is: **cost tier → latency → chain order**.

---

## Configuration

```bash
AI_PROVIDER=routing
AI_PROVIDER_CHAIN=anthropic,openai,mock

AI_COST_AWARE_ROUTING_ENABLED=true
AI_PROVIDER_COST_TIERS=mock:1,openai:5,anthropic:8

AI_PROVIDER_QUOTAS=openai:500,anthropic:200,mock:10000
```

Quota format: `provider:limit` pairs separated by commas (also accepts `=`). Unlisted providers have no quota. Limits reset at UTC midnight.

Works with primary + fallbacks (`AI_PROVIDER_FALLBACKS`) as well as `routing` mode.

---

## Health API

```bash
GET /api/v1/assistants/provider-health
```

New fields per provider:

| Field | Meaning |
|-------|---------|
| `costTier` | Relative cost tier (lower = cheaper) |
| `dailyQuota` | Configured daily limit (null = unlimited) |
| `quotaUsedToday` | Requests counted today (UTC) |
| `quotaRemaining` | Remaining quota (null when unlimited) |
| `quotaExhausted` | `true` when daily limit reached |

---

## Routing behavior

1. Skip providers with open circuits (existing)
2. Skip providers with exhausted daily quotas
3. Sort remaining providers by cost tier (when cost-aware enabled)
4. Then by rolling average latency (when adaptive enabled)
5. Failover to next provider on errors (existing)

---

## Smoke test

```bash
export IMAGE_TAG=v0.2.40-beta
export AI_PROVIDER=routing
export AI_PROVIDER_CHAIN=anthropic,openai,mock
export AI_COST_AWARE_ROUTING_ENABLED=true
export AI_PROVIDER_QUOTAS=mock:5

curl -fsS "$API/api/v1/assistants/provider-health" -H "Authorization: Bearer $TOKEN" \
  | jq '.providers[] | {id, costTier, dailyQuota, quotaUsedToday}'
```

Send chat messages until `mock` quota exhausts; routing should failover to `openai` before `anthropic` when cost-aware is on.

---

## Related

| Doc | Topic |
|-----|-------|
| [40-ADAPTIVE-AI-ROUTING-GUIDE.md](40-ADAPTIVE-AI-ROUTING-GUIDE.md) | Latency-based ordering |
| [34-MULTI-PROVIDER-ROUTING-GUIDE.md](34-MULTI-PROVIDER-ROUTING-GUIDE.md) | Provider chains |
| [36-AI-PROVIDER-HEALTH-CIRCUIT-GUIDE.md](36-AI-PROVIDER-HEALTH-CIRCUIT-GUIDE.md) | Circuit breaker + probes |
