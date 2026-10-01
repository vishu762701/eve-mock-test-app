-- Migration 0008: Add publish_mode to exams
ALTER TABLE exams ADD COLUMN publish_mode TEXT NOT NULL DEFAULT 'inherit';
