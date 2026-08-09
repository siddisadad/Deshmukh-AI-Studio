# Token budget caps and org-level AI routing

**Version:** v0.2.41-beta  
**Scope:** Per-organization daily token budgets and custom provider routing chains.

Complements cost-aware routing ([45-COST-AWARE-AI-ROUTING-GUIDE.md](45-COST-AWARE-AI-ROUTING-GUIDE.md)) and seat/overage metering ([28-BILLING-SEAT-USAGE-METERING-GUIDE.md](28-BILLING-SEAT-USAGE-METERING-GUIDE.md)).

---

## Token budgets

| Source | Default daily tokens |
|--------|---------------------|
| FREE | 200,000 |
| PRO | 2,000,000 |
| TEAM | 20,000,000 |

Org override: `organization_subscriptions.daily_token_budget` (nullable).

Usage tracked in `ai_usage_daily.token_count` (UTC day). Before each chat request the API estimates `input + maxOutput` tokens; after completion it records actual `inputTokens + outputTokens` from the provider.

When the budget would be exceeded, chat returns **402 PAYMENT_REQUIRED** with `PLAN_LIMIT` (same code as action limits).

Billing overview includes `aiTokensUsedToday` and `effectiveDailyTokenBudget`.

---

## Org routing policy

Organizations can override the platform provider chain (e.g. force `mock` only for a tenant):

```http
GET /api/v1/organizations/{orgId}/ai-policy
PUT /api/v1/organizations/{orgId}/ai-policy   # OWNER only
```

```json
{
  "providerChain": "mock,openai",
  "dailyTokenBudget": 500000
}
```

Empty `providerChain` clears the override (platform `AI_PROVIDER_CHAIN` / fallbacks apply).

`RoutingAiProvider` resolves the org chain per request via `OrgAiRoutingContext`. Cost-aware and adaptive ordering still apply on the org chain.

---

## API response (`ai-policy`)

| Field | Meaning |
|-------|---------|
| `providerChain` | Stored override (null = platform default) |
| `dailyTokenBudget` | Org override (null = plan default) |
| `effectiveDailyTokenBudget` | Budget enforced today |
| `tokensUsedToday` | Tokens consumed today (UTC) |
| `tokenBudgetRemaining` | Remaining tokens (null if unlimited) |

---

## Smoke test

```bash
export IMAGE_TAG=v0.2.41-beta

# Set org policy
curl -fsS -X PUT "$API/api/v1/organizations/$ORG_ID/ai-policy" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"providerChain":"mock","dailyTokenBudget":1000}'

# Billing overview shows token usage
curl -fsS "$API/api/v1/organizations/$ORG_ID/billing/overview" \
  -H "Authorization: Bearer $TOKEN" | jq '.aiTokensUsedToday,.effectiveDailyTokenBudget'
```

---

## Related

| Doc | Topic |
|-----|-------|
| [45-COST-AWARE-AI-ROUTING-GUIDE.md](45-COST-AWARE-AI-ROUTING-GUIDE.md) | Platform cost tiers and quotas |
| [28-BILLING-SEAT-USAGE-METERING-GUIDE.md](28-BILLING-SEAT-USAGE-METERING-GUIDE.md) | AI action overage |
| [23-STREAMING-TOKEN-UX-GUIDE.md](23-STREAMING-TOKEN-UX-GUIDE.md) | SSE usage metadata |
