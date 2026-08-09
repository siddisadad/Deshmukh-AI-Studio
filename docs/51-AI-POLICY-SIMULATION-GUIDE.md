# AI routing policy simulation (dry-run)

**Version:** v0.2.46-beta  
**Scope:** Preview org AI routing policy changes before apply — effective provider chains, budgets, and approval requirements.

Complements audit log and approvals ([50-AI-POLICY-AUDIT-APPROVAL-GUIDE.md](50-AI-POLICY-AUDIT-APPROVAL-GUIDE.md)) and org routing UI ([49-ORG-AI-ROUTING-UI-GUIDE.md](49-ORG-AI-ROUTING-UI-GUIDE.md)).

---

## API

Dry-run merges the proposed `UpdateOrgAiPolicyRequest` onto the current subscription without persisting:

```http
POST /api/v1/organizations/{orgId}/ai-policy/simulate
Content-Type: application/json

{
  "providerChain": "mock,openai",
  "dailyTokenBudget": 75000,
  "modelMap": "DEVELOPER=openai:gpt-4o-mini",
  "deployRegion": "us-east"
}
```

**Roles:** OWNER and ADMIN (same as `PUT /ai-policy`).

Response includes:

| Field | Description |
|-------|-------------|
| `current` / `simulated` | Snapshot of policy fields and effective budget/region |
| `currentEffectiveProviderChain` | Resolved chain from platform + cross-region + org override (current) |
| `simulatedEffectiveProviderChain` | Resolved chain after proposed changes |
| `missingProviders` | Provider ids in simulated chain not registered in `AiProviderRegistry` |
| `wouldRequireApproval` | `true` when approval mode is on and caller is ADMIN |

Resolution uses `OrgAiPolicyRoutingPreview` — same merge rules as apply, including cross-region registry and org provider chain override.

---

## Settings UI

`/settings/ai-routing` adds **Preview changes** next to **Save policy**:

- Shows current vs simulated effective provider chains
- Warns on missing providers
- Notes when the change would queue for owner approval
- Clears preview after a successful save

---

## Smoke test

```bash
export IMAGE_TAG=v0.2.46-beta

# POST /ai-policy/simulate with new chain/region → simulatedEffectiveProviderChain updated
# GET /ai-policy → live policy unchanged until PUT or approve
```

With approval mode:

```bash
export AI_POLICY_CHANGE_APPROVAL_ENABLED=true
# ADMIN simulate → wouldRequireApproval: true
# OWNER simulate → wouldRequireApproval: false
```

---

## Related

| Doc | Topic |
|-----|-------|
| [50-AI-POLICY-AUDIT-APPROVAL-GUIDE.md](50-AI-POLICY-AUDIT-APPROVAL-GUIDE.md) | Audit log and approvals |
| [48-PROVIDER-NATIVE-CACHE-CROSS-REGION-GUIDE.md](48-PROVIDER-NATIVE-CACHE-CROSS-REGION-GUIDE.md) | Cross-region chain resolution |
| [46-TOKEN-BUDGET-ORG-ROUTING-GUIDE.md](46-TOKEN-BUDGET-ORG-ROUTING-GUIDE.md) | Token budgets and chains |
