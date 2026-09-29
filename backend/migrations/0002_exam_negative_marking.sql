-- Migration 0002: Add negative marking support to exams
ALTER TABLE exams ADD COLUMN negative_marking_text TEXT NOT NULL DEFAULT '0';
ALTER TABLE exams ADD COLUMN negative_marking_value REAL NOT NULL DEFAULT 0;
