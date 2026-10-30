-- Migration 0022: Admin Rights & Custom Titles (Telegram Model)
-- Granular administrative rights, owner immutability, and per-admin custom titles.

CREATE TABLE IF NOT EXISTS admin_rights (
    email TEXT PRIMARY KEY,
    rights TEXT NOT NULL,
    custom_title TEXT,
    updated_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_admin_rights_email ON admin_rights (email);

-- Backfill all existing dynamic admins with complete canonical rights set
INSERT OR IGNORE INTO admin_rights (email, rights, custom_title, updated_at)
SELECT lower(trim(email)), '["manage_exams","manage_questions","manage_generated_tests","manage_syllabus","manage_users","manage_admins_rights","manage_premium","send_broadcasts","moderate_feedback","manage_config_maintenance","manage_content","view_analytics_logs","export_data"]', 'Admin', unixepoch() * 1000
FROM admins;

-- Backfill hardcoded owner accounts
INSERT OR IGNORE INTO admin_rights (email, rights, custom_title, updated_at) VALUES
('pronlike9@gmail.com', '["manage_exams","manage_questions","manage_generated_tests","manage_syllabus","manage_users","manage_admins_rights","manage_premium","send_broadcasts","moderate_feedback","manage_config_maintenance","manage_content","view_analytics_logs","export_data"]', 'Owner', unixepoch() * 1000),
('own.keni@gmail.com', '["manage_exams","manage_questions","manage_generated_tests","manage_syllabus","manage_users","manage_admins_rights","manage_premium","send_broadcasts","moderate_feedback","manage_config_maintenance","manage_content","view_analytics_logs","export_data"]', 'Owner', unixepoch() * 1000),
('anyqueairdrop@gmail.com', '["manage_exams","manage_questions","manage_generated_tests","manage_syllabus","manage_users","manage_admins_rights","manage_premium","send_broadcasts","moderate_feedback","manage_config_maintenance","manage_content","view_analytics_logs","export_data"]', 'Owner', unixepoch() * 1000),
('ghatisarkar56@gmail.com', '["manage_exams","manage_questions","manage_generated_tests","manage_syllabus","manage_users","manage_admins_rights","manage_premium","send_broadcasts","moderate_feedback","manage_config_maintenance","manage_content","view_analytics_logs","export_data"]', 'Owner', unixepoch() * 1000);
