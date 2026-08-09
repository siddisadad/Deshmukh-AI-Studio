# AI routing policy audit log and change approvals

**Version:** v0.2.45-beta  
**Scope:** Immutable change history and optional owner approval for admin-proposed routing updates.

Complements org routing policy UI ([49-ORG-AI-ROUTING-UI-GUIDE.md](49-ORG-AI-ROUTING-UI-GUIDE.md)).

---

## Audit log

Every applied policy update creates an `APPLIED` row in `org_ai_policy_changes` with `previous_policy` JSON snapshot.

```http
GET /api/v1/organizations/{orgId}/ai-policy/changes?limit=50
```

Members can read the log; entries include `status`, proposed/reviewed user ids, field values, and timestamps.

---

## Change approvals

When enabled, **ADMIN** `PUT` requests create a **PENDING** change instead of applying immediately. **OWNER** applies directly and can approve or reject pending proposals.

```bash
AI_POLICY_CHANGE_APPROVAL_ENABLED=true
```

| Endpoint | Role | Action |
|----------|------|--------|
| `PUT /ai-policy` | ADMIN | Submit pending change (when approval enabled) |
| `PUT /ai-policy` | OWNER | Apply immediately + audit log |
| `POST /ai-policy/pending/approve` | OWNER | Apply pending change |
| `POST /ai-policy/pending/reject` | OWNER | Reject pending change |

`GET /ai-policy` includes `changeApprovalRequired` and `pendingChange` when a proposal is queued.

Only one `PENDING` change per organization (new proposals replace the previous pending row).

---

## Settings UI

`/settings/ai-routing` shows:

- Pending change banner with Approve/Reject (owners)
- Change history list (last 20 entries)
- Note when approval mode is active

---

## Smoke test

```bash
export IMAGE_TAG=v0.2.45-beta
export AI_POLICY_CHANGE_APPROVAL_ENABLED=true

# As ADMIN: PUT ai-policy → pendingChange populated, live policy unchanged
# As OWNER: POST pending/approve → policy applied, history shows APPLIED
```

---

## Related

| Doc | Topic |
|-----|-------|
| [49-ORG-AI-ROUTING-UI-GUIDE.md](49-ORG-AI-ROUTING-UI-GUIDE.md) | Policy fields and UI |
| [46-TOKEN-BUDGET-ORG-ROUTING-GUIDE.md](46-TOKEN-BUDGET-ORG-ROUTING-GUIDE.md) | Token budgets and chains |
