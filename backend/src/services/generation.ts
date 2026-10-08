import { generateQuestions, GeminiProviderError, getIstTimeAndDate, incrementTestNumber } from '../ai/generator';
import { resolvePublishStatus } from '../util/publishMode';
import { Env, ExamRow } from '../types';

export class GenerationBusyError extends Error {}

/** Manual and scheduled generation share one database lease and an atomic publish. */
export async function runGeneration(env: Env, exam: ExamRow, count: number, testNumber: string, prompt: string, source: string, requestKey?: string) {
  const db = env.DB;
  if (!Number.isInteger(count) || count < 1 || count > 200) throw new Error('Invalid question count');
  const now = Date.now();
  // Expire stale jobs before a repeated request key can short-circuit recovery.
  await db.prepare("UPDATE generation_jobs SET status = 'failed', completed_at = ?, error_category = 'TIMEOUT', failed_count = requested_count, retryable = 1 WHERE status = 'running' AND started_at < ?").bind(now, now - 180_000).run();
  if (requestKey) {
    const previous = await db.prepare('SELECT * FROM generation_jobs WHERE request_key = ?').bind(requestKey).first<any>();
    if (previous?.exam_id !== undefined && previous.exam_id !== exam.id) throw new GenerationBusyError('Request key belongs to a different exam');
    if (previous?.status === 'success') return { testId: previous.test_id, questionCount: previous.generated_count, jobId: previous.id };
    if (previous) throw new GenerationBusyError('This generation request was already processed. Refresh the monitor.');
  }
  const id = crypto.randomUUID();
  const lease = await db.prepare("UPDATE exams SET generating_lock_until = ?, last_generation_status = 'running', last_generation_time = ? WHERE id = ? AND (generating_lock_until IS NULL OR generating_lock_until <= ?)").bind(now + 180_000, now, exam.id, now).run();
  if (!lease.meta.changes) throw new GenerationBusyError('Generation already running for this exam');
  try {
    await db.prepare("INSERT INTO generation_jobs (id, exam_id, exam_name, source, requested_count, status, started_at, request_key) VALUES (?, ?, ?, ?, ?, 'running', ?, ?)").bind(id, exam.id, exam.exam_name, source, count, now, requestKey || null).run();
    const questions = await generateQuestions(env, exam.exam_name, exam.syllabus || '', count, prompt);
    const testId = crypto.randomUUID();
    const finish = Date.now();
    const nextTestNumber = incrementTestNumber(testNumber);
    const status = await resolvePublishStatus(db, exam);
    await db.batch([
      db.prepare(`INSERT INTO generated_tests (id, exam_id, exam_name, test_number, title, generated_at, status, question_count, syllabus_used, prompt_used, questions_json) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`).bind(testId, exam.id, exam.exam_name, testNumber, `${exam.exam_name} - ${testNumber}`, finish, status, count, exam.syllabus || '', prompt, JSON.stringify(questions)),
      db.prepare("UPDATE exams SET generating_lock_until = 0, last_generated_date = ?, last_generation_status = 'success', last_generation_error = '', last_generation_time = ?, test_number = ? WHERE id = ? AND generating_lock_until = ?").bind(getIstTimeAndDate().todayDate, finish, nextTestNumber, exam.id, now + 180_000),
      db.prepare("UPDATE generation_jobs SET status = 'success', generated_count = ?, completed_at = ?, test_id = ? WHERE id = ?").bind(count, finish, testId, id),
      db.prepare("INSERT INTO generation_logs (id, exam_id, exam_name, status, message, timestamp) VALUES (?, ?, ?, 'success', ?, ?)").bind(id, exam.id, exam.exam_name, `Generated ${count} questions`, finish),
    ]);
    return { testId, questionCount: count, testNumber, nextTestNumber, jobId: id };
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
