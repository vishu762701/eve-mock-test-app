import test from 'node:test';
import assert from 'node:assert/strict';
import { DatabaseSync } from 'node:sqlite';
import { Hono } from 'hono';
import { apiErrorContract, adminJsonContract } from '../src/middleware/apiContract.ts';
import { operationMonitor } from '../src/middleware/operationMonitor.ts';
import { checkRateLimit } from '../src/middleware/rateLimiter.ts';
import workerModule from '../src/index.ts';
const worker = workerModule.default || workerModule;

function rateDatabase(t) {
  const db = new DatabaseSync(':memory:'); t.after(() => db.close());
  db.exec('CREATE TABLE rate_limits (key TEXT PRIMARY KEY, count INTEGER, reset_at INTEGER)');
  return { prepare(sql) { let args = []; return {
    bind(...values) { args = values; return this; },
    async first() { return db.prepare(sql).get(...args) ?? null; },
  }; } };
}
test('real Worker failures have stable codes and correlation IDs', async t => {
  const response = await worker.fetch(new Request('https://example.test/api/admin/users', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: 'null',
  }), { DB: rateDatabase(t) }, {});
  assert.equal(response.status, 401);
  const body = await response.json();
  assert.equal(body.code, 'AUTH_REQUIRED'); assert.equal(body.success, false);
  assert.ok(body.requestId); assert.equal(body.requestId, response.headers.get('X-Request-ID'));
});
test('rate-limit store failure closes real Worker requests with typed 503', async () => {
  const DB = { prepare() { throw Error('sensitive database internals'); } };
  const response = await worker.fetch(new Request('https://example.test/api/admin/users'), { DB }, {});
  assert.equal(response.status, 503);
  const body = await response.json();
  assert.equal(body.code, 'SERVICE_UNAVAILABLE'); assert.equal(body.success, false);
  assert.equal(JSON.stringify(body).includes('sensitive database internals'), false);
});
test('atomic rate bucket limits racing requests and resets after its window', async t => {
  const DB = rateDatabase(t); let now = 1800000000000;
  t.mock.method(Date, 'now', () => now);
  const checks = await Promise.all(Array.from({ length: 20 }, () => checkRateLimit(DB, 'uid:test', 3, 60)));
  assert.equal(checks.filter(x => x.limited).length, 17);
  assert.ok(checks.filter(x => x.limited).every(x => x.retryAfter === 60));
  now += 60000; assert.equal((await checkRateLimit(DB, 'uid:test', 3, 60)).limited, false);
});
test('invalid admin JSON/types are rejected before dispatch; valid bodies remain readable', async () => {
  const app = new Hono(); let writes = 0;
  app.use('*', apiErrorContract); app.use('*', operationMonitor);
  app.use('*', async (c, next) => { c.set('user', { uid: 'admin', isAdmin: true }); await next(); });
  app.use('*', adminJsonContract);
  app.post('/api/exams', async c => { writes++; return c.json({ success: true, data: await c.req.json() }); });
  const send = body => app.request('/api/exams', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body });
  for (const body of ['broken', 'null', '[]', 'true', '{"examName":{}}', '{"timeLimitMinutes":true}', '{"enabled":"false"}']) {
    const response = await send(body); assert.equal(response.status, 400, body);
    assert.equal((await response.json()).success, false); assert.equal(writes, 0);
  }
  const valid = { examName: 'Exam', timeLimitMinutes: '30', enabled: false };
  const response = await send(JSON.stringify(valid));
  assert.equal(response.status, 200); assert.deepEqual((await response.json()).data, valid); assert.equal(writes, 1);
});
test('business error codes and Retry-After survive normalization', async () => {
  const app = new Hono(); app.use('*', apiErrorContract);
  app.get('/api/limited', c => { c.header('Retry-After', '12'); return c.json({ success: false, code: 'PROVIDER_QUOTA', error: 'Please wait.' }, 429); });
  const response = await app.request('/api/limited');
  assert.equal(response.status, 429); assert.equal(response.headers.get('Retry-After'), '12');
  assert.equal((await response.json()).code, 'PROVIDER_QUOTA');
});
