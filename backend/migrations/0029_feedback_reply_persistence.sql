-- Repair existing admin reply persistence without ALTER conflicts on installations
-- where the old handler added reply_text/replied_at at runtime. Legacy columns
-- remain readable; new writes use this additive, transaction-safe table.
CREATE TABLE IF NOT EXISTS feedback_admin_replies (
  message_id TEXT PRIMARY KEY REFERENCES feedback_messages(id) ON DELETE CASCADE,
  reply_text TEXT NOT NULL,
  replied_at INTEGER NOT NULL
);
