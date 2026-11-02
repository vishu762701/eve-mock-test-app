-- Back existing Android cutoff fields; no score/evaluation changes.
-- Additive default preserves existing exams. Production application is manual-only.
ALTER TABLE exams ADD COLUMN cutoffs_json TEXT NOT NULL DEFAULT '{}';
