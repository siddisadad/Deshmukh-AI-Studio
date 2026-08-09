CREATE TABLE project_git_links (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID NOT NULL UNIQUE REFERENCES projects(id) ON DELETE CASCADE,
    provider        VARCHAR(20) NOT NULL DEFAULT 'github',
    repository      VARCHAR(200) NOT NULL,
    branch          VARCHAR(100) NOT NULL DEFAULT 'main',
    webhook_secret  VARCHAR(128) NOT NULL,
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    last_synced_at  TIMESTAMPTZ,
    last_sync_status VARCHAR(40) NOT NULL DEFAULT 'never',
    last_sync_error TEXT,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_project_git_provider CHECK (provider IN ('github', 'mock'))
);

CREATE INDEX idx_project_git_links_project ON project_git_links(project_id);
