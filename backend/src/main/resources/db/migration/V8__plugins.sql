-- Plugin catalog + per-organization enablement (assistant/tool SPI)
CREATE TABLE plugins (
    id                VARCHAR(80) PRIMARY KEY,
    name              VARCHAR(120) NOT NULL,
    version           VARCHAR(40) NOT NULL,
    plugin_type       VARCHAR(20) NOT NULL,
    description       TEXT NOT NULL DEFAULT '',
    builtin           BOOLEAN NOT NULL DEFAULT FALSE,
    default_enabled   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_plugin_type CHECK (plugin_type IN ('ASSISTANT', 'TOOL'))
);

CREATE TABLE organization_plugins (
    organization_id   UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    plugin_id         VARCHAR(80) NOT NULL REFERENCES plugins(id) ON DELETE CASCADE,
    enabled           BOOLEAN NOT NULL,
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (organization_id, plugin_id)
);

CREATE INDEX idx_org_plugins_org ON organization_plugins(organization_id);
