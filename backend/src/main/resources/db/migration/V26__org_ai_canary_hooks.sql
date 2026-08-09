ALTER TABLE organization_subscriptions
    ADD COLUMN ai_canary_auto_promote_enabled BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN ai_canary_auto_abort_enabled BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN ai_canary_hook_webhook_url VARCHAR(512) NULL,
    ADD COLUMN ai_canary_min_samples INT NOT NULL DEFAULT 20,
    ADD COLUMN ai_canary_abort_error_rate_percent INT NOT NULL DEFAULT 25,
    ADD COLUMN ai_canary_promote_min_samples INT NOT NULL DEFAULT 50,
    ADD COLUMN ai_canary_promote_max_error_rate_percent INT NOT NULL DEFAULT 5;

CREATE TABLE org_ai_canary_outcomes (
    organization_id UUID PRIMARY KEY REFERENCES organizations(id),
    canary_success_count BIGINT NOT NULL DEFAULT 0,
    canary_failure_count BIGINT NOT NULL DEFAULT 0,
    stable_success_count BIGINT NOT NULL DEFAULT 0,
    stable_failure_count BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL
);
