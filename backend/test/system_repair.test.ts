import test from 'node:test';
import assert from 'node:assert/strict';
import { DatabaseSync } from 'node:sqlite';
import { readFileSync, readdirSync } from 'node:fs';
import { Hono } from 'hono';
import { parseAndValidateQuestions, generateQuestions } from '../src/ai/generator';
import { generatedTestRoutes } from '../src/routes/generatedTests';
import { attemptRoutes } from '../src/routes/attempts';
import { adminRoutes } from '../src/routes/admin';
import { questionRoutes } from '../src/routes/questions';
import { monitoringRoutes } from '../src/routes/monitoring';
import { operationMonitor } from '../src/middleware/operationMonitor';
import { runGeneration } from '../src/services/generation';
import { handleScheduledTestGeneration } from '../src/cron/scheduledTestGeneration';
import worker from '../src/index';

// Execute production route SQL on SQLite, including D1's transactional batch semantics.
class LocalD1 {
  sqlite = new DatabaseSync(':memory:');
  failPattern = '';
  queries = 0;
  batchTail: Promise<void> = Promise.resolve();
  constructor() {
    this.sqlite.exec('PRAGMA foreign_keys = ON');
    for (const name of readdirSync(new URL('../migrations/', import.meta.url)).filter((x) => x.endsWith('.sql')).sort()) this.sqlite.exec(readFileSync(new URL(`../migrations/${name}`, import.meta.url), 'utf8'));
    this.sqlite.exec("INSERT INTO users (id, email, display_name, created_at, last_active) VALUES ('u', 'student@example.com', 'Student', 1, 1)");
    this.sqlite.exec("INSERT INTO exams (id, exam_name, category, time_limit_minutes, question_count) VALUES ('exam', 'Exam', 'General', 10, 3)");
  }
  prepare(sql: string) {
    this.queries++;
    const db = this;
    return { params: [] as any[], bind(...args: any[]) { this.params = args; return this; },
      async first() { await db.batchTail; return db.sqlite.prepare(sql).get(...this.params) ?? null; },
      async all() { await db.batchTail; return { results: db.sqlite.prepare(sql).all(...this.params), success: true }; },
      async run() { await db.batchTail; return this.execute(); },
      execute() { if (db.failPattern && sql.includes(db.failPattern)) throw new Error('Injected database failure'); const result = db.sqlite.prepare(sql).run(...this.params); return { success: true, meta: { changes: Number(result.changes) } }; },
    };
  }
  async batch(statements: any[]) {
    const previous = this.batchTail; let unlock!: () => void;
    this.batchTail = new Promise<void>((resolve) => { unlock = resolve; });
    await previous;
    this.sqlite.exec('BEGIN');
    try { const result = []; for (const statement of statements) result.push(statement.execute()); this.sqlite.exec('COMMIT'); return result; }
    catch (e) { this.sqlite.exec('ROLLBACK'); throw e; }
    finally { unlock(); }
  }
  count(table: string) { return Number(this.sqlite.prepare(`SELECT COUNT(*) AS n FROM ${table}`).get()!.n); }
}
const q = (i = 0) => ({ questionText: `Question ${i}`, optionA: 'one', optionB: 'two', optionC: 'three', optionD: 'four', correctAnswer: 'B', explanation: 'Two is correct.' });
const envFor = (DB: LocalD1) => ({ DB, GEMINI_API_KEY: 'test-only-key', GEMINI_MODEL: 'test-model', GEMINI_FALLBACK_MODELS: 'test-model', FIREBASE_PROJECT_ID: 'test-project' }) as any;
function harness(db: LocalD1, admin = false) {
  const app = new Hono();
  app.use('*', operationMonitor);
  app.use('*', async (c, next) => { c.set('user', { uid: 'u', email: 'student@example.com', displayName: 'Student', isAdmin: admin }); await next(); });
  app.route('/api/questions', questionRoutes as any); app.route('/api/attempts', attemptRoutes as any); app.route('/api/generated-tests', generatedTestRoutes as any); app.route('/api/admin/system', monitoringRoutes as any); app.route('/api/admin', adminRoutes as any);
  app.onError((_e, c) => c.json({ success: false, error: 'Database operation failed' }, 500));
  return (path: string, body?: any, method?: string) => app.request(path, { method: method || (body ? 'POST' : 'GET'), headers: { 'Content-Type': 'application/json', 'X-Eve-Client': '2' }, body: body ? JSON.stringify(body) : undefined }, envFor(db));
}
function seed(db: LocalD1, n = 3, status = 'published') {
  db.sqlite.prepare('INSERT INTO generated_tests (id, exam_id, exam_name, test_number, title, generated_at, status, question_count, questions_json) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)').run('test', 'exam', 'Exam', 'Test 1', 'Exam Test 1', Date.now(), status, n, JSON.stringify(Array.from({ length: n }, (_, i) => q(i))));
}
const picks = (n = 3) => ({ examId: 'exam__test', clientAttemptId: 'attempt-one', answers: Array.from({ length: n }, (_, i) => ({ questionId: `test_${i}`, selected: i === 0 ? 'B' : i === 1 ? 'A' : '', number: i + 1, timeTakenSeconds: 1 })) });
const ai = (questions: any[]) => new Response(JSON.stringify({ candidates: [{ content: { parts: [{ text: JSON.stringify(questions) }] } }] }), { status: 200 });

for (const [label, value] of [['malformed', 'broken'], ['empty', '[]'], ['invalid answer', JSON.stringify([{ ...q(), correctAnswer: '9' }])], ['duplicates', JSON.stringify([q(), q()])], ['missing fields', '[{"questionText":"Missing"}]'], ['duplicate options', JSON.stringify([{ ...q(), optionB: 'one' }])]]) test(`real parser rejects ${label}`, () => assert.throws(() => parseAndValidateQuestions(value)));
test('real parser accepts fences and recognized answer aliases', () => assert.equal(parseAndValidateQuestions('```json\n' + JSON.stringify([{ ...q(), correctAnswer: '2' }]) + '\n```')[0].correctAnswer, 'B'));
test('generation completes more than three chunks and attaches abort deadline', async () => {
  let i = 0;
  assert.equal((await generateQuestions(envFor(new LocalD1()), 'Exam', '', 40, '', async (_url, options) => { assert.ok(options?.signal); return ai(Array.from({ length: 10 }, () => q(i++))); })).length, 40);
});
test('partial and repeated AI output never publish incomplete tests', async () => assert.rejects(generateQuestions(envFor(new LocalD1()), 'Exam', '', 20, '', async () => ai([q()])), /No test was published/));
test('network timeout produces retryable provider error', async () => assert.rejects(generateQuestions(envFor(new LocalD1()), 'Exam', '', 1, '', async (_url, options) => { assert.ok(options?.signal); throw new DOMException('timeout', 'TimeoutError'); }), (e: any) => e.code === 'NETWORK_ERROR' && e.isTransient));
test('generation database failure rolls back publication and tracks failure', async () => {
  const db = new LocalD1(); db.failPattern = 'INSERT INTO generated_tests'; const original = globalThis.fetch; globalThis.fetch = async () => ai([q()]);
  try { await assert.rejects(runGeneration(envFor(db), { id: 'exam', exam_name: 'Exam' } as any, 1, 'Test 1', '', 'manual')); } finally { globalThis.fetch = original; }
  assert.equal(db.count('generated_tests'), 0); assert.equal(db.sqlite.prepare('SELECT status FROM generation_jobs').get()!.status, 'failed');
});
test('generation validates counts, deduplicates request keys and rejects concurrent lease', async () => {
  const db = new LocalD1(); const request = harness(db, true);
  assert.equal((await request('/api/generated-tests/generate-now', { examId: 'exam', questionCount: 'bad' })).status, 400);
  const original = globalThis.fetch; globalThis.fetch = async () => ai([q()]);
  try {
    const body = { examId: 'exam', questionCount: 1, requestId: 'generate-one' };
    const a = await (await request('/api/generated-tests/generate-now', body)).json(); const b = await (await request('/api/generated-tests/generate-now', body)).json();
    assert.equal(a.success, true); assert.equal(a.data.testId, b.data.testId);
    db.sqlite.exec("UPDATE exams SET generating_lock_until = 9999999999999 WHERE id = 'exam'");
    assert.equal((await request('/api/generated-tests/generate-now', { examId: 'exam', questionCount: 1 })).status, 409);
  } finally { globalThis.fetch = original; }
  assert.equal(db.count('generated_tests'), 1);
});
test('students cannot generate or use admin monitors; unauthenticated submit rejected', async () => {
  const db = new LocalD1(); const request = harness(db);
  for (const name of ['overview', 'health', 'operations']) assert.equal((await request(`/api/admin/system/${name}`)).status, 403);
  assert.equal((await request('/api/generated-tests/generate-now', { examId: 'exam' })).status, 403);
  assert.equal((await worker.fetch(new Request('https://example.test/api/attempts/submit', { method: 'POST' }), envFor(db), {} as any)).status, 401);
});
test('manual and timer expiry use server scores that match history', async () => {
  for (const expired of [false, true]) {
    const db = new LocalD1(); seed(db); db.sqlite.exec("UPDATE exams SET negative_marking_value = 0.25 WHERE id = 'exam'"); const request = harness(db);
    assert.equal((await request('/api/attempts/start', { examId: 'exam__test' })).status, 200);
    if (expired) db.sqlite.exec('UPDATE attempt_sessions SET last_resumed_at = last_resumed_at - 601000');
    const response = await request('/api/attempts/submit', { ...picks(), score: 99999 }); assert.equal(response.status, 200);
    const result = (await response.json()).data;
    assert.equal(result.score, 0.75); assert.equal(result.correct, 1); assert.equal(result.wrong, 1); assert.equal(result.unattempted, 1); assert.equal(result.counted, 1);
    const history = (await (await request('/api/attempts')).json()).data;
    assert.equal(history[0].id, result.attemptId); assert.equal(history[0].score, result.score); assert.equal(history[0].answers.length, result.total);
  }
});
test('racing retries and lost responses award exactly one result', async () => {
  const db = new LocalD1(); seed(db); const request = harness(db); await request('/api/attempts/start', { examId: 'exam__test' });
  const results = await Promise.all([request('/api/attempts/submit', picks()), request('/api/attempts/submit', picks())]); const data = await Promise.all(results.map((r) => r.json()));
  assert.ok(data.every((r) => r.success)); assert.equal(data[0].data.attemptId, data[1].data.attemptId);
  assert.equal((await request('/api/attempts/submit', picks())).status, 200);
  assert.equal(db.count('attempts'), 1); assert.equal(db.count('attempt_answers'), 3); assert.equal(db.sqlite.prepare('SELECT tests_taken FROM overall_leaderboard').get()!.tests_taken, 1);
});
test('database failure on a 60-question test rolls back all results and permits retry', async () => {
  const db = new LocalD1(); seed(db, 60); const request = harness(db); await request('/api/attempts/start', { examId: 'exam__test' }); db.failPattern = 'INSERT INTO overall_leaderboard';
  assert.equal((await request('/api/attempts/submit', picks(60))).status, 500);
  for (const table of ['attempts', 'attempt_answers', 'leaderboard']) assert.equal(db.count(table), 0);
  assert.equal(db.count('attempt_sessions'), 1); db.failPattern = '';
  assert.equal((await request('/api/attempts/submit', picks(60))).status, 200); assert.equal(db.count('attempt_answers'), 60);
});
test('invalid/duplicate/foreign answers and attempt IDs from another exam rejected', async () => {
  const db = new LocalD1(); seed(db); const request = harness(db);
  for (const answers of [[{ questionId: 'other', selected: 'A' }], [{ questionId: 'test_0', selected: 'X' }], [{ questionId: 'test_0', selected: 'A' }, { questionId: 'test_0', selected: 'B' }]]) assert.equal((await request('/api/attempts/submit', { ...picks(), answers })).status, 400);
  await request('/api/attempts/start', { examId: 'exam__test' });
  await request('/api/attempts/submit', picks()); assert.equal((await request('/api/attempts/submit', { ...picks(), examId: 'exam' })).status, 409);
});
test('student details hide answer keys; invalid stored questions cannot publish', async () => {
  const db = new LocalD1(); seed(db); const student = harness(db); const admin = harness(db, true);
  const data = (await (await student('/api/generated-tests/test')).json()).data; assert.equal(data.questions[0].correctAnswer, ''); assert.equal(data.questions[0].explanation, '');
  db.sqlite.prepare("UPDATE generated_tests SET questions_json = ?, status = 'paused' WHERE id = 'test'").run(JSON.stringify([{ ...q(), correctAnswer: '9' }]));
  assert.equal((await admin('/api/generated-tests/test/status', { status: 'published' }, 'PUT')).status, 400);
  assert.equal((await student('/api/attempts/start', { examId: 'test' })).status, 403);
});
test('dashboard returns real aggregates, submission activity, safe failures and bounded pages', async () => {
  const db = new LocalD1(); seed(db); const student = harness(db); const admin = harness(db, true);
  await student('/api/attempts/start', { examId: 'exam__test' });
  await student('/api/attempts/submit', picks()); await student('/api/attempts/submit', { ...picks(), clientAttemptId: 'bad', answers: [{ questionId: 'wrong', selected: 'A', token: 'private-token' }] });
  const overview = (await (await admin('/api/admin/system/overview')).json()).data;
  assert.equal(overview.totals.attempts, 1); assert.equal(overview.totals.questions, 3); assert.equal(overview.activity.length, 1);
  const result = await (await admin('/api/admin/system/operations')).json(); assert.equal(result.data.submissions.length, 1); assert.equal(result.data.failures[0].category, 'VALIDATION');
  assert.ok(!JSON.stringify(result).includes('private-token')); assert.ok(!JSON.stringify(result).includes('selected'));
  assert.equal((await admin('/api/admin/system/operations?offset=-1')).status, 400);
});
test('health distinguishes schema failure, unavailable Firebase and configured-only AI', async () => {
  const db = new LocalD1(); const admin = harness(db, true); db.sqlite.exec('DROP TABLE generation_jobs'); const original = globalThis.fetch; globalThis.fetch = async () => { throw new Error('offline'); };
  try { const data = (await (await admin('/api/admin/system/health')).json()).data; assert.equal(data.database.status, 'degraded'); assert.equal(data.firebase.status, 'unavailable'); assert.equal(data.ai.status, 'configured'); } finally { globalThis.fetch = original; }
});

test('queued submission cannot be reassigned by an account switch', async () => {
  const db = new LocalD1(); seed(db); const request = harness(db);
  assert.equal((await request('/api/attempts/submit', { ...picks(), expectedUid: 'other-student' })).status, 403);
  assert.equal(db.count('attempts'), 0);
});
test('generation monitor shows committed counts and only retries retryable jobs', async () => {
  const db = new LocalD1(); const request = harness(db, true); const original = globalThis.fetch; globalThis.fetch = async () => ai([q()]);
  try {
    await runGeneration(envFor(db), { id: 'exam', exam_name: 'Exam' } as any, 1, 'Test 1', '', 'manual');
    const jobs = (await (await request('/api/admin/system/operations')).json()).data.jobs;
    assert.equal(jobs[0].status, 'success'); assert.equal(jobs[0].generated_count, 1); assert.ok(jobs[0].completed_at >= jobs[0].started_at);
    assert.equal((await request(`/api/admin/system/jobs/${jobs[0].id}/retry`, {})).status, 409);
    db.sqlite.exec("UPDATE generation_jobs SET status = 'failed', retryable = 1, failed_count = 1");
    assert.equal((await request(`/api/admin/system/jobs/${jobs[0].id}/retry`, {})).status, 200);
    assert.equal(db.count('generation_jobs'), 2);
  } finally { globalThis.fetch = original; }
});

test('real Firebase signature verification rejects expired/forged JWTs and unverified admin email', async () => {
  const { verifyFirebaseIdToken } = await import('../src/auth');
  const pair = await crypto.subtle.generateKey({ name: 'RSASSA-PKCS1-v1_5', modulusLength: 2048, publicExponent: new Uint8Array([1, 0, 1]), hash: 'SHA-256' }, true, ['sign', 'verify']);
  const jwk = { ...await crypto.subtle.exportKey('jwk', pair.publicKey), kid: 'test-key', alg: 'RS256', use: 'sig' };
  const original = globalThis.fetch;
  globalThis.fetch = async () => new Response(JSON.stringify({ keys: [jwk] }), { headers: { 'Cache-Control': 'max-age=300' } });
  const token = async (changes: object = {}) => {
    const header = Buffer.from(JSON.stringify({ alg: 'RS256', kid: 'test-key' })).toString('base64url');
    const payload = Buffer.from(JSON.stringify({ iss: 'https://securetoken.google.com/test-project', aud: 'test-project', sub: 'u', exp: Math.floor(Date.now() / 1000) + 3600, email: 'anyqueairdrop@gmail.com', email_verified: true, ...changes })).toString('base64url');
    const signature = await crypto.subtle.sign('RSASSA-PKCS1-v1_5', pair.privateKey, new TextEncoder().encode(`${header}.${payload}`));
    return `${header}.${payload}.${Buffer.from(signature).toString('base64url')}`;
  };
  try {
    assert.equal((await verifyFirebaseIdToken(await token(), 'test-project')).isAdmin, true);
    assert.equal((await verifyFirebaseIdToken(await token({ email_verified: false }), 'test-project')).isAdmin, false);
    await assert.rejects(verifyFirebaseIdToken(await token({ exp: 1 }), 'test-project'), /expired/i);
    await assert.rejects(verifyFirebaseIdToken(await token({ exp: 'not-a-time' }), 'test-project'), /expired/i);
    await assert.rejects(verifyFirebaseIdToken(await token({ aud: 'another-project' }), 'test-project'), /audience/i);
    await assert.rejects(verifyFirebaseIdToken((await token()).slice(0, -10) + 'tampered', 'test-project'), /signature/i);
    const db = new LocalD1(); db.sqlite.exec('ALTER TABLE users ADD COLUMN disabled INTEGER DEFAULT 0'); db.sqlite.exec("UPDATE users SET disabled = 1 WHERE id = 'u'");
    const response = await worker.fetch(new Request('https://example.test/api/auth/me', { headers: { Authorization: `Bearer ${await token()}` } }), envFor(db), {} as any);
    assert.equal(response.status, 403);
  } finally { globalThis.fetch = original; }
});

test('200-question submission stays below free-tier D1 query limit and updates analytics', async () => {
  const db = new LocalD1(); seed(db, 200); const request = harness(db);
  await request('/api/attempts/start', { examId: 'exam__test' }); db.queries = 0;
  assert.equal((await request('/api/attempts/submit', picks(200))).status, 200);
  assert.ok(db.queries < 30, `Submission issued ${db.queries} prepared queries`);
  assert.equal(db.count('attempt_answers'), 200); assert.equal(db.count('admin_analytics_questions'), 200);
  const analytics = (await (await harness(db, true)('/api/admin/analytics/exams')).json()).data;
  assert.equal(analytics[0].attemptCount, 1); assert.equal(analytics[0].uniqueUsers, 1);
});
test('history remains readable past 100 attempts without exceeding D1 bind limits', async () => {
  const db = new LocalD1();
  const insert = db.sqlite.prepare('INSERT INTO attempts (id, user_id, exam_id, exam_name, category, score, total, correct, wrong, unattempted, timestamp, client_attempt_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)');
  for (let i = 0; i < 110; i++) insert.run(`a${i}`, 'u', 'exam', 'Exam', 'General', 0, 0, 0, 0, 0, i, `client${i}`);
  const response = await harness(db)('/api/attempts');
  assert.equal(response.status, 200); assert.equal((await response.json()).data.length, 110);
});

test('server session snapshot preserves grading and total through question edits and unpublishing', async () => {
  const db = new LocalD1(); seed(db); const request = harness(db);
  await request('/api/attempts/start', { examId: 'exam__test' });
  db.sqlite.prepare("UPDATE generated_tests SET questions_json = ?, question_count = 1, status = 'paused' WHERE id = 'test'").run(JSON.stringify([{ ...q(), correctAnswer: 'A' }]));
  const response = await request('/api/attempts/submit', picks());
  assert.equal(response.status, 200); const data = (await response.json()).data;
  assert.equal(data.total, 3); assert.equal(data.correct, 1); assert.equal(data.answers[0].correct, 'B');
});

test('negative marking remains the policy from the start of the attempt', async () => {
  const db = new LocalD1(); seed(db); db.sqlite.exec("UPDATE exams SET negative_marking_value = 0.25 WHERE id = 'exam'"); const request = harness(db);
  await request('/api/attempts/start', { examId: 'exam__test' });
  db.sqlite.exec("UPDATE exams SET negative_marking_value = 1 WHERE id = 'exam'");
  const result = (await (await request('/api/attempts/submit', picks())).json()).data;
  assert.equal(result.score, 0.75);
});

test('two devices reuse the server session identity and cannot award two attempts', async () => {
  const db = new LocalD1(); seed(db); const request = harness(db);
  const first = (await (await request('/api/attempts/start', { examId: 'exam__test' })).json()).data;
  const resumed = (await (await request('/api/attempts/start', { examId: 'exam__test' })).json()).data;
  assert.ok(first.clientAttemptId); assert.equal(first.clientAttemptId, resumed.clientAttemptId);
  const responses = await Promise.all(['device-one', 'device-two'].map((clientAttemptId) => request('/api/attempts/submit', { ...picks(), clientAttemptId })));
  const results = await Promise.all(responses.map((r) => r.json()));
  assert.ok(results.every((r) => r.success)); assert.equal(results[0].data.attemptId, results[1].data.attemptId);
  assert.equal(db.count('attempts'), 1); assert.equal(db.sqlite.prepare('SELECT tests_taken FROM overall_leaderboard').get()!.tests_taken, 1);
  assert.equal((await request('/api/attempts/submit', { ...picks(), clientAttemptId: first.clientAttemptId })).status, 200);
});

test('standard submission needs a started session while bookmark practice remains supported', async () => {
  const db = new LocalD1(); seed(db); const request = harness(db);
  assert.equal((await request('/api/attempts/submit', picks())).status, 409);
  const response = await request('/api/attempts/submit', { ...picks(), practice: true });
  assert.equal(response.status, 200); assert.equal((await response.json()).data.counted, 0);
});

test('active snapshot resumes even when current test JSON is malformed', async () => {
  const db = new LocalD1(); seed(db); const request = harness(db);
  assert.equal((await request('/api/attempts/start', { examId: 'exam__test' })).status, 200);
  db.sqlite.exec("UPDATE generated_tests SET questions_json = 'broken', status = 'unpublished'");
  const response = await request('/api/attempts/start', { examId: 'exam__test' });
  assert.equal(response.status, 200); const data = (await response.json()).data;
  assert.equal(data.questions.length, 3); assert.equal(data.questions[0].questionText, 'Question 0'); assert.equal(data.questions[0].correctAnswer, '');
  assert.equal((await request('/api/attempts/submit', picks())).status, 200);
});
test('parser rejects structured objects used as question text', () => assert.throws(() => parseAndValidateQuestions(JSON.stringify([{ ...q(), questionText: { invalid: true } }]))));


test('test deletion preserves active student sessions and raw answer aliases stay private', async () => {
  const db = new LocalD1(); seed(db); const student = harness(db); const admin = harness(db, true);
  db.sqlite.prepare('UPDATE generated_tests SET questions_json = ?').run(JSON.stringify(Array.from({ length: 3 }, (_, i) => ({ ...q(i), answer: 'B' }))));
  const detail = (await (await student('/api/generated-tests/test')).json()).data;
  assert.equal(detail.questions[0].answer, '');
  await student('/api/attempts/start', { examId: 'exam__test' });
  assert.equal((await admin('/api/generated-tests/test', undefined, 'DELETE')).status, 409);
  assert.equal(db.count('generated_tests'), 1);
});
test('question management persists validated canonical answer aliases and rejects invalid payloads', async () => {
  const db = new LocalD1(); const admin = harness(db, true);
  assert.equal((await admin('/api/questions', { ...q(), examId: 'exam', correctAnswer: '2' })).status, 201);
  assert.equal(db.sqlite.prepare('SELECT correct_answer FROM questions').get()!.correct_answer, 'B');
  assert.equal((await admin('/api/questions/batch', { questions: [{ ...q(1), examId: 'exam', correctAnswer: 'invalid' }] })).status, 400);
  assert.equal((await harness(db)('/api/questions', { ...q(), examId: 'exam' })).status, 403);
});


test('practice flag cannot expose a draft or future generated test answer key', async () => {
  const db = new LocalD1(); seed(db, 3, 'draft'); const request = harness(db);
  assert.equal((await request('/api/attempts/submit', { ...picks(), practice: true })).status, 403);
  db.sqlite.exec("UPDATE generated_tests SET status = 'published', available_from = 9999999999999");
  assert.equal((await request('/api/attempts/submit', { ...picks(), practice: true })).status, 403);
  assert.equal(db.count('attempts'), 0);
});


test('large question import is atomic and uses a bounded D1 query count', async () => {
  const db = new LocalD1(); const admin = harness(db, true);
  const questions = Array.from({ length: 200 }, (_, i) => ({ ...q(i), examId: 'exam', id: `import-${i}` }));
  assert.equal((await admin('/api/questions/batch', { questions })).status, 201);
  assert.equal(db.count('questions'), 200); assert.ok(db.queries < 5);
  // An existing primary key in the last row must roll back all new rows.
  const broken = questions.map((q, i) => ({ ...q, id: i === 199 ? 'import-199' : `new-${i}` }));
  assert.equal((await admin('/api/questions/batch', { questions: broken })).status, 500);
  assert.equal(db.count('questions'), 200);
});


test('reopening a saved session after a lost successful response does not start another attempt', async () => {
  const db = new LocalD1(); seed(db); const request = harness(db);
  const started = (await (await request('/api/attempts/start', { examId: 'exam__test' })).json()).data;
  const submitted = (await (await request('/api/attempts/submit', { ...picks(), clientAttemptId: started.clientAttemptId })).json()).data;
  const restored = (await (await request('/api/attempts/start', { examId: 'exam__test', resumeAttemptId: started.clientAttemptId })).json()).data;
  assert.equal(restored.alreadySubmitted, true); assert.equal(restored.completedAttemptId, submitted.attemptId);
  assert.equal(db.count('attempt_sessions'), 0); assert.equal(db.count('attempts'), 1);
  const next = (await (await request('/api/attempts/start', { examId: 'exam__test' })).json()).data;
  assert.notEqual(next.clientAttemptId, started.clientAttemptId);
});


test('an expired generation job becomes retryable before its repeated request key is rejected', async () => {
  const db = new LocalD1();
  db.sqlite.prepare("INSERT INTO generation_jobs (id, exam_id, exam_name, source, requested_count, status, started_at, request_key) VALUES ('stale', 'exam', 'Exam', 'scheduled', 1, 'running', ?, 'same-key')").run(Date.now() - 181000);
  await assert.rejects(runGeneration(envFor(db), { id: 'exam', exam_name: 'Exam' } as any, 1, 'Test 1', '', 'scheduled', 'same-key'));
  const job = db.sqlite.prepare("SELECT * FROM generation_jobs WHERE id = 'stale'").get()!;
  assert.equal(job.status, 'failed'); assert.equal(job.retryable, 1); assert.equal(job.error_category, 'TIMEOUT');
});


test('scheduled cleanup expires orphaned jobs even when automatic generation is disabled', async () => {
  const db = new LocalD1(); const old = Date.now() - 181000;
  db.sqlite.prepare("INSERT INTO generation_jobs (id, exam_id, exam_name, source, requested_count, status, started_at) VALUES ('orphan', 'exam', 'Exam', 'manual', 1, 'running', ?)").run(old);
  db.sqlite.prepare("UPDATE exams SET last_generation_status = 'running', generating_lock_until = ? WHERE id = 'exam'").run(old + 180000);
  await handleScheduledTestGeneration({} as any, envFor(db), {} as any);
  assert.equal(db.sqlite.prepare('SELECT status FROM generation_jobs').get()!.status, 'failed');
  assert.equal(db.sqlite.prepare('SELECT last_generation_status FROM exams').get()!.last_generation_status, 'failed');
});


test('racing submissions cannot observe uncommitted success when the database fails', async () => {
  const db = new LocalD1(); seed(db); const request = harness(db);
  await request('/api/attempts/start', { examId: 'exam__test' });
  db.failPattern = 'INSERT INTO overall_leaderboard';
  const failed = await Promise.all(['one', 'two'].map((clientAttemptId) => request('/api/attempts/submit', { ...picks(), clientAttemptId })));
  assert.ok(failed.every((response) => response.status === 500));
  assert.equal(db.count('attempts'), 0); assert.equal(db.count('attempt_answers'), 0); assert.equal(db.count('attempt_sessions'), 1);
  db.failPattern = '';
  assert.equal((await request('/api/attempts/submit', { ...picks(), clientAttemptId: 'one' })).status, 200);
  assert.equal(db.count('attempts'), 1);
});
