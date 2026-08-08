ALTER TABLE projects
    ADD COLUMN chat_retention_days INTEGER NULL;

ALTER TABLE conversations
    ADD COLUMN legal_hold BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN retention_expires_at TIMESTAMPTZ NULL;

COMMENT ON COLUMN projects.chat_retention_days IS 'Optional auto-delete window for chat threads (days since last update); null disables expiry';
COMMENT ON COLUMN conversations.legal_hold IS 'When true, thread cannot be deleted or auto-purged';
COMMENT ON COLUMN conversations.retention_expires_at IS 'Computed from project policy; purge when past and not on legal hold';
