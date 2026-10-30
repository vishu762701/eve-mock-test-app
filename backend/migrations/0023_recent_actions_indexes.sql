-- Migration 0023: Indexes for recent actions audit log filtering and cursor pagination
CREATE INDEX IF NOT EXISTS idx_admin_audit_log_admin_time ON admin_audit_log (admin_email, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_admin_audit_log_type_time ON admin_audit_log (action_type, timestamp DESC);
