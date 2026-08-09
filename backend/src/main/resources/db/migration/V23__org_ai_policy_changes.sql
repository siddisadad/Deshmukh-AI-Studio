CREATE TABLE org_ai_policy_changes (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    status VARCHAR(20) NOT NULL,
    proposed_by_user_id UUID NOT NULL REFERENCES users(id),
    reviewed_by_user_id UUID REFERENCES users(id),
    provider_chain VARCHAR(255),
    daily_token_budget BIGINT,
    model_map VARCHAR(512),
    deploy_region VARCHAR(64),
    previous_policy JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL,
    reviewed_at TIMESTAMPTZ
);

CREATE INDEX idx_org_ai_policy_changes_org_created
    ON org_ai_policy_changes (organization_id, created_at DESC);

CREATE UNIQUE INDEX idx_org_ai_policy_changes_pending
    ON org_ai_policy_changes (organization_id)
    WHERE status = 'PENDING';
