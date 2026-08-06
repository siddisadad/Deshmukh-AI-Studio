CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE knowledge_chunks (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id   UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    source_type  VARCHAR(40) NOT NULL,
    source_id    UUID NOT NULL,
    chunk_index  INT NOT NULL,
    title        VARCHAR(300) NOT NULL DEFAULT '',
    content      TEXT NOT NULL,
    embedding    vector(384) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_knowledge_source_type CHECK (source_type IN (
        'REQUIREMENT', 'DOCUMENT', 'CONTEXT_ASSET', 'TASK'
    )),
    CONSTRAINT uq_knowledge_chunk UNIQUE (project_id, source_type, source_id, chunk_index)
);

CREATE INDEX idx_knowledge_chunks_project ON knowledge_chunks(project_id);
CREATE INDEX idx_knowledge_chunks_source ON knowledge_chunks(project_id, source_type, source_id);
