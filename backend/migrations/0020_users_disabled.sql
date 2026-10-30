-- Migration 0020: Formalize users.disabled column
-- Adds disabled column and index to users table for authoritative account moderation.
ALTER TABLE users ADD COLUMN disabled INTEGER NOT NULL DEFAULT 0;
CREATE INDEX IF NOT EXISTS idx_users_disabled ON users (disabled);
