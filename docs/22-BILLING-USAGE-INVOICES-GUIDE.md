# Billing usage metering and invoices

Daily **AI action metering** and **Stripe invoice** visibility for org owners. Complements checkout, portal, and webhooks ([14-STAGING-DOGFOOD-GUIDE.md](14-STAGING-DOGFOOD-GUIDE.md)).

**MVP scope:** usage history API + UI; invoice list from Stripe (empty for mock provider).

---

## 1. Usage metering

Each billable AI action (chat message, doc generate, etc.) increments `ai_usage_daily` for the organization. Plan limits use `max_ai_actions_per_day` on the active plan.

### API

```http
GET /api/v1/organizations/{orgId}/billing/usage?days=30
```

Returns one row per day (UTC), including zeros for days with no usage. `days` capped at **90**.

Example response:

```json
[
  { "date": "2026-08-01", "actionCount": 12 },
  { "date": "2026-08-02", "actionCount": 0 }
]
```

### UI

**Settings → Billing** shows the last 14 days of usage and today's progress bar against the plan limit.

---

## 2. Invoices (Stripe)

Org **owners** can list recent Stripe invoices:

```http
GET /api/v1/organizations/{orgId}/billing/invoices?limit=12
```

| Field | Meaning |
|---|---|
| `number` | Stripe invoice number |
| `status` | `paid`, `open`, `void`, etc. |
| `amountDueCents` | Amount due in cents |
| `hostedInvoiceUrl` | Stripe-hosted invoice page |
| `invoicePdfUrl` | PDF download |

With `BILLING_PROVIDER=mock`, the list is empty — use Stripe test mode for real invoices.

**Customer portal** (`POST /billing/portal`) remains the primary path for payment method and invoice management; the list API surfaces recent invoices in-app.

---

## 3. Stripe dogfood checklist

```bash
BILLING_PROVIDER=stripe
STRIPE_API_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...
STRIPE_PRO_PRICE_ID=price_...
STRIPE_TEAM_PRICE_ID=price_...
```

1. Upgrade via checkout (test card `4242…`).
2. Confirm webhook updates plan on `checkout.session.completed`.
3. Open **Manage subscription & invoices** (portal) or view **Recent invoices** in settings.
4. Check **AI usage** after sending chat messages.

---

## 4. Related

| Doc | Topic |
|---|---|
| [14-STAGING-DOGFOOD-GUIDE.md](14-STAGING-DOGFOOD-GUIDE.md) | Staging Stripe setup |
| [13-DEPLOYMENT-GUIDE.md](13-DEPLOYMENT-GUIDE.md) | Production Stripe env |

---

## Document control

| Version | Date | Notes |
|---|---|---|
| 1.0 | 2026-08-08 | Usage history API + invoice list |
