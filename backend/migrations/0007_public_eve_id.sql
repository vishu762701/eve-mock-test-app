-- Migration 0007: Add public EVE ID to users
ALTER TABLE users ADD COLUMN eve_id TEXT;
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_eve_id ON users (eve_id);
