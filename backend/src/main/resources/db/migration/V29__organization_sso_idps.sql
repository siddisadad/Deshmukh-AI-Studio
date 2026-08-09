CREATE TABLE organization_sso_idps (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    slug VARCHAR(64) NOT NULL,
    protocol VARCHAR(8) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    issuer_uri VARCHAR(512) NULL,
    client_id VARCHAR(256) NULL,
    client_secret VARCHAR(512) NULL,
    scopes VARCHAR(256) NULL,
    metadata_url VARCHAR(512) NULL,
    entity_id VARCHAR(256) NULL,
    acs_url VARCHAR(512) NULL,
    sp_private_key TEXT NULL,
    sp_certificate TEXT NULL,
    want_encrypted_assertions BOOLEAN NOT NULL DEFAULT false,
    metadata_json TEXT NULL,
    metadata_fetched_at TIMESTAMPTZ NULL,
    metadata_refresh_error VARCHAR(512) NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (organization_id, slug)
);

CREATE INDEX idx_organization_sso_idps_org ON organization_sso_idps (organization_id);
CREATE INDEX idx_organization_sso_idps_enabled ON organization_sso_idps (enabled);
