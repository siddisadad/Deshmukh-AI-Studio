# Org AI routing policy UI and region overrides

**Version:** v0.2.44-beta  
**Scope:** Settings UI and org-level deploy region overrides for AI routing.

Complements token budgets / org chains ([46-TOKEN-BUDGET-ORG-ROUTING-GUIDE.md](46-TOKEN-BUDGET-ORG-ROUTING-GUIDE.md)) and cross-region routing ([48-PROVIDER-NATIVE-CACHE-CROSS-REGION-GUIDE.md](48-PROVIDER-NATIVE-CACHE-CROSS-REGION-GUIDE.md)).

---

## Org deploy region override

```sql
organization_subscriptions.ai_deploy_region  -- e.g. eu-west, us-east
```

When `AI_CROSS_REGION_ROUTING_ENABLED=true`, org override wins over platform `AISTUDIO_DEPLOY_REGION` for resolving `AI_PROVIDER_REGION_CHAINS`.

`ConversationService` sets `OrgAiRoutingContext.deployRegion` per chat request from the org subscription.

---

## API (`ai-policy`)

`GET/PUT /api/v1/organizations/{orgId}/ai-policy` (PUT requires OWNER):

```json
{
  "providerChain": "mock,openai",
  "dailyTokenBudget": 500000,
  "modelMap": "DEVELOPER=openai:gpt-4o-mini",
  "deployRegion": "eu-west",
  "effectiveDailyTokenBudget": 500000,
  "tokensUsedToday": 1200,
  "tokenBudgetRemaining": 498800,
  "effectiveDeployRegion": "eu-west"
}
```

Empty strings on PUT clear overrides (`providerChain`, `modelMap`, `deployRegion`, `dailyTokenBudget` ≤ 0).

---

## Settings UI

Route: **`/settings/ai-routing`** (nav: **AI routing** in app shell).

| Field | Purpose |
|-------|---------|
| Provider chain | Org `ai_provider_chain` override |
| Daily token budget | Org daily token cap override |
| Model map | Per-assistant `provider:model` routing |
| Deploy region | Cross-region chain selection |

Owners can edit; members can view token usage and effective region.

---

## Smoke test

```bash
export IMAGE_TAG=v0.2.44-beta
# Log in as org owner → Settings → AI routing
# Set providerChain=mock and deployRegion=eu-west → Save
curl -fsS -X PUT "$API/api/v1/organizations/$ORG_ID/ai-policy" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"deployRegion":"eu-west","providerChain":"mock"}'
```

---

## Related

| Doc | Topic |
|-----|-------|
| [46-TOKEN-BUDGET-ORG-ROUTING-GUIDE.md](46-TOKEN-BUDGET-ORG-ROUTING-GUIDE.md) | Token budgets and org chains |
| [48-PROVIDER-NATIVE-CACHE-CROSS-REGION-GUIDE.md](48-PROVIDER-NATIVE-CACHE-CROSS-REGION-GUIDE.md) | Platform cross-region config |
| [47-MODEL-ROUTING-PROMPT-CACHE-GUIDE.md](47-MODEL-ROUTING-PROMPT-CACHE-GUIDE.md) | Model map format |
