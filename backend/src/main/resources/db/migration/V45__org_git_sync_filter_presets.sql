CREATE TABLE org_git_sync_filter_presets (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    scope               VARCHAR(20) NOT NULL,
    label               VARCHAR(40) NOT NULL,
    filters             JSONB NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_org_git_sync_filter_scope CHECK (scope IN ('overview', 'runs'))
);

CREATE INDEX idx_org_git_sync_filter_presets_org_user
    ON org_git_sync_filter_presets (organization_id, user_id);

CREATE UNIQUE INDEX uq_org_git_sync_filter_presets_user_scope_label
    ON org_git_sync_filter_presets (organization_id, user_id, scope, label);
