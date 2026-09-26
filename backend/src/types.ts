// ============================================================================
// Types & Interfaces for Cloudflare Worker API
// ============================================================================

export interface Env {
  DB: D1Database;
  FIREBASE_PROJECT_ID: string;
  SUPABASE_PROJECT_URL: string;
  SUPABASE_BUCKET_NAME: string;
  SUPABASE_PUBLISHABLE_KEY: string;
  SUPABASE_SERVICE_ROLE_KEY?: string; // Cloudflare secret
  GEMINI_API_KEY?: string;           // Cloudflare secret
  GEMINI_MODEL?: string;
}

export interface AuthUser {
  uid: string;
  email: string;
  displayName: string;
  isAdmin: boolean;
}

export interface ApiResponse<T = any> {
  success: boolean;
  data?: T;
  error?: string;
  message?: string;
}

export interface ExamRow {
  id: string;
  exam_name: string;
  time_limit_minutes: number;
  category: string;
  syllabus: string;
  question_count: number;
  custom_prompt_notes: string;
  auto_generation_enabled: number;
  auto_gen_time: string;
  timezone: string;
  test_number: string;
  syllabus_url: string;
  syllabus_file_name: string;
  generation_prompt: string;
  image_url: string;
  last_generated_date: string;
  last_generation_status: string;
  last_generation_error: string;
  last_generation_time: number;
  generating_lock_until: number;
}

export interface QuestionRow {
  id: string;
  exam_id: string;
  question_text: string;
  option_a: string;
  option_b: string;
  option_c: string;
  option_d: string;
  correct_answer: string;
  explanation: string;
  topic: string;
  is_pyq: number;
  pyq_year: number;
  pyq_paper: string;
  question_text_hi: string;
  option_a_hi: string;
  option_b_hi: string;
  option_c_hi: string;
  option_d_hi: string;
  explanation_hi: string;
}

export interface AttemptRow {
  id: string;
  user_id: string;
  display_name: string;
  exam_id: string;
  exam_name: string;
  category: string;
  score: number;
  total: number;
  correct: number;
  wrong: number;
  unattempted: number;
  timestamp: number;
}

export interface AttemptAnswerRow {
  id: string;
  attempt_id: string;
  question_id: string;
  question_number: number;
  question_text: string;
  selected: string;
  selected_text: string;
  correct: string;
  correct_text: string;
  explanation: string;
  is_bookmarked: number;
  topic: string;
  question_text_hi: string;
  selected_text_hi: string;
  correct_text_hi: string;
  explanation_hi: string;
}

export interface LeaderboardRow {
  id: string;
  user_id: string;
  exam_id: string;
  exam_name: string;
  category: string;
  display_name: string;
  score: number;
  total: number;
  timestamp: number;
}

export interface OverallLeaderboardRow {
  user_id: string;
  display_name: string;
  score: number;
  total_score: number;
  total: number;
  tests_taken: number;
  total_correct: number;
  accuracy: number;
  timestamp: number;
}

export interface NotificationRow {
  id: string;
  title: string;
  message: string;
  sent_at: number;
  sent_by: string;
  type: string;
}

export interface FeedbackMessageRow {
  id: string;
  message: string;
  user_id: string;
  user_name: string;
  user_email: string;
  timestamp: number;
  read: number;
  post_id: string | null;
  post_title: string | null;
}

export interface FeedbackPostRow {
  id: string;
  title: string;
  message: string;
  author_id: string;
  author_email: string;
  timestamp: number;
}

export interface FeedbackReplyRow {
  id: string;
  post_id: string;
  user_id: string;
  name: string;
  email: string;
  text: string;
  timestamp: number;
  read: number;
}

export interface HomeBannerRow {
  id: string;
  image_url: string;
  storage_path: string;
  order_index: number;
  uploaded_at: number;
  uploaded_by: string;
  active: number;
}

export interface PollRow {
  id: string;
  question: string;
  options_json: string;
  created_at: number;
  ends_at: number;
  active: number;
  created_by: string;
}

export interface GeneratedTestRow {
  id: string;
  exam_id: string;
  exam_name: string;
  test_number: string;
  title: string;
  generated_at: number;
  status: string;
  question_count: number;
  syllabus_used: string;
  prompt_used: string;
  questions_json: string;
}

export interface AppContentRow {
  id: string;
  title: string;
  body: string;
  updated_at: number;
  updated_by: string;
  support_email: string;
  phone: string;
  website: string;
  address: string;
}

export interface UserRow {
  id: string;
  email: string;
  display_name: string;
  dob: string;
  category: string;
  created_at: number;
  last_active: number;
  last_updated: number;
  fcm_token: string;
}
