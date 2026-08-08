ALTER TABLE conversations
    ADD COLUMN share_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN share_token_hash VARCHAR(128),
    ADD COLUMN share_expires_at TIMESTAMPTZ,
    ADD COLUMN share_created_at TIMESTAMPTZ;

CREATE UNIQUE INDEX idx_conversations_share_token_hash
    ON conversations (share_token_hash)
    WHERE share_token_hash IS NOT NULL;
