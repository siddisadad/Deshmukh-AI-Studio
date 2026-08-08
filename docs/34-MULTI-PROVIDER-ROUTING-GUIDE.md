# Multi-provider AI routing and failover

**Version:** v0.2.29-beta  
**Scope:** Ordered provider chains with automatic failover on `generate()` and `stream()`.

Complements provider-native streaming ([29-PROVIDER-NATIVE-STREAMING-GUIDE.md](29-PROVIDER-NATIVE-STREAMING-GUIDE.md)) and AI architecture ([07-AI-ARCHITECTURE.md](07-AI-ARCHITECTURE.md)).

---

## Overview

| Component | Role |
|-----------|------|
| `AiProviderRegistry` | Registers `mock` always; OpenAI/Anthropic when API keys are set |
| `RoutingAiProvider` | Tries providers in order until one succeeds |
| `AiProviderConfiguration` | Wires `AiProviderPort` from env (`routing` mode or per-provider fallbacks) |

Chat, document generation, and requirement AI all use the same `AiProviderPort` bean. The SSE `done` event and persisted messages report `provider` from the adapter that actually succeeded.

---

## Environment modes

### Explicit routing chain

```bash
AI_PROVIDER=routing
AI_PROVIDER_CHAIN=openai,anthropic,mock
OPENAI_API_KEY=sk-...
ANTHROPIC_API_KEY=sk-ant-...
```

`AI_PROVIDER=routing` requires a non-empty `AI_PROVIDER_CHAIN`. Providers without API keys are skipped (logged as warnings).

### Primary provider + fallbacks

```bash
AI_PROVIDER=openai
AI_PROVIDER_FALLBACKS=anthropic,mock
OPENAI_API_KEY=sk-...
ANTHROPIC_API_KEY=sk-ant-...
```

When fallbacks are set, the effective chain is `openai → anthropic → mock`. With a single provider and no fallbacks, behavior matches pre-v0.2.29 (direct adapter).

### Default (dev / CI)

```bash
AI_PROVIDER=mock
```

No external API calls.

---

## Failover behavior

1. For each provider ID in the chain, skip if not registered (missing API key).
2. Call `generate()` or `stream()` on the adapter.
3. On `AiProviderException`, log a warning and try the next provider.
4. If all providers fail, throw the last `AiProviderException`.
5. `providerId()` returns the adapter that succeeded for the current request (thread-local).

Streaming failover retries the full provider stream — partial deltas from a failed provider are not forwarded to the client.

---

## Smoke test

```bash
export IMAGE_TAG=v0.2.29-beta
export AI_PROVIDER=routing
export AI_PROVIDER_CHAIN=openai,mock
export OPENAI_API_KEY=sk-invalid-on-purpose
# mock should succeed after openai fails

./scripts/staging-ghcr-deploy.sh
```

Send a chat message. Response footer / `done` event should show `provider: mock`.

---

## Related

| Doc | Topic |
|-----|-------|
| [29-PROVIDER-NATIVE-STREAMING-GUIDE.md](29-PROVIDER-NATIVE-STREAMING-GUIDE.md) | OpenAI/Anthropic SSE |
| [23-STREAMING-TOKEN-UX-GUIDE.md](23-STREAMING-TOKEN-UX-GUIDE.md) | Client SSE contract |
| [07-AI-ARCHITECTURE.md](07-AI-ARCHITECTURE.md) | Provider ports and context |
