# AI provider health probes and circuit breaking

**Version:** v0.2.31-beta  
**Scope:** Per-provider health probes, circuit breaker on routing failover, and staging probe integration.

Complements multi-provider routing ([34-MULTI-PROVIDER-ROUTING-GUIDE.md](34-MULTI-PROVIDER-ROUTING-GUIDE.md)).

---

## Overview

| Component | Role |
|-----------|------|
| `AiProviderCircuitBreaker` | Opens circuit after N consecutive failures; skips provider until cooldown |
| `AiProviderHealthService` | Reports circuit state + optional live probes per registered provider |
| `RoutingAiProvider` | Records success/failure; skips providers with open circuits |
| Provider `probeHealth()` | OpenAI `/v1/models`; Anthropic minimal message; mock always up |

---

## Environment

```bash
AI_CIRCUIT_BREAKER_ENABLED=true
AI_CIRCUIT_BREAKER_FAILURE_THRESHOLD=3
AI_CIRCUIT_BREAKER_OPEN_SECONDS=60
```

Default: enabled with threshold **3** and **60s** open window.

---

## API

```http
GET /api/v1/assistants/provider-health
GET /api/v1/assistants/provider-health?probe=true
```

Response (per configured provider):

| Field | Description |
|-------|-------------|
| `id` | `mock`, `openai`, `anthropic` |
| `configured` | Provider registered (API key present for external providers) |
| `circuitState` | `closed`, `open`, or `half_open` |
| `failureCount` | Consecutive failures in current window |
| `circuitOpenUntil` | When open circuit resets (if open) |
| `probeStatus` | `up` / `down` when `probe=true` |
| `probedAt` | Timestamp of last probe |

Requires authentication (same as `/assistants`).

---

## Circuit behavior

1. Each `AiProviderException` from a provider in the routing chain increments that provider's failure count.
2. At threshold, circuit **opens** — routing skips that provider without calling the API.
3. After `open-seconds`, circuit enters **half_open** and allows a trial request.
4. Success clears failure state; failure re-opens the circuit.

Single-provider mode (no routing chain) does not wrap with circuit breaker on the bean itself — circuit tracking still applies when using `RoutingAiProvider` (fallbacks or `routing` mode).

---

## Staging probes

When `AI_PROVIDER` is not `mock`, `scripts/staging-provider-probes.sh` calls:

```http
GET /assistants/provider-health?probe=true
```

and requires every configured provider `probeStatus: up`.

---

## Smoke test

```bash
export IMAGE_TAG=v0.2.31-beta
export AI_PROVIDER=routing
export AI_PROVIDER_CHAIN=openai,mock
# Provoke openai failures (invalid key) until circuit opens, then chat should use mock

curl -fsS "$API/api/v1/assistants/provider-health" -H "Authorization: Bearer $TOKEN"
```

---

## Related

| Doc | Topic |
|-----|-------|
| [34-MULTI-PROVIDER-ROUTING-GUIDE.md](34-MULTI-PROVIDER-ROUTING-GUIDE.md) | Provider chains |
| [24-STAGING-PROVIDER-PROBES-GUIDE.md](24-STAGING-PROVIDER-PROBES-GUIDE.md) | Staging probe script |
| [29-PROVIDER-NATIVE-STREAMING-GUIDE.md](29-PROVIDER-NATIVE-STREAMING-GUIDE.md) | Provider adapters |
