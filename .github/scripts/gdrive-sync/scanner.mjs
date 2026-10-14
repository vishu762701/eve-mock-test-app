/**
 * File Scanner and Categorizer for EVE JARVIS Gemini Sync
 * Discovers repository files, applies security exclusions, and assigns files to knowledge volumes.
 */

import fs from 'node:fs';
import path from 'node:path';
import { isSensitivePath } from './security.mjs';

export const BINARY_EXTENSIONS = new Set([
  '.png', '.jpg', '.jpeg', '.gif', '.webp', '.ico',
  '.ttf', '.otf', '.woff', '.woff2',
  '.binobj', '.apk', '.aab', '.jar', '.aar', '.so', '.dylib', '.dll', '.exe',
  '.zip', '.tar', '.gz', '.7z',
  '.mp3', '.mp4', '.wav', '.flac',
  '.pyc', '.pyo', '.pyd'
]);

export const IGNORED_DIRECTORIES = new Set([
  '.git',
  '.gradle',
  '.gemini',
  '.idea',
  '.vscode',
  'build',
  'node_modules',
  'dist',
  'coverage',
  '.husky',
  '__pycache__'
]);

export const VOLUME_DEFINITIONS = [
  {
    id: '01_ENGINEERING_RULES_AND_STANDARDS',
    title: 'Engineering Rules, Standards & Architecture',
    filename: '01_ENGINEERING_RULES_AND_STANDARDS.md',
    description: 'Standing engineering rules, architectural decisions, audit history, and backlog.'
  },
  {
    id: '02_ANDROID_CORE_AND_UI',
    title: 'Android Production Kotlin Source Code',
    filename: '02_ANDROID_CORE_AND_UI.md',
    description: 'All production Kotlin UI, ViewModels, repository layers, and custom view implementations.'
  },
  {
    id: '03_ANDROID_LAYOUTS_AND_RESOURCES',
    title: 'Android Layouts, Themes & Resources',
    filename: '03_ANDROID_LAYOUTS_AND_RESOURCES.md',
    description: 'Production XML layouts, Apple iOS 26 typography styles, colors, and theme attributes.'
  },
  {
    id: '04_BACKEND_WORKER_AND_STORAGE',
    title: 'Backend Cloudflare Worker & D1 Database',
    filename: '04_BACKEND_WORKER_AND_STORAGE.md',
    description: 'Hono TypeScript server routes, server-authoritative scoring, D1 schema, and migrations.'
  },
  {
    id: '05_TEST_SUITES_AND_VERIFICATION',
    title: 'Automated Test Suites & Verification',
    filename: '05_TEST_SUITES_AND_VERIFICATION.md',
    description: 'Android JVM unit tests, native UI verification, backend integration tests, and linters.'
  },
  {
    id: '06_CI_CD_WORKFLOWS_AND_SCRIPTS',
    title: 'CI/CD Workflows, Automation & Build Configuration',
    filename: '06_CI_CD_WORKFLOWS_AND_SCRIPTS.md',
    description: 'GitHub Actions workflows, automated build scripts, and Gradle build configurations.'
  }
];

/**
 * Assigns a repository-relative path to a target volume.
 * @param {string} relPath
 * @returns {string|null} Volume ID or null if unassigned
 */
export function assignVolume(relPath) {
  const norm = relPath.replace(/\\/g, '/');

  // 1. Engineering Rules, Standards & Docs
  if (
    norm === 'ENGINEERING_STANDARD.md' ||
    norm === 'SOURCE_OF_TRUTH.md' ||
    norm === 'AGENTS.md' ||
    norm === 'GEMINI.md' ||
    norm === 'CLAUDE.md' ||
    norm === 'DECISIONS.md' ||
    norm === 'BACKLOG.md' ||
    norm === 'APPROVED_UI_IMPLEMENTATION_AUDIT.md' ||
    norm === 'README.md' ||
    norm === 'CODEX_STATE.md' ||
    norm === 'PROGRESS.md' ||
    norm === 'firestore.rules' ||
    (norm.startsWith('docs/') && norm.endsWith('.md'))
  ) {
    return '01_ENGINEERING_RULES_AND_STANDARDS';
  }

  // 5. Test Suites (checked before main code)
  if (
    norm.startsWith('app/src/test/') ||
    norm.startsWith('app/src/androidTest/') ||
    norm.startsWith('backend/test/') ||
    norm.startsWith('.github/scripts/tests/')
  ) {
    return '05_TEST_SUITES_AND_VERIFICATION';
  }

  // 2. Android Core & UI (Kotlin source)
  if (norm.startsWith('app/src/main/java/') && norm.endsWith('.kt')) {
    return '02_ANDROID_CORE_AND_UI';
  }

  // 3. Android Layouts & Resources (XML)
  if (norm.startsWith('app/src/main/res/') && norm.endsWith('.xml')) {
    return '03_ANDROID_LAYOUTS_AND_RESOURCES';
  }

  // 4. Backend Worker & Storage
  if (
    norm.startsWith('backend/src/') ||
    norm === 'backend/schema.sql' ||
    norm.startsWith('backend/migrations/') ||
    norm === 'backend/wrangler.toml' ||
    norm === 'backend/package.json'
  ) {
    return '04_BACKEND_WORKER_AND_STORAGE';
  }

  // 6. CI/CD Workflows, Automation & Build Configuration
  if (
    norm.startsWith('.github/workflows/') ||
    norm.startsWith('.github/scripts/') ||
    norm === 'build.gradle.kts' ||
    norm === 'app/build.gradle.kts' ||
    norm === 'settings.gradle.kts' ||
    norm === 'gradlew'
  ) {
    return '06_CI_CD_WORKFLOWS_AND_SCRIPTS';
  }

  return null;
}

/**
 * Recursively scans directory and categorizes files into included volumes or excluded list.
 * @param {string} rootDir
 * @returns {{ included: Array<{ path: string, volume: string, size: number, lineCount: number }>, excluded: Array<{ path: string, reason: string }> }}
 */
export function scanRepository(rootDir) {
  const included = [];
  const excluded = [];

  function walk(currentDir) {
    const entries = fs.readdirSync(currentDir, { withFileTypes: true });

    for (const entry of entries) {
      const fullPath = path.join(currentDir, entry.name);
      const relPath = path.relative(rootDir, fullPath).replace(/\\/g, '/');

      // Directory filtering
      if (entry.isDirectory()) {
        if (IGNORED_DIRECTORIES.has(entry.name)) {
          excluded.push({ path: relPath, reason: `Ignored directory (${entry.name})` });
          continue;
        }
        walk(fullPath);
        continue;
      }

      // Check for sensitive credential path
      if (isSensitivePath(relPath)) {
        excluded.push({ path: relPath, reason: 'Credential or secret file pattern' });
        continue;
      }

      // Check binary extension
      const ext = path.extname(entry.name).toLowerCase();
      if (BINARY_EXTENSIONS.has(ext)) {
        excluded.push({ path: relPath, reason: `Binary file format (${ext})` });
        continue;
      }

      // Check specific untracked/ignored files
      if (relPath === 'functions/package-lock.json') {
        excluded.push({ path: relPath, reason: 'Untracked lockfile per SOURCE_OF_TRUTH.md' });
        continue;
      }
      if (entry.name === '.DS_Store' || entry.name === 'Thumbs.db') {
        excluded.push({ path: relPath, reason: 'OS metadata file' });
        continue;
      }

      // Assign volume
      const volume = assignVolume(relPath);
      if (!volume) {
        excluded.push({ path: relPath, reason: 'Outside designated Gemini Knowledge scope' });
        continue;
      }

      // Inspect file stats and lines
      try {
        const stat = fs.statSync(fullPath);
        const content = fs.readFileSync(fullPath, 'utf8');
        const lines = content.split('\n');

        included.push({
          path: relPath,
          volume,
          size: stat.size,
          lineCount: lines.length
        });
      } catch (err) {
        excluded.push({ path: relPath, reason: `Unreadable or binary text error: ${err.message}` });
      }
    }
  }

  walk(rootDir);

  // Sort included files by path
  included.sort((a, b) => a.path.localeCompare(b.path));
  excluded.sort((a, b) => a.path.localeCompare(b.path));

  return { included, excluded };
}
