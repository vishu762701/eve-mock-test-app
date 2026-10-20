import test from 'node:test';
import assert from 'node:assert/strict';
import { createHmac } from 'node:crypto';
import { Hono } from 'hono';
import { premiumRoutes, premiumWebhookSecret } from '../src/routes/premium';
import type { AuthUser, Env } from '../src/types';

const strongKey = 'unit-test-only-security-fixture-32-chars-plus';
const db = {
  prepare(_sql: string) {
    return {
      bind() { return this; },
      async first() { return null; },
    };
  }
} as unknown as D1Database;

function harness(user: AuthUser, extra: Partial<Env> = {}) {
  const app = new Hono<{ Bindings: Env; Variables: { user: AuthUser } }>();
  app.use('*', async (c, next) => { c.set('user', user); await next(); });
  app.route('/api/premium', premiumRoutes);
  const env = { DB: db, ...extra } as Env;
  return { app, env };
}
const student: AuthUser = { uid: 'student', email: 'student@example.com', displayName: 'Student', isAdmin: false };
const admin: AuthUser = { uid: 'admin', email: 'admin@example.com', displayName: 'Admin', isAdmin: true };

test('known development and missing payment secrets are never trusted', () => {
  assert.equal(premiumWebhookSecret({} as Env), null);
  assert.equal(premiumWebhookSecret({ PREMIUM_WEBHOOK_SECRET: 'eve_whsec_dev' } as Env), null);
  assert.equal(premiumWebhookSecret({ PREMIUM_WEBHOOK_SECRET: 'short' } as Env), null);
  assert.equal(premiumWebhookSecret({ PREMIUM_WEBHOOK_SECRET: strongKey } as Env), strongKey);
});
test('plan and order creation fail closed when verifier is unavailable', async () => {
  const { app, env } = harness(student);
  const plan = await app.request('/api/premium/plan', {}, env);
  assert.equal(plan.status, 200);
  const body = await plan.json() as any;
  assert.equal(body.data.isEnabled, false);
  assert.deepEqual(body.data.paymentMethods, []);
  const order = await app.request('/api/premium/orders/create', { method: 'POST', body: JSON.stringify({ paymentMethod: 'upi' }), headers: { 'Content-Type': 'application/json' } }, env);
  assert.equal(order.status, 503);
});
test('webhook fails closed for missing and public development secret', async () => {
  for (const secret of [undefined, 'eve_whsec_dev']) {
    const { app, env } = harness(student, { PREMIUM_WEBHOOK_SECRET: secret });
    const response = await app.request('/api/premium/webhook', { method: 'POST', body: '{}' }, env);
    assert.equal(response.status, 503);
  }
});
test('webhook uses a real HMAC verifier with strong server-only signing key', async () => {
  const { app, env } = harness(student, { PREMIUM_WEBHOOK_SECRET: strongKey });
  const body = JSON.stringify({ orderId: 'nonexistent-order', status: 'SUCCESS', amount: 99, currency: 'INR' });
  const invalid = await app.request('/api/premium/webhook', { method: 'POST', body, headers: { 'X-Eve-Signature': '00'.repeat(32) } }, env);
  assert.equal(invalid.status, 401);
  const sig = createHmac('sha256', strongKey).update(body).digest('hex');
  const signed = await app.request('/api/premium/webhook', { method: 'POST', body, headers: { 'X-Eve-Signature': sig } }, env);
  assert.equal(signed.status, 404); // Signature valid, order lookup reached
});
test('student cannot grant real premium through sandbox even if a flag is accidentally enabled', async () => {
  for (const flag of [undefined, 'true']) {
    const { app, env } = harness(student, { PREMIUM_SANDBOX_ENABLED: flag });
    const response = await app.request('/api/premium/orders/x/simulate-sandbox-payment', { method: 'POST' }, env);
    assert.equal(response.status, 403);
  }
});
test('sandbox is disabled for admins unless explicitly enabled', async () => {
  const disabled = harness(admin);
  assert.equal((await disabled.app.request('/api/premium/orders/x/simulate-sandbox-payment', { method: 'POST' }, disabled.env)).status, 403);
  const enabled = harness(admin, { PREMIUM_SANDBOX_ENABLED: 'true' });
  assert.equal((await enabled.app.request('/api/premium/orders/x/simulate-sandbox-payment', { method: 'POST' }, enabled.env)).status, 404);
});
