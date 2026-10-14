/**
 * Enterprise Google Drive API v3 Client for EVE JARVIS Gemini Sync
 * Pure Node.js (Zero external dependencies).
 * Supports Dual-Mode Authentication:
 *   1. Google User OAuth 2.0 (Refresh Token) — solves personal Google Drive quota limitations (files owned by user).
 *   2. Google Cloud Service Account (RS256 JWT) — for Google Workspace Shared Drives.
 * Scope: https://www.googleapis.com/auth/drive.file
 * Preserves stable Google Drive file IDs via files.update.
 * Features automatic retries with exponential backoff and transparent token re-authentication.
 */

import crypto from 'node:crypto';

export class GDriveClient {
  /**
   * @param {object|string} authOptions Authentication configuration:
   *   - Service Account: JSON string or object with `client_email` and `private_key` (or { serviceAccountKey })
   *   - User OAuth 2.0: object with { clientId, clientSecret, refreshToken }
   * @param {typeof fetch} [fetchFn] Optional custom fetch implementation (for unit testing)
   */
  constructor(authOptions, fetchFn = globalThis.fetch) {
    if (!authOptions) {
      throw new Error('Authentication options (Service Account or User OAuth) are required');
    }

    this.fetch = fetchFn;
    this.accessToken = null;
    this.tokenExpiresAt = 0;

    let parsed = authOptions;
    if (typeof authOptions === 'string') {
      try {
        parsed = JSON.parse(authOptions);
      } catch (e) {
        throw new Error('Invalid JSON string provided for credentials: ' + e.message);
      }
    }

    // Check if wrapped in { serviceAccountKey }
    if (parsed.serviceAccountKey) {
      parsed = typeof parsed.serviceAccountKey === 'string'
        ? JSON.parse(parsed.serviceAccountKey)
        : parsed.serviceAccountKey;
    }

    // Determine authentication mode
    if (parsed.refreshToken || parsed.refresh_token) {
      // User OAuth 2.0
      this.authMode = 'user_oauth';
      this.clientId = parsed.clientId || parsed.client_id;
      this.clientSecret = parsed.clientSecret || parsed.client_secret;
      this.refreshToken = parsed.refreshToken || parsed.refresh_token;
      this.tokenUri = parsed.tokenUri || parsed.token_uri || 'https://oauth2.googleapis.com/token';

      if (!this.clientId || !this.clientSecret || !this.refreshToken) {
        throw new Error('Invalid User OAuth configuration: clientId, clientSecret, and refreshToken are required');
      }
    } else if (parsed.client_email && parsed.private_key) {
      // Service Account
      this.authMode = 'service_account';
      this.clientEmail = parsed.client_email;
      this.privateKey = parsed.private_key;
      this.tokenUri = parsed.token_uri || 'https://oauth2.googleapis.com/token';
    } else {
      throw new Error('Invalid credentials: must provide either Service Account (client_email, private_key) or User OAuth (clientId, clientSecret, refreshToken)');
    }
  }

  /**
   * Generates a signed RS256 JWT for Service Account token exchange.
   * Scope: https://www.googleapis.com/auth/drive.file
   * @returns {string} Signed JWT
   */
  generateJwt() {
    if (this.authMode !== 'service_account') {
      throw new Error('generateJwt is only available in service_account authentication mode');
    }

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
   * Exchanges Service Account JWT or User Refresh Token for Google OAuth bearer access token.
   * Caches token until 60 seconds before expiration.
   * @returns {Promise<string>}
   */
  async getAccessToken() {
    const now = Date.now();
    if (this.accessToken && this.tokenExpiresAt > now + 60000) {
      return this.accessToken;
    }

    if (this.authMode === 'user_oauth') {
      return this._exchangeRefreshToken();
    } else {
      return this._exchangeServiceAccountJwt();
    }
  }

  /**
   * @private
   */
  async _exchangeRefreshToken() {
    const params = new URLSearchParams({
      grant_type: 'refresh_token',
      client_id: this.clientId,
      client_secret: this.clientSecret,
      refresh_token: this.refreshToken
    });

    const res = await this.fetch(this.tokenUri, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: params.toString()
    });

    if (!res.ok) {
      const errBody = await res.text();
      throw new Error(`Google OAuth Refresh Token exchange failed (HTTP ${res.status}): ${errBody}`);
    }

    const data = await res.json();
    this.accessToken = data.access_token;
    const expiresIn = typeof data.expires_in === 'number' ? data.expires_in : 3600;
    this.tokenExpiresAt = Date.now() + (expiresIn * 1000);
    return this.accessToken;
  }

  /**
   * @private
   */
  async _exchangeServiceAccountJwt() {
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
    const expiresIn = typeof data.expires_in === 'number' ? data.expires_in : 3600;
    this.tokenExpiresAt = Date.now() + (expiresIn * 1000);
    return this.accessToken;
  }

  /**
   * Executes fetch with automatic retries for transient errors (429, 5xx) and token refresh for 401.
   * @param {string} url
   * @param {object} options
   * @param {number} [maxRetries=4]
   * @param {number} [baseDelay=500]
   * @returns {Promise<Response>}
   */
  async fetchWithRetry(url, options = {}, maxRetries = 4, baseDelay = 500) {
    let attempt = 0;
    let reauthenticated = false;

    while (true) {
      const token = await this.getAccessToken();
      const headers = {
        ...(options.headers || {}),
        'Authorization': `Bearer ${token}`
      };

      try {
        const res = await this.fetch(url, { ...options, headers });

        // Handle 401 Unauthorized (expired or revoked token)
        if (res.status === 401 && !reauthenticated) {
          reauthenticated = true;
          this.accessToken = null; // force token refresh on next attempt
          continue;
        }

        // Handle 429 Too Many Requests or 5xx Server Transient Errors
        if ((res.status === 429 || (res.status >= 500 && res.status <= 504)) && attempt < maxRetries) {
          attempt++;
          let delayMs = baseDelay * Math.pow(2, attempt - 1) + Math.floor(Math.random() * 100);
          const retryAfter = res.headers && typeof res.headers.get === 'function'
            ? res.headers.get('Retry-After')
            : null;
          if (retryAfter) {
            const parsed = parseInt(retryAfter, 10);
            if (!isNaN(parsed) && parsed > 0) delayMs = parsed * 1000;
          }
          await new Promise(r => setTimeout(r, delayMs));
          continue;
        }

        return res;
      } catch (err) {
        if (attempt < maxRetries) {
          attempt++;
          const delayMs = baseDelay * Math.pow(2, attempt - 1) + Math.floor(Math.random() * 100);
          await new Promise(r => setTimeout(r, delayMs));
          continue;
        }
        throw err;
      }
    }
  }

  /**
   * Securely discovers an app-created folder or automatically provisions a new one.
   * Under the Google Drive 'drive.file' scope, an application can ONLY access files and folders
   * created by the application itself. Manually created folders outside the app will return 404.
   * This method ensures full accessibility by auto-provisioning the folder under the app's authority.
   * @param {string} [folderName='EVE-JARVIS-KNOWLEDGE']
   * @param {string} [explicitFolderId=null] Optional folder ID to verify before provisioning
   * @returns {Promise<string>} The verified accessible Google Drive folder ID
   */
  async getOrCreateAppFolder(folderName = 'EVE-JARVIS-KNOWLEDGE', explicitFolderId = null) {
    // 1. If explicit folder ID is provided, verify accessibility under current credentials
    if (explicitFolderId) {
      try {
        const url = `https://www.googleapis.com/drive/v3/files/${explicitFolderId}?fields=id,name,mimeType,trashed&supportsAllDrives=true`;
        const res = await this.fetchWithRetry(url, { headers: { 'Accept': 'application/json' } });
        if (res.ok) {
          const data = await res.json();
          if (!data.trashed) {
            console.log(`[EVE JARVIS SYNC] Using verified accessible folder ID: ${explicitFolderId} ('${data.name}')`);
            return explicitFolderId;
          }
        }
      } catch {
        // Fallback to app discovery/creation
      }
      console.warn(`[EVE JARVIS SYNC] Notice: Specified folder ID '${explicitFolderId}' is not accessible under current scope (drive.file requires app-created folders). Discovering or auto-provisioning app-owned folder...`);
    }

    // 2. Query for existing app-created folder
    const safeName = folderName.replace(/'/g, "\\'");
    const q = `mimeType = 'application/vnd.google-apps.folder' and name = '${safeName}' and trashed = false`;
    const searchUrl = `https://www.googleapis.com/drive/v3/files?q=${encodeURIComponent(q)}&fields=files(id,name,trashed)&spaces=drive&supportsAllDrives=true&includeItemsFromAllDrives=true`;

    const searchRes = await this.fetchWithRetry(searchUrl, { headers: { 'Accept': 'application/json' } });
    if (searchRes.ok) {
      const data = await searchRes.json();
      const activeFolders = (data.files || []).filter(f => !f.trashed);
      if (activeFolders.length > 0) {
        console.log(`[EVE JARVIS SYNC] Discovered existing app-owned folder '${folderName}' (ID: ${activeFolders[0].id}).`);
        return activeFolders[0].id;
      }
    }

    // 3. Provision new app-owned folder
    console.log(`[EVE JARVIS SYNC] Provisioning new app-owned Google Drive folder '${folderName}'...`);
    const createUrl = 'https://www.googleapis.com/drive/v3/files?supportsAllDrives=true';
    const createRes = await this.fetchWithRetry(createUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: folderName,
        mimeType: 'application/vnd.google-apps.folder',
        description: 'Synchronized Knowledge Base for EVE JARVIS Gemini Gem'
      })
    });

    if (!createRes.ok) {
      const errText = await createRes.text();
      throw new Error(`Failed to provision app-owned folder '${folderName}' (HTTP ${createRes.status}): ${errText}`);
    }

    const folderData = await createRes.json();
    console.log(`[EVE JARVIS SYNC] Successfully provisioned app-owned folder '${folderName}' (ID: ${folderData.id}).`);
    return folderData.id;
  }

  /**
   * Searches designated parent folder for a file by exact name.
   * Supports both regular personal folders and Google Workspace Shared Drives.
   * @param {string} folderId
   * @param {string} fileName
   * @returns {Promise<{ id: string, name: string, mimeType: string } | null>}
   */
  async findFileByName(folderId, fileName) {
    const safeName = fileName.replace(/'/g, "\\'");
    const q = `'${folderId}' in parents and name = '${safeName}' and trashed = false`;
    const url = `https://www.googleapis.com/drive/v3/files?q=${encodeURIComponent(q)}&fields=files(id,name,mimeType,trashed)&spaces=drive&supportsAllDrives=true&includeItemsFromAllDrives=true`;

    const res = await this.fetchWithRetry(url, {
      headers: { 'Accept': 'application/json' }
    });

    if (!res.ok) {
      const errBody = await res.text();
      throw new Error(`Failed to query file '${fileName}' in folder '${folderId}' (HTTP ${res.status}): ${errBody}`);
    }

    const data = await res.json();
    const files = (data.files || []).filter(f => !f.trashed);
    if (files.length > 0) {
      if (files.length > 1) {
        console.warn(`[EVE JARVIS SYNC] Notice: Multiple non-trashed files named '${fileName}' found in folder '${folderId}'. Using primary document ID: ${files[0].id}`);
      }
      return files[0];
    }
    return null;
  }

  /**
   * Updates an existing Google Drive file in-place, PRESERVING its stable document ID.
   * Supports Shared Drives and personal Drives.
   * @param {string} fileId
   * @param {string} content
   * @param {string} [mimeType='text/markdown']
   * @returns {Promise<{ id: string, action: string }>}
   */
  async updateFile(fileId, content, mimeType = 'text/markdown') {
    const url = `https://www.googleapis.com/upload/drive/v3/files/${fileId}?uploadType=media&supportsAllDrives=true`;

    const res = await this.fetchWithRetry(url, {
      method: 'PATCH',
      headers: {
        'Content-Type': `${mimeType}; charset=utf-8`
      },
      body: content
    });

    if (!res.ok) {
      const errBody = await res.text();
      if (res.status === 403 && (errBody.includes('storageQuotaExceeded') || errBody.includes('quota'))) {
        throw new Error(
          `Google Drive API Error (403 storageQuotaExceeded): Storage quota exceeded. Note: Google Service Accounts do not have storage quota in personal Google Drive folders. To fix this for personal Google accounts, use Google User OAuth credentials (GDRIVE_CLIENT_ID, GDRIVE_CLIENT_SECRET, GDRIVE_REFRESH_TOKEN) or a Google Workspace Shared Drive.`
        );
      }
      throw new Error(`Failed to update file '${fileId}' (HTTP ${res.status}): ${errBody}`);
    }

    const data = await res.json();
    return { id: data.id || fileId, action: 'updated' };
  }

  /**
   * Creates a new file inside the designated parent folder using multipart upload.
   * Supports Shared Drives and personal Drives.
   * @param {string} folderId
   * @param {string} fileName
   * @param {string} content
   * @param {string} [mimeType='text/markdown']
   * @returns {Promise<{ id: string, action: string }>}
   */
  async createFile(folderId, fileName, content, mimeType = 'text/markdown') {
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

    const url = 'https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&supportsAllDrives=true';

    const res = await this.fetchWithRetry(url, {
      method: 'POST',
      headers: {
        'Content-Type': `multipart/related; boundary=${boundary}`
      },
      body: multipartBody
    });

    if (!res.ok) {
      const errBody = await res.text();
      if (res.status === 403 && (errBody.includes('storageQuotaExceeded') || errBody.includes('quota'))) {
        throw new Error(
          `Google Drive API Error (403 storageQuotaExceeded): Storage quota exceeded. Note: Google Service Accounts do not have storage quota in personal Google Drive folders. To fix this for personal Google accounts, use Google User OAuth credentials (GDRIVE_CLIENT_ID, GDRIVE_CLIENT_SECRET, GDRIVE_REFRESH_TOKEN) or a Google Workspace Shared Drive.`
        );
      }
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
   * Supports Shared Drives.
   * @param {string} fileId
   * @returns {Promise<Array<{ id: string, type: string, role: string, emailAddress?: string }>>}
   */
  async listPermissions(fileId) {
    const url = `https://www.googleapis.com/drive/v3/files/${fileId}/permissions?fields=permissions(id,type,role,emailAddress)&supportsAllDrives=true`;

    const res = await this.fetchWithRetry(url, {
      headers: { 'Accept': 'application/json' }
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

      const url = `https://www.googleapis.com/drive/v3/files/${fileId}/permissions?sendNotificationEmail=false&supportsAllDrives=true`;
      const res = await this.fetchWithRetry(url, {
        method: 'POST',
        headers: {
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
    const perms = await this.listPermissions(fileId);
    const publicPerms = perms.filter(p => p.type === 'anyone');

    let removed = 0;
    for (const p of publicPerms) {
      const url = `https://www.googleapis.com/drive/v3/files/${fileId}/permissions/${p.id}?supportsAllDrives=true`;
      const res = await this.fetchWithRetry(url, {
        method: 'DELETE'
      });
      if (res.ok) {
        removed++;
      }
    }
    return removed;
  }
}
