CREATE TABLE background_jobs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    created_by      UUID REFERENCES users(id),
    job_type        VARCHAR(40) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payload         JSONB NOT NULL DEFAULT '{}'::jsonb,
    result          JSONB NOT NULL DEFAULT '{}'::jsonb,
    error_message   TEXT,
    attempts        INT NOT NULL DEFAULT 0,
    started_at      TIMESTAMPTZ,
    finished_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_job_type CHECK (job_type IN (
        'KNOWLEDGE_REINDEX', 'DOCUMENT_GENERATE'
    )),
    CONSTRAINT ck_job_status CHECK (status IN (
        'PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELLED'
    ))
);

CREATE INDEX idx_background_jobs_status_created ON background_jobs(status, created_at);
CREATE INDEX idx_background_jobs_project_created ON background_jobs(project_id, created_at DESC);
