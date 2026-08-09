CREATE TABLE org_ai_policy_simulations (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    simulated_by_user_id UUID NOT NULL REFERENCES users(id),
    provider_chain VARCHAR(255),
    daily_token_budget BIGINT,
    model_map VARCHAR(512),
    deploy_region VARCHAR(64),
    missing_providers JSONB NOT NULL DEFAULT '[]',
    current_effective_chain JSONB NOT NULL DEFAULT '[]',
    simulated_effective_chain JSONB NOT NULL DEFAULT '[]',
    gate_passed BOOLEAN NOT NULL,
    applied_change_id UUID REFERENCES org_ai_policy_changes(id),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_org_ai_policy_simulations_org_created
    ON org_ai_policy_simulations (organization_id, created_at DESC);
