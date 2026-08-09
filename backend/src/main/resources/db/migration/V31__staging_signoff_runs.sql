CREATE TABLE staging_signoff_runs (
    id UUID PRIMARY KEY,
    run_type VARCHAR(16) NOT NULL,
    host VARCHAR(512) NOT NULL,
    environment_label VARCHAR(64) NULL,
    image_tag VARCHAR(64) NOT NULL,
    overall VARCHAR(8) NOT NULL,
    pass_count INT NOT NULL,
    fail_count INT NOT NULL,
    skip_count INT NOT NULL,
    report_json TEXT NOT NULL,
    s3_uri VARCHAR(512) NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_staging_signoff_runs_image_tag_created
    ON staging_signoff_runs (image_tag, created_at DESC);

CREATE INDEX idx_staging_signoff_runs_overall_created
    ON staging_signoff_runs (overall, created_at DESC);
