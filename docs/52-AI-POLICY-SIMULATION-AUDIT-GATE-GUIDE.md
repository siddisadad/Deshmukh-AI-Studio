# AI policy simulation audit trail and rollout gates

**Version:** v0.2.47-beta  
**Scope:** Immutable simulation history and optional staged rollout gates before policy apply.

Extends dry-run simulation ([51-AI-POLICY-SIMULATION-GUIDE.md](51-AI-POLICY-SIMULATION-GUIDE.md)).

---

## Simulation audit trail

Every `POST /ai-policy/simulate` persists a row in `org_ai_policy_simulations` with proposed fields, effective chains, missing providers, and `gate_passed`.

```http
GET /api/v1/organizations/{orgId}/ai-policy/simulations?limit=50
```

Members can read the trail. When a change is applied with a linked simulation, `applied_change_id` is set on the simulation row.

Simulate response now includes:

| Field | Description |
|-------|-------------|
| `simulationId` | Audit row id — pass as `simulationId` on `PUT` when gate is enabled |
| `gatePassed` | `true` when simulated chain has no missing providers |

---

## Staged rollout gates

When enabled, `PUT /ai-policy` requires a recent passing simulation that matches the proposed fields.

```bash
AI_POLICY_SIMULATION_GATE_ENABLED=true
AI_POLICY_SIMULATION_GATE_TTL_MINUTES=30   # default 30
```

Gate rules:

1. `simulationId` required on `PUT`
2. Simulation must be run by the user applying the change
3. `gatePassed` must be `true` (no missing providers)
4. Simulation must be within TTL
5. Proposed fields must match the simulation snapshot
6. Simulation must not already be linked to an applied change

`GET /ai-policy` includes `simulationGateEnabled` when the gate is active.

ADMIN pending proposals still require a passing simulation at `PUT` time (approval applies the pre-validated pending row).

---

## Settings UI

`/settings/ai-routing`:

- Simulation history (last 20) with PASSED/FAILED and applied marker
- Gate note when rollout gates are enabled
- Save disabled until preview passes gates
- Preview panel shows gate pass/fail status
- Save sends `simulationId` from the last preview

---

## Smoke test

```bash
export IMAGE_TAG=v0.2.47-beta
export AI_POLICY_SIMULATION_GATE_ENABLED=true

# PUT without simulate → 400 validation error
# POST simulate → simulationId + gatePassed
# PUT with simulationId → applied, simulation shows appliedChangeId
# GET simulations → audit trail row
```

---

## Related

| Doc | Topic |
|-----|-------|
| [51-AI-POLICY-SIMULATION-GUIDE.md](51-AI-POLICY-SIMULATION-GUIDE.md) | Dry-run simulation |
| [50-AI-POLICY-AUDIT-APPROVAL-GUIDE.md](50-AI-POLICY-AUDIT-APPROVAL-GUIDE.md) | Change approvals |
