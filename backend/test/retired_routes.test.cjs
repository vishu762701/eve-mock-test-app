const assert = require('node:assert/strict');
const fs = require('node:fs');
const test = require('node:test');
const ts = require('typescript');
require.extensions['.ts'] = (module, filename) => {
  module._compile(ts.transpileModule(fs.readFileSync(filename, 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 },
    fileName: filename,
  }).outputText, filename);
};
// Keep the actual router and authorization middleware; isolate Firebase verification.
const auth = require('../src/auth');
auth.verifyFirebaseIdToken = async () => ({ uid: 'test-admin', email: 'admin@example.com', emailVerified: true });
auth.isUserAdmin = async () => true;
const worker = require('../src/index').default;
const queries = [];
const env = { DB: { prepare(sql) {
  queries.push(sql);
  return { bind() { return this; }, async first() { return { count: 1, reset_at: 9999999999 }; } };
} } };

test('retired presentation routes return 404 without touching stored configuration', async () => {
  for (const [path, method] of [
    ['/api/ui-studio/published', 'GET'], ['/api/admin/ui-studio/draft', 'GET'],
    ['/api/admin/ui-studio/draft', 'PUT'], ['/api/admin/ui-studio/publish', 'POST'],
    ['/api/admin/ui-studio/versions', 'GET'], ['/api/admin/ui-studio/restore/1', 'POST'],
    ['/api/admin/ui-studio/reset', 'POST'], ['/api/admin/ui-studio/audit-log', 'GET'],
  ]) {
    const response = await worker.fetch(new Request(`https://example.com${path}`, {
      method, headers: { Authorization: 'Bearer test-token' },
    }), env, {});
    assert.equal(response.status, 404, `${method} ${path}`);
  }
  assert.ok(queries.every(sql => !sql.includes('ui_studio')));
});

test('retired public route no longer bypasses authentication', async () => {
  const response = await worker.fetch(new Request('https://example.com/api/ui-studio/published'), env, {});
  assert.equal(response.status, 401);
});
