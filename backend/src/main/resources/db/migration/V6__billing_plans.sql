CREATE TABLE plans (
    code                    VARCHAR(20) PRIMARY KEY,
    name                    VARCHAR(80) NOT NULL,
    price_cents_monthly     INT NOT NULL DEFAULT 0,
    max_projects            INT NOT NULL,
    max_ai_actions_per_day  INT NOT NULL,
    features                JSONB NOT NULL DEFAULT '[]'::jsonb
);

INSERT INTO plans (code, name, price_cents_monthly, max_projects, max_ai_actions_per_day, features) VALUES
    ('FREE', 'Free', 0, 3, 50, '["mock_ai","shared_context"]'::jsonb),
    ('PRO', 'Pro', 2900, 25, 500, '["real_ai","rag","streaming","background_jobs"]'::jsonb),
    ('TEAM', 'Team', 9900, 100, 5000, '["real_ai","rag","streaming","background_jobs","sso_ready","priority_support"]'::jsonb);

CREATE TABLE organization_subscriptions (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id           UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    plan_code                 VARCHAR(20) NOT NULL REFERENCES plans(code),
    status                    VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    external_customer_id      VARCHAR(120),
    external_subscription_id  VARCHAR(120),
    current_period_end        TIMESTAMPTZ,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_org_subscription UNIQUE (organization_id),
    CONSTRAINT ck_subscription_status CHECK (status IN ('ACTIVE', 'TRIALING', 'PAST_DUE', 'CANCELED'))
);

CREATE TABLE ai_usage_daily (
    organization_id  UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    usage_date       DATE NOT NULL,
    action_count     INT NOT NULL DEFAULT 0,
    PRIMARY KEY (organization_id, usage_date)
);

-- Backfill FREE subscriptions for existing orgs
INSERT INTO organization_subscriptions (organization_id, plan_code, status)
SELECT id, 'FREE', 'ACTIVE' FROM organizations
ON CONFLICT (organization_id) DO NOTHING;
