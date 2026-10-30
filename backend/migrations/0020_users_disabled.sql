-- Migration 0020: Formalize users.disabled column
-- Note: Production D1 already has the 'disabled' column on 'users' from legacy runtime alterations.
-- Base table definition in 0001_initial_schema.sql includes disabled INTEGER DEFAULT 0.
-- This migration idempotently establishes the index on users(disabled).
CREATE INDEX IF NOT EXISTS idx_users_disabled ON users (disabled);
