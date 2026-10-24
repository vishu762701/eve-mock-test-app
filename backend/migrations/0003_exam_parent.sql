ALTER TABLE exams ADD COLUMN parent_exam_id TEXT NOT NULL DEFAULT '';
CREATE INDEX IF NOT EXISTS idx_exams_parent ON exams (parent_exam_id);
