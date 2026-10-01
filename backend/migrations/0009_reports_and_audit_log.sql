-- Migration 0009: Question reports and admin audit log

CREATE TABLE IF NOT EXISTS question_reports (
  id TEXT PRIMARY KEY,
  question_id TEXT NOT NULL,
  exam_id TEXT NOT NULL,
  exam_name TEXT NOT NULL,
  question_text TEXT NOT NULL,
  reason TEXT NOT NULL,
  comment TEXT,
  student_id TEXT NOT NULL,
  student_email TEXT NOT NULL,
  timestamp INTEGER NOT NULL,
  status TEXT NOT NULL DEFAULT 'pending',
  report_type TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_question_reports_type_status_time ON question_reports (report_type, status, timestamp);
CREATE INDEX IF NOT EXISTS idx_question_reports_question_id ON question_reports (question_id);

CREATE TABLE IF NOT EXISTS admin_audit_log (
  id TEXT PRIMARY KEY,
  action_type TEXT NOT NULL,
  description TEXT NOT NULL,
  admin_email TEXT NOT NULL,
  timestamp INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_admin_audit_log_timestamp ON admin_audit_log (timestamp);
