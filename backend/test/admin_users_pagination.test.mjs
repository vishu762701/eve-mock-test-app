import test from 'node:test';
import assert from 'node:assert/strict';
import { DatabaseSync } from 'node:sqlite';
import { Hono } from 'hono';
import { adminRoutes } from '../src/routes/admin.ts';
import { HARDCODED_ADMIN_EMAILS } from '../src/auth.ts';

// Real SQLite executes the production D1 SQL, not a mocked list/filter algorithm.
function setup(count, admin = true) {
  const sql = new DatabaseSync(':memory:');
  sql.exec(`CREATE TABLE users (
    id TEXT PRIMARY KEY, eve_id TEXT, email TEXT, display_name TEXT,
    dob TEXT, category TEXT, created_at INTEGER, last_active INTEGER,
    disabled INTEGER DEFAULT 0
  )`);
  const insert = sql.prepare('INSERT INTO users (id, eve_id, email, display_name, created_at, last_active, disabled) VALUES (?, ?, ?, ?, ?, ?, ?)');
  for (let i = 1; i <= count; i++) {
    const key = String(i).padStart(4, '0');
    insert.run('u' + key, 'EVE' + key,
      i === count ? 'last.only@example.com' : 'student' + key + '@example.com',
      i === count ? 'Unique Final Student' : 'Student', i, count - i, i % 2);
  }
  const DB = {
    prepare(statement) {
      const args = [];
      return {
        bind(...values) { args.push(...values); return this; },
        async first() { return sql.prepare(statement).get(...args) ?? null; },
        async all() { return { results: sql.prepare(statement).all(...args) }; },
        async run() {
          const changes = sql.prepare(statement).run(...args).changes;
          return { meta: { changes: Number(changes) } };
        },
      };
    },
  };
  const app = new Hono();
  app.use('*', async (c, next) => {
    c.set('user', { uid: 'admin', email: admin ? [...HARDCODED_ADMIN_EMAILS][0] : 'student@example.com', isAdmin: admin });
    await next();
  });
  app.route('/api/admin', adminRoutes);
  return {
    async request(query = '') {
      const response = await app.request('/api/admin/users' + query, {}, { DB });
      return { status: response.status, json: await response.json() };
    },
    async changeStatus(id, disabled) {
      const response = await app.request('/api/admin/users/' + id + '/status', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ disabled }),
      }, { DB });
      return { status: response.status, json: await response.json() };
    },
    close: () => sql.close(),
  };
}
test('real users endpoint paginates 0, 1, 199, 200, 201 and 250 records', async () => {
  for (const n of [0, 1, 199, 200, 201, 250]) {
    const fixture = setup(n);
    try {
      const first = await fixture.request('?page=1&pageSize=50');
      assert.equal(first.status, 200);
      assert.equal(first.json.pagination.total, n);
      assert.equal(first.json.pagination.totalPages, Math.ceil(n / 50));
      assert.equal(first.json.data.length, Math.min(n, 50));
      const finalPage = Math.max(1, Math.ceil(n / 50));
      const final = await fixture.request('?page=' + finalPage + '&pageSize=50');
      assert.equal(final.json.data.length, n === 0 ? 0 : n - (finalPage - 1) * 50);
      assert.equal(final.json.pagination.hasNext, false);
      const beyond = await fixture.request('?page=999&pageSize=50');
      assert.equal(beyond.json.data.length, 0);
      assert.equal(beyond.json.pagination.total, n);
      const legacy = await fixture.request();
      assert.equal(legacy.json.data.length, Math.min(n, 200));
      assert.ok(Array.isArray(legacy.json.data));
      assert.equal(typeof legacy.json.data[0]?.disabled, n ? 'boolean' : 'undefined');
    } finally { fixture.close(); }
  }
});
test('full database search includes records beyond the previous 200 limit, plus Eve ID', async () => {
  const f = setup(250);
  try {
    for (const search of ['Unique Final Student', 'last.only@example.com', 'EVE0250']) {
      const result = await f.request('?page=1&pageSize=25&q=' + encodeURIComponent(search));
      assert.equal(result.json.pagination.total, 1);
      assert.equal(result.json.data[0].id, 'u0250');
    }
    const ordered = await f.request('?page=5&pageSize=50');
    assert.equal(ordered.json.data.length, 50);
    assert.equal(new Set(ordered.json.data.map(x => x.id)).size, 50);
  } finally { f.close(); }
});
test('LIKE wildcard characters are literal, ordering deterministic and no extra fields leak', async () => {
  const f = setup(3);
  try {
    for (const q of ['%', '_', '!']) {
      const data = await f.request('?page=1&q=' + encodeURIComponent(q));
      assert.equal(data.json.pagination.total, 0);
    }
    const data = await f.request('?page=1&pageSize=2&q=student');
    assert.deepEqual(data.json.data.map(u => u.id), ['u0001', 'u0002']);
    assert.deepEqual(Object.keys(data.json.data[0]).sort(),
      ['id', 'eveId', 'email', 'displayName', 'dob', 'category', 'createdAt', 'lastActive', 'disabled'].sort());
  } finally { f.close(); }
});
test('malformed and excessive paging parameters fail closed', async () => {
  const f = setup(4);
  try {
    for (const query of ['?page=0', '?page=-1', '?page=abc', '?page=1.5',
      '?page=1000001', '?pageSize=0', '?pageSize=201', '?pageSize=Infinity']) {
      assert.equal((await f.request(query)).status, 400, query);
    }
    assert.equal((await f.request('?q=' + 'a'.repeat(161))).status, 400);
  } finally { f.close(); }
});
test('unauthorized users cannot access records even with pagination parameters', async () => {
  const f = setup(250, false);
  try {
    const result = await f.request('?page=5&pageSize=50&q=student');
    assert.equal(result.status, 403);
    assert.equal(result.json.success, false);
    assert.equal(result.json.data, undefined);
  } finally { f.close(); }
});

test('filtered-page ban/unban reflects server-confirmed disabled state and missing users fail', async () => {
  const f = setup(250);
  try {
    const before = await f.request('?page=1&pageSize=10&q=EVE0250');
    assert.equal(before.json.data[0].disabled, false);
    assert.equal((await f.changeStatus('u0250', true)).status, 200);
    const after = await f.request('?page=1&pageSize=10&q=EVE0250');
    assert.equal(after.json.data[0].disabled, true);
    assert.equal(after.json.pagination.total, 1);
    assert.equal((await f.changeStatus('u0250', false)).status, 200);
    assert.equal((await f.request('?page=1&q=EVE0250')).json.data[0].disabled, false);
    const missing = await f.changeStatus('does-not-exist', true);
    assert.equal(missing.status, 404);
    assert.equal(missing.json.success, false);
  } finally { f.close(); }
});
test('unauthorized status changes are rejected', async () => {
  const f = setup(250, false);
  try {
    assert.equal((await f.changeStatus('u0250', true)).status, 403);
  } finally { f.close(); }
});
