-- Stripe metered price IDs for seat and AI overage billing
ALTER TABLE plans
    ADD COLUMN IF NOT EXISTS stripe_seat_metered_price_id VARCHAR(120),
    ADD COLUMN IF NOT EXISTS stripe_ai_overage_metered_price_id VARCHAR(120);

COMMENT ON COLUMN plans.stripe_seat_metered_price_id IS 'Stripe metered price for extra seats (per-seat monthly)';
COMMENT ON COLUMN plans.stripe_ai_overage_metered_price_id IS 'Stripe metered price for AI action overage';

ALTER TABLE organization_subscriptions
    ADD COLUMN IF NOT EXISTS stripe_base_subscription_item_id VARCHAR(120),
    ADD COLUMN IF NOT EXISTS stripe_seat_subscription_item_id VARCHAR(120),
    ADD COLUMN IF NOT EXISTS stripe_ai_overage_subscription_item_id VARCHAR(120),
    ADD COLUMN IF NOT EXISTS stripe_metered_usage_synced_at TIMESTAMPTZ;
