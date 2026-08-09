ALTER TABLE project_git_links
    ADD COLUMN path_ignore_patterns JSONB NOT NULL DEFAULT '[]'::jsonb;
