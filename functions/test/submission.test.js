const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const crypto = require('node:crypto');

class FakeFirestore {
  constructor() { this.rows = new Map([['questions/q1', { examId: 'exam', questionText: 'One?', optionA: 'One', optionB: 'Two', optionC: 'Three', optionD: 'Four', correctAnswer: 'A' }]]); this.tail = Promise.resolve(); this.failCommit = false; }
  collection(name) {
    const filters = [];
    const query = { where: (key, _op, value) => { filters.push([key, value]); return query; }, limit: () => query, get: async () => ({ empty: ![...this.rows].some(([path, value]) => path.startsWith(`${name}/`) && filters.every(([key, expected]) => value[key] === expected)) }) };
    return { ...query, doc: (id = 'new') => {
      const path = `${name}/${id}`;
      return { id, path, get: async () => this.snap(path), set: async (value) => this.rows.set(path, value) };
    } };
  }
  snap(path) { return { exists: this.rows.has(path), data: () => this.rows.get(path) }; }
  async getAll(...refs) { return refs.map((ref) => this.snap(ref.path)); }
  async runTransaction(block) {
    const previous = this.tail; let unlock; this.tail = new Promise((resolve) => { unlock = resolve; }); await previous;
    const staged = [];
    try {
      const result = await block({ get: async (ref) => this.snap(ref.path), create: (ref, value) => staged.push([ref.path, value]), update: (ref, value) => staged.push([ref.path, value]) });
      if (this.failCommit) throw new Error('database unavailable');
      for (const [path, value] of staged) this.rows.set(path, value);
      return result;
    } finally { unlock(); }
  }
}
function load(db) {
  class HttpsError extends Error { constructor(code, message) { super(message); this.code = code; } }
  const modules = {
    'firebase-functions/v2/firestore': { onDocumentCreated: (_path, callback) => callback },
    'firebase-functions/v2/https': { HttpsError, onCall: (...args) => args.at(-1) },
    'firebase-functions/v2/scheduler': { onSchedule: (...args) => args.at(-1) },
    'firebase-functions/params': { defineSecret: () => ({ value: () => '' }) },
    'firebase-admin/app': { initializeApp() {} },
    'firebase-admin/messaging': { getMessaging: () => ({}) },
    'firebase-admin/firestore': { getFirestore: () => db, FieldValue: { increment: (value) => value } },
    'firebase-admin/auth': { getAuth: () => ({ getUser: async () => ({ displayName: 'Student' }) }) },
    'firebase-admin/remote-config': { getRemoteConfig: () => ({}) },
    'crypto': crypto,
    './questionValidation': require('../questionValidation'),
  };
  const context = { exports: {}, require: (name) => { if (!(name in modules)) throw new Error(`Unexpected module ${name}`); return modules[name]; }, console, process: { env: {} }, setTimeout, fetch: () => { throw new Error('Network must not run'); }, AbortSignal };
  vm.runInNewContext(fs.readFileSync(require.resolve('../index.js'), 'utf8'), context);
  return context.exports;
}
const request = () => ({ auth: { uid: 'student', token: { email: 'student@example.com', email_verified: true } }, data: { examId: 'exam', clientAttemptId: 'client-one', answers: [{ questionId: 'q1', selected: 'A', number: 1 }] } });
test('real optional Firebase callable confirms one attempt on racing retries', async () => {
  const db = new FakeFirestore(); const fn = load(db).submitAttempt;
  const result = await Promise.all([fn(request()), fn(request())]);
  assert.equal(result[0].attemptId, result[1].attemptId); assert.equal(result[0].score, 1);
  assert.equal([...db.rows.keys()].filter((key) => key.startsWith('attempts/')).length, 1);
  assert.equal([...db.rows.keys()].filter((key) => key.startsWith('attempt_locks/')).length, 1);
});
test('Firebase transaction failure leaves no attempt lock and same request can retry', async () => {
  const db = new FakeFirestore(); const fn = load(db).submitAttempt; db.failCommit = true;
  await assert.rejects(fn(request()), /database unavailable/);
  assert.equal([...db.rows.keys()].filter((key) => key.startsWith('attempts/') || key.startsWith('attempt_locks/')).length, 0);
  db.failCommit = false; assert.equal((await fn(request())).score, 1);
});
test('optional Firebase generation rejects student admin access before provider request', async () => {
  const db = new FakeFirestore(); const fn = load(db).triggerAiTestGeneration;
  await assert.rejects(fn(request()), (error) => error.code === 'permission-denied');
});
test('optional Firebase submission rejects missing auth and never trusts client score', async () => {
  const db = new FakeFirestore(); const fn = load(db).submitAttempt;
  await assert.rejects(fn({ data: request().data }), (error) => error.code === 'unauthenticated');
  const input = request(); input.data.score = 99999; input.data.answers[0].selected = 'B';
  assert.equal((await fn(input)).score, 0);
});

test('optional Firebase rejects invalid answers before storing a lock or attempt', async () => {
  const db = new FakeFirestore(); const fn = load(db).submitAttempt; const input = request(); input.data.answers[0].selected = '9';
  await assert.rejects(fn(input), (error) => error.code === 'invalid-argument');
  assert.equal([...db.rows.keys()].filter((key) => key.startsWith('attempt_locks/')).length, 0);
});

test('optional Firebase preserves legacy already-submitted ownership constraint', async () => {
  const db = new FakeFirestore(); db.rows.set('attempts/historical', { userId: 'student', examId: 'exam' });
  await assert.rejects(load(db).submitAttempt(request()), (error) => error.code === 'already-exists');
  assert.equal([...db.rows.keys()].filter((key) => key.startsWith('attempts/')).length, 1);
});

test('optional Firebase rejects foreign questions rather than silently dropping them', async () => {
  const db = new FakeFirestore(); db.rows.get('questions/q1').examId = 'other-exam';
  await assert.rejects(load(db).submitAttempt(request()), (error) => error.code === 'invalid-argument');
});
test('optional Firebase does not reveal draft generated answer keys', async () => {
  const db = new FakeFirestore(); db.rows.set('generated_tests/draft', { examId: 'exam', status: 'paused', questions: [db.rows.get('questions/q1')] });
  const input = request(); input.data.answers[0].questionId = 'draft_0';
  await assert.rejects(load(db).submitAttempt(input), (error) => error.code === 'permission-denied');
});
