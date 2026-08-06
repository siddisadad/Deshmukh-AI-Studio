CREATE UNIQUE INDEX IF NOT EXISTS uq_context_assets_project_type
    ON project_context_assets(project_id, asset_type);
