-- ============================================================================
-- D1 Migration 0001: Initial Schema for Eve App
-- Database: eve-database
-- Purpose: Complete relational translation of all 19 Firestore entities
-- ============================================================================

-- 1. Users table (profiles, activity, tokens)
CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,                       -- Firebase Auth UID
    email TEXT NOT NULL,
    display_name TEXT DEFAULT 'Student',
    dob TEXT DEFAULT '',
    category TEXT DEFAULT 'General',
    created_at INTEGER NOT NULL,               -- Epoch millis
    last_active INTEGER NOT NULL,              -- Epoch millis
    last_updated INTEGER DEFAULT 0,
    fcm_token TEXT DEFAULT ''
);
CREATE INDEX IF NOT EXISTS idx_users_last_active ON users (last_active);
CREATE INDEX IF NOT EXISTS idx_users_email ON users (email);

-- 2. Pinned exams per user (subcollection users/{userId}/pinned_exams/{examId})
CREATE TABLE IF NOT EXISTS pinned_exams (
    user_id TEXT NOT NULL,
    exam_id TEXT NOT NULL,
    pinned_at INTEGER NOT NULL,
    PRIMARY KEY (user_id, exam_id),
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_pinned_exams_user ON pinned_exams (user_id);

-- 3. Bookmarks per user (subcollection users/{userId}/bookmarks/{questionId})
CREATE TABLE IF NOT EXISTS bookmarks (
    id TEXT PRIMARY KEY,                       -- Deterministic or question ID
    user_id TEXT NOT NULL,
    question_id TEXT NOT NULL,
    exam_id TEXT NOT NULL,
    exam_name TEXT NOT NULL,
    question_number INTEGER NOT NULL,
    question_text TEXT NOT NULL,
    question_text_hi TEXT DEFAULT '',
    option_a TEXT NOT NULL,
    option_b TEXT NOT NULL,
    option_c TEXT NOT NULL,
    option_d TEXT NOT NULL,
    option_a_hi TEXT DEFAULT '',
    option_b_hi TEXT DEFAULT '',
    option_c_hi TEXT DEFAULT '',
    option_d_hi TEXT DEFAULT '',
    correct_answer TEXT NOT NULL,
    explanation TEXT DEFAULT '',
    explanation_hi TEXT DEFAULT '',
    topic TEXT DEFAULT '',
    is_pyq INTEGER DEFAULT 0,
    pyq_year INTEGER DEFAULT 0,
    pyq_paper TEXT DEFAULT '',
    bookmarked_at INTEGER NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_bookmarks_user ON bookmarks (user_id, bookmarked_at DESC);

-- 4. Dynamic Admins whitelist
CREATE TABLE IF NOT EXISTS admins (
    email TEXT PRIMARY KEY,                    -- Lowercase email
    created_at INTEGER NOT NULL
);

-- 5. Exams table
CREATE TABLE IF NOT EXISTS exams (
    id TEXT PRIMARY KEY,
    exam_name TEXT NOT NULL,
    time_limit_minutes INTEGER NOT NULL DEFAULT 30,
    category TEXT NOT NULL DEFAULT 'Other',
    syllabus TEXT DEFAULT '',
    question_count INTEGER NOT NULL DEFAULT 20,
    custom_prompt_notes TEXT DEFAULT '',
    auto_generation_enabled INTEGER NOT NULL DEFAULT 1, -- 0 or 1
    auto_gen_time TEXT NOT NULL DEFAULT '00:00',
    timezone TEXT NOT NULL DEFAULT 'Asia/Kolkata',
    test_number TEXT NOT NULL DEFAULT 'Test 1',
    syllabus_url TEXT DEFAULT '',               -- Supabase Storage Public URL
    syllabus_file_name TEXT DEFAULT '',
    generation_prompt TEXT DEFAULT '',
    image_url TEXT DEFAULT '',                  -- Base64 or Supabase Public URL
    last_generated_date TEXT DEFAULT '',        -- YYYY-MM-DD
    last_generation_status TEXT DEFAULT '',     -- 'running' | 'success' | 'failed'
    last_generation_error TEXT DEFAULT '',
    last_generation_time INTEGER DEFAULT 0,
    generating_lock_until INTEGER DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_exams_category ON exams (category);
CREATE INDEX IF NOT EXISTS idx_exams_auto_gen ON exams (auto_generation_enabled, auto_gen_time);

-- 6. Questions table (mock test & PYQ & practice bank)
CREATE TABLE IF NOT EXISTS questions (
    id TEXT PRIMARY KEY,
    exam_id TEXT NOT NULL,
    question_text TEXT NOT NULL,
    option_a TEXT NOT NULL,
    option_b TEXT NOT NULL,
    option_c TEXT NOT NULL,
    option_d TEXT NOT NULL,
    correct_answer TEXT NOT NULL,              -- 'A' | 'B' | 'C' | 'D'
    explanation TEXT DEFAULT '',
    topic TEXT DEFAULT '',
    is_pyq INTEGER NOT NULL DEFAULT 0,
    pyq_year INTEGER NOT NULL DEFAULT 0,
    pyq_paper TEXT DEFAULT '',
    question_text_hi TEXT DEFAULT '',
    option_a_hi TEXT DEFAULT '',
    option_b_hi TEXT DEFAULT '',
    option_c_hi TEXT DEFAULT '',
    option_d_hi TEXT DEFAULT '',
    explanation_hi TEXT DEFAULT '',
    FOREIGN KEY (exam_id) REFERENCES exams (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_questions_exam_id ON questions (exam_id);
CREATE INDEX IF NOT EXISTS idx_questions_topic ON questions (exam_id, topic);
CREATE INDEX IF NOT EXISTS idx_questions_pyq ON questions (exam_id, is_pyq, pyq_year, pyq_paper);

-- 7. Attempt Locks (anti-cheat single-submission lock per user per exam)
CREATE TABLE IF NOT EXISTS attempt_locks (
    id TEXT PRIMARY KEY,                       -- '${user_id}_${exam_id}'
    user_id TEXT NOT NULL,
    exam_id TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    source TEXT NOT NULL DEFAULT 'submit'
);
CREATE INDEX IF NOT EXISTS idx_attempt_locks_user ON attempt_locks (user_id);
CREATE INDEX IF NOT EXISTS idx_attempt_locks_user_exam ON attempt_locks (user_id, exam_id);

-- 8. Attempts table (completed test results)
CREATE TABLE IF NOT EXISTS attempts (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    display_name TEXT NOT NULL DEFAULT 'Student',
    exam_id TEXT NOT NULL,
    exam_name TEXT NOT NULL,
    category TEXT NOT NULL DEFAULT '',
    score REAL NOT NULL,
    total INTEGER NOT NULL,
    correct INTEGER NOT NULL,
    wrong INTEGER NOT NULL,
    unattempted INTEGER NOT NULL,
    timestamp INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_attempts_user_time ON attempts (user_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_attempts_exam ON attempts (exam_id);

-- 9. Attempt Answers table (evaluated answer sheet per attempt)
CREATE TABLE IF NOT EXISTS attempt_answers (
    id TEXT PRIMARY KEY,
    attempt_id TEXT NOT NULL,
    question_id TEXT NOT NULL,
    question_number INTEGER NOT NULL,
    question_text TEXT NOT NULL,
    selected TEXT NOT NULL DEFAULT '',         -- '' | 'A' | 'B' | 'C' | 'D'
    selected_text TEXT NOT NULL DEFAULT '',
    correct TEXT NOT NULL,
    correct_text TEXT NOT NULL,
    explanation TEXT DEFAULT '',
    is_bookmarked INTEGER DEFAULT 0,
    topic TEXT DEFAULT '',
    question_text_hi TEXT DEFAULT '',
    selected_text_hi TEXT DEFAULT '',
    correct_text_hi TEXT DEFAULT '',
    explanation_hi TEXT DEFAULT '',
    FOREIGN KEY (attempt_id) REFERENCES attempts (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_attempt_answers_attempt ON attempt_answers (attempt_id);

-- 10. Per-Exam Leaderboard table
CREATE TABLE IF NOT EXISTS leaderboard (
    id TEXT PRIMARY KEY,                       -- '${exam_id}_${user_id}'
    user_id TEXT NOT NULL,
    exam_id TEXT NOT NULL,
    exam_name TEXT NOT NULL,
    category TEXT NOT NULL DEFAULT '',
    display_name TEXT NOT NULL DEFAULT 'Student',
    score REAL NOT NULL,
    total INTEGER NOT NULL,
    timestamp INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_leaderboard_exam_score ON leaderboard (exam_id, score DESC);
CREATE INDEX IF NOT EXISTS idx_leaderboard_user ON leaderboard (user_id);

-- 11. Overall Leaderboard table
CREATE TABLE IF NOT EXISTS overall_leaderboard (
    user_id TEXT PRIMARY KEY,
    display_name TEXT NOT NULL DEFAULT 'Student',
    score REAL NOT NULL DEFAULT 0.0,
    total_score REAL NOT NULL DEFAULT 0.0,
    total INTEGER NOT NULL DEFAULT 0,
    tests_taken INTEGER NOT NULL DEFAULT 0,
    total_correct INTEGER NOT NULL DEFAULT 0,
    accuracy INTEGER NOT NULL DEFAULT 0,
    timestamp INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_overall_lb_score ON overall_leaderboard (score DESC);

-- 12. App Content (Privacy, Terms, Contact)
CREATE TABLE IF NOT EXISTS app_content (
    id TEXT PRIMARY KEY,                       -- 'privacy' | 'terms' | 'contact'
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    updated_at INTEGER NOT NULL,
    updated_by TEXT NOT NULL DEFAULT 'admin',
    support_email TEXT DEFAULT '',
    phone TEXT DEFAULT '',
    website TEXT DEFAULT '',
    address TEXT DEFAULT ''
);

-- 13. Home Promotional Banners
CREATE TABLE IF NOT EXISTS home_banners (
    id TEXT PRIMARY KEY,
    image_url TEXT NOT NULL,                   -- Supabase Storage Public URL or base64
    storage_path TEXT DEFAULT '',              -- Supabase object path (e.g. banners/{id}.jpg)
    order_index INTEGER NOT NULL DEFAULT 0,
    uploaded_at INTEGER NOT NULL,
    uploaded_by TEXT NOT NULL DEFAULT '',
    active INTEGER NOT NULL DEFAULT 1
);
CREATE INDEX IF NOT EXISTS idx_home_banners_order ON home_banners (order_index ASC);

-- 14. Notifications / Broadcast Messages
CREATE TABLE IF NOT EXISTS notifications (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    message TEXT NOT NULL,
    sent_at INTEGER NOT NULL,
    sent_by TEXT NOT NULL DEFAULT '',
    type TEXT NOT NULL DEFAULT 'general'
);
CREATE INDEX IF NOT EXISTS idx_notifications_sent_at ON notifications (sent_at DESC);

-- 15. Community Polls
CREATE TABLE IF NOT EXISTS polls (
    id TEXT PRIMARY KEY,
    question TEXT NOT NULL,
    options_json TEXT NOT NULL,                -- JSON string array, e.g. ["A", "B", "C"]
    created_at INTEGER NOT NULL,
    ends_at INTEGER NOT NULL DEFAULT 0,
    active INTEGER NOT NULL DEFAULT 1,
    created_by TEXT NOT NULL DEFAULT ''
);
CREATE INDEX IF NOT EXISTS idx_polls_active_created ON polls (active, created_at DESC);

-- 16. Poll Votes (subcollection polls/{pollId}/votes/{uid})
CREATE TABLE IF NOT EXISTS poll_votes (
    poll_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    option_index INTEGER NOT NULL,
    voted_at INTEGER NOT NULL,
    PRIMARY KEY (poll_id, user_id),
    FOREIGN KEY (poll_id) REFERENCES polls (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_poll_votes_poll ON poll_votes (poll_id);

-- 17. Feedback Messages (Private student-to-admin)
CREATE TABLE IF NOT EXISTS feedback_messages (
    id TEXT PRIMARY KEY,
    message TEXT NOT NULL,
    user_id TEXT NOT NULL,
    user_name TEXT NOT NULL DEFAULT 'Student',
    user_email TEXT NOT NULL DEFAULT '',
    timestamp INTEGER NOT NULL,
    read INTEGER NOT NULL DEFAULT 0,
    post_id TEXT DEFAULT NULL,
    post_title TEXT DEFAULT NULL
);
CREATE INDEX IF NOT EXISTS idx_feedback_messages_time ON feedback_messages (timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_feedback_messages_user ON feedback_messages (user_id);

-- 18. Feedback Discussion Posts (Admin announcements)
CREATE TABLE IF NOT EXISTS feedback_posts (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    message TEXT NOT NULL,
    author_id TEXT NOT NULL,
    author_email TEXT NOT NULL,
    timestamp INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_feedback_posts_time ON feedback_posts (timestamp DESC);

-- 19. Feedback Discussion Replies (subcollection feedback_posts/{postId}/replies)
CREATE TABLE IF NOT EXISTS feedback_post_replies (
    id TEXT PRIMARY KEY,
    post_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    name TEXT NOT NULL DEFAULT 'Student',
    email TEXT NOT NULL DEFAULT '',
    text TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    read INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (post_id) REFERENCES feedback_posts (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_post_replies_post ON feedback_post_replies (post_id, timestamp DESC);

-- 20. AI Generated Tests (draft / live / rejected)
CREATE TABLE IF NOT EXISTS generated_tests (
    id TEXT PRIMARY KEY,
    exam_id TEXT NOT NULL,
    exam_name TEXT NOT NULL,
    test_number TEXT NOT NULL DEFAULT 'Test 1',
    title TEXT NOT NULL,
    generated_at INTEGER NOT NULL,
    status TEXT NOT NULL DEFAULT 'paused',     -- 'paused' | 'live' | 'rejected'
    question_count INTEGER NOT NULL DEFAULT 0,
    syllabus_used TEXT DEFAULT '',
    prompt_used TEXT DEFAULT '',
    questions_json TEXT NOT NULL DEFAULT '[]', -- JSON array of generated questions
    FOREIGN KEY (exam_id) REFERENCES exams (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_generated_tests_exam ON generated_tests (exam_id, generated_at DESC);
CREATE INDEX IF NOT EXISTS idx_generated_tests_status ON generated_tests (status);

-- 21. Admin Analytics: Pre-aggregated Exam Metrics
CREATE TABLE IF NOT EXISTS admin_analytics_exams (
    exam_id TEXT PRIMARY KEY,
    exam_name TEXT NOT NULL,
    category TEXT NOT NULL DEFAULT '',
    attempt_count INTEGER NOT NULL DEFAULT 0,
    unique_users INTEGER NOT NULL DEFAULT 0,
    last_attempt_at INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_analytics_exams_attempts ON admin_analytics_exams (attempt_count DESC);

-- 22. Admin Analytics: Pre-aggregated Question Metrics
CREATE TABLE IF NOT EXISTS admin_analytics_questions (
    id TEXT PRIMARY KEY,                       -- '${exam_id}_${question_id}'
    exam_id TEXT NOT NULL,
    exam_name TEXT NOT NULL,
    question_id TEXT NOT NULL,
    question_number INTEGER NOT NULL DEFAULT 0,
    question_text TEXT NOT NULL DEFAULT '',
    topic TEXT NOT NULL DEFAULT '',
    attempts INTEGER NOT NULL DEFAULT 0,
    correct INTEGER NOT NULL DEFAULT 0,
    wrong INTEGER NOT NULL DEFAULT 0,
    unattempted INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_analytics_questions_exam ON admin_analytics_questions (exam_id);

-- 23. Admin Analytics: Unique User Exam Markers (Deduplication)
CREATE TABLE IF NOT EXISTS admin_analytics_exam_users (
    id TEXT PRIMARY KEY,                       -- '${exam_id}_${user_id}'
    exam_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    created_at INTEGER NOT NULL
);

-- 24. AI Generation Logs (Cron Trigger & On-Demand Runs)
CREATE TABLE IF NOT EXISTS generation_logs (
    id TEXT PRIMARY KEY,
    exam_id TEXT NOT NULL,
    exam_name TEXT NOT NULL,
    status TEXT NOT NULL,                      -- 'success' | 'failed' | 'skipped'
    message TEXT DEFAULT '',
    timestamp INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_gen_logs_exam ON generation_logs (exam_id, timestamp DESC);

-- 25. Rate Limiting Table (per-IP / per-UID window counters)
CREATE TABLE IF NOT EXISTS rate_limits (
    key TEXT PRIMARY KEY,                      -- 'ip:1.2.3.4' or 'uid:xyz'
    count INTEGER NOT NULL DEFAULT 0,
    reset_at INTEGER NOT NULL
);
