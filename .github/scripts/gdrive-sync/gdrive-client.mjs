/**
 * Least-Privilege Google Drive API v3 Client for EVE JARVIS Gemini Sync
 * Pure Node.js (Zero external dependencies).
 * Uses Google Cloud Service Account with RS256 JWT exchange.
 * Scope: https://www.googleapis.com/auth/drive.file
 * Preserves stable Google Drive file IDs via files.update.
 */

import crypto from 'node:crypto';

export class GDriveClient {
  /**
   * @param {object|string} serviceAccountKey Parsed JSON or string JSON of service account credentials
   * @param {typeof fetch} [fetchFn] Optional custom fetch implementation (for unit testing)
   */
  constructor(serviceAccountKey, fetchFn = globalThis.fetch) {
    if (!serviceAccountKey) {
      throw new Error('Service account key is required');
    }

    const key = typeof serviceAccountKey === 'string'
      ? JSON.parse(serviceAccountKey)
      : serviceAccountKey;

    if (!key.client_email || !key.private_key) {
      throw new Error('Invalid service account key: missing client_email or private_key');
    }

    this.clientEmail = key.client_email;
    this.privateKey = key.private_key;
    this.tokenUri = key.token_uri || 'https://oauth2.googleapis.com/token';
    this.fetch = fetchFn;
    this.accessToken = null;
    this.tokenExpiresAt = 0;
  }

  /**
   * Generates a signed RS256 JWT for Google OAuth2 token exchange.
   * Scope is strictly least-privilege: drive.file.
   * @returns {string} Signed JWT
   */
  generateJwt() {
    const header = {
      alg: 'RS256',
      typ: 'JWT'
    };

    const now = Math.floor(Date.now() / 1000);
    const claims = {
      iss: this.clientEmail,
      scope: 'https://www.googleapis.com/auth/drive.file',
      aud: this.tokenUri,
      exp: now + 3600,
      iat: now
    };

    const encodedHeader = Buffer.from(JSON.stringify(header)).toString('base64url');
    const encodedClaims = Buffer.from(JSON.stringify(claims)).toString('base64url');
    const unsignedToken = `${encodedHeader}.${encodedClaims}`;

    const signer = crypto.createSign('RSA-SHA256');
    signer.update(unsignedToken);
    signer.end();
    const signature = signer.sign(this.privateKey, 'base64url');

    return `${unsignedToken}.${signature}`;
  }

  /**
   * Exchanges signed JWT for a Google OAuth bearer access token.
   * @returns {Promise<string>}
   */
  async getAccessToken() {
    const now = Date.now();
    if (this.accessToken && this.tokenExpiresAt > now + 60000) {
      return this.accessToken;
    }

    const jwt = this.generateJwt();
    const params = new URLSearchParams({
      grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer',
      assertion: jwt
    });

    const res = await this.fetch(this.tokenUri, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: params.toString()
    });

    if (!res.ok) {
      const errBody = await res.text();
      throw new Error(`Google OAuth token exchange failed (HTTP ${res.status}): ${errBody}`);
    }

    const data = await res.json();
    this.accessToken = data.access_token;
    this.tokenExpiresAt = now + (data.expires_in * 1000);
    return this.accessToken;
  }

  /**
   * Searches designated parent folder for a file by exact name.
   * @param {string} folderId
   * @param {string} fileName
   * @returns {Promise<{ id: string, name: string, mimeType: string } | null>}
   */
  async findFileByName(folderId, fileName) {
    const token = await this.getAccessToken();
    const safeName = fileName.replace(/'/g, "\\'");
    const q = `'${folderId}' in parents and name = '${safeName}' and trashed = false`;
    const url = `https://www.googleapis.com/drive/v3/files?q=${encodeURIComponent(q)}&fields=files(id,name,mimeType)&spaces=drive`;

    const res = await this.fetch(url, {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Accept': 'application/json'
      }
    });

    if (!res.ok) {
      const errBody = await res.text();
      throw new Error(`Failed to query file '${fileName}' in folder '${folderId}' (HTTP ${res.status}): ${errBody}`);
    }

    const data = await res.json();
    if (data.files && data.files.length > 0) {
      return data.files[0];
    }
    return null;
  }

  /**
   * Updates an existing Google Drive file in-place, PRESERVING its stable document ID.
   * @param {string} fileId
   * @param {string} content
   * @param {string} [mimeType='text/markdown']
   * @returns {Promise<{ id: string, action: string }>}
   */
  async updateFile(fileId, content, mimeType = 'text/markdown') {
    const token = await this.getAccessToken();
    const url = `https://www.googleapis.com/upload/drive/v3/files/${fileId}?uploadType=media`;

    const res = await this.fetch(url, {
      method: 'PATCH',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': `${mimeType}; charset=utf-8`
      },
      body: content
    });

    if (!res.ok) {
      const errBody = await res.text();
      throw new Error(`Failed to update file '${fileId}' (HTTP ${res.status}): ${errBody}`);
    }

    const data = await res.json();
    return { id: data.id || fileId, action: 'updated' };
  }

  /**
   * Creates a new file inside the designated parent folder using multipart upload.
   * @param {string} folderId
   * @param {string} fileName
   * @param {string} content
   * @param {string} [mimeType='text/markdown']
   * @returns {Promise<{ id: string, action: string }>}
   */
  async createFile(folderId, fileName, content, mimeType = 'text/markdown') {
    const token = await this.getAccessToken();
    const boundary = '-------eve_jarvis_boundary_' + Date.now();
    const delimiter = `\r\n--${boundary}\r\n`;
    const closeDelimiter = `\r\n--${boundary}--`;

    const metadata = {
      name: fileName,
      parents: [folderId],
      mimeType: mimeType
    };

    const multipartBody =
      delimiter +
      'Content-Type: application/json; charset=UTF-8\r\n\r\n' +
      JSON.stringify(metadata) +
      delimiter +
      `Content-Type: ${mimeType}; charset=utf-8\r\n\r\n` +
      content +
      closeDelimiter;

    const url = 'https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart';

    const res = await this.fetch(url, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': `multipart/related; boundary=${boundary}`
      },
      body: multipartBody
    });

    if (!res.ok) {
      const errBody = await res.text();
      throw new Error(`Failed to create file '${fileName}' (HTTP ${res.status}): ${errBody}`);
    }

    const data = await res.json();
    return { id: data.id, action: 'created' };
  }

  /**
   * Synchronizes a document to Google Drive:
   * Finds existing document by name in parent folder to update in-place (preserving file ID),
   * or creates it if not yet existing.
   * @param {string} folderId
   * @param {string} fileName
   * @param {string} content
   * @param {string} [mimeType='text/markdown']
   * @returns {Promise<{ id: string, fileName: string, action: 'created' | 'updated' }>}
   */
  async syncDocument(folderId, fileName, content, mimeType = 'text/markdown') {
    const existing = await this.findFileByName(folderId, fileName);
    if (existing) {
      const result = await this.updateFile(existing.id, content, mimeType);
      return { id: result.id, fileName, action: 'updated' };
    } else {
      const result = await this.createFile(folderId, fileName, content, mimeType);
      return { id: result.id, fileName, action: 'created' };
    }
  }

  /**
   * Fetches current permissions list for a file or folder.
   * @param {string} fileId
   * @returns {Promise<Array<{ id: string, type: string, role: string, emailAddress?: string }>>}
   */
  async listPermissions(fileId) {
    const token = await this.getAccessToken();
    const url = `https://www.googleapis.com/drive/v3/files/${fileId}/permissions?fields=permissions(id,type,role,emailAddress)`;

    const res = await this.fetch(url, {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Accept': 'application/json'
      }
    });

    if (!res.ok) {
      const errBody = await res.text();
      throw new Error(`Failed to list permissions for '${fileId}' (HTTP ${res.status}): ${errBody}`);
    }

    const data = await res.json();
    return data.permissions || [];
  }

  /**
   * Shares the folder or file with explicitly authorized Google user accounts.
   * Never makes documents public.
   * @param {string} fileId
   * @param {string[]} authorizedEmails
   * @param {'reader'|'commenter'|'writer'} [role='reader']
   * @returns {Promise<Array<{ email: string, status: string }>>}
   */
  async shareWithUsers(fileId, authorizedEmails, role = 'reader') {
    if (!authorizedEmails || authorizedEmails.length === 0) {
      return [];
    }

    const token = await this.getAccessToken();
    const existingPerms = await this.listPermissions(fileId);
    const existingEmails = new Set(
      existingPerms
        .filter(p => p.emailAddress)
        .map(p => p.emailAddress.toLowerCase())
    );

    const results = [];

    for (const rawEmail of authorizedEmails) {
      const email = rawEmail.trim().toLowerCase();
      if (!email || !email.includes('@')) continue;

      if (existingEmails.has(email)) {
        results.push({ email, status: 'already_authorized' });
        continue;
      }

      const url = `https://www.googleapis.com/drive/v3/files/${fileId}/permissions?sendNotificationEmail=false`;
      const res = await this.fetch(url, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          role,
          type: 'user',
          emailAddress: email
        })
      });

      if (!res.ok) {
        const errText = await res.text();
        results.push({ email, status: `failed: ${errText}` });
      } else {
        results.push({ email, status: 'granted' });
      }
    }

    return results;
  }

  /**
   * Verifies that no public ('anyone') access is granted on the file or folder.
   * If any public permission is detected, it is immediately deleted.
   * @param {string} fileId
   * @returns {Promise<number>} Number of public permissions removed
   */
  async removePublicPermissions(fileId) {
    const token = await this.getAccessToken();
    const perms = await this.listPermissions(fileId);
    const publicPerms = perms.filter(p => p.type === 'anyone');

    let removed = 0;
    for (const p of publicPerms) {
      const url = `https://www.googleapis.com/drive/v3/files/${fileId}/permissions/${p.id}`;
      const res = await this.fetch(url, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (res.ok) {
        removed++;
      }
    }
    return removed;
  }
}
