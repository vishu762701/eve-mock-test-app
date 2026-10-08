-- Each new server session identifies one attempt, including retries from multiple devices.
-- Empty values keep historical attempts compatible and do not change existing student data.
ALTER TABLE attempt_sessions ADD COLUMN session_instance_id TEXT NOT NULL DEFAULT '';
ALTER TABLE attempts ADD COLUMN server_session_id TEXT NOT NULL DEFAULT '';
CREATE UNIQUE INDEX IF NOT EXISTS idx_attempts_server_session ON attempts(user_id, server_session_id) WHERE server_session_id <> '';
