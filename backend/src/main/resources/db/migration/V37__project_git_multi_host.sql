ALTER TABLE project_git_links DROP CONSTRAINT IF EXISTS ck_project_git_provider;
ALTER TABLE project_git_links ADD CONSTRAINT ck_project_git_provider CHECK (provider IN (
    'github', 'gitlab', 'bitbucket', 'mock'
));
