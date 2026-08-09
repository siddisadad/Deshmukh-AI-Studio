# Provider-native prompt cache and cross-region routing

**Version:** v0.2.43-beta  
**Scope:** OpenAI/Anthropic ephemeral prompt caching and deploy-region provider chains with regional endpoints.

Complements in-process prompt cache ([47-MODEL-ROUTING-PROMPT-CACHE-GUIDE.md](47-MODEL-ROUTING-PROMPT-CACHE-GUIDE.md)) and multi-provider routing ([34-MULTI-PROVIDER-ROUTING-GUIDE.md](34-MULTI-PROVIDER-ROUTING-GUIDE.md)).

---

## Provider-native prompt cache

When enabled, chat requests include provider `cache_control` breakpoints on the assembled system prompt:

```bash
AI_PROVIDER_NATIVE_PROMPT_CACHE_ENABLED=true
```

`ConversationService` sets metadata `nativePromptCache=true`. Adapters translate that to:

- **OpenAI** — system message `content` array with `cache_control: {type: ephemeral}`
- **Anthropic** — `system` array blocks with `cache_control: {type: ephemeral}`

Works alongside `AI_PROMPT_CACHE_ENABLED` (in-process assembly cache). Native caching reduces billed tokens on repeated long system prompts at the provider.

---

## Cross-region routing

Route each deployment to a regional provider chain and optional alternate API base URLs:

```bash
AI_CROSS_REGION_ROUTING_ENABLED=true
AISTUDIO_DEPLOY_REGION=eu-west
AI_PROVIDER_REGION_CHAINS=us-east=openai,anthropic,mock;eu-west=openai-eu,anthropic-eu,mock
AI_PROVIDER_ENDPOINT_MAP=openai-eu=https://eu.api.openai.com,anthropic-eu=https://api.eu.anthropic.com
OPENAI_API_KEY=sk-...
ANTHROPIC_API_KEY=sk-ant-...
```

| Component | Role |
|-----------|------|
| `AiProviderCrossRegionRegistry` | Parses region chains and endpoint map |
| `AiProviderRegistry` | Registers `openai-eu` / `anthropic-eu` aliases sharing platform API keys |
| `RoutingAiProvider` | Resolves platform chain for `AISTUDIO_DEPLOY_REGION` before org/model routing |

Org `ai_provider_chain` overrides still win when set. Cost tiers inherit from base provider (`openai-eu` → `openai` tier).

---

## Smoke test

```bash
export IMAGE_TAG=v0.2.43-beta
export AI_PROVIDER=routing
export AI_PROVIDER_CHAIN=openai,anthropic,mock
export AI_PROVIDER_NATIVE_PROMPT_CACHE_ENABLED=true
export AI_CROSS_REGION_ROUTING_ENABLED=true
export AISTUDIO_DEPLOY_REGION=eu-west
export AI_PROVIDER_REGION_CHAINS=eu-west=mock
export OPENAI_API_KEY=sk-...
```

Send chat — provider should be `mock` when eu-west chain is `mock` only. Enable native cache and inspect outbound OpenAI body in staging logs (cache_control present).

---

## Related

| Doc | Topic |
|-----|-------|
| [47-MODEL-ROUTING-PROMPT-CACHE-GUIDE.md](47-MODEL-ROUTING-PROMPT-CACHE-GUIDE.md) | In-process prompt cache |
| [34-MULTI-PROVIDER-ROUTING-GUIDE.md](34-MULTI-PROVIDER-ROUTING-GUIDE.md) | Provider chains and failover |
| [40-ADAPTIVE-AI-ROUTING-GUIDE.md](40-ADAPTIVE-AI-ROUTING-GUIDE.md) | Latency-based reorder |
