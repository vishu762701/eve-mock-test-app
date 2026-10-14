import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  isSensitivePath,
  detectSecrets,
  sanitizeContent
} from '../security.mjs';

test('isSensitivePath correctly identifies credential files', () => {
  assert.strictEqual(isSensitivePath('app/eve-debug.keystore'), true);
  assert.strictEqual(isSensitivePath('app/release.jks'), true);
  assert.strictEqual(isSensitivePath('.env'), true);
  assert.strictEqual(isSensitivePath('.env.production'), true);
  assert.strictEqual(isSensitivePath('app/google-services.json'), true);
  assert.strictEqual(isSensitivePath('google-services.json'), true);
  assert.strictEqual(isSensitivePath('secrets/service-account-key.json'), true);
  assert.strictEqual(isSensitivePath('credentials.json'), true);
  assert.strictEqual(isSensitivePath('id_rsa'), true);
  assert.strictEqual(isSensitivePath('id_ed25519'), true);

  // Safe production files must return false
  assert.strictEqual(isSensitivePath('app/src/main/java/com/eve/app/ui/test/TestActivity.kt'), false);
  assert.strictEqual(isSensitivePath('app/src/main/res/layout/activity_main.xml'), false);
  assert.strictEqual(isSensitivePath('backend/src/index.ts'), false);
  assert.strictEqual(isSensitivePath('ENGINEERING_STANDARD.md'), false);
});

test('detectSecrets identifies real-looking private keys and tokens across adversarial strings', () => {
  const samplePrivateKey = `
-----BEGIN RSA PRIVATE KEY-----
MIIEowIBAAKCAQEA0Yv...fakekeycontent...
-----END RSA PRIVATE KEY-----
  `;
  const detected = detectSecrets(samplePrivateKey);
  assert.strictEqual(detected.length, 1);
  assert.strictEqual(detected[0].name, 'Private Key block');

  const sampleGhToken = 'ghp_' + 'A'.repeat(36);
  const detectedGh = detectSecrets(sampleGhToken);
  assert.strictEqual(detectedGh.length, 1);
  assert.strictEqual(detectedGh[0].name, 'GitHub Personal Access Token');

  // GCP API Key
  const gcpApiKey = 'AIzaSyA' + '1'.repeat(32);
  const detectedGcp = detectSecrets(`key = "${gcpApiKey}"`);
  assert.ok(detectedGcp.some(d => d.name === 'Google Cloud API Key'));

  // Stripe secret key
  const stripeKey = 'sk_live_' + 'X'.repeat(24);
  const detectedStripe = detectSecrets(`STRIPE_SECRET=${stripeKey}`);
  assert.ok(detectedStripe.some(d => d.name === 'Stripe Secret Key'));

  // Supabase service key
  const supabaseKey = 'sbp_' + 'Y'.repeat(40);
  const detectedSupabase = detectSecrets(`SUPABASE_SERVICE=${supabaseKey}`);
  assert.ok(detectedSupabase.some(d => d.name === 'Supabase Service Role Key'));

  // URI embedded password
  const uriCredentials = 'https://admin:SuperSecretPass123!@db.production.internal:5432/eve';
  const detectedUri = detectSecrets(uriCredentials);
  assert.ok(detectedUri.some(d => d.name === 'URI Embedded Password Credentials'));

  // Ignored placeholders
  const placeholder = 'api_key: "dummy_placeholder_for_test"';
  const detectedPlaceholder = detectSecrets(placeholder);
  assert.strictEqual(detectedPlaceholder.length, 0);
});

test('sanitizeContent redacts sensitive tokens and preserves safe text', () => {
  const input = `
# System Config
API_URL=https://api.example.com
SECRET_TOKEN=ghp_${'B'.repeat(36)}
GCP_KEY=AIzaSyB${'2'.repeat(32)}
DB_URI=https://user:SuperSecretP@db.eve.internal
SAFE_SETTING=true
  `;

  const { sanitized, redactedCount } = sanitizeContent('config.txt', input);
  assert.ok(redactedCount >= 3);
  assert.match(sanitized, /\[REDACTED_BY_EVE_JARVIS_SYNC: GitHub Personal Access Token\]/);
  assert.match(sanitized, /\[REDACTED_BY_EVE_JARVIS_SYNC: Google Cloud API Key\]/);
  assert.match(sanitized, /\[REDACTED_BY_EVE_JARVIS_SYNC: URI Embedded Password Credentials\]/);
  assert.match(sanitized, /SAFE_SETTING=true/);
  assert.doesNotMatch(sanitized, /ghp_B{36}/);
});
