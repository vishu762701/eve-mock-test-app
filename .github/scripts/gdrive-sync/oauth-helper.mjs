#!/usr/bin/env node

/**
 * Google User OAuth 2.0 Token Helper for EVE JARVIS Gemini Sync
 * Pure Node.js (Zero external dependencies).
 *
 * Helps users generate a Google OAuth Refresh Token for personal Google accounts (@gmail.com)
 * with the least-privilege scope: https://www.googleapis.com/auth/drive.file
 *
 * Usage:
 *   node .github/scripts/gdrive-sync/oauth-helper.mjs --client-id=<ID>
 *   node .github/scripts/gdrive-sync/oauth-helper.mjs --client-id=<ID> --client-secret=<SECRET> --code=<AUTH_CODE>
 */

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

async function main() {
  const args = parseArgs();
  const clientId = args.clientId || process.env.GDRIVE_CLIENT_ID;
  const clientSecret = args.clientSecret || process.env.GDRIVE_CLIENT_SECRET;
  const code = args.code;

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
    node .github/scripts/gdrive-sync/oauth-helper.mjs --client-id=YOUR_CLIENT_ID
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

5. Exchange the code for your Refresh Token:
   Run:
     node .github/scripts/gdrive-sync/oauth-helper.mjs \\
       --client-id=${clientId} \\
       --client-secret=YOUR_CLIENT_SECRET \\
       --code=YOUR_AUTHORIZATION_CODE
`);
    process.exit(0);
  }

  if (!clientSecret) {
    console.error('Error: --client-secret is required to exchange authorization code for tokens.');
    process.exit(1);
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
  if (!data.refresh_token) {
    console.warn('\n⚠️ Warning: No refresh token returned. If you already authorized this app before, you must revoke access at https://myaccount.google.com/permissions or pass prompt=consent.');
  }

  console.log(`
✅ Success! Google OAuth tokens generated.

Add these Repository Secrets to GitHub (Settings -> Secrets and variables -> Actions):
--------------------------------------------------------------------------------
GDRIVE_CLIENT_ID:      ${clientId}
GDRIVE_CLIENT_SECRET:  ${clientSecret}
GDRIVE_REFRESH_TOKEN:  ${data.refresh_token || '(Existing token retained)'}
--------------------------------------------------------------------------------
`);
}

main().catch(err => {
  console.error('Fatal error:', err);
  process.exit(1);
});
