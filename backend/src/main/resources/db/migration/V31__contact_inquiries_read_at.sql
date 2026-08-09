ALTER TABLE contact_inquiries
    ADD COLUMN read_at TIMESTAMPTZ NULL;

CREATE INDEX idx_contact_inquiries_unread ON contact_inquiries (created_at DESC)
    WHERE read_at IS NULL;
