-- Preserve the marking policy chosen when the session starts; NULL retains legacy behavior.
ALTER TABLE attempt_sessions ADD COLUMN negative_marking_value REAL;
