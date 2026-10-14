import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { fileURLToPath } from 'node:url';
import { runSync } from '../sync-engine.mjs';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const REPO_ROOT = path.resolve(__dirname, '../../../..');

function createMockKey() {
  const { privateKey } = crypto.generateKeyPairSync('rsa', { modulusLength: 2048 });
  return {
    client_email: 'eve-jarvis-sa@test-project.iam.gserviceaccount.com',
    private_key: privateKey.export({ type: 'pkcs8', format: 'pem' }),
    token_uri: 'https://oauth2.googleapis.com/token'
  };
}

test('runSync produces all 7 volumes in dry-run mode without credentials', async () => {
  const testOutDir = path.join(REPO_ROOT, 'build/test-jarvis-sync-output');

  const result = await runSync({
    rootDir: REPO_ROOT,
    commitSha: 'a3d3144513ae320562e57a597db2ca76893fdd10',
    branch: 'main',
    triggerEvent: 'test',
    dryRun: true,
    outDir: testOutDir
  });

  assert.strictEqual(result.status, 'LOCAL_GENERATED');
  assert.strictEqual(result.mode, 'dry-run');
  assert.ok(result.includedCount > 500);
  assert.ok(result.excludedCount > 50);

  // Check generated files
  const expectedFiles = [
    '00_INDEX_AND_SYSTEM_STATUS.md',
    '01_ENGINEERING_RULES_AND_STANDARDS.md',
    '02_ANDROID_CORE_AND_UI.md',
    '03_ANDROID_LAYOUTS_AND_RESOURCES.md',
    '04_BACKEND_WORKER_AND_STORAGE.md',
    '05_TEST_SUITES_AND_VERIFICATION.md',
    '06_CI_CD_WORKFLOWS_AND_SCRIPTS.md'
  ];

  for (const fileName of expectedFiles) {
    const fullPath = path.join(testOutDir, fileName);
    assert.ok(fs.existsSync(fullPath), `File ${fileName} must exist`);
    const stat = fs.statSync(fullPath);
    assert.ok(stat.size > 100, `File ${fileName} must have content (got ${stat.size} bytes)`);
  }

  // Clean up test output
  fs.rmSync(testOutDir, { recursive: true, force: true });
});

test('runSync executes live sync against mocked Google Drive endpoints (Service Account mode)', async () => {
  const mockKey = createMockKey();
  const folderId = 'mock-folder-id';
  const testOutDir = path.join(REPO_ROOT, 'build/test-jarvis-live-output');

  const mockFetch = async (url, opts) => {
    // Token exchange
    if (url === mockKey.token_uri) {
      return {
        ok: true,
        status: 200,
        json: async () => ({ access_token: 'mock-token', expires_in: 3600 })
      };
    }

    // Folder accessibility check
    if (url.includes(`/drive/v3/files/${folderId}`)) {
      return { ok: true, status: 200, json: async () => ({ id: folderId, name: 'EVE-JARVIS-KNOWLEDGE', mimeType: 'application/vnd.google-apps.folder', trashed: false }) };
    }

    // Permissions check
    if (url.includes(`/permissions`)) {
      if (opts?.method === 'POST') {
        return { ok: true, status: 200, json: async () => ({ id: 'perm-new' }) };
      }
      return { ok: true, status: 200, json: async () => ({ permissions: [] }) };
    }

    // Query files
    if (url.includes('/drive/v3/files?q=')) {
      return { ok: true, status: 200, json: async () => ({ files: [] }) };
    }

    // Multipart create
    if (url.includes('uploadType=multipart')) {
      const generatedId = 'mock-file-id-' + Math.random().toString(36).substring(7);
      return { ok: true, status: 200, json: async () => ({ id: generatedId }) };
    }

    throw new Error(`Unhandled mock fetch URL: ${url}`);
  };

  const result = await runSync({
    rootDir: REPO_ROOT,
    commitSha: 'a3d3144513ae320562e57a597db2ca76893fdd10',
    branch: 'main',
    triggerEvent: 'push',
    serviceAccountKey: mockKey,
    folderId,
    authorizedUsers: ['engineer@example.com'],
    outDir: testOutDir,
    fetchFn: mockFetch
  });

  assert.strictEqual(result.status, 'SYNCED');
  assert.strictEqual(result.mode, 'live');
  assert.strictEqual(result.syncedDocuments.length, 7);

  // Clean up
  fs.rmSync(testOutDir, { recursive: true, force: true });
});

test('runSync executes live sync against mocked Google Drive endpoints (User OAuth mode)', async () => {
  const folderId = 'mock-user-folder-id';
  const testOutDir = path.join(REPO_ROOT, 'build/test-jarvis-user-oauth-output');

  let refreshTokenUsed = false;

  const mockFetch = async (url, opts) => {
    // Token exchange
    if (url.includes('/token')) {
      refreshTokenUsed = true;
      assert.match(opts.body, /grant_type=refresh_token/);
      return {
        ok: true,
        status: 200,
        json: async () => ({ access_token: 'mock-user-token', expires_in: 3600 })
      };
    }

    // Folder accessibility check
    if (url.includes(`/drive/v3/files/${folderId}`)) {
      return { ok: true, status: 200, json: async () => ({ id: folderId, name: 'EVE-JARVIS-KNOWLEDGE', mimeType: 'application/vnd.google-apps.folder', trashed: false }) };
    }

    // Permissions check
    if (url.includes(`/permissions`)) {
      return { ok: true, status: 200, json: async () => ({ permissions: [] }) };
    }

    // Query files
    if (url.includes('/drive/v3/files?q=')) {
      return { ok: true, status: 200, json: async () => ({ files: [] }) };
    }

    // Multipart create
    if (url.includes('uploadType=multipart')) {
      const generatedId = 'user-file-id-' + Math.random().toString(36).substring(7);
      return { ok: true, status: 200, json: async () => ({ id: generatedId }) };
    }

    throw new Error(`Unhandled mock fetch URL: ${url}`);
  };

  const result = await runSync({
    rootDir: REPO_ROOT,
    commitSha: 'a3d3144513ae320562e57a597db2ca76893fdd10',
    branch: 'main',
    triggerEvent: 'push',
    clientId: 'mock-user-client-id',
    clientSecret: 'mock-user-client-secret',
    refreshToken: 'mock-user-refresh-token',
    folderId,
    outDir: testOutDir,
    fetchFn: mockFetch
  });

  assert.strictEqual(result.status, 'SYNCED');
  assert.strictEqual(result.mode, 'live');
  assert.strictEqual(refreshTokenUsed, true);
  assert.strictEqual(result.syncedDocuments.length, 7);

  // Clean up
  fs.rmSync(testOutDir, { recursive: true, force: true });
});
