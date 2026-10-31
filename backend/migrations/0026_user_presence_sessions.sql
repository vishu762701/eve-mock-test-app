-- Migration 0026: bounded per-device authenticated foreground presence leases.
-- Never derive online from last_active and never expose this table publicly.
CREATE TABLE IF NOT EXISTS user_presence_sessions (
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    session_id TEXT NOT NULL,
    expires_at INTEGER NOT NULL,
    PRIMARY KEY (user_id, session_id)
);
CREATE INDEX IF NOT EXISTS idx_presence_expiry ON user_presence_sessions(expires_at);
CREATE INDEX IF NOT EXISTS idx_presence_user_expiry ON user_presence_sessions(user_id, expires_at);
