import { generateQuestions, GeminiProviderError, getIstTimeAndDate, incrementTestNumber } from '../ai/generator';
import { resolvePublishStatus } from '../util/publishMode';
import { AuthUser, Env, ExamRow } from '../types';
import { prepareAdminAuditLog } from '../util/auditLog';

export class GenerationBusyError extends Error {}

/** Manual and scheduled generation share one database lease and an atomic publish. */
export async function runGeneration(env: Env, exam: ExamRow, count: number, testNumber: string, prompt: string, source: string, requestKey?: string, auditActor?: AuthUser) {
  const db = env.DB;
  if (!Number.isInteger(count) || count < 1 || count > 200) throw new Error('Invalid question count');
  const now = Date.now();
  // Expire stale jobs before a repeated request key can short-circuit recovery.
  await db.prepare("UPDATE generation_jobs SET status = 'failed', completed_at = ?, error_category = 'TIMEOUT', failed_count = requested_count, retryable = 1 WHERE status = 'running' AND started_at < ?").bind(now, now - 180_000).run();
  if (requestKey) {
    const previous = await db.prepare('SELECT * FROM generation_jobs WHERE request_key = ?').bind(requestKey).first<any>();
    if (previous?.exam_id !== undefined && previous.exam_id !== exam.id) throw new GenerationBusyError('Request key belongs to a different exam');
    if (previous?.status === 'success') {
      const persisted = await db.prepare("SELECT id, question_count FROM generated_tests WHERE id = ? AND exam_id = ?")
        .bind(previous.test_id, exam.id).first<{ id: string; question_count: number }>();
      if (!persisted || !Number.isInteger(previous.generated_count) || previous.generated_count < 1 ||
          persisted.question_count !== previous.generated_count) {
        throw new GenerationBusyError("Completed generation is not confirmed. Refresh the system monitor.");
      }
      return { testId: persisted.id, questionCount: persisted.question_count, jobId: previous.id };
    }
    if (previous) throw new GenerationBusyError('This generation request was already processed. Refresh the monitor.');
  }
  const id = crypto.randomUUID();
  const lease = await db.prepare("UPDATE exams SET generating_lock_until = ?, last_generation_status = 'running', last_generation_time = ? WHERE id = ? AND (generating_lock_until IS NULL OR generating_lock_until <= ?)").bind(now + 180_000, now, exam.id, now).run();
  if (!lease.meta.changes) throw new GenerationBusyError('Generation already running for this exam');
  try {
    await db.prepare("INSERT INTO generation_jobs (id, exam_id, exam_name, source, requested_count, status, started_at, request_key) VALUES (?, ?, ?, ?, ?, 'running', ?, ?)").bind(id, exam.id, exam.exam_name, source, count, now, requestKey || null).run();
    const questions = await generateQuestions(env, exam.exam_name, exam.syllabus || '', count, prompt);
    if (questions.length !== count) throw new Error("Generated question count was incomplete");
    const testId = crypto.randomUUID();
    const finish = Date.now();
    const nextTestNumber = incrementTestNumber(testNumber);
    const status = await resolvePublishStatus(db, exam);
    // All five possible writes share a single atomic D1 batch. Each step is
    // conditional on the original lease and preceding rows; no lease means
    // no generated test, status transition, log, or phantom admin audit.
    const statements = [
      db.prepare(`INSERT INTO generated_tests
        (id, exam_id, exam_name, test_number, title, generated_at, status, question_count, syllabus_used, prompt_used, questions_json)
        SELECT ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
        WHERE EXISTS (SELECT 1 FROM exams WHERE id = ? AND generating_lock_until = ?)
          AND EXISTS (SELECT 1 FROM generation_jobs WHERE id = ? AND exam_id = ? AND status = 'running')`)
        .bind(testId, exam.id, exam.exam_name, testNumber, `${exam.exam_name} - ${testNumber}`,
          finish, status, count, exam.syllabus || '', prompt, JSON.stringify(questions),
          exam.id, now + 180_000, id, exam.id),
      db.prepare(`UPDATE exams
        SET generating_lock_until = 0, last_generated_date = ?, last_generation_status = 'success',
            last_generation_error = '', last_generation_time = ?, test_number = ?
        WHERE id = ? AND generating_lock_until = ?
          AND EXISTS (SELECT 1 FROM generated_tests WHERE id = ? AND exam_id = ?)`)
        .bind(getIstTimeAndDate().todayDate, finish, nextTestNumber, exam.id, now + 180_000, testId, exam.id),
      db.prepare(`UPDATE generation_jobs
        SET status = 'success', generated_count = ?, completed_at = ?, test_id = ?
        WHERE id = ? AND exam_id = ? AND status = 'running'
          AND EXISTS (SELECT 1 FROM generated_tests WHERE id = ? AND exam_id = ?)`)
        .bind(count, finish, testId, id, exam.id, testId, exam.id),
      db.prepare(`INSERT INTO generation_logs (id, exam_id, exam_name, status, message, timestamp)
        SELECT ?, ?, ?, 'success', ?, ?
        WHERE EXISTS (SELECT 1 FROM generation_jobs WHERE id = ? AND status = 'success' AND test_id = ?)`)
        .bind(id, exam.id, exam.exam_name, `Generated ${count} questions`, finish, id, testId),
    ];
    if (auditActor) {
      const audit = prepareAdminAuditLog(db, auditActor, {
        actionType: "GENERATE_NOW_TRIGGERED",
        description: `Triggered AI generation for exam '${exam.exam_name}' (${exam.id})`,
        targetType: "generated_test",
        targetId: exam.id,
        metadata: { examId: exam.id, examName: exam.exam_name, questionCount: count, testNumber },
        condition: {
          sql: `EXISTS (SELECT 1 FROM generated_tests WHERE id = ? AND exam_id = ?)
                AND EXISTS (SELECT 1 FROM generation_jobs WHERE id = ? AND status = 'success' AND test_id = ?)`,
          bindings: [testId, exam.id, id, testId],
        },
      });
      statements.push(audit.stmt);
    }
    if (typeof db.batch !== 'function') throw new Error("Atomic D1 batch is required for generation");
    const result = await db.batch(statements);
    if (result.length !== statements.length ||
        result.some(row => row?.success !== true || row?.meta?.changes !== 1)) {
      throw new Error("Generated test publication was not fully acknowledged");
    }
    return { testId, questionCount: questions.length, testNumber, nextTestNumber, jobId: id };
  } catch (err) {
    const category = err instanceof GeminiProviderError ? err.code : 'GENERATION_FAILED';
    const retryable = err instanceof GeminiProviderError ? err.isTransient : true;
    // Store only categories, never provider output, prompts or database messages.
    try {
      await db.batch([
        db.prepare("UPDATE exams SET generating_lock_until = 0, last_generation_status = 'failed', last_generation_error = ?, last_generation_time = ? WHERE id = ? AND generating_lock_until = ?").bind(category, Date.now(), exam.id, now + 180_000),
        db.prepare("UPDATE generation_jobs SET status = 'failed', failed_count = requested_count, completed_at = ?, error_category = ?, retryable = ? WHERE id = ?").bind(Date.now(), category, retryable ? 1 : 0, id),
      ]);
    } catch { /* The lease expires even if the database is unavailable. */ }
    throw err;
  }
}
