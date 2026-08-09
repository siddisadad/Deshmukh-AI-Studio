ALTER TABLE project_git_links
    ADD COLUMN path_include_patterns JSONB NOT NULL DEFAULT '[]'::jsonb;
