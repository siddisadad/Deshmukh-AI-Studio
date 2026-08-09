CREATE TABLE organization_git_credentials (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    provider            VARCHAR(20) NOT NULL,
    display_name        VARCHAR(120) NOT NULL,
    api_token           VARCHAR(512) NOT NULL,
    api_base_url        VARCHAR(512),
    enabled             BOOLEAN NOT NULL DEFAULT TRUE,
    last_tested_at      TIMESTAMPTZ,
    last_test_status    VARCHAR(20),
    last_test_error     VARCHAR(512),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_org_git_cred_provider UNIQUE (organization_id, provider),
    CONSTRAINT ck_org_git_provider CHECK (provider IN ('github', 'gitlab', 'bitbucket'))
);

CREATE INDEX idx_org_git_credentials_org ON organization_git_credentials (organization_id);
