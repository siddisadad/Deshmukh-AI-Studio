ALTER TABLE conversations
    ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'PROJECT';

ALTER TABLE conversations
    ADD CONSTRAINT ck_conversation_visibility
        CHECK (visibility IN ('PROJECT', 'PRIVATE'));
