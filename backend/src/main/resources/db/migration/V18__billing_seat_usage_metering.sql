-- Seat limits and usage-based overage pricing on plans
ALTER TABLE plans
    ADD COLUMN max_seats INT NOT NULL DEFAULT 3,
    ADD COLUMN price_cents_per_seat_monthly INT NOT NULL DEFAULT 0,
    ADD COLUMN price_cents_per_ai_action_overage INT NOT NULL DEFAULT 0;

UPDATE plans SET
    max_seats = 3,
    price_cents_per_seat_monthly = 0,
    price_cents_per_ai_action_overage = 0
WHERE code = 'FREE';

UPDATE plans SET
    max_seats = 10,
    price_cents_per_seat_monthly = 500,
    price_cents_per_ai_action_overage = 2
WHERE code = 'PRO';

UPDATE plans SET
    max_seats = 50,
    price_cents_per_seat_monthly = 300,
    price_cents_per_ai_action_overage = 1
WHERE code = 'TEAM';

ALTER TABLE ai_usage_daily
    ADD COLUMN overage_count INT NOT NULL DEFAULT 0;
