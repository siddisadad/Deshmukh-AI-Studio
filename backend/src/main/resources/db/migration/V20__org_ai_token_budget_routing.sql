-- Per-plan token budgets and org-level AI routing overrides
ALTER TABLE plans
    ADD COLUMN max_ai_tokens_per_day BIGINT NOT NULL DEFAULT 200000;

UPDATE plans SET max_ai_tokens_per_day = 200000 WHERE code = 'FREE';
UPDATE plans SET max_ai_tokens_per_day = 2000000 WHERE code = 'PRO';
UPDATE plans SET max_ai_tokens_per_day = 20000000 WHERE code = 'TEAM';

ALTER TABLE ai_usage_daily
    ADD COLUMN token_count BIGINT NOT NULL DEFAULT 0;

ALTER TABLE organization_subscriptions
    ADD COLUMN daily_token_budget BIGINT NULL,
    ADD COLUMN ai_provider_chain VARCHAR(255) NULL;
