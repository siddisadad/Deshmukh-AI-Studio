# AI routing policy canary rollout

**Version:** v0.2.48-beta  
**Scope:** Gradual provider chain shifts via per-conversation canary routing before full promotion.

Extends org routing policy ([46-TOKEN-BUDGET-ORG-ROUTING-GUIDE.md](46-TOKEN-BUDGET-ORG-ROUTING-GUIDE.md)) and simulation gates ([52-AI-POLICY-SIMULATION-AUDIT-GATE-GUIDE.md](52-AI-POLICY-SIMULATION-AUDIT-GATE-GUIDE.md)).

---

## Canary fields

Stored on `organization_subscriptions`:

| Column | Description |
|--------|-------------|
| `ai_canary_provider_chain` | Candidate chain (comma-separated provider ids) |
| `ai_canary_percent` | 1–100 — share of conversation threads routed to canary |

`GET /ai-policy` returns `canaryProviderChain` and `canaryPercent` when active.

---

## Routing behavior

`OrgAiRoutingPolicyService` selects stable vs canary chain per request:

- Hash bucket from `organizationId` + `conversationId` (sticky per thread)
- When bucket &lt; `canaryPercent`, use canary chain; otherwise stable org chain (or platform default)
- `percent >= 100` routes all traffic to canary

`ConversationService` sets `OrgAiRoutingContext.conversationId` for chat requests.

---

## API

| Endpoint | Role | Action |
|----------|------|--------|
| `PUT /ai-policy/canary` | OWNER/ADMIN | Start or update canary `{ providerChain, percent }` |
| `POST /ai-policy/canary/promote` | OWNER/ADMIN | Copy canary chain to stable policy and clear canary |
| `DELETE /ai-policy/canary` | OWNER/ADMIN | Abort canary without changing stable policy |

Promote applies the canary chain as the org `providerChain` override. Abort clears canary fields only.

---

## Settings UI

`/settings/ai-routing` **Canary rollout** section:

- Canary chain + traffic percent inputs
- Start / update canary, Promote, Abort actions
- Active canary banner when configured

---

## Smoke test

```bash
export IMAGE_TAG=v0.2.48-beta

# PUT canary at 25% → GET ai-policy shows canaryProviderChain + canaryPercent
# Chat in multiple threads → subset uses canary chain (check provider metrics/logs)
# POST canary/promote → providerChain updated, canary cleared
```

---

## Related

| Doc | Topic |
|-----|-------|
| [51-AI-POLICY-SIMULATION-GUIDE.md](51-AI-POLICY-SIMULATION-GUIDE.md) | Dry-run before apply |
| [34-MULTI-PROVIDER-ROUTING-GUIDE.md](34-MULTI-PROVIDER-ROUTING-GUIDE.md) | Provider failover chains |
