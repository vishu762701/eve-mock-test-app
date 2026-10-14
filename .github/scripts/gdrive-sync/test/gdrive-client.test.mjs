import { test } from 'node:test';
import assert from 'node:assert/strict';
import crypto from 'node:crypto';
import { GDriveClient } from '../gdrive-client.mjs';

function createMockKey() {
  const { privateKey } = crypto.generateKeyPairSync('rsa', { modulusLength: 2048 });
  return {
    client_email: 'eve-jarvis-sa@test-project.iam.gserviceaccount.com',
    private_key: privateKey.export({ type: 'pkcs8', format: 'pem' }),
    token_uri: 'https://oauth2.googleapis.com/token'
  };
}

test('GDriveClient validates constructor key structure', () => {
  assert.throws(() => new GDriveClient(null), /Authentication options .* are required/);
  assert.throws(() => new GDriveClient({}), /Invalid credentials: must provide either Service Account/);
});

test('GDriveClient generates cryptographically valid RS256 JWT with drive.file scope', () => {
  const mockKey = createMockKey();
  const client = new GDriveClient(mockKey);
  const jwt = client.generateJwt();

  const parts = jwt.split('.');
  assert.strictEqual(parts.length, 3, 'JWT must have 3 parts');

  const header = JSON.parse(Buffer.from(parts[0], 'base64url').toString('utf8'));
  const payload = JSON.parse(Buffer.from(parts[1], 'base64url').toString('utf8'));

  assert.strictEqual(header.alg, 'RS256');
  assert.strictEqual(header.typ, 'JWT');
  assert.strictEqual(payload.iss, mockKey.client_email);
  assert.strictEqual(payload.scope, 'https://www.googleapis.com/auth/drive.file');
  assert.strictEqual(payload.aud, mockKey.token_uri);
  assert.ok(payload.exp > payload.iat);
});

test('GDriveClient exchanges JWT for bearer token using mocked fetch (Service Account mode)', async () => {
  const mockKey = createMockKey();

  const mockFetch = async (url, opts) => {
    assert.strictEqual(url, mockKey.token_uri);
    assert.strictEqual(opts.method, 'POST');
    return {
      ok: true,
      status: 200,
      json: async () => ({
        access_token: 'mock-access-token-12345',
        expires_in: 3600
      })
    };
  };

  const client = new GDriveClient(mockKey, mockFetch);
  const token = await client.getAccessToken();

  assert.strictEqual(token, 'mock-access-token-12345');
});

test('GDriveClient supports Google User OAuth 2.0 with refresh token', async () => {
  const userConfig = {
    clientId: 'user-client-id-123.apps.googleusercontent.com',
    clientSecret: 'user-client-secret-xyz',
    refreshToken: 'user-refresh-token-456'
  };

  let tokenExchangeCalled = false;

  const mockFetch = async (url, opts) => {
    if (url.includes('/token')) {
      tokenExchangeCalled = true;
      assert.strictEqual(opts.method, 'POST');
      assert.match(opts.body, /grant_type=refresh_token/);
      assert.match(opts.body, /client_id=user-client-id-123/);
      assert.match(opts.body, /refresh_token=user-refresh-token-456/);
      return {
        ok: true,
        status: 200,
        json: async () => ({
          access_token: 'oauth-user-access-token-999',
          expires_in: 3600
        })
      };
    }
    throw new Error('Unexpected URL: ' + url);
  };

  const client = new GDriveClient(userConfig, mockFetch);
  const token = await client.getAccessToken();

  assert.strictEqual(token, 'oauth-user-access-token-999');
  assert.strictEqual(tokenExchangeCalled, true);
});

test('GDriveClient handles 401 Unauthorized by refreshing token and retrying transparently', async () => {
  const userConfig = {
    clientId: 'user-client-id',
    clientSecret: 'user-secret',
    refreshToken: 'user-refresh'
  };

  let tokenCount = 0;
  let apiAttempt = 0;

  const mockFetch = async (url, opts) => {
    if (url.includes('/token')) {
      tokenCount++;
      return {
        ok: true,
        status: 200,
        json: async () => ({
          access_token: `token-version-${tokenCount}`,
          expires_in: 3600
        })
      };
    }

    if (url.includes('/drive/v3/files')) {
      apiAttempt++;
      if (apiAttempt === 1) {
        // First attempt fails with 401
        return {
          ok: false,
          status: 401,
          text: async () => 'Unauthorized: token expired'
        };
      }
      // Second attempt with refreshed token succeeds
      assert.strictEqual(opts.headers.Authorization, 'Bearer token-version-2');
      return {
        ok: true,
        status: 200,
        json: async () => ({ files: [] })
      };
    }

    throw new Error('Unexpected URL: ' + url);
  };

  const client = new GDriveClient(userConfig, mockFetch);
  const file = await client.findFileByName('folder-id', 'test.md');
  assert.strictEqual(file, null);
  assert.strictEqual(tokenCount, 2, 'Should fetch token twice due to 401 retry');
  assert.strictEqual(apiAttempt, 2, 'Should retry API call after refreshing token');
});

test('GDriveClient retries on HTTP 429 and 503 transient errors', async () => {
  const userConfig = {
    clientId: 'user-client-id',
    clientSecret: 'user-secret',
    refreshToken: 'user-refresh'
  };

  let attempts = 0;

  const mockFetch = async (url) => {
    if (url.includes('/token')) {
      return {
        ok: true,
        status: 200,
        json: async () => ({ access_token: 'valid-token', expires_in: 3600 })
      };
    }

    if (url.includes('/drive/v3/files')) {
      attempts++;
      if (attempts === 1) {
        return {
          ok: false,
          status: 429,
          headers: new Map([['Retry-After', '0']]),
          text: async () => 'Rate limit exceeded'
        };
      }
      if (attempts === 2) {
        return {
          ok: false,
          status: 503,
          headers: new Map(),
          text: async () => 'Backend service unavailable'
        };
      }
      return {
        ok: true,
        status: 200,
        json: async () => ({ files: [{ id: 'doc-ok', name: 'test.md' }] })
      };
    }

    throw new Error('Unexpected URL: ' + url);
  };

  const client = new GDriveClient(userConfig, mockFetch);
  const file = await client.findFileByName('folder-id', 'test.md');
  assert.ok(file);
  assert.strictEqual(file.id, 'doc-ok');
  assert.strictEqual(attempts, 3, 'Must succeed on third attempt after 429 and 503 retries');
});

test('GDriveClient produces clear diagnostic error when Service Account hits 403 storageQuotaExceeded', async () => {
  const mockKey = createMockKey();

  const mockFetch = async (url) => {
    if (url.includes('/token')) {
      return {
        ok: true,
        status: 200,
        json: async () => ({ access_token: 'valid-token', expires_in: 3600 })
      };
    }

    if (url.includes('/drive/v3/files?q=')) {
      return {
        ok: true,
        status: 200,
        json: async () => ({ files: [] })
      };
    }

    if (url.includes('uploadType=multipart')) {
      return {
        ok: false,
        status: 403,
        text: async () => JSON.stringify({
          error: {
            errors: [{ domain: 'usageLimits', reason: 'storageQuotaExceeded', message: "The user's Drive storage quota has been exceeded." }],
            code: 403,
            message: "The user's Drive storage quota has been exceeded."
          }
        })
      };
    }

    throw new Error('Unexpected URL: ' + url);
  };

  const client = new GDriveClient(mockKey, mockFetch);
  await assert.rejects(
    async () => client.syncDocument('folder-id', 'volume.md', 'content'),
    /Google Drive API Error \(403 storageQuotaExceeded\): Storage quota exceeded\. Note: Google Service Accounts do not have storage quota/
  );
});

test('syncDocument updates existing file in-place, preserving stable document ID', async () => {
  const mockKey = createMockKey();
  const folderId = 'folder-xyz';
  const fileName = '01_ENGINEERING_RULES_AND_STANDARDS.md';
  const existingFileId = 'stable-doc-id-999';

  let patchCalled = false;
  let postCalled = false;

  const mockFetch = async (url, opts) => {
    // 1. Token exchange
    if (url === mockKey.token_uri) {
      return {
        ok: true,
        status: 200,
        json: async () => ({ access_token: 'mock-token', expires_in: 3600 })
      };
    }

    // 2. Query file by name
    if (url.includes('/drive/v3/files?q=')) {
      assert.ok(url.includes('supportsAllDrives=true'), 'Must support Shared Drives');
      return {
        ok: true,
        status: 200,
        json: async () => ({
          files: [{ id: existingFileId, name: fileName, mimeType: 'text/markdown', trashed: false }]
        })
      };
    }

    // 3. Patch file (in-place update)
    if (url.includes(`/upload/drive/v3/files/${existingFileId}?uploadType=media`)) {
      assert.strictEqual(opts.method, 'PATCH');
      assert.ok(url.includes('supportsAllDrives=true'));
      patchCalled = true;
      return {
        ok: true,
        status: 200,
        json: async () => ({ id: existingFileId, name: fileName })
      };
    }

    // 4. Multipart create (should NOT be called if file already exists)
    if (url.includes('uploadType=multipart')) {
      postCalled = true;
      return {
        ok: true,
        status: 200,
        json: async () => ({ id: 'new-id' })
      };
    }

    throw new Error(`Unexpected URL: ${url}`);
  };

  const client = new GDriveClient(mockKey, mockFetch);
  const result = await client.syncDocument(folderId, fileName, '# Updated Content', 'text/markdown');

  assert.strictEqual(result.id, existingFileId, 'Must preserve stable file ID');
  assert.strictEqual(result.action, 'updated');
  assert.strictEqual(patchCalled, true, 'PATCH must be called');
  assert.strictEqual(postCalled, false, 'POST must NOT be called for existing file');
});

test('findFileByName handles duplicate files safely by selecting primary active file', async () => {
  const mockKey = createMockKey();
  const folderId = 'folder-xyz';
  const fileName = '02_ANDROID_CORE_AND_UI.md';

  const mockFetch = async (url) => {
    if (url.includes('/token')) {
      return {
        ok: true,
        status: 200,
        json: async () => ({ access_token: 'mock-token', expires_in: 3600 })
      };
    }

    if (url.includes('/drive/v3/files?q=')) {
      return {
        ok: true,
        status: 200,
        json: async () => ({
          files: [
            { id: 'primary-doc-111', name: fileName, trashed: false },
            { id: 'duplicate-doc-222', name: fileName, trashed: false },
            { id: 'trashed-doc-333', name: fileName, trashed: true }
          ]
        })
      };
    }

    throw new Error('Unexpected URL: ' + url);
  };

  const client = new GDriveClient(mockKey, mockFetch);
  const file = await client.findFileByName(folderId, fileName);
  assert.ok(file);
  assert.strictEqual(file.id, 'primary-doc-111', 'Should select primary non-trashed file');
});

test('shareWithUsers adds authorized accounts without duplicate grants and without public access', async () => {
  const mockKey = createMockKey();
  const folderId = 'folder-xyz';
  const targetEmail = 'engineer@example.com';

  let permissionCreated = false;

  const mockFetch = async (url, opts) => {
    if (url === mockKey.token_uri) {
      return {
        ok: true,
        status: 200,
        json: async () => ({ access_token: 'mock-token', expires_in: 3600 })
      };
    }

    // List permissions
    if (url.includes(`/drive/v3/files/${folderId}/permissions`)) {
      if (opts && opts.method === 'POST') {
        assert.strictEqual(opts.method, 'POST');
        const body = JSON.parse(opts.body);
        assert.strictEqual(body.emailAddress, targetEmail);
        assert.strictEqual(body.type, 'user');
        assert.notStrictEqual(body.type, 'anyone', 'Must NEVER be anyone');
        permissionCreated = true;
        return {
          ok: true,
          status: 200,
          json: async () => ({ id: 'perm-2' })
        };
      }

      return {
        ok: true,
        status: 200,
        json: async () => ({
          permissions: [
            { id: 'perm-1', type: 'user', role: 'owner', emailAddress: 'owner@example.com' }
          ]
        })
      };
    }

    throw new Error(`Unexpected URL: ${url}`);
  };

  const client = new GDriveClient(mockKey, mockFetch);
  const results = await client.shareWithUsers(folderId, [targetEmail], 'reader');

  assert.strictEqual(results.length, 1);
  assert.strictEqual(results[0].status, 'granted');
  assert.strictEqual(permissionCreated, true);
});

test('removePublicPermissions finds and removes any public permissions', async () => {
  const mockKey = createMockKey();
  const folderId = 'folder-xyz';

  let deleteCalled = false;

  const mockFetch = async (url, opts) => {
    if (url === mockKey.token_uri) {
      return {
        ok: true,
        status: 200,
        json: async () => ({ access_token: 'mock-token', expires_in: 3600 })
      };
    }

    if (url.endsWith(`/drive/v3/files/${folderId}/permissions?fields=permissions(id,type,role,emailAddress)&supportsAllDrives=true`)) {
      return {
        ok: true,
        status: 200,
        json: async () => ({
          permissions: [
            { id: 'public-perm-1', type: 'anyone', role: 'reader' }
          ]
        })
      };
    }

    if (url.includes(`/drive/v3/files/${folderId}/permissions/public-perm-1`)) {
      assert.strictEqual(opts.method, 'DELETE');
      deleteCalled = true;
      return { ok: true, status: 204, text: async () => '' };
    }

    throw new Error(`Unexpected URL: ${url}`);
  };

  const client = new GDriveClient(mockKey, mockFetch);
  const removed = await client.removePublicPermissions(folderId);

  assert.strictEqual(removed, 1);
  assert.strictEqual(deleteCalled, true);
});
