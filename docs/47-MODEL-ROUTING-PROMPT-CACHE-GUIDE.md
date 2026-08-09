# Model-specific routing and prompt cache

**Version:** v0.2.42-beta  
**Scope:** Per-assistant provider/model selection and TTL caching of assembled chat prompts.

Complements org routing policies ([46-TOKEN-BUDGET-ORG-ROUTING-GUIDE.md](46-TOKEN-BUDGET-ORG-ROUTING-GUIDE.md)) and cost-aware routing ([45-COST-AWARE-AI-ROUTING-GUIDE.md](45-COST-AWARE-AI-ROUTING-GUIDE.md)).

---

## Model-specific routing

Map assistant roles to `provider:model` pairs:

```bash
AI_ASSISTANT_MODEL_MAP=DEVELOPER=openai:gpt-4o-mini,QA_ENGINEER=anthropic:claude-sonnet-4-20250514
```

Org override via `modelMap` on `PUT /organizations/{id}/ai-policy` (stored in `organization_subscriptions.ai_model_map`).

At chat time:

1. Resolve route for `assistantRole` (org map wins over platform map)
2. Set `OrgAiRoutingContext` model route
3. `RoutingAiProvider` puts the mapped provider first in the failover chain
4. Request metadata includes `model` — OpenAI/Anthropic adapters use it instead of the default env model

---

## Prompt cache

```bash
AI_PROMPT_CACHE_ENABLED=true
AI_PROMPT_CACHE_TTL_SECONDS=300
AI_PROMPT_CACHE_MAX_ENTRIES=500
```

When enabled, caches:

- Project context strings (`ContextBuilder` output per project + user message hash)
- Full system prompt assembly (assistant template + shared context)

Reduces repeated DB/RAG work for active threads. Cache is in-process per API replica (not shared across pods).

---

## API (`ai-policy`)

`GET/PUT /api/v1/organizations/{orgId}/ai-policy` includes `modelMap`:

```json
{
  "providerChain": "mock,openai",
  "dailyTokenBudget": 500000,
  "modelMap": "DEVELOPER=openai:gpt-4o-mini",
  "effectiveDailyTokenBudget": 500000,
  "tokensUsedToday": 1200,
  "tokenBudgetRemaining": 498800
}
```

---

## Smoke test

```bash
export IMAGE_TAG=v0.2.42-beta
export AI_ASSISTANT_MODEL_MAP=DEVELOPER=mock:mock-1
export AI_PROMPT_CACHE_ENABLED=true

curl -fsS -X PUT "$API/api/v1/organizations/$ORG_ID/ai-policy" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"modelMap":"QA_ENGINEER=mock:mock-1"}'
```

Send chat as DEVELOPER — response `model` should reflect routed model when provider returns it.

---

## Related

| Doc | Topic |
|-----|-------|
| [46-TOKEN-BUDGET-ORG-ROUTING-GUIDE.md](46-TOKEN-BUDGET-ORG-ROUTING-GUIDE.md) | Org provider chain + token budgets |
| [34-MULTI-PROVIDER-ROUTING-GUIDE.md](34-MULTI-PROVIDER-ROUTING-GUIDE.md) | Platform provider chains |
| [23-STREAMING-TOKEN-UX-GUIDE.md](23-STREAMING-TOKEN-UX-GUIDE.md) | SSE usage metadata |
