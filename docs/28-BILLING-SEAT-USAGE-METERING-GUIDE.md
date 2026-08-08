# Billing seat metering and usage-based overage

**Version:** v0.2.23-beta  
**Scope:** Per-plan seat limits, per-seat pricing estimates, and AI action overage metering.

Complements usage history and invoices ([22-BILLING-USAGE-INVOICES-GUIDE.md](22-BILLING-USAGE-INVOICES-GUIDE.md)).

---

## Seat metering

| Plan | `max_seats` | Extra seat price |
|------|-------------|------------------|
| FREE | 3 | — |
| PRO | 10 | $5/seat/mo |
| TEAM | 50 | $3/seat/mo |

- Invites enforce `max_seats` (`PLAN_LIMIT` when full).
- Billing overview shows `activeMemberCount` / `maxSeats`.
- `estimatedSeatCentsMonthly` = base price + (members − 1) × `price_cents_per_seat_monthly`.

---

## Usage-based AI overage

Included daily actions remain `max_ai_actions_per_day`. When included quota is exhausted:

| Plan | Overage rate | Behavior |
|------|--------------|----------|
| FREE | — | Hard cap (no overage) |
| PRO | 2¢ / action | Soft limit — overage counted |
| TEAM | 1¢ / action | Soft limit — overage counted |

`ai_usage_daily.overage_count` tracks overage per day. Overview fields:

- `aiActionsOverageToday`
- `periodOverageActions` (calendar month UTC)
- `estimatedOverageCentsThisPeriod`

Usage history API includes `overageCount` per day.

---

## API (overview extensions)

`GET /organizations/{id}/billing` now includes seat and overage metering fields on `BillingOverviewResponse`.

Plans list includes `maxSeats`, `priceCentsPerSeatMonthly`, `priceCentsPerAiActionOverage`.

---

## Smoke test

```bash
export IMAGE_TAG=v0.2.23-beta
# Settings → Billing: members bar, overage estimate after heavy PRO usage
# Invite until seat limit on FREE org
```

Stripe checkout still uses base plan price when metered IDs are omitted. With metered price IDs configured, checkout attaches seat + AI overage items and `scheduled-stripe-usage-sync.sh` reports usage — see [32-STRIPE-METERED-PRICES-SYNC-GUIDE.md](32-STRIPE-METERED-PRICES-SYNC-GUIDE.md).
