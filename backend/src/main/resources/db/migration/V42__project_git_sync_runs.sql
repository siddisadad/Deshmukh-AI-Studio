CREATE TABLE project_git_sync_runs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    git_link_id     UUID NOT NULL REFERENCES project_git_links(id) ON DELETE CASCADE,
    source          VARCHAR(20) NOT NULL,
    status          VARCHAR(20) NOT NULL,
    file_count      INTEGER NOT NULL DEFAULT 0,
    error_message   TEXT,
    started_at      TIMESTAMPTZ NOT NULL,
    finished_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_git_sync_run_source CHECK (source IN ('manual', 'scheduled', 'webhook')),
    CONSTRAINT ck_git_sync_run_status CHECK (status IN ('success', 'failed'))
);

CREATE INDEX idx_git_sync_runs_project_finished
    ON project_git_sync_runs(project_id, finished_at DESC);
