#!/usr/bin/env node

/**
 * Command-Line Interface for EVE JARVIS Gemini Sync
 *
 * Usage:
 *   node .github/scripts/gdrive-sync/cli.mjs [--dry-run] [--out <dir>]
 */

import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { runSync } from './sync-engine.mjs';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const REPO_ROOT = path.resolve(__dirname, '../../..');

const args = process.argv.slice(2);
const dryRun = args.includes('--dry-run');
const outIndex = args.indexOf('--out');
const outDir = outIndex !== -1 && args[outIndex + 1] ? path.resolve(process.cwd(), args[outIndex + 1]) : undefined;

runSync({
  rootDir: REPO_ROOT,
  dryRun,
  outDir
})
  .then(res => {
    console.log('\n--- Sync Execution Summary ---');
    console.log(`Status: ${res.status} (${res.mode})`);
    console.log(`Commit: ${res.commitSha}`);
    console.log(`Included Files: ${res.includedCount}`);
    console.log(`Excluded Files: ${res.excludedCount}`);

    if (res.syncedDocuments) {
      console.log('\nGoogle Drive Documents:');
      for (const doc of res.syncedDocuments) {
        console.log(` - [${doc.action.toUpperCase()}] ${doc.filename} -> ID: ${doc.id}`);
      }
    } else if (res.outDir) {
      console.log(`Local Output Directory: ${res.outDir}`);
    }

    process.exit(0);
  })
  .catch(err => {
    console.error(`\n❌ [FATAL] Sync failed: ${err.message}`);
    if (err.stack) console.error(err.stack);
    process.exit(1);
  });
