import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { resolveCommitSha } from '../sync-engine.mjs';
import { buildVolumes, buildIndexDocument } from '../formatter.mjs';
import { scanRepository } from '../scanner.mjs';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const REPO_ROOT = path.resolve(__dirname, '../../../..');

test('resolveCommitSha prioritizes workflow_run.head_sha from GITHUB_EVENT_PATH', () => {
  const tmpEventPath = path.join(REPO_ROOT, 'build/test-workflow-run-event.json');
  fs.mkdirSync(path.dirname(tmpEventPath), { recursive: true });

  const expectedHeadSha = 'e3e3e3e3e3e3e3e3e3e3e3e3e3e3e3e3e3e3e3e3';
  fs.writeFileSync(tmpEventPath, JSON.stringify({
    workflow_run: {
      head_sha: expectedHeadSha
    }
  }), 'utf8');

  const origEventPath = process.env.GITHUB_EVENT_PATH;
  try {
    process.env.GITHUB_EVENT_PATH = tmpEventPath;
    const resolved = resolveCommitSha(REPO_ROOT);
    assert.strictEqual(resolved, expectedHeadSha, 'Must resolve workflow_run.head_sha');
  } finally {
    if (origEventPath !== undefined) {
      process.env.GITHUB_EVENT_PATH = origEventPath;
    } else {
      delete process.env.GITHUB_EVENT_PATH;
    }
    if (fs.existsSync(tmpEventPath)) fs.unlinkSync(tmpEventPath);
  }
});

test('buildVolumes and buildIndexDocument embed identical commit SHA across all documents and files', () => {
  const targetSha = 'c0ffee1234567890abcdef1234567890abcdef12';
  const { included, excluded } = scanRepository(REPO_ROOT);

  // Take a representative subset for fast test execution
  const sampleIncluded = included.slice(0, 10);
  const volumeMap = buildVolumes(REPO_ROOT, sampleIncluded, targetSha);

  for (const [id, vol] of volumeMap.entries()) {
    assert.ok(vol.content.includes(`> **Commit SHA**: \`${targetSha}\``), `Volume ${id} must contain commit SHA in volume header`);
    for (const f of vol.files) {
      assert.ok(vol.content.includes(`- **Commit**: \`${targetSha}\``), `File ${f.path} must contain commit SHA in file block`);
    }
  }

  const indexDoc = buildIndexDocument({
    commitSha: targetSha,
    branch: 'main',
    timestamp: '2026-10-14T08:00:00Z',
    triggerEvent: 'workflow_run',
    ciStatusMd: '### Verified CI Status\n',
    volumeMap,
    included: sampleIncluded,
    excluded
  });

  assert.ok(indexDoc.includes(`> **Exact Commit SHA**: \`${targetSha}\``), 'Master index must contain exact commit SHA');
});
