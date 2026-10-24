-- Migration 0012: UI Studio schema
-- Tables for published config, working drafts, version history snapshots, and audit log.

CREATE TABLE IF NOT EXISTS ui_studio_published (
  id TEXT PRIMARY KEY,
  version INTEGER NOT NULL,
  config_json TEXT NOT NULL,
  published_at INTEGER NOT NULL,
  published_by TEXT NOT NULL,
  notes TEXT
);

CREATE TABLE IF NOT EXISTS ui_studio_drafts (
  id TEXT PRIMARY KEY,
  config_json TEXT NOT NULL,
  updated_at INTEGER NOT NULL,
  updated_by TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS ui_studio_versions (
  version INTEGER PRIMARY KEY,
  config_json TEXT NOT NULL,
  created_at INTEGER NOT NULL,
  created_by TEXT NOT NULL,
  notes TEXT
);

CREATE TABLE IF NOT EXISTS ui_studio_audit_log (
  id TEXT PRIMARY KEY,
  action TEXT NOT NULL,
  performed_by TEXT NOT NULL,
  version INTEGER,
  details TEXT,
  timestamp INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ui_studio_audit_time ON ui_studio_audit_log (timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_ui_studio_versions_time ON ui_studio_versions (created_at DESC);
