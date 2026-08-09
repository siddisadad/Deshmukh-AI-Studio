# Stripe revenue reconciliation and dunning automation

**Version:** v0.2.51-beta  
**Scope:** MTD revenue reconciliation vs Stripe paid invoices, staged dunning reminders, webhook-driven payment failure handling.

Complements metered usage sync ([32-STRIPE-METERED-PRICES-SYNC-GUIDE.md](32-STRIPE-METERED-PRICES-SYNC-GUIDE.md)) and billing anomaly alerts ([41-BILLING-ANOMALY-FORECAST-GUIDE.md](41-BILLING-ANOMALY-FORECAST-GUIDE.md)).

---

## Database (V28)

`organization_subscriptions` adds:

| Column | Description |
|--------|-------------|
| `dunning_stage` | Current reminder stage (0 = none) |
| `dunning_last_notified_at` | Last dunning email timestamp |
| `reconciliation_delta_cents` | Stripe paid − internal MTD estimate |
| `reconciliation_checked_at` | Last reconciliation run time |

Audit tables:

- `billing_reconciliation_runs` — per-run aggregate stats
- `billing_dunning_events` — per-org dunning audit log

---

## Reconciliation

`BillingReconciliationService` compares internal MTD estimate (base plan + extra seats + overage) against Stripe **paid** invoice totals for each Pro/Team org with an external subscription.

Operator endpoint (same token as metered sync):

```bash
export BILLING_USAGE_SYNC_TOKEN=...
export BILLING_RECONCILIATION_ENABLED=true
./scripts/scheduled-stripe-reconciliation.sh
```

`POST /api/v1/billing/stripe/reconcile` — requires `Authorization: Bearer $BILLING_USAGE_SYNC_TOKEN`.

Tolerance: `BILLING_RECONCILIATION_TOLERANCE_CENTS` (default 500).

---

## Dunning

Stripe webhooks `invoice.payment_failed` and `invoice.payment_succeeded` update subscription status and dunning stage via `BillingDunningService`.

Scheduled reminders for `PAST_DUE` subscriptions:

```bash
export BILLING_DUNNING_ENABLED=true
export BILLING_USAGE_SYNC_TOKEN=...
./scripts/scheduled-billing-dunning.sh
```

`POST /api/v1/billing/stripe/dunning/run` — operator token required.

| Env var | Default | Purpose |
|---------|---------|---------|
| `BILLING_DUNNING_ENABLED` | `false` | Enable scheduler + scheduled run |
| `BILLING_DUNNING_INTERVAL_HOURS` | `72` | Min hours between reminders per org |
| `BILLING_DUNNING_MAX_STAGE` | `3` | Max reminder stage before urgent subject |
| `BILLING_DUNNING_SCHEDULER_INTERVAL_MS` | `259200000` | Scheduler tick (72h) |

Emails go to org owners via `EmailPort`.

---

## UI

`/settings/billing` shows:

- Past-due banner with dunning stage when `subscriptionStatus` is `PAST_DUE`
- MTD reconciliation delta when a reconciliation run has been recorded

---

## Metrics and alerts

Prometheus gauge `aistudio_billing_reconciliation_delta_cents{organization_id}` (refreshed every 5m).

`monitoring/billing-alerts.yml` adds `BillingReconciliationMismatch` when any org delta exceeds tolerance.

---

## Staging smoke

```bash
export IMAGE_TAG=v0.2.51-beta
export BILLING_PROVIDER=stripe
export BILLING_USAGE_SYNC_TOKEN=...
export BILLING_RECONCILIATION_ENABLED=true
export BILLING_DUNNING_ENABLED=true
./scripts/scheduled-stripe-reconciliation.sh
./scripts/scheduled-billing-dunning.sh
```
