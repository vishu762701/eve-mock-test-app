/**
 * End-to-End Synchronization Engine for EVE JARVIS Gemini Knowledge
 * Orchestrates scanning, sanitization, status fetching, formatting, and Google Drive publishing.
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
 * @param {string} rootDir
 * @returns {string}
 */
export function resolveCommitSha(rootDir) {
  if (process.env.GITHUB_SHA) {
    return process.env.GITHUB_SHA;
  }
  try {
    return execSync('git rev-parse HEAD', { cwd: rootDir, encoding: 'utf8' }).trim();
  } catch {
    return '0000000000000000000000000000000000000000';
  }
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
 * @param {string} [options.folderId] Google Drive folder ID
 * @param {string[]} [options.authorizedUsers] Authorized Google account emails
 * @param {boolean} [options.dryRun=false] Force dry run (local files only)
 * @param {string} [options.outDir] Output directory for local files
 * @param {typeof fetch} [options.fetchFn] Custom fetch (for testing)
 * @returns {Promise<object>} Execution result summary
 */
export async function runSync(options) {
  const rootDir = options.rootDir || process.cwd();
  const commitSha = options.commitSha || resolveCommitSha(rootDir);
  const branch = options.branch || resolveBranch(rootDir);
  const triggerEvent = options.triggerEvent || process.env.GITHUB_EVENT_NAME || 'manual';
  const outDir = options.outDir || path.join(rootDir, 'build/jarvis-gemini-knowledge');

  const serviceAccountKey = options.serviceAccountKey || process.env.GDRIVE_SERVICE_ACCOUNT_KEY;
  const folderId = options.folderId || process.env.GDRIVE_FOLDER_ID;
  const rawUsers = options.authorizedUsers !== undefined
    ? options.authorizedUsers
    : (process.env.GDRIVE_AUTHORIZED_USERS ? process.env.GDRIVE_AUTHORIZED_USERS.split(',') : []);
  const authorizedUsers = Array.isArray(rawUsers) ? rawUsers : [rawUsers];

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
  const hasCredentials = Boolean(serviceAccountKey && folderId);
  const isDryRun = options.dryRun || !hasCredentials;

  if (isDryRun) {
    if (!hasCredentials && !options.dryRun) {
      console.warn('[EVE JARVIS SYNC] Notice: GDRIVE_SERVICE_ACCOUNT_KEY and/or GDRIVE_FOLDER_ID are not set.');
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
  console.log(`[EVE JARVIS SYNC] Connecting to Google Drive (folder: ${folderId})...`);
  const client = new GDriveClient(serviceAccountKey, options.fetchFn);

  // Enforce privacy: Remove any accidental public permissions on the target folder
  const publicPermsRemoved = await client.removePublicPermissions(folderId);
  if (publicPermsRemoved > 0) {
    console.warn(`[EVE JARVIS SYNC] Removed ${publicPermsRemoved} public 'anyone' permissions on Drive folder.`);
  }

  // Share folder with authorized accounts
  if (authorizedUsers.length > 0) {
    const shareResults = await client.shareWithUsers(folderId, authorizedUsers, 'reader');
    console.log(`[EVE JARVIS SYNC] Configured access for ${shareResults.length} authorized user account(s).`);
  }

  // Sync all 6 volume documents (preserving stable file IDs)
  const syncResults = [];

  for (const vol of volumeMap.values()) {
    console.log(`[EVE JARVIS SYNC] Syncing volume '${vol.definition.filename}'...`);
    const res = await client.syncDocument(folderId, vol.definition.filename, vol.content, 'text/markdown');
    syncResults.push({
      filename: vol.definition.filename,
      id: res.id,
      action: res.action,
      sizeBytes: vol.sizeBytes,
      fileCount: vol.fileCount
    });
  }

  // Sync master index document
  console.log(`[EVE JARVIS SYNC] Syncing master index '00_INDEX_AND_SYSTEM_STATUS.md'...`);
  const indexRes = await client.syncDocument(folderId, '00_INDEX_AND_SYSTEM_STATUS.md', indexContent, 'text/markdown');
  syncResults.unshift({
    filename: '00_INDEX_AND_SYSTEM_STATUS.md',
    id: indexRes.id,
    action: indexRes.action,
    sizeBytes: Buffer.byteLength(indexContent, 'utf8'),
    fileCount: 1
  });

  console.log(`[EVE JARVIS SYNC] Successfully synchronized ${syncResults.length} documents to Google Drive folder ${folderId}.`);

  return {
    status: 'SYNCED',
    mode: 'live',
    commitSha,
    branch,
    includedCount: included.length,
    excludedCount: excluded.length,
    folderId,
    syncedDocuments: syncResults
  };
}
