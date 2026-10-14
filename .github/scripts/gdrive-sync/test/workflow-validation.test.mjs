import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const REPO_ROOT = path.resolve(__dirname, '../../../..');

test('GitHub Actions workflow file sync-jarvis-gemini.yml satisfies all architectural invariants', () => {
  const workflowPath = path.join(REPO_ROOT, '.github/workflows/sync-jarvis-gemini.yml');
  assert.ok(fs.existsSync(workflowPath), 'Workflow file must exist');

  const content = fs.readFileSync(workflowPath, 'utf8');

  // 1. Triggers
  assert.match(content, /push:\s*\n\s*branches:\s*\[\s*main\s*\]/, 'Must trigger on push to main');
  assert.match(content, /workflow_run:\s*\n\s*workflows:\s*\[\s*"Build Eve APK"\s*\]/, 'Must trigger on Build Eve APK completion');
  assert.match(content, /workflow_dispatch:/, 'Must support workflow_dispatch');

  // 2. Concurrency
  assert.match(content, /cancel-in-progress:\s*false/, 'Concurrency must not cancel running sync in progress to prevent partial writes');

  // 3. Commit SHA consistency
  assert.match(content, /GITHUB_SHA:\s*\${{\s*github\.event\.workflow_run\.head_sha\s*\|\|\s*github\.sha\s*}}/, 'Must prioritize workflow_run head SHA');

  // 4. Secret masking
  assert.match(content, /::add-mask::/, 'Must mask secrets in runner log');

  // 5. Dual-mode secrets
  assert.match(content, /GDRIVE_REFRESH_TOKEN/, 'Must support GDRIVE_REFRESH_TOKEN');
  assert.match(content, /GDRIVE_CLIENT_ID/, 'Must support GDRIVE_CLIENT_ID');
  assert.match(content, /GDRIVE_CLIENT_SECRET/, 'Must support GDRIVE_CLIENT_SECRET');
  assert.match(content, /GDRIVE_SERVICE_ACCOUNT_KEY/, 'Must support GDRIVE_SERVICE_ACCOUNT_KEY');
  // 6. Explicit Checkout Ref on workflow_run
  assert.match(content, /ref:\s*\${{\s*github\.event\.workflow_run\.head_sha\s*\|\|\s*github\.ref\s*}}/, 'Must explicitly checkout completed build head_sha');
});
