-- Dashboard: filter recent activity by actor without scanning all audit rows.
CREATE INDEX IF NOT EXISTS idx_audit_actor_created
    ON audit_logs(actor_user_id, created_at DESC);
