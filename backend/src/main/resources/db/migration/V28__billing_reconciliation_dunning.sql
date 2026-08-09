ALTER TABLE organization_subscriptions
    ADD COLUMN dunning_stage INT NOT NULL DEFAULT 0,
    ADD COLUMN dunning_last_notified_at TIMESTAMPTZ NULL,
    ADD COLUMN reconciliation_delta_cents BIGINT NULL,
    ADD COLUMN reconciliation_checked_at TIMESTAMPTZ NULL;

CREATE TABLE billing_reconciliation_runs (
    id UUID PRIMARY KEY,
    processed_orgs INT NOT NULL,
    matched_orgs INT NOT NULL,
    mismatched_orgs INT NOT NULL,
    total_internal_cents BIGINT NOT NULL,
    total_stripe_cents BIGINT NOT NULL,
    tolerance_cents BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE billing_dunning_events (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    event_type VARCHAR(32) NOT NULL,
    dunning_stage INT NOT NULL,
    email_sent BOOLEAN NOT NULL DEFAULT false,
    detail VARCHAR(512) NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_billing_dunning_events_org ON billing_dunning_events (organization_id, created_at DESC);
