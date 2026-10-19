import { test } from 'node:test';
import assert from 'node:assert/strict';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { assignVolume, scanRepository } from '../scanner.mjs';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const REPO_ROOT = path.resolve(__dirname, '../../../..');

test('assignVolume maps files to authoritative knowledge volumes', () => {
  assert.strictEqual(assignVolume('ENGINEERING_STANDARD.md'), '01_ENGINEERING_RULES_AND_STANDARDS');
  assert.strictEqual(assignVolume('SOURCE_OF_TRUTH.md'), '01_ENGINEERING_RULES_AND_STANDARDS');
  assert.strictEqual(assignVolume('DECISIONS.md'), '01_ENGINEERING_RULES_AND_STANDARDS');
  assert.strictEqual(assignVolume('firestore.rules'), '01_ENGINEERING_RULES_AND_STANDARDS');
  assert.strictEqual(assignVolume('docs/design-reset/report.md'), '01_ENGINEERING_RULES_AND_STANDARDS');

  assert.strictEqual(assignVolume('app/src/main/java/com/eve/app/ui/test/TestActivity.kt'), '02_ANDROID_CORE_AND_UI');
  assert.strictEqual(assignVolume('app/src/main/res/layout/activity_main.xml'), '03_ANDROID_LAYOUTS_AND_RESOURCES');
  assert.strictEqual(assignVolume('app/src/main/res/values/themes.xml'), '03_ANDROID_LAYOUTS_AND_RESOURCES');

  assert.strictEqual(assignVolume('backend/src/index.ts'), '04_BACKEND_WORKER_AND_STORAGE');
  assert.strictEqual(assignVolume('backend/schema.sql'), '04_BACKEND_WORKER_AND_STORAGE');
  assert.strictEqual(assignVolume('backend/migrations/0001_initial.sql'), '04_BACKEND_WORKER_AND_STORAGE');

  assert.strictEqual(assignVolume('app/src/test/java/com/eve/app/ExampleUnitTest.kt'), '05_TEST_SUITES_AND_VERIFICATION');
  assert.strictEqual(assignVolume('backend/test/scoring.test.mjs'), '05_TEST_SUITES_AND_VERIFICATION');
  assert.strictEqual(assignVolume('.github/scripts/tests/test_design_system.py'), '05_TEST_SUITES_AND_VERIFICATION');

  assert.strictEqual(assignVolume('.github/workflows/build.yml'), '06_CI_CD_WORKFLOWS_AND_SCRIPTS');
  assert.strictEqual(assignVolume('build.gradle.kts'), '06_CI_CD_WORKFLOWS_AND_SCRIPTS');
  assert.strictEqual(assignVolume('app/build.gradle.kts'), '06_CI_CD_WORKFLOWS_AND_SCRIPTS');
});

test('scanRepository strictly excludes binaries and keystores and includes core code', () => {
  const { included, excluded } = scanRepository(REPO_ROOT);

  assert.ok(included.length > 500, `Expected > 500 included files, got ${included.length}`);
  assert.ok(excluded.length > 50, `Expected > 50 excluded files, got ${excluded.length}`);

  // Keystore must be strictly in excluded list
  const keystoreExcluded = excluded.find(e => e.path === 'app/eve-debug.keystore');
  assert.ok(keystoreExcluded, 'app/eve-debug.keystore must be in excluded list');
  assert.match(keystoreExcluded.reason, /credential|secret/i);

  // PNG files must be excluded
  const pngExcluded = excluded.filter(e => e.path.endsWith('.png'));
  assert.ok(pngExcluded.length > 0, 'PNG files must be excluded');

  // Verify core architectural files are included
  const includedPaths = new Set(included.map(i => i.path));
  assert.ok(includedPaths.has('ENGINEERING_STANDARD.md'));
  assert.ok(includedPaths.has('SOURCE_OF_TRUTH.md'));
  assert.ok(includedPaths.has('app/src/main/java/com/eve/app/ui/test/TestActivity.kt'));
  assert.ok(includedPaths.has('backend/src/index.ts'));
  assert.ok(includedPaths.has('app/src/main/res/values/themes.xml'));
});
