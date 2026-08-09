CREATE TABLE contact_inquiries (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(320) NOT NULL,
    topic VARCHAR(80) NOT NULL,
    message TEXT NOT NULL,
    source_ip VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_contact_inquiries_created_at ON contact_inquiries (created_at DESC);
CREATE INDEX idx_contact_inquiries_email_created ON contact_inquiries (email, created_at DESC);
