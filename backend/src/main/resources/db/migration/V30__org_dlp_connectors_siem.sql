CREATE TABLE organization_dlp_connectors (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    slug VARCHAR(64) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    connector_type VARCHAR(16) NOT NULL,
    webhook_url VARCHAR(512) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    block_on_match BOOLEAN NOT NULL DEFAULT true,
    custom_patterns_json TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (organization_id, slug)
);

CREATE TABLE thread_export_dlp_events (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    conversation_id UUID NULL REFERENCES conversations(id) ON DELETE SET NULL,
    export_id UUID NOT NULL,
    exported_by_user_id UUID NOT NULL,
    match_categories VARCHAR(512) NOT NULL,
    blocked BOOLEAN NOT NULL,
    siem_exported_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_org_dlp_connectors_org ON organization_dlp_connectors (organization_id);
CREATE INDEX idx_thread_dlp_events_org_created ON thread_export_dlp_events (organization_id, created_at DESC);
