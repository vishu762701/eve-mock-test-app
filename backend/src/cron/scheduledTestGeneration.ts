// ============================================================================
// Phase 4: Scheduled AI Test Generation Cron Handler (Every 5 minutes)
// ============================================================================

import { runGeneration, GenerationBusyError } from "../services/generation";
import { getIstTimeAndDate, GeminiProviderError } from "../ai/generator";
import { Env, ExamRow } from "../types";

export async function handleScheduledTestGeneration(event: ScheduledEvent, env: Env, ctx: ExecutionContext) {
  const db = env.DB;
  const { todayDate, currentTime } = getIstTimeAndDate();
  const now = Date.now();

  // Cleanup rate limits and bounded monitoring retention; preserve offline attempt sessions
  try {
    const nowSec = Math.floor(now / 1000);
    await db.batch([
      db.prepare("UPDATE generation_jobs SET status = 'failed', completed_at = ?, error_category = 'TIMEOUT', failed_count = requested_count, retryable = 1 WHERE status = 'running' AND started_at < ?").bind(now, now - 180_000),
      db.prepare("UPDATE exams SET generating_lock_until = 0, last_generation_status = 'failed', last_generation_error = 'TIMEOUT', last_generation_time = ? WHERE last_generation_status = 'running' AND generating_lock_until > 0 AND generating_lock_until <= ?").bind(now, now),
      db.prepare("DELETE FROM rate_limits WHERE reset_at < ?").bind(nowSec - 3600),
      db.prepare("DELETE FROM operation_events WHERE timestamp < ?").bind(now - 30 * 86400_000),
      db.prepare("DELETE FROM generation_jobs WHERE status != 'running' AND started_at < ?").bind(now - 30 * 86400_000),
      db.prepare("DELETE FROM generation_logs WHERE timestamp < ?").bind(now - 30 * 86400_000),
    ]);
  } catch (err: any) {
    console.error("[Scheduler] Cleanup failed:", err.message);
  }

  const model = env.GEMINI_MODEL || "gemini-3.5-flash-lite";

  const { results: exams } = await db
    .prepare("SELECT * FROM exams WHERE auto_generation_enabled IS NULL OR auto_generation_enabled != 0")
    .all<ExamRow>();

  if (!exams || exams.length === 0) {
    console.log(`[Scheduler Start] IST: ${todayDate} ${currentTime}, Model: ${model}, Exams Scanned: 0, Eligible: 0`);
    console.log("[Scheduler] No active auto-generation exams found.");
    return;
  }

  const eligibleExams = exams.filter((e) => {
    const autoGenTime = String(e.auto_gen_time || "00:00").trim();
    const lastGenDate = String(e.last_generated_date || "").trim();
    return currentTime >= autoGenTime && lastGenDate !== todayDate;
  });

  console.log(
    `[Scheduler Start] IST: ${todayDate} ${currentTime}, Model: ${model}, Exams Scanned: ${exams.length}, Eligible: ${eligibleExams.length}`
  );

  for (const exam of exams) {
    const examId = exam.id;
    const examName = exam.exam_name || "Mock Test";
    const autoGenTime = String(exam.auto_gen_time || "00:00").trim();
    const lastGenDate = String(exam.last_generated_date || "").trim();

    // Skip auto-generation for any exam that has sub-exams
    const hasSubExams = await db
      .prepare("SELECT 1 FROM exams WHERE parent_exam_id = ? LIMIT 1")
      .bind(examId)
      .first();
    if (hasSubExams) {
      console.log(`[Scheduler] Exam '${examName}' (${examId}) skipped: has sub-exams`);
      continue;
    }

    // Condition 1: Current IST time must be >= scheduled autoGenTime
    if (currentTime < autoGenTime) {
      console.log(`[Scheduler] Exam '${examName}' (${examId}) skipped: time not reached (${currentTime} < ${autoGenTime})`);
      continue;
    }

    // Condition 2: Must not have already generated today
    if (lastGenDate === todayDate) {
      console.log(`[Scheduler] Exam '${examName}' (${examId}) skipped: already generated today (${todayDate})`);
      continue;
    }

    // Condition 3: Lock check for idempotency (10 minute lock)
    if (exam.generating_lock_until && now < exam.generating_lock_until) {
      console.log(`[Scheduler] Exam '${examName}' (${examId}) skipped: locked (generatingLockUntil: ${exam.generating_lock_until})`);
      continue;
    }

    try {
      await runGeneration(env, exam, Number(exam.question_count || 20), exam.test_number || "Test 1", exam.generation_prompt || exam.custom_prompt_notes || "", "scheduled", `scheduled:${exam.id}:${todayDate}`);
    } catch (err) {
      console.warn("Scheduled generation:", err instanceof GenerationBusyError ? "BUSY" : err instanceof GeminiProviderError ? err.code : "GENERATION_FAILED");
    }
  }
}
