import { D1Database } from "@cloudflare/workers-types";

/**
 * Server-authoritative API usage and resource tracking.
 * Month boundaries are computed in UTC (from 00:00:00.000 UTC on the 1st of the month
 * to 00:00:00.000 UTC on the 1st of the subsequent month).
 */

export interface MonthlyApiUsageResult {
  yearMonth: string;
  monthDisplayName: string;
  geminiCalls: number;
  geminiSuccessfulCalls: number;
  geminiFailedCalls: number;
  documentsCreated: number;
  testSubmissions: number;
  countedSince: string | null;
  lastUpdated: number;
}

export function getUtcYearMonth(date: Date = new Date()): string {
  const y = date.getUTCFullYear();
  const m = String(date.getUTCMonth() + 1).padStart(2, "0");
  return `${y}-${m}`;
}

const MONTH_NAMES = [
  "January", "February", "March", "April", "May", "June",
  "July", "August", "September", "October", "November", "December"
];

export function getUtcMonthDetails(yearMonth: string): { startMs: number; endMs: number; displayName: string } {
  const match = /^(\d{4})-(0[1-9]|1[0-2])$/.exec(yearMonth);
  if (!match) {
    throw new Error(`Invalid yearMonth format '${yearMonth}'. Expected YYYY-MM`);
  }
  const year = parseInt(match[1], 10);
  const monthIdx = parseInt(match[2], 10) - 1; // 0-based
  const startMs = Date.UTC(year, monthIdx, 1, 0, 0, 0, 0);
  const endMs = Date.UTC(year, monthIdx + 1, 1, 0, 0, 0, 0);
  const displayName = `${MONTH_NAMES[monthIdx]} ${year}`;
  return { startMs, endMs, displayName };
}

export async function trackGeminiCall(db: D1Database, success: boolean, nowMs: number = Date.now()): Promise<void> {
  const ym = getUtcYearMonth(new Date(nowMs));
  const successVal = success ? 1 : 0;
  const failVal = success ? 0 : 1;

  await db.prepare(`
    INSERT INTO api_usage_monthly (
      year_month,
      gemini_calls,
      gemini_successful_calls,
      gemini_failed_calls,
      first_recorded_at,
      last_updated_at
    ) VALUES (?1, 1, ?2, ?3, ?4, ?4)
    ON CONFLICT(year_month) DO UPDATE SET
      gemini_calls = gemini_calls + 1,
      gemini_successful_calls = gemini_successful_calls + ?2,
      gemini_failed_calls = gemini_failed_calls + ?3,
      last_updated_at = ?4
  `).bind(ym, successVal, failVal, nowMs).run();
}

export async function getMonthlyApiUsage(db: D1Database, yearMonth?: string): Promise<MonthlyApiUsageResult> {
  const ym = yearMonth && /^(\d{4})-(0[1-9]|1[0-2])$/.test(yearMonth) ? yearMonth : getUtcYearMonth();
  const { startMs, endMs, displayName } = getUtcMonthDetails(ym);

  // 1. Fetch Gemini calls from api_usage_monthly
  let usageRow: {
    gemini_calls: number;
    gemini_successful_calls: number;
    gemini_failed_calls: number;
    first_recorded_at: number;
    last_updated_at: number;
  } | null = null;

  try {
    usageRow = await db.prepare(
      "SELECT gemini_calls, gemini_successful_calls, gemini_failed_calls, first_recorded_at, last_updated_at FROM api_usage_monthly WHERE year_month = ?"
    ).bind(ym).first<{
      gemini_calls: number;
      gemini_successful_calls: number;
      gemini_failed_calls: number;
      first_recorded_at: number;
      last_updated_at: number;
    }>();
  } catch {}

  // Find overall earliest recorded timestamp to truthfully report countedSince
  let earliestRow: { earliest_ts: number | null } | null = null;
  try {
    earliestRow = await db.prepare(
      "SELECT MIN(first_recorded_at) as earliest_ts FROM api_usage_monthly"
    ).first<{ earliest_ts: number | null }>();
  } catch {}

  // 2. Count real test submissions in attempts table
  let testSubmissions = 0;
  try {
    const attemptsRow = await db.prepare(
      "SELECT COUNT(*) as count FROM attempts WHERE timestamp >= ?1 AND timestamp < ?2"
    ).bind(startMs, endMs).first<{ count: number }>();
    testSubmissions = attemptsRow?.count || 0;
  } catch {}

  // 3. Count documents created:
  // a) admin_audit_log entries representing creation/generation/broadcast
  let auditCount = 0;
  try {
    const auditRow = await db.prepare(`
      SELECT COUNT(*) as count FROM admin_audit_log 
      WHERE timestamp >= ?1 AND timestamp < ?2 
      AND (
        action_type LIKE '%CREATED%' 
        OR action_type LIKE '%SENT%' 
        OR action_type LIKE '%ADDED%' 
        OR action_type LIKE '%GENERATE%'
        OR action_type LIKE '%REPLY%'
        OR action_type LIKE '%INSERT%'
      )
    `).bind(startMs, endMs).first<{ count: number }>();
    auditCount = auditRow?.count || 0;
  } catch {}

  // b) feedback_posts created by users
  let feedbackCount = 0;
  try {
    const feedbackRow = await db.prepare(
      "SELECT COUNT(*) as count FROM feedback_posts WHERE timestamp >= ?1 AND timestamp < ?2"
    ).bind(startMs, endMs).first<{ count: number }>();
    feedbackCount = feedbackRow?.count || 0;
  } catch {}

  const documentsCreated = auditCount + feedbackCount;

  let countedSince: string | null = null;
  if (usageRow?.first_recorded_at) {
    countedSince = new Date(usageRow.first_recorded_at).toISOString();
  } else if (earliestRow?.earliest_ts) {
    if (endMs <= earliestRow.earliest_ts) {
      countedSince = `Not tracked (Tracking started ${new Date(earliestRow.earliest_ts).toISOString().split("T")[0]})`;
    } else {
      countedSince = new Date(earliestRow.earliest_ts).toISOString();
    }
  }

  return {
    yearMonth: ym,
    monthDisplayName: displayName,
    geminiCalls: usageRow?.gemini_calls || 0,
    geminiSuccessfulCalls: usageRow?.gemini_successful_calls || 0,
    geminiFailedCalls: usageRow?.gemini_failed_calls || 0,
    documentsCreated,
    testSubmissions,
    countedSince,
    lastUpdated: usageRow?.last_updated_at || Date.now(),
  };
}
