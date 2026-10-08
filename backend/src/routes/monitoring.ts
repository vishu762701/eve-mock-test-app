import { Hono } from 'hono';
import { requireAdmin } from '../middleware/authMiddleware';
import { getGeminiApiKeys } from '../ai/generator';
import { runGeneration, GenerationBusyError } from '../services/generation';
import { AuthUser, Env, ExamRow } from '../types';

export const monitoringRoutes = new Hono<{ Bindings: Env; Variables: { user: AuthUser } }>();
monitoringRoutes.use('*', requireAdmin);

monitoringRoutes.get('/overview', async (c) => {
  const totals = await c.env.DB.prepare(`SELECT
    (SELECT COUNT(*) FROM users) AS users,
    (SELECT COUNT(*) FROM exams) AS exams,
    (SELECT COUNT(*) FROM generated_tests WHERE status IN ('live', 'published')) AS publishedTests,
    (SELECT COUNT(*) FROM questions) + (SELECT COALESCE(SUM(question_count), 0) FROM generated_tests) AS questions,
    (SELECT COUNT(*) FROM attempts) AS attempts`).first();
  const { results: activity } = await c.env.DB.prepare('SELECT id, exam_name, timestamp, counted FROM attempts ORDER BY timestamp DESC LIMIT 20').all();
  return c.json({ success: true, data: { totals, activity } });
});

monitoringRoutes.get('/health', async (c) => {
  const checks: Record<string, { status: string; detail: string }> = {
    backend: { status: 'verified', detail: 'Authenticated API request reached the Worker' },
    database: { status: 'unconfigured', detail: 'D1 binding is missing' },
    firebase: { status: 'unconfigured', detail: 'Firebase project is missing' },
    firestore: { status: 'unavailable', detail: 'No authorized Firestore probe is configured in the Worker' },
    messaging: { status: 'unavailable', detail: 'FCM delivery is not tested by this read-only check' },
    media: { status: c.env.SUPABASE_PROJECT_URL && c.env.SUPABASE_BUCKET_NAME && c.env.SUPABASE_SERVICE_ROLE_KEY ? 'configured' : 'unconfigured', detail: 'Media binding configuration only; storage access is not tested' },
    ai: { status: getGeminiApiKeys(c.env).length ? 'configured' : 'unconfigured', detail: 'Configuration check only; use the existing AI connection test to verify provider access' },
  };
  if (c.env.DB) {
    try {
      await c.env.DB.prepare('SELECT 1 AS ok').first();
      // Schema readiness is required as well as connectivity.
      await c.env.DB.prepare('SELECT id FROM generation_jobs LIMIT 1').first();
      await c.env.DB.prepare('SELECT id FROM operation_events LIMIT 1').first();
      await c.env.DB.prepare('SELECT questions_json, negative_marking_value, session_instance_id FROM attempt_sessions LIMIT 1').first();
      checks.database = { status: 'verified', detail: 'D1 query and monitoring schema are available' };
    } catch { checks.database = { status: 'degraded', detail: 'D1 query or required migration failed' }; }
  }
  if (c.env.FIREBASE_PROJECT_ID) {
    try {
      const response = await fetch('https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com', { signal: AbortSignal.timeout(5000) });
      const body = await response.json() as { keys?: unknown[] };
      checks.firebase = response.ok && Array.isArray(body.keys) && body.keys.length > 0
        ? { status: 'verified', detail: 'Firebase Auth public signing keys are reachable; Firestore and FCM are not tested by this check' }
        : { status: 'degraded', detail: 'Firebase Auth signing key check failed' };
    } catch { checks.firebase = { status: 'unavailable', detail: 'Firebase Auth signing key check timed out or failed' }; }
  }
  return c.json({ success: true, data: checks });
});

// Bounded pages. No user IDs, answer payloads, tokens or provider raw messages.
monitoringRoutes.get('/operations', async (c) => {
  const offset = Number(c.req.query('offset') || 0);
  if (!Number.isInteger(offset) || offset < 0 || offset > 10000) return c.json({ success: false, error: 'Invalid page offset' }, 400);
  const db = c.env.DB;
  const [jobs, failures, submissions] = await Promise.all([
    db.prepare('SELECT * FROM generation_jobs ORDER BY started_at DESC, id DESC LIMIT 25 OFFSET ?').bind(offset).all(),
    db.prepare('SELECT * FROM operation_events ORDER BY timestamp DESC, id DESC LIMIT 25 OFFSET ?').bind(offset).all(),
    db.prepare('SELECT id, exam_name, timestamp, counted FROM attempts ORDER BY timestamp DESC, id DESC LIMIT 25 OFFSET ?').bind(offset).all(),
  ]);
  return c.json({ success: true, data: { jobs: jobs.results || [], failures: failures.results || [], submissions: submissions.results || [], nextOffset: [jobs, failures, submissions].some((r) => r.results?.length === 25) && offset < 10000 ? offset + 25 : null } });
});

monitoringRoutes.post('/jobs/:id/retry', async (c) => {
  const job = await c.env.DB.prepare('SELECT * FROM generation_jobs WHERE id = ?').bind(c.req.param('id')).first<any>();
  if (!job) return c.json({ success: false, error: 'Generation job not found' }, 404);
  if (job.status !== 'failed' || !job.retryable) return c.json({ success: false, error: 'This job is not retryable' }, 409);
  const exam = await c.env.DB.prepare('SELECT * FROM exams WHERE id = ?').bind(job.exam_id).first<ExamRow>();
  if (!exam) return c.json({ success: false, error: 'Exam no longer exists' }, 404);
  try {
    // A retry uses current syllabus/settings and the failed job's requested count.
    const data = await runGeneration(c.env, exam, job.requested_count, exam.test_number || 'Test 1', exam.generation_prompt || exam.custom_prompt_notes || '', 'retry', `retry:${job.id}`);
    return c.json({ success: true, data });
  } catch (err) {
    return c.json({ success: false, error: err instanceof GenerationBusyError ? err.message : 'Generation retry failed; refresh operations for the safe category' }, err instanceof GenerationBusyError ? 409 : 502);
  }
});
