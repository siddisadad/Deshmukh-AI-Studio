-- Allow multiple conversation threads per project + assistant role
ALTER TABLE conversations DROP CONSTRAINT IF EXISTS uq_conversation_project_role;

CREATE INDEX IF NOT EXISTS idx_conversations_project_role_updated
    ON conversations(project_id, assistant_role, updated_at DESC);
