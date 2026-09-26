// ============================================================================
// Phase 4: Scheduled AI Test Generation Cron Handler (Every 5 minutes)
// ============================================================================

import { generateQuestions, getIstTimeAndDate, incrementTestNumber } from "../ai/generator";
import { Env, ExamRow } from "../types";

export async function handleScheduledTestGeneration(event: ScheduledEvent, env: Env, ctx: ExecutionContext) {
  const db = env.DB;
  const { todayDate, currentTime } = getIstTimeAndDate();
  const now = Date.now();

  console.log(`[Scheduler Start] IST: ${todayDate} ${currentTime}, checking exams...`);

  const { results: exams } = await db
    .prepare("SELECT * FROM exams WHERE auto_generation_enabled = 1")
    .all<ExamRow>();

  if (!exams || exams.length === 0) {
    console.log("[Scheduler] No active auto-generation exams found.");
    return;
  }

  for (const exam of exams) {
    const examId = exam.id;
    const examName = exam.exam_name || "Mock Test";
    const autoGenTime = String(exam.auto_gen_time || "00:00").trim();
    const lastGenDate = String(exam.last_generated_date || "").trim();

    // Condition 1: Current IST time must be >= scheduled autoGenTime
    if (currentTime < autoGenTime) {
      continue;
    }

    // Condition 2: Must not have already generated today
    if (lastGenDate === todayDate) {
      continue;
    }

    // Condition 3: Lock check for idempotency (10 minute lock)
    if (exam.generating_lock_until && now < exam.generating_lock_until) {
      console.log(`[Scheduler] Exam '${examName}' is currently locked. Skipping.`);
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

    console.log(`[Scheduler] Starting AI test generation for '${examName}' scheduled at ${autoGenTime}...`);

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

      const testId = crypto.randomUUID();
      const title = `${examName} - ${testNumber}`;
      const nextTestNumber = incrementTestNumber(testNumber);
      const finishTime = Date.now();

      await db.batch([
        db
          .prepare(
            `INSERT INTO generated_tests (id, exam_id, exam_name, test_number, title, generated_at, status, question_count, syllabus_used, prompt_used, questions_json)
             VALUES (?, ?, ?, ?, ?, ?, 'paused', ?, ?, ?, ?)`
          )
          .bind(
            testId,
            examId,
            examName,
            testNumber,
            title,
            finishTime,
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

      console.log(`[Scheduler] Successfully generated test for '${examName}' (${questions.length} questions).`);
    } catch (err: any) {
      console.error(`[Scheduler] Generation failed for '${examName}':`, err.message);

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
          .bind(String(err.message || "Unknown error").slice(0, 200), finishTime, examId),
        db
          .prepare(
            "INSERT INTO generation_logs (id, exam_id, exam_name, status, message, timestamp) VALUES (?, ?, ?, ?, ?, ?)"
          )
          .bind(
            crypto.randomUUID(),
            examId,
            examName,
            "failed",
            String(err.message || "Unknown error").slice(0, 200),
            finishTime
          ),
      ]);
    }
  }
}
