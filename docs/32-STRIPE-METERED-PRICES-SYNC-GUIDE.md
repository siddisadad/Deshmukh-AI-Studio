# Stripe metered prices sync

**Version:** v0.2.27-beta  
**Scope:** Sync seat and AI overage usage to Stripe metered subscription prices for PRO/TEAM plans.

Complements seat metering ([28-BILLING-SEAT-USAGE-METERING-GUIDE.md](28-BILLING-SEAT-USAGE-METERING-GUIDE.md)) and invoices ([22-BILLING-USAGE-INVOICES-GUIDE.md](22-BILLING-USAGE-INVOICES-GUIDE.md)).

---

## Stripe Dashboard setup

For each paid plan create **three** prices (test mode first):

| Price | Type | Example unit |
|-------|------|----------------|
| Base plan | Recurring fixed | PRO $29/mo, TEAM $99/mo |
| Extra seat | Recurring **metered** | PRO $5/seat/mo, TEAM $3/seat/mo |
| AI overage | Recurring **metered** | PRO 2¢/action, TEAM 1¢/action |

Record price IDs in `.env`:

```bash
STRIPE_PRO_PRICE_ID=price_...
STRIPE_TEAM_PRICE_ID=price_...
STRIPE_PRO_SEAT_METERED_PRICE_ID=price_...
STRIPE_TEAM_SEAT_METERED_PRICE_ID=price_...
STRIPE_PRO_AI_OVERAGE_METERED_PRICE_ID=price_...
STRIPE_TEAM_AI_OVERAGE_METERED_PRICE_ID=price_...
```

Validate:

```bash
./scripts/sync-stripe-metered-prices.sh
```

Optional: set `plans.stripe_seat_metered_price_id` / `stripe_ai_overage_metered_price_id` in DB instead of env (Flyway `V19`).

---

## Checkout

When metered price IDs are configured, Stripe Checkout creates a subscription with:

1. Base plan line item (quantity 1)
2. Seat metered line item (when configured)
3. AI overage metered line item (when configured)

Webhook handlers store Stripe subscription item IDs on `organization_subscriptions` for usage reporting.

---

## Usage sync API

Cron or operator triggers:

```http
POST /api/v1/billing/stripe/sync-metered-usage
Authorization: Bearer ${BILLING_USAGE_SYNC_TOKEN}
```

Response:

```json
{
  "processed": 2,
  "synced": 1,
  "skipped": 1,
  "failed": 0,
  "messages": ["..."]
}
```

Per org (PRO/TEAM with active Stripe subscription):

| Meter | Stripe action | Quantity |
|-------|---------------|----------|
| Extra seats | `SET` on seat subscription item | `max(0, members − 1)` |
| AI overage today | `INCREMENT` on overage item | today's `overage_count` sum |

Set `BILLING_USAGE_SYNC_TOKEN` in API env (same pattern as `METRICS_SCRAPE_TOKEN`).

---

## Cron script

```bash
export BILLING_USAGE_SYNC_TOKEN="$(openssl rand -hex 32)"
# add to .env and redeploy API
./scripts/scheduled-stripe-usage-sync.sh
# API_URL=http://localhost:8080 for direct API
```

Schedule daily (e.g. 02:00 UTC) after usage has accumulated.

---

## Smoke test

```bash
export IMAGE_TAG=v0.2.27-beta
export BILLING_PROVIDER=stripe
./scripts/sync-stripe-metered-prices.sh
# Complete checkout on staging → verify subscription has 3 items in Stripe Dashboard
./scripts/scheduled-stripe-usage-sync.sh
```

---

## Related

| Doc | Topic |
|-----|-------|
| [28-BILLING-SEAT-USAGE-METERING-GUIDE.md](28-BILLING-SEAT-USAGE-METERING-GUIDE.md) | In-app seat/overage metering |
| [22-BILLING-USAGE-INVOICES-GUIDE.md](22-BILLING-USAGE-INVOICES-GUIDE.md) | Usage history + invoices |
| [14-STAGING-DOGFOOD-GUIDE.md](14-STAGING-DOGFOOD-GUIDE.md) | Stripe test-mode dogfood |
