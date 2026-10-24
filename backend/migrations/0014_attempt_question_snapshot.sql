-- Existing sessions remain valid with an empty snapshot and use the legacy lookup.
-- New sessions preserve the server answer key and question set across admin edits.
ALTER TABLE attempt_sessions ADD COLUMN questions_json TEXT NOT NULL DEFAULT '';
