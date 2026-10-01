// ============================================================================
// Phase 4: Scheduled AI Test Generation Cron Handler (Every 5 minutes)
// ============================================================================

import { generateQuestions, getIstTimeAndDate, incrementTestNumber, GeminiProviderError } from "../ai/generator";
import { resolvePublishStatus } from "../util/publishMode";
import { Env, ExamRow } from "../types";

export async function handleScheduledTestGeneration(event: ScheduledEvent, env: Env, ctx: ExecutionContext) {
  const db = env.DB;
  const { todayDate, currentTime } = getIstTimeAndDate();
  const now = Date.now();

  // Cleanup expired rate_limits (> 1 hour old) and attempt_sessions (> 2 days old)
  try {
    const nowSec = Math.floor(now / 1000);
    const twoDaysAgoMs = now - 2 * 86400 * 1000;
    await db.batch([
      db.prepare("DELETE FROM rate_limits WHERE reset_at < ?").bind(nowSec - 3600),
      db.prepare("DELETE FROM attempt_sessions WHERE started_at < ?").bind(twoDaysAgoMs),
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

  let executedCount = 0;
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

    // Acquire lock
    const lockExpiry = now + 10 * 60 * 1000;
    const lockAcquired = await db
      .prepare(
        `UPDATE exams SET
          generating_lock_until = ?,
          last_generation_status = 'running',
          last_generation_time = ?
         WHERE id = ? AND (generating_lock_until IS NULL OR generating_lock_until < ?)`
      )
      .bind(lockExpiry, now, examId, now)
      .run();

    if (!lockAcquired.meta.changes || lockAcquired.meta.changes === 0) {
      console.log(`[Scheduler] Failed to acquire lock for '${examName}'. Skipping.`);
      continue;
    }
    console.log(`[Scheduler] Lock acquired for '${examName}' until ${new Date(lockExpiry).toISOString()}`);

    // Requirement 4e: Stagger execution when multiple exams are scheduled at the same time
    if (executedCount > 0) {
      console.log(`[Scheduler] Staggering next generation for '${examName}' to prevent API rate limits...`);
      await new Promise((resolve) => setTimeout(resolve, 3000));
    }
    executedCount++;

    console.log(`[Scheduler] Exam '${examName}' selected for generation. Calling Gemini (${model})...`);

    const targetCount = Math.max(1, Math.min(200, Number(exam.question_count || 20)));
    const testNumber = exam.test_number || "Test 1";
    const prompt = exam.generation_prompt || exam.custom_prompt_notes || "";

    try {
      const questions = await generateQuestions(
        env,
        examName,
        exam.syllabus || "",
        targetCount,
        prompt
      );

      console.log(
        `[Scheduler] Gemini generation succeeded for '${examName}' (${questions.length} questions received). Inserting test and updating exam metadata...`
      );

      const testId = crypto.randomUUID();
      const title = `${examName} - ${testNumber}`;
      const nextTestNumber = incrementTestNumber(testNumber);
      const finishTime = Date.now();
      const initialStatus = await resolvePublishStatus(db, exam);

      await db.batch([
        db
          .prepare(
            `INSERT INTO generated_tests (id, exam_id, exam_name, test_number, title, generated_at, status, question_count, syllabus_used, prompt_used, questions_json)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
          )
          .bind(
            testId,
            examId,
            examName,
            testNumber,
            title,
            finishTime,
            initialStatus,
            questions.length,
            exam.syllabus || "",
            prompt,
            JSON.stringify(questions)
          ),
        db
          .prepare(
            `UPDATE exams SET
              generating_lock_until = 0,
              last_generated_date = ?,
              last_generation_status = 'success',
              last_generation_error = '',
              last_generation_time = ?,
              test_number = ?
             WHERE id = ?`
          )
          .bind(todayDate, finishTime, nextTestNumber, examId),
        db
          .prepare(
            "INSERT INTO generation_logs (id, exam_id, exam_name, status, message, timestamp) VALUES (?, ?, ?, ?, ?, ?)"
          )
          .bind(
            crypto.randomUUID(),
            examId,
            examName,
            "success",
            `Generated ${questions.length} questions for ${testNumber}`,
            finishTime
          ),
      ]);

      console.log(
        `[Scheduler] Successfully inserted test '${title}' (${questions.length} questions). Exam metadata updated and lock released for '${examName}'.`
      );
    } catch (err: any) {
      const safeMsg = (err instanceof GeminiProviderError ? err.userFacingMessage : err.message) || "Unknown error";
      console.error(`[Scheduler] Generation failed for '${examName}':`, safeMsg);

      const finishTime = Date.now();
      await db.batch([
        db
          .prepare(
            `UPDATE exams SET
              generating_lock_until = 0,
              last_generation_status = 'failed',
              last_generation_error = ?,
              last_generation_time = ?
             WHERE id = ?`
          )
          .bind(safeMsg.slice(0, 200), finishTime, examId),
        db
          .prepare(
            "INSERT INTO generation_logs (id, exam_id, exam_name, status, message, timestamp) VALUES (?, ?, ?, ?, ?, ?)"
          )
          .bind(
            crypto.randomUUID(),
            examId,
            examName,
            "failed",
            safeMsg.slice(0, 200),
            finishTime
          ),
      ]);

      console.log(`[Scheduler] Released lock and recorded failed status for '${examName}'.`);
    }
  }
}
