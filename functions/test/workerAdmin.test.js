const test = require('node:test');
const assert = require('node:assert/strict');
const { verifyWorkerAdmin } = require('../workerAdmin');
const request = () => ({ auth: { uid: 'admin-uid', token: { email: 'admin@example.test', email_verified: true } }, rawRequest: { headers: { authorization: 'Bearer test-only-token' } } });
function worker({ uid = 'admin-uid', email = 'admin@example.test', isAdmin = true, rights = ['manage_questions'], success = true } = {}) {
  return async (url, options) => {
    assert.equal(options.headers.Authorization, 'Bearer test-only-token');
    assert.ok(options.signal);
    assert.ok(url.startsWith('https://eve-backend.anyqueairdrop.workers.dev/api/'));
    return { ok: true, json: async () => ({ success, data: url.endsWith('/auth/me') ? { uid, email, isAdmin } : { email, rights } }) };
  };
}
test('Firebase administrative access requires both matching Worker identity and the specific right', async () => {
  assert.equal(await verifyWorkerAdmin(request(), 'manage_questions', worker()), true);
  for (const override of [{ uid: 'another-user' }, { email: 'someone-else@example.test' }, { isAdmin: false }, { rights: [] }, { success: false }]) {
    assert.equal(await verifyWorkerAdmin(request(), 'manage_questions', worker(override)), false);
  }
  assert.equal(await verifyWorkerAdmin(request(), 'manage_config_maintenance', worker()), false);
});
test('missing bearer or unverified email cannot use any independent Firebase role', async () => {
  const fail = () => { throw Error('Must not call network'); };
  const missing = request(); delete missing.rawRequest;
  assert.equal(await verifyWorkerAdmin(missing, 'manage_questions', fail), false);
  const unverified = request(); unverified.auth.token.email_verified = false;
  assert.equal(await verifyWorkerAdmin(unverified, 'manage_questions', fail), false);
});
test('Worker failure, expired authorization and malformed JSON fail closed', async () => {
  for (const fetcher of [async () => { throw Error('timeout'); }, async () => ({ ok: false }), async () => ({ ok: true, json: async () => { throw Error('malformed'); } })]) {
    assert.equal(await verifyWorkerAdmin(request(), 'manage_questions', fetcher), false);
  }
});
