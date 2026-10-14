import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  formatFileWithLineNumbers,
  buildIndexDocument
} from '../formatter.mjs';

test('formatFileWithLineNumbers prefixes every line with line numbers and metadata', () => {
  const filePath = 'app/src/main/java/com/eve/app/ui/test/TestActivity.kt';
  const content = 'package com.eve.app.ui.test\n\nimport android.os.Bundle\nclass TestActivity';
  const commitSha = 'a3d3144513ae320562e57a597db2ca76893fdd10';

  const output = formatFileWithLineNumbers(filePath, content, commitSha);

  assert.match(output, /## File: app\/src\/main\/java\/com\/eve\/app\/ui\/test\/TestActivity\.kt/);
  assert.match(output, /- \*\*Commit\*\*: `a3d3144513ae320562e57a597db2ca76893fdd10`/);
  assert.match(output, /- \*\*Lines\*\*: 1–4/);
  assert.match(output, /```kotlin/);
  assert.match(output, /1: package com\.eve\.app\.ui\.test/);
  assert.match(output, /2: /);
  assert.match(output, /3: import android\.os\.Bundle/);
  assert.match(output, /4: class TestActivity/);
});

test('buildIndexDocument includes all required index sections and invariant rules', () => {
  const dummyVolumeMap = new Map([
    [
      '01_ENGINEERING_RULES_AND_STANDARDS',
      {
        definition: {
          id: '01_ENGINEERING_RULES_AND_STANDARDS',
          title: 'Engineering Rules',
          filename: '01_ENGINEERING_RULES_AND_STANDARDS.md',
          description: 'Rules'
        },
        fileCount: 5,
        lineCount: 500,
        sizeBytes: 15000
      }
    ]
  ]);

  const md = buildIndexDocument({
    commitSha: 'a3d3144513ae320562e57a597db2ca76893fdd10',
    branch: 'main',
    timestamp: '2026-10-14T07:30:00Z',
    triggerEvent: 'push',
    ciStatusMd: '| Workflow | Status |\n| Build | Pass |\n',
    volumeMap: dummyVolumeMap,
    included: [{ path: 'ENGINEERING_STANDARD.md', volume: '01_ENGINEERING_RULES_AND_STANDARDS', lines: 68, size: 7600 }],
    excluded: [{ path: 'app/eve-debug.keystore', reason: 'Credential file' }]
  });

  assert.match(md, /# EVE MOCK TEST APP — JARVIS GEMINI KNOWLEDGE INDEX/);
  assert.match(md, /`a3d3144513ae320562e57a597db2ca76893fdd10`/);
  assert.match(md, /Permanent Architectural Invariants/);
  assert.match(md, /#FFFFFF/);
  assert.match(md, /Telegram circular reveal/);
  assert.match(md, /Complete Included Files Index \(1 Files\)/);
  assert.match(md, /Complete Excluded Files Index \(1 Files\)/);
  assert.match(md, /app\/eve-debug\.keystore/);
});
