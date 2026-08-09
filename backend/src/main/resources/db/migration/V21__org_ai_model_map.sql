-- Per-assistant model routing overrides at org level
ALTER TABLE organization_subscriptions
    ADD COLUMN ai_model_map VARCHAR(512) NULL;
