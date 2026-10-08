-- Additive only: preserve all exams, tests, sessions, attempts and prior migrations.
CREATE TABLE IF NOT EXISTS generation_jobs (
 id TEXT PRIMARY KEY, exam_id TEXT NOT NULL, exam_name TEXT NOT NULL,
 source TEXT NOT NULL, requested_count INTEGER NOT NULL,
 generated_count INTEGER NOT NULL DEFAULT 0, failed_count INTEGER NOT NULL DEFAULT 0,
 status TEXT NOT NULL, started_at INTEGER NOT NULL, completed_at INTEGER,
 error_category TEXT NOT NULL DEFAULT '', retryable INTEGER NOT NULL DEFAULT 0,
 test_id TEXT, request_key TEXT UNIQUE
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_generation_running ON generation_jobs(exam_id) WHERE status = 'running';
CREATE INDEX IF NOT EXISTS idx_generation_jobs_started ON generation_jobs(started_at DESC, id DESC);
CREATE TABLE IF NOT EXISTS operation_events (
 id TEXT PRIMARY KEY, operation TEXT NOT NULL, timestamp INTEGER NOT NULL,
 category TEXT NOT NULL, status INTEGER NOT NULL, retryable INTEGER NOT NULL,
 correlation_id TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_operation_events_time ON operation_events(timestamp DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_attempts_timestamp ON attempts(timestamp DESC);
