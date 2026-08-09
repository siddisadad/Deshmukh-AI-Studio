ALTER TABLE project_git_links
    ADD COLUMN scheduled_sync_enabled BOOLEAN NOT NULL DEFAULT TRUE;
