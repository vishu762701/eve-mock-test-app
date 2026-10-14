import { test } from 'node:test';
import assert from 'node:assert/strict';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { runSync } from '../sync-engine.mjs';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const REPO_ROOT = path.resolve(__dirname, '../../../..');

test('Partial upload failure: Halts sync, does NOT publish master index, reports PARTIAL_FAILURE', async () => {
  const testOutDir = path.join(REPO_ROOT, 'build/test-recovery-output');
  let indexPublishAttempted = false;
  let syncedVolumes = [];

  const mockFetch = async (url, opts) => {
    // 1. Token exchange (OAuth or SA)
    if (url.includes('/token')) {
      return {
        ok: true,
        status: 200,
        json: async () => ({ access_token: 'mock-token', expires_in: 3600 })
      };
    }

    // 2. Query file by name
    if (url.includes('/drive/v3/files?q=')) {
      return {
        ok: true,
        status: 200,
        json: async () => ({ files: [] })
      };
    }

    // 3. Permissions checks
    if (url.includes('/permissions')) {
      return {
        ok: true,
        status: 200,
        json: async () => ({ permissions: [] })
      };
    }

    // 4. File uploads: Simulate failure on volume 3
    if (url.includes('uploadType=multipart')) {
      const body = opts.body;
      if (body.includes('"name":"03_ANDROID_LAYOUTS_AND_RESOURCES.md"')) {
        // Intentionally simulate network or server error on volume 3
        return {
          ok: false,
          status: 500,
          text: async () => 'Internal Server Error during volume 3 upload'
        };
      }

      if (body.includes('"name":"00_INDEX_AND_SYSTEM_STATUS.md"')) {
        indexPublishAttempted = true;
      } else {
        const match = body.match(/"name":"(0[1-6]_[A-Z_]+\.md)"/);
        if (match) syncedVolumes.push(match[1]);
      }

      return {
        ok: true,
        status: 200,
        json: async () => ({ id: 'mock-file-id' })
      };
    }

    throw new Error('Unexpected URL: ' + url);
  };

  const userConfig = {
    rootDir: REPO_ROOT,
    commitSha: 'a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2',
    branch: 'main',
    clientId: 'test-client-id',
    clientSecret: 'test-client-secret',
    refreshToken: 'test-refresh-token',
    folderId: 'mock-folder-id',
    outDir: testOutDir,
    fetchFn: mockFetch
  };

  await assert.rejects(
    async () => runSync(userConfig),
    (err) => {
      assert.strictEqual(err.status, 'PARTIAL_FAILURE');
      assert.strictEqual(indexPublishAttempted, false, 'Master index must NEVER be published on partial failure');
      assert.ok(err.failedVolumes.some(f => f.filename === '03_ANDROID_LAYOUTS_AND_RESOURCES.md'));
      return true;
    }
  );
});
