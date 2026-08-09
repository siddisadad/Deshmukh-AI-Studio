ALTER TABLE organization_subscriptions
    ADD COLUMN ai_canary_provider_chain VARCHAR(255) NULL,
    ADD COLUMN ai_canary_percent INT NULL;
