#!/usr/bin/env node

/**
 * Google User OAuth 2.0 Token Helper for EVE JARVIS Gemini Sync
 * Pure Node.js (Zero external dependencies).
 *
 * Helps users generate a Google OAuth Refresh Token for personal Google accounts (@gmail.com)
 * with the least-privilege scope: https://www.googleapis.com/auth/drive.file
 *
 * Security:
 * - Never prints raw secrets/tokens in terminal stdout.
 * - Saves confidential tokens to .jarvis-oauth-secrets.env with 0600 file permissions.
 * - Supports reading secrets via environment variables to avoid exposure in process tables (ps).
 */

import fs from 'node:fs';
import path from 'node:path';
import readline from 'node:readline';

const REDIRECT_URI = 'https://developers.google.com/oauthplayground';
const SCOPE = 'https://www.googleapis.com/auth/drive.file';

function parseArgs() {
  const args = {};
  for (const arg of process.argv.slice(2)) {
    if (arg.startsWith('--')) {
      const eqIdx = arg.indexOf('=');
      if (eqIdx !== -1) {
        const key = arg.slice(2, eqIdx).replace(/-([a-z])/g, (_, c) => c.toUpperCase());
        args[key] = arg.slice(eqIdx + 1);
      } else {
        args[arg.slice(2)] = true;
      }
    }
  }
  return args;
}

function maskSecret(str) {
  if (!str) return '***';
  if (str.length <= 8) return '********';
  return str.slice(0, 4) + '...' + str.slice(-4);
}

function promptInput(query) {
  const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout
  });
  return new Promise(resolve => {
    rl.question(query, answer => {
      rl.close();
      resolve(answer.trim());
    });
  });
}

async function main() {
  const args = parseArgs();
  let clientId = args.clientId || process.env.GDRIVE_CLIENT_ID;
  let clientSecret = args.clientSecret || process.env.GDRIVE_CLIENT_SECRET;
  let code = args.code;

  if (!clientId) {
    console.log(`
=== EVE JARVIS — Google User OAuth Setup Helper ===

This tool generates a Refresh Token for your personal Google account, allowing GitHub Actions
to upload EVE knowledge volumes to Google Drive using your personal storage quota.

Step 1: In Google Cloud Console (https://console.cloud.google.com/apis/credentials):
  - Click "Create Credentials" -> "OAuth Client ID"
  - Application type: "Web application"
  - Name: "EVE JARVIS Sync"
  - Authorized redirect URIs: Add "${REDIRECT_URI}"
  - Click "Create" and copy the Client ID & Client Secret.

Step 2: Generate Authorization URL:
  Run:
    GDRIVE_CLIENT_ID=your_id node .github/scripts/gdrive-sync/oauth-helper.mjs
`);
    process.exit(0);
  }

  if (!code) {
    const authUrl = new URL('https://accounts.google.com/o/oauth2/v2/auth');
    authUrl.searchParams.set('client_id', clientId);
    authUrl.searchParams.set('redirect_uri', REDIRECT_URI);
    authUrl.searchParams.set('response_type', 'code');
    authUrl.searchParams.set('scope', SCOPE);
    authUrl.searchParams.set('access_type', 'offline');
    authUrl.searchParams.set('prompt', 'consent');

    console.log(`
=== EVE JARVIS — Google Authorization URL ===

1. Open this URL in your web browser:
${authUrl.toString()}

2. Sign in with the Google Account that owns your EVE JARVIS Gemini Gem.
3. Grant permission to access Google Drive files created by this app.
4. Google will redirect to OAuth Playground. Copy the "code" parameter from the browser URL or page:
   (Example: 4/0AQl6...)

5. Run helper with the code:
   GDRIVE_CLIENT_ID=${clientId} node .github/scripts/gdrive-sync/oauth-helper.mjs --code=YOUR_CODE
`);
    process.exit(0);
  }

  if (!clientSecret) {
    // Prompt securely without passing secret in CLI flags
    clientSecret = await promptInput('Enter Google OAuth Client Secret (hidden from process list): ');
    if (!clientSecret) {
      console.error('Error: Client Secret is required.');
      process.exit(1);
    }
  }

  console.log('Exchanging authorization code for Google OAuth Refresh Token...');

  const params = new URLSearchParams({
    code,
    client_id: clientId,
    client_secret: clientSecret,
    redirect_uri: REDIRECT_URI,
    grant_type: 'authorization_code'
  });

  const res = await fetch('https://oauth2.googleapis.com/token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: params.toString()
  });

  if (!res.ok) {
    const errText = await res.text();
    console.error(`\n❌ Token exchange failed (HTTP ${res.status}): ${errText}`);
    process.exit(1);
  }

  const data = await res.json();
  const refreshToken = data.refresh_token;

  if (!refreshToken) {
    console.warn('\n⚠️ Warning: No refresh token returned. If you already authorized this app before, you must revoke access at https://myaccount.google.com/permissions or pass prompt=consent.');
  }

  // Write secrets securely to local .jarvis-oauth-secrets.env with 0600 permissions
  const secretsPath = path.resolve(process.cwd(), '.jarvis-oauth-secrets.env');
  const envContent = [
    `# EVE JARVIS Google Drive Credentials (CONFIDENTIAL - DO NOT SHARE)`,
    `GDRIVE_CLIENT_ID=${clientId}`,
    `GDRIVE_CLIENT_SECRET=${clientSecret}`,
    `GDRIVE_REFRESH_TOKEN=${refreshToken || ''}`,
    ``
  ].join('\n');

  fs.writeFileSync(secretsPath, envContent, { mode: 0o600 });

  console.log(`
✅ Success! Google OAuth tokens generated.

🔒 Secrets securely written to: ${path.basename(secretsPath)} (mode 0600 - user read/write only).
(This file is protected by .gitignore and will never be committed).

Summary of Credentials (Masked):
--------------------------------------------------------------------------------
GDRIVE_CLIENT_ID:      ${clientId}
GDRIVE_CLIENT_SECRET:  ${maskSecret(clientSecret)}
GDRIVE_REFRESH_TOKEN:  ${maskSecret(refreshToken)}
--------------------------------------------------------------------------------

To complete setup, copy the values from .jarvis-oauth-secrets.env into GitHub Repository Secrets:
https://github.com/vishu762701/eve-mock-test-app/settings/secrets/actions
`);
}

main().catch(err => {
  console.error('Fatal error:', err);
  process.exit(1);
});
