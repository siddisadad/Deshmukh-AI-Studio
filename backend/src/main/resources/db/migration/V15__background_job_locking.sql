ALTER TABLE background_jobs
    ADD COLUMN locked_by VARCHAR(128),
    ADD COLUMN locked_at TIMESTAMPTZ;

CREATE INDEX idx_background_jobs_running_locked_at
    ON background_jobs (locked_at)
    WHERE status = 'RUNNING';
