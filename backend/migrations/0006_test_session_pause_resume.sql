-- Migration 0006: Add active time and pause/resume tracking to attempt_sessions
ALTER TABLE attempt_sessions ADD COLUMN accumulated_active_seconds INTEGER NOT NULL DEFAULT 0;
ALTER TABLE attempt_sessions ADD COLUMN status TEXT NOT NULL DEFAULT 'RUNNING';
ALTER TABLE attempt_sessions ADD COLUMN last_resumed_at INTEGER NOT NULL DEFAULT 0;
