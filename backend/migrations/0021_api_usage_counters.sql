-- Migration 0021: API Usage Monthly Counters
-- Server-authoritative tracking for Gemini AI calls and monthly activity metrics.

CREATE TABLE IF NOT EXISTS api_usage_monthly (
    year_month TEXT PRIMARY KEY,
    gemini_calls INTEGER NOT NULL DEFAULT 0,
    gemini_successful_calls INTEGER NOT NULL DEFAULT 0,
    gemini_failed_calls INTEGER NOT NULL DEFAULT 0,
    first_recorded_at INTEGER NOT NULL,
    last_updated_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_api_usage_monthly_ym ON api_usage_monthly (year_month);
