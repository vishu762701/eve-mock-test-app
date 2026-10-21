-- Migration 0017: Typography Studio schema
-- Tables for published typography profile, working drafts, version history snapshots, and audit log.

CREATE TABLE IF NOT EXISTS typography_published (
  id TEXT PRIMARY KEY,
  version INTEGER NOT NULL,
  config_json TEXT NOT NULL,
  published_at INTEGER NOT NULL,
  published_by TEXT NOT NULL,
  notes TEXT
);

CREATE TABLE IF NOT EXISTS typography_drafts (
  id TEXT PRIMARY KEY,
  config_json TEXT NOT NULL,
  updated_at INTEGER NOT NULL,
  updated_by TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS typography_versions (
  version INTEGER PRIMARY KEY,
  config_json TEXT NOT NULL,
  created_at INTEGER NOT NULL,
  created_by TEXT NOT NULL,
  notes TEXT
);

CREATE TABLE IF NOT EXISTS typography_audit_log (
  id TEXT PRIMARY KEY,
  action TEXT NOT NULL,
  performed_by TEXT NOT NULL,
  version INTEGER,
  details TEXT,
  timestamp INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_typography_audit_time ON typography_audit_log (timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_typography_versions_time ON typography_versions (created_at DESC);
