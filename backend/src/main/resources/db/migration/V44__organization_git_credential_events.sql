CREATE TABLE organization_git_credential_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    provider        VARCHAR(20) NOT NULL,
    action          VARCHAR(32) NOT NULL,
    actor_user_id   UUID REFERENCES users(id) ON DELETE SET NULL,
    display_name    VARCHAR(120),
    api_base_url    VARCHAR(512),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_org_git_cred_event_action CHECK (
        action IN ('CREATED', 'UPDATED', 'TOKEN_ROTATED', 'DELETED')
    )
);

CREATE INDEX idx_org_git_cred_events_org_created
    ON organization_git_credential_events (organization_id, created_at DESC);
