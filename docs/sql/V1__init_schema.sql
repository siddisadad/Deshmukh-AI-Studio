-- AI Studio MVP — baseline schema
-- Flyway: V1__init_schema.sql

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(320) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(120) NOT NULL,
    theme           VARCHAR(20) NOT NULL DEFAULT 'SYSTEM',
    email_verified  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_theme CHECK (theme IN ('LIGHT', 'DARK', 'SYSTEM'))
);

CREATE TABLE organizations (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(200) NOT NULL,
    slug        VARCHAR(100) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_organizations_slug UNIQUE (slug)
);

CREATE TABLE memberships (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id  UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    user_id          UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role             VARCHAR(20) NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_membership UNIQUE (organization_id, user_id),
    CONSTRAINT ck_membership_role CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER', 'VIEWER'))
);

CREATE TABLE projects (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id   UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name              VARCHAR(200) NOT NULL,
    project_key       VARCHAR(10) NOT NULL,
    description       TEXT,
    status            VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    archived_at       TIMESTAMPTZ,
    created_by        UUID REFERENCES users(id),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_project_org_key UNIQUE (organization_id, project_key),
    CONSTRAINT ck_project_status CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);

CREATE TABLE project_members (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id  UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role        VARCHAR(20) NOT NULL,
    CONSTRAINT uq_project_member UNIQUE (project_id, user_id),
    CONSTRAINT ck_project_member_role CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER', 'VIEWER'))
);

CREATE TABLE requirements (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id             UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    title                  VARCHAR(300) NOT NULL,
    description            TEXT NOT NULL DEFAULT '',
    improved_description   TEXT,
    user_stories           TEXT,
    acceptance_criteria    TEXT,
    status                 VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    priority               VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    sort_order             INT NOT NULL DEFAULT 0,
    created_by             UUID REFERENCES users(id),
    created_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_req_status CHECK (status IN ('DRAFT', 'READY', 'IN_PROGRESS', 'DONE', 'DEPRECATED')),
    CONSTRAINT ck_req_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

CREATE TABLE tasks (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id       UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    requirement_id   UUID REFERENCES requirements(id) ON DELETE SET NULL,
    title            VARCHAR(300) NOT NULL,
    description      TEXT,
    status           VARCHAR(20) NOT NULL DEFAULT 'TODO',
    priority         VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    assignee_id      UUID REFERENCES users(id) ON DELETE SET NULL,
    sort_order       INT NOT NULL DEFAULT 0,
    created_by       UUID REFERENCES users(id),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_task_status CHECK (status IN ('TODO', 'IN_PROGRESS', 'REVIEW', 'DONE')),
    CONSTRAINT ck_task_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

CREATE TABLE labels (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id  UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name        VARCHAR(60) NOT NULL,
    color       VARCHAR(7) NOT NULL DEFAULT '#6B7280',
    CONSTRAINT uq_label_project_name UNIQUE (project_id, name)
);

CREATE TABLE task_labels (
    task_id   UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    label_id  UUID NOT NULL REFERENCES labels(id) ON DELETE CASCADE,
    PRIMARY KEY (task_id, label_id)
);

CREATE TABLE documents (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id   UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    title        VARCHAR(300) NOT NULL,
    doc_type     VARCHAR(30) NOT NULL DEFAULT 'OTHER',
    content_md   TEXT NOT NULL DEFAULT '',
    created_by   UUID REFERENCES users(id),
    updated_by   UUID REFERENCES users(id),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_doc_type CHECK (doc_type IN ('README', 'API_DOC', 'RELEASE_NOTES', 'TECH_DOC', 'OTHER'))
);

CREATE TABLE conversations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    assistant_role  VARCHAR(40) NOT NULL,
    title           VARCHAR(200),
    created_by      UUID REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_assistant_role CHECK (assistant_role IN (
        'BUSINESS_ANALYST', 'DEVELOPER', 'QA_ENGINEER', 'DOCUMENTATION_WRITER'
    )),
    CONSTRAINT uq_conversation_project_role UNIQUE (project_id, assistant_role)
);

CREATE TABLE messages (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id  UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender           VARCHAR(20) NOT NULL,
    content          TEXT NOT NULL,
    metadata         JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_message_sender CHECK (sender IN ('USER', 'ASSISTANT', 'SYSTEM'))
);

CREATE TABLE project_context_assets (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id   UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    asset_type   VARCHAR(40) NOT NULL,
    title        VARCHAR(200) NOT NULL,
    content      TEXT NOT NULL DEFAULT '',
    metadata     JSONB NOT NULL DEFAULT '{}'::jsonb,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_asset_type CHECK (asset_type IN (
        'DATABASE_DESIGN', 'API_SPEC', 'SOURCE_METADATA', 'OTHER'
    ))
);

CREATE TABLE refresh_tokens (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash   VARCHAR(128) NOT NULL,
    expires_at   TIMESTAMPTZ NOT NULL,
    revoked_at   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash)
);

CREATE TABLE password_reset_tokens (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash   VARCHAR(128) NOT NULL,
    expires_at   TIMESTAMPTZ NOT NULL,
    used_at      TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_password_reset_hash UNIQUE (token_hash)
);

CREATE TABLE audit_logs (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_user_id  UUID REFERENCES users(id) ON DELETE SET NULL,
    action         VARCHAR(80) NOT NULL,
    entity_type    VARCHAR(80),
    entity_id      UUID,
    details        JSONB NOT NULL DEFAULT '{}'::jsonb,
    ip_address     VARCHAR(45),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_memberships_user ON memberships(user_id);
CREATE INDEX idx_projects_org ON projects(organization_id);
CREATE INDEX idx_projects_status ON projects(organization_id, status);
CREATE INDEX idx_project_members_user ON project_members(user_id);
CREATE INDEX idx_requirements_project ON requirements(project_id);
CREATE INDEX idx_tasks_project_status ON tasks(project_id, status);
CREATE INDEX idx_tasks_requirement ON tasks(requirement_id);
CREATE INDEX idx_documents_project ON documents(project_id);
CREATE INDEX idx_messages_conversation ON messages(conversation_id, created_at);
CREATE INDEX idx_context_assets_project ON project_context_assets(project_id, asset_type);
CREATE INDEX idx_audit_created ON audit_logs(created_at);
CREATE INDEX idx_refresh_user ON refresh_tokens(user_id);
