ALTER TABLE plans
    ADD COLUMN IF NOT EXISTS stripe_price_id VARCHAR(120);

COMMENT ON COLUMN plans.stripe_price_id IS 'Stripe Price ID for checkout (set when aistudio.billing.provider=stripe)';
