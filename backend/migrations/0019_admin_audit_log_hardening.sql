-- Migration 0019: Extend admin_audit_log with server-side actor and metadata fields
ALTER TABLE admin_audit_log ADD COLUMN actor_uid TEXT;
ALTER TABLE admin_audit_log ADD COLUMN target_type TEXT;
ALTER TABLE admin_audit_log ADD COLUMN target_id TEXT;
ALTER TABLE admin_audit_log ADD COLUMN metadata TEXT;
ALTER TABLE admin_audit_log ADD COLUMN source TEXT DEFAULT 'server';
