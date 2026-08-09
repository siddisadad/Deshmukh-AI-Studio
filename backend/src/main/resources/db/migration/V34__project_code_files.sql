CREATE TABLE project_code_files (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id   UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    path         VARCHAR(500) NOT NULL,
    language     VARCHAR(40) NOT NULL DEFAULT '',
    snippet      TEXT NOT NULL DEFAULT '',
    size_bytes   INT NOT NULL DEFAULT 0,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_project_code_files_path UNIQUE (project_id, path)
);

CREATE INDEX idx_project_code_files_project ON project_code_files(project_id);

ALTER TABLE knowledge_chunks DROP CONSTRAINT IF EXISTS ck_knowledge_source_type;
ALTER TABLE knowledge_chunks ADD CONSTRAINT ck_knowledge_source_type CHECK (source_type IN (
    'REQUIREMENT', 'DOCUMENT', 'CONTEXT_ASSET', 'TASK', 'CODE_FILE'
));
