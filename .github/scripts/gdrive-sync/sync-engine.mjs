/**
 * End-to-End Synchronization Engine for EVE JARVIS Gemini Knowledge
 * Orchestrates scanning, sanitization, status fetching, formatting, and Google Drive publishing.
 * Supports Dual-Mode Authentication (User OAuth & Service Account).
 * Features atomic publishing: Master index is updated ONLY after all 6 volumes succeed.
 */

import fs from 'node:fs';
import path from 'node:path';
import { execSync } from 'node:child_process';
import { scanRepository } from './scanner.mjs';
import { fetchCommitStatus, formatStatusMarkdown } from './status-fetcher.mjs';
import { buildVolumes, buildIndexDocument } from './formatter.mjs';
import { GDriveClient } from './gdrive-client.mjs';

/**
 * Resolves current Git commit SHA safely.
 * Prioritizes workflow_run head SHA if triggered by workflow_run,
 * then GITHUB_SHA, then git rev-parse HEAD.
 * @param {string} rootDir
 * @returns {string}
 */
export function resolveCommitSha(rootDir) {
  let headSha = null;
  try {
    headSha = execSync('git rev-parse HEAD', { cwd: rootDir, encoding: 'utf8' }).trim();
  } catch {
    // git CLI unavailable in minimal test environments
  }

  let eventSha = null;
  if (process.env.GITHUB_EVENT_PATH && fs.existsSync(process.env.GITHUB_EVENT_PATH)) {
    try {
      const evt = JSON.parse(fs.readFileSync(process.env.GITHUB_EVENT_PATH, 'utf8'));
      if (evt.workflow_run && evt.workflow_run.head_sha) {
        eventSha = evt.workflow_run.head_sha;
      }
    } catch {
      // ignore
    }
  }

  const envSha = process.env.GITHUB_SHA;
  const targetSha = eventSha || envSha || headSha || '0000000000000000000000000000000000000000';

  if (headSha && targetSha && headSha !== targetSha) {
    console.warn(`[EVE JARVIS SYNC] ⚠️ Notice: Target commit SHA (${targetSha.slice(0, 7)}) differs from local git HEAD (${headSha.slice(0, 7)}). Ensuring packaged files and embedded metadata reflect the target commit.`);
  } else if (headSha && headSha === targetSha) {
    console.log(`[EVE JARVIS SYNC] Verified: Actual git HEAD (${headSha.slice(0, 7)}) equals embedded commit SHA.`);
  }

  return targetSha;
}

/**
 * Resolves current Git branch safely.
 * @param {string} rootDir
 * @returns {string}
 */
export function resolveBranch(rootDir) {
  if (process.env.GITHUB_REF_NAME) {
    return process.env.GITHUB_REF_NAME;
  }
  try {
    return execSync('git rev-parse --abbrev-ref HEAD', { cwd: rootDir, encoding: 'utf8' }).trim();
  } catch {
    return 'main';
  }
}

/**
 * Executes synchronization pipeline.
 * @param {object} options
 * @param {string} options.rootDir Repository root directory
 * @param {string} [options.commitSha] Git commit SHA
 * @param {string} [options.branch] Git branch
 * @param {string} [options.triggerEvent] CI event trigger
 * @param {string} [options.serviceAccountKey] Service account JSON string
 * @param {string} [options.clientId] User OAuth client ID
 * @param {string} [options.clientSecret] User OAuth client secret
 * @param {string} [options.refreshToken] User OAuth refresh token
 * @param {string} [options.folderId] Google Drive folder ID
 * @param {string[]} [options.authorizedUsers] Authorized Google account emails
 * @param {boolean} [options.dryRun=false] Force dry run (local files only)
 * @param {string} [options.outDir] Output directory for local files
 * @param {typeof fetch} [options.fetchFn] Custom fetch (for testing)
 * @returns {Promise<object>} Execution result summary
 */
export async function runSync(options = {}) {
  const rootDir = options.rootDir || process.cwd();
  const commitSha = options.commitSha || resolveCommitSha(rootDir);
  const branch = options.branch || resolveBranch(rootDir);
  const triggerEvent = options.triggerEvent || process.env.GITHUB_EVENT_NAME || 'manual';
  const outDir = options.outDir || path.join(rootDir, 'build/jarvis-gemini-knowledge');

  const serviceAccountKey = options.serviceAccountKey || process.env.GDRIVE_SERVICE_ACCOUNT_KEY;
  const clientId = options.clientId || process.env.GDRIVE_CLIENT_ID;
  const clientSecret = options.clientSecret || process.env.GDRIVE_CLIENT_SECRET;
  const refreshToken = options.refreshToken || process.env.GDRIVE_REFRESH_TOKEN;
  const folderId = options.folderId || process.env.GDRIVE_FOLDER_ID;

  const rawUsers = options.authorizedUsers !== undefined
    ? options.authorizedUsers
    : (process.env.GDRIVE_AUTHORIZED_USERS ? process.env.GDRIVE_AUTHORIZED_USERS.split(',') : []);
  const authorizedUsers = Array.isArray(rawUsers) ? rawUsers : [rawUsers];

  const hasUserOAuth = Boolean(refreshToken && clientId && clientSecret);
  const hasServiceAccount = Boolean(serviceAccountKey && (folderId || options.dryRun !== undefined));
  const hasCredentials = hasUserOAuth || hasServiceAccount;

  console.log(`[EVE JARVIS SYNC] Starting synchronization for commit ${commitSha.slice(0, 7)} on branch '${branch}'...`);

  // Step 1: Scan repository
  const { included, excluded } = scanRepository(rootDir);
  console.log(`[EVE JARVIS SYNC] Scanned repository: ${included.length} files included, ${excluded.length} files excluded.`);

  // Step 2: Fetch real CI Status
  const statusData = await fetchCommitStatus(commitSha);
  const ciStatusMd = formatStatusMarkdown(statusData, commitSha);

  // Step 3: Build Volumes & Master Index
  const volumeMap = buildVolumes(rootDir, included, commitSha);
  const timestamp = new Date().toISOString();
  const indexContent = buildIndexDocument({
    commitSha,
    branch,
    timestamp,
    triggerEvent,
    ciStatusMd,
    volumeMap,
    included,
    excluded
  });

  // Always write local output for inspection or local verification
  fs.mkdirSync(outDir, { recursive: true });
  fs.writeFileSync(path.join(outDir, '00_INDEX_AND_SYSTEM_STATUS.md'), indexContent, 'utf8');

  for (const vol of volumeMap.values()) {
    fs.writeFileSync(path.join(outDir, vol.definition.filename), vol.content, 'utf8');
  }
  console.log(`[EVE JARVIS SYNC] Wrote ${volumeMap.size + 1} structured knowledge documents to ${outDir}.`);

  // Determine if Google Drive sync can proceed
  const isDryRun = options.dryRun || !hasCredentials;

  if (isDryRun) {
    if (!hasCredentials && !options.dryRun) {
      console.warn('[EVE JARVIS SYNC] Notice: Neither Google User OAuth (GDRIVE_CLIENT_ID, GDRIVE_CLIENT_SECRET, GDRIVE_REFRESH_TOKEN) nor Service Account (GDRIVE_SERVICE_ACCOUNT_KEY) + GDRIVE_FOLDER_ID are set.');
      console.warn('[EVE JARVIS SYNC] Running in local pre-generation mode. Documents generated locally without error.');
    } else {
      console.log('[EVE JARVIS SYNC] Dry-run completed successfully.');
    }

    return {
      status: 'LOCAL_GENERATED',
      mode: 'dry-run',
      commitSha,
      branch,
      includedCount: included.length,
      excludedCount: excluded.length,
      volumeCount: volumeMap.size + 1,
      outDir,
      credentialsConfigured: hasCredentials
    };
  }

  // Step 4: Publish to Google Drive
  let client;
  let authModeDescription;
  if (hasUserOAuth) {
    authModeDescription = 'user_oauth (Personal Google Drive quota)';
    console.log(`[EVE JARVIS SYNC] Mode: Google User OAuth 2.0. Files owned by user, using personal Drive quota.`);
    client = new GDriveClient({ clientId, clientSecret, refreshToken }, options.fetchFn);
  } else {
    authModeDescription = 'service_account (Workspace / Shared Drive)';
    console.log(`[EVE JARVIS SYNC] Mode: Google Cloud Service Account.`);
    client = new GDriveClient(serviceAccountKey, options.fetchFn);
  }

  // Auto-discover or provision app-owned folder (Solves drive.file scope external folder limitation)
  const targetFolderId = await client.getOrCreateAppFolder('EVE-JARVIS-KNOWLEDGE', folderId);
  console.log(`[EVE JARVIS SYNC] Connecting to Google Drive (target folder: ${targetFolderId})...`);

  // Enforce privacy: Remove any accidental public permissions on the target folder
  const publicPermsRemoved = await client.removePublicPermissions(targetFolderId);
  if (publicPermsRemoved > 0) {
    console.warn(`[EVE JARVIS SYNC] Removed ${publicPermsRemoved} public 'anyone' permissions on Drive folder.`);
  }

  // Share folder with authorized accounts
  if (authorizedUsers.length > 0) {
    const shareResults = await client.shareWithUsers(targetFolderId, authorizedUsers, 'reader');
    console.log(`[EVE JARVIS SYNC] Configured access for ${shareResults.length} authorized user account(s).`);
  }

  // Sync all 6 volume documents first (preserving stable file IDs)
  const syncResults = [];
  const failedVolumes = [];

  for (const vol of volumeMap.values()) {
    console.log(`[EVE JARVIS SYNC] Syncing volume '${vol.definition.filename}'...`);
    try {
      const res = await client.syncDocument(targetFolderId, vol.definition.filename, vol.content, 'text/markdown');
      syncResults.push({
        filename: vol.definition.filename,
        id: res.id,
        action: res.action,
        sizeBytes: vol.sizeBytes,
        fileCount: vol.fileCount
      });
    } catch (volErr) {
      console.error(`[EVE JARVIS SYNC] ❌ Failed to sync volume '${vol.definition.filename}': ${volErr.message}`);
      failedVolumes.push({
        filename: vol.definition.filename,
        error: volErr.message
      });
      // Stop sync immediately; do NOT touch master index!
      break;
    }
  }

  // Consistent Verification Gate: If ANY volume failed, halt and DO NOT publish master index
  if (failedVolumes.length > 0) {
    const errorMsg = `Partial sync failure: Volume '${failedVolumes[0].filename}' failed to sync (${failedVolumes[0].error}). Master index '00_INDEX_AND_SYSTEM_STATUS.md' was intentionally NOT published to prevent exposing incomplete knowledge.`;
    console.error(`[EVE JARVIS SYNC] 🛑 ${errorMsg}`);
    const partialError = new Error(errorMsg);
    partialError.status = 'PARTIAL_FAILURE';
    partialError.succeededCount = syncResults.length;
    partialError.failedVolumes = failedVolumes;
    throw partialError;
  }

  // Only publish master index if ALL 6 volumes were published cleanly (Two-Phase Verification)
  console.log(`[EVE JARVIS SYNC] All ${volumeMap.size} volumes successfully synchronized. Syncing master index '00_INDEX_AND_SYSTEM_STATUS.md'...`);
  const indexRes = await client.syncDocument(targetFolderId, '00_INDEX_AND_SYSTEM_STATUS.md', indexContent, 'text/markdown');
  syncResults.unshift({
    filename: '00_INDEX_AND_SYSTEM_STATUS.md',
    id: indexRes.id,
    action: indexRes.action,
    sizeBytes: Buffer.byteLength(indexContent, 'utf8'),
    fileCount: 1
  });

  console.log(`[EVE JARVIS SYNC] Successfully synchronized all ${syncResults.length} documents to Google Drive folder ${targetFolderId}.`);

  return {
    status: 'SYNCED',
    mode: 'live',
    authMode: authModeDescription,
    commitSha,
    branch,
    includedCount: included.length,
    excludedCount: excluded.length,
    folderId: targetFolderId,
    syncedDocuments: syncResults
  };
}
