DROP INDEX IF EXISTS uq_org_git_sync_filter_presets_user_scope_label;

ALTER TABLE org_git_sync_filter_presets
    ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'private';

ALTER TABLE org_git_sync_filter_presets
    ADD CONSTRAINT ck_org_git_sync_filter_visibility CHECK (visibility IN ('private', 'org'));

CREATE UNIQUE INDEX uq_org_git_sync_filter_presets_private_label
    ON org_git_sync_filter_presets (organization_id, user_id, scope, label)
    WHERE visibility = 'private';

CREATE UNIQUE INDEX uq_org_git_sync_filter_presets_org_label
    ON org_git_sync_filter_presets (organization_id, scope, label)
    WHERE visibility = 'org';
