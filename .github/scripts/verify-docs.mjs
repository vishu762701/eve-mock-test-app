#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const REPO_ROOT = path.resolve(__dirname, '../..');

const REQUIRED_FILES = [
  'README.md',
  'AGENTS.md',
  'GEMINI.md',
  'CLAUDE.md',
  'ENGINEERING_STANDARD.md',
  'SOURCE_OF_TRUTH.md',
  'APPROVED_UI_IMPLEMENTATION_AUDIT.md',
  'CODEX_STATE.md'
];

let errorsCount = 0;
let warningsCount = 0;

function error(msg) {
  console.error(`❌ [ERROR] ${msg}`);
  errorsCount++;
}

function warn(msg) {
  console.warn(`⚠️  [WARN]  ${msg}`);
  warningsCount++;
}

function info(msg) {
  console.log(`ℹ️  [INFO]  ${msg}`);
}

function success(msg) {
  console.log(`✅ [PASS]  ${msg}`);
}

// 1. Verify required files exist
info('Checking required repository documentation files...');
for (const relPath of REQUIRED_FILES) {
  const fullPath = path.join(REPO_ROOT, relPath);
  if (!fs.existsSync(fullPath)) {
    error(`Required documentation file missing: ${relPath}`);
  } else {
    const stat = fs.statSync(fullPath);
    if (!stat.isFile()) {
      error(`Required path is not a file: ${relPath}`);
    } else if (stat.size === 0) {
      error(`Required documentation file is empty: ${relPath}`);
    } else {
      success(`Found ${relPath} (${stat.size} bytes)`);
    }
  }
}

// 1b. Keep agent entrypoints aligned so retired or conflicting instructions
// cannot silently replace the canonical repository rules.
const ENTRYPOINT = 'Always read and follow ENGINEERING_STANDARD.md first. Then read SOURCE_OF_TRUTH.md.';
for (const relPath of ['AGENTS.md', 'GEMINI.md', 'CLAUDE.md']) {
  const fullPath = path.join(REPO_ROOT, relPath);
  if (!fs.existsSync(fullPath) || !fs.statSync(fullPath).isFile()) continue;
  const content = fs.readFileSync(fullPath, 'utf8');
  const canonicalStart = relPath === 'AGENTS.md'
    ? content.startsWith('# TELEGRAM FIDELITY CONTRACT\n') &&
      content.slice(0, content.indexOf('# Repository agent entrypoint')).includes(ENTRYPOINT)
    : content.startsWith(ENTRYPOINT);
  if (!canonicalStart) {
    error(`${relPath}: Agent entrypoint no longer begins with canonical engineering/source instructions`);
  } else {
    success(`${relPath}: Canonical instruction entrypoint present`);
  }
  if (relPath !== 'AGENTS.md' && !content.includes('Follow AGENTS.md')) {
    error(`${relPath}: Missing reference to shared AGENTS.md policy`);
  }
}

// 2. Discover all markdown files
function findMarkdownFiles(dir) {
  const files = [];
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  for (const entry of entries) {
    if (path.resolve(dir, entry.name) === path.join(REPO_ROOT, 'third_party', 'telegram-android')) continue;
    if (entry.name === 'node_modules' || entry.name === '.git' || entry.name === 'build' || entry.name === '.gradle') {
      continue;
    }
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      files.push(...findMarkdownFiles(full));
    } else if (entry.isFile() && entry.name.endsWith('.md')) {
      files.push(full);
    }
  }
  return files;
}

const mdFiles = findMarkdownFiles(REPO_ROOT);
info(`Found ${mdFiles.length} Markdown documentation files to validate.`);

// Patterns for secret detection
const SECRET_PATTERNS = [
  { name: 'Private Key block', regex: /-----BEGIN (RSA |EC |DSA |OPENSSH |PGP )?PRIVATE KEY-----/ },
  { name: 'GitHub Personal Access Token', regex: /(?:ghp|gho|ghu|ghs|ghr)_[A-Za-z0-9_]{36}/ },
  { name: 'Fine-grained GitHub Token', regex: /github_pat_[A-Za-z0-9_]{82}/ },
  { name: 'Slack Bot/User Token', regex: /xox[baprs]-[0-9]{10,13}-[0-9]{10,13}-[a-zA-Z0-9]{24}/ },
  { name: 'AWS Access Key ID', regex: /(?:A3T[A-Z0-9]|AKIA|AGPA|AIDA|AROA|AIPA|ANPA|ANVA|ASIA)[A-Z0-9]{16}/ },
  { name: 'Hardcoded password assignment', regex: /(?:password|secret|api_key|apikey)\s*[:=]\s*["'][A-Za-z0-9@#$%^&*-_+=]{16,}["']/i }
];
function detectPotentialSecrets(text) {
  return SECRET_PATTERNS.filter(({ regex }) => regex.test(text)).map(({ name }) => name);
}

// Security regression contracts run on every documentation verification.
// These strings are synthetic fixtures, never real credentials.
for (const testCase of [
  { name: 'normal prose', content: 'Documentation with no tokens', expected: 0 },
  { name: 'template placeholder', content: 'api_key = "<REDACTED>"', expected: 0 },
  { name: 'example prose must not hide a token', content: 'Example configuration\\nsecret = "SyntheticCredentialValue123456789"', expected: 1 },
  { name: 'REDACTED elsewhere must not hide a token', content: 'REDACTED\\napi_key = "SyntheticCredentialValue123456789"', expected: 1 },
  { name: 'dummy elsewhere must not hide a token', content: 'dummy\\npassword = "SyntheticCredentialValue123456789"', expected: 1 }
]) {
  const found = detectPotentialSecrets(testCase.content);
  if (found.length !== testCase.expected) {
    error(`Security scanner regression: ${testCase.name} (expected ${testCase.expected}, found ${found.length})`);
  }
}

for (const filePath of mdFiles) {
  const relPath = path.relative(REPO_ROOT, filePath);
  let content = '';

  // Valid UTF-8 check
  try {
    const raw = fs.readFileSync(filePath);
    content = new TextDecoder('utf-8', { fatal: true }).decode(raw);
  } catch (err) {
    error(`${relPath}: Invalid UTF-8 encoding (${err.message})`);
    continue;
  }

  // Non-empty check
  if (content.trim().length === 0) {
    error(`${relPath}: File is completely empty`);
    continue;
  }

  // Structural readability: balanced code fences
  const codeFenceMatches = content.match(/^```/gm);
  if (codeFenceMatches && codeFenceMatches.length % 2 !== 0) {
    warn(`${relPath}: Unbalanced Markdown code fences (odd count: ${codeFenceMatches.length})`);
  }

  // Secrets scan: evaluate every real signature, regardless of surrounding text.
  // A mention of "example" or "REDACTED" elsewhere must never suppress detection.
  for (const pattern of SECRET_PATTERNS) {
    if (pattern.regex.test(content)) {
      error(`${relPath}: Potential secret detected matching pattern "${pattern.name}"`);
    }
  }

  // Local relative reference verification
  const linkRegex = /\[([^\]]+)\]\(([^)]+)\)/g;
  let match;
  while ((match = linkRegex.exec(content)) !== null) {
    const rawTarget = match[2].trim();
    if (
      rawTarget.startsWith('http://') ||
      rawTarget.startsWith('https://') ||
      rawTarget.startsWith('mailto:') ||
      rawTarget.startsWith('tel:') ||
      rawTarget.startsWith('#')
    ) {
      continue;
    }

    // Clean anchor and parameters
    let targetPath = rawTarget.split('#')[0].split('?')[0].trim();
    if (!targetPath) continue;

    // Handle file:// absolute workspace links generated by termux/IDE
    const prefix = 'file:///data/data/com.termux/files/home/eve-mock-test-app/';
    if (targetPath.startsWith(prefix)) {
      targetPath = targetPath.slice(prefix.length);
    } else if (targetPath.startsWith('file://')) {
      targetPath = targetPath.slice(7);
    }

    let resolved;
    if (path.isAbsolute(targetPath)) {
      resolved = targetPath;
    } else {
      // First try resolving relative to containing markdown file
      resolved = path.resolve(path.dirname(filePath), targetPath);
      if (!fs.existsSync(resolved)) {
        // Fallback: try resolving relative to repo root
        const fromRoot = path.resolve(REPO_ROOT, targetPath);
        if (fs.existsSync(fromRoot)) {
          resolved = fromRoot;
        }
      }
    }

    if (!fs.existsSync(resolved)) {
      // If it looks like a code anchor or hypothetical command link, warn instead of hard fail
      if (targetPath.includes('*') || targetPath.includes('<') || targetPath.endsWith('.png') || targetPath.endsWith('.jpg')) {
        warn(`${relPath}: Unresolved media/wildcard link target: "${targetPath}"`);
      } else {
        warn(`${relPath}: Broken local file link: "${targetPath}" (resolved as ${resolved})`);
      }
    }
  }
}

console.log('\n--- Documentation Verification Summary ---');
console.log(`Files validated: ${mdFiles.length}`);
console.log(`Errors: ${errorsCount}`);
console.log(`Warnings: ${warningsCount}`);

if (errorsCount > 0) {
  console.error('\nDocumentation validation FAILED.');
  process.exit(1);
} else {
  console.log('\nDocumentation validation PASSED successfully.');
  process.exit(0);
}
