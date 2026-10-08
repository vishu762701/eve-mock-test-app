import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtempSync, writeFileSync, readFileSync, chmodSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { spawnSync } from 'node:child_process';

function run(fail = false) {
  const directory = mkdtempSync(join(tmpdir(), 'eve-smoke-'));
  try {
    const curl = join(directory, 'curl');
    writeFileSync(curl, `#!/usr/bin/env bash
url="\${@: -1}"
case "$url" in
  *api.cloudflare.com*) echo '{"subdomain":"test-only"}';;
  */api/health) printf '{"status":"ok"}\\nHTTP_STATUS:200';;
  */api/banners|*/api/app-content/*) printf '{"success":true}\\nHTTP_STATUS:200';;
  */api/exams) if [ "$EVE_FAIL_GUARD" = 1 ]; then printf '{"success":true}\\nHTTP_STATUS:200'; else printf '{}\\nHTTP_STATUS:401'; fi;;
  *) printf '{}\\nHTTP_STATUS:401';;
esac
`);
    chmodSync(curl, 0o755);
    const summary = join(directory, 'summary.md');
    const result = spawnSync('bash', ['scripts/smoke-test.sh'], { cwd: new URL('../', import.meta.url), encoding: 'utf8', env: { ...process.env, PATH: `${directory}:${process.env.PATH}`, CLOUDFLARE_API_TOKEN: '', SUPABASE_SERVICE_ROLE_KEY: '', DIAGNOSTIC_KEY: '', GITHUB_STEP_SUMMARY: summary, EVE_FAIL_GUARD: fail ? '1' : '0' } });
    return { result, summary: readFileSync(summary, 'utf8') };
  } finally { rmSync(directory, { recursive: true, force: true }); }
}
test('deployment summary never claims unexecuted AI or diagnostic checks passed', () => {
  const { result, summary } = run();
  assert.equal(result.status, 0, result.stderr);
  assert.match(summary, /AI Test Generation.*NOT RUN/);
  assert.match(summary, /Authenticated D1 Operations.*SKIPPED/);
  assert.match(summary, /Supabase Storage Operations.*SKIPPED/);
  assert.match(summary, /Authentication Guard.*PASS/);
});
test('deployment summary records a failed auth guard and exits unsuccessfully', () => {
  const { result, summary } = run(true);
  assert.equal(result.status, 1);
  assert.match(summary, /Authentication Guard.*FAIL/);
});
