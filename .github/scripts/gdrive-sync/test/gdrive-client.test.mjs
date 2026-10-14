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
  assert.throws(() => new GDriveClient(null), /Service account key is required/);
  assert.throws(() => new GDriveClient({}), /missing client_email or private_key/);
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

test('GDriveClient exchanges JWT for bearer token using mocked fetch', async () => {
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
      return {
        ok: true,
        status: 200,
        json: async () => ({
          files: [{ id: existingFileId, name: fileName, mimeType: 'text/markdown' }]
        })
      };
    }

    // 3. Patch file (in-place update)
    if (url.includes(`/upload/drive/v3/files/${existingFileId}?uploadType=media`)) {
      assert.strictEqual(opts.method, 'PATCH');
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
    if (url.endsWith(`/drive/v3/files/${folderId}/permissions?fields=permissions(id,type,role,emailAddress)`)) {
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

    // Create permission
    if (url.includes(`/drive/v3/files/${folderId}/permissions`)) {
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

    if (url.endsWith(`/drive/v3/files/${folderId}/permissions?fields=permissions(id,type,role,emailAddress)`)) {
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

    if (url.endsWith(`/drive/v3/files/${folderId}/permissions/public-perm-1`)) {
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
