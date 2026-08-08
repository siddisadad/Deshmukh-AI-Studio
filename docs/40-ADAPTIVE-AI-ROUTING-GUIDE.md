# Adaptive AI routing (latency-based selection)

**Version:** v0.2.35-beta  
**Scope:** Reorder multi-provider chains by recent latency before each `generate()` / `stream()` call.

Complements multi-provider failover ([34-MULTI-PROVIDER-ROUTING-GUIDE.md](34-MULTI-PROVIDER-ROUTING-GUIDE.md)) and circuit breaking ([36-AI-PROVIDER-HEALTH-CIRCUIT-GUIDE.md](36-AI-PROVIDER-HEALTH-CIRCUIT-GUIDE.md)).

---

## Overview

| Component | Role |
|-----------|------|
| `AiProviderLatencyTracker` | Rolling latency samples per provider (default window 50) |
| `RoutingAiProvider` | When enabled, sorts chain by average latency (fastest first) before each request |
| `GET /assistants/provider-health` | Reports `averageLatencyMs` and `latencySampleCount` per provider |

Failover behavior is unchanged: if the fastest provider fails or its circuit is open, routing continues to the next provider in the adaptive order.

---

## Configuration

```bash
AI_PROVIDER=routing
AI_PROVIDER_CHAIN=openai,anthropic,mock
AI_ADAPTIVE_ROUTING_ENABLED=true
AI_ADAPTIVE_ROUTING_SAMPLE_SIZE=50   # rolling window per provider
```

Works with primary + fallbacks mode as well (`AI_PROVIDER_FALLBACKS`).

Cold start (no samples yet): chain order from env is preserved. After successful calls, latency samples bias routing toward faster providers.

---

## Health API

```bash
GET /api/v1/assistants/provider-health
```

Each provider entry includes:

| Field | Meaning |
|-------|---------|
| `averageLatencyMs` | Rolling mean latency (null when no samples) |
| `latencySampleCount` | Samples in the current window |

---

## Smoke test

```bash
export IMAGE_TAG=v0.2.35-beta
export AI_PROVIDER=routing
export AI_PROVIDER_CHAIN=openai,anthropic,mock
export AI_ADAPTIVE_ROUTING_ENABLED=true
export OPENAI_API_KEY=sk-...
export ANTHROPIC_API_KEY=sk-ant-...

./scripts/staging-ghcr-deploy.sh
```

Send several chat messages, then:

```bash
curl -fsS "$API/api/v1/assistants/provider-health" -H "Authorization: Bearer $TOKEN"
```

Confirm `averageLatencyMs` populates for providers that handled requests.

---

## Related

| Doc | Topic |
|-----|-------|
| [34-MULTI-PROVIDER-ROUTING-GUIDE.md](34-MULTI-PROVIDER-ROUTING-GUIDE.md) | Provider chains and failover |
| [36-AI-PROVIDER-HEALTH-CIRCUIT-GUIDE.md](36-AI-PROVIDER-HEALTH-CIRCUIT-GUIDE.md) | Circuit breaker + probes |
