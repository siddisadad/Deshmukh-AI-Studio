CREATE TABLE plugin_packs (
    id VARCHAR(64) PRIMARY KEY,
    slug VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    publisher VARCHAR(120) NOT NULL,
    version VARCHAR(40) NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE plugin_pack_members (
    pack_id VARCHAR(64) NOT NULL REFERENCES plugin_packs(id) ON DELETE CASCADE,
    plugin_id VARCHAR(80) NOT NULL REFERENCES plugins(id) ON DELETE CASCADE,
    PRIMARY KEY (pack_id, plugin_id)
);

CREATE TABLE organization_plugin_packs (
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    pack_id VARCHAR(64) NOT NULL REFERENCES plugin_packs(id) ON DELETE CASCADE,
    installed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (organization_id, pack_id)
);

CREATE INDEX idx_org_plugin_packs_org ON organization_plugin_packs(organization_id);
