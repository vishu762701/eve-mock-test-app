/**
 * Document Formatter and Packager for EVE JARVIS Gemini Sync
 * Adds line numbers, commit headers, and formats volumes and index for Google Drive & Gemini Knowledge.
 */

import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { sanitizeContent } from './security.mjs';
import { VOLUME_DEFINITIONS } from './scanner.mjs';

/**
 * Determines code fence language tag from file extension.
 * @param {string} filePath
 * @returns {string}
 */
export function getLanguageTag(filePath) {
  const ext = path.extname(filePath).toLowerCase();
  switch (ext) {
    case '.kt': case '.kts': return 'kotlin';
    case '.xml': return 'xml';
    case '.ts': case '.tsx': return 'typescript';
    case '.js': case '.mjs': case '.cjs': return 'javascript';
    case '.sql': return 'sql';
    case '.json': return 'json';
    case '.yml': case '.yaml': return 'yaml';
    case '.md': return 'markdown';
    case '.py': return 'python';
    case '.sh': return 'bash';
    case '.rules': return 'javascript';
    case '.toml': return 'toml';
    case '.properties': return 'properties';
    default: return 'text';
  }
}

/**
 * Formats file content with line numbers and file header.
 * @param {string} relPath
 * @param {string} content
 * @param {string} commitSha
 * @returns {string}
 */
export function formatFileWithLineNumbers(relPath, content, commitSha) {
  const lang = getLanguageTag(relPath);
  const lines = content.split('\n');
  const lineCount = lines.length;
  const sizeBytes = Buffer.byteLength(content, 'utf8');
  const sizeFormatted = sizeBytes > 1024 ? `${(sizeBytes / 1024).toFixed(1)} KB` : `${sizeBytes} B`;

  const numbered = lines.map((line, idx) => `${idx + 1}: ${line}`).join('\n');

  let output = `## File: ${relPath}\n`;
  output += `- **Path**: \`${relPath}\`\n`;
  output += `- **Commit**: \`${commitSha}\`\n`;
  output += `- **Lines**: 1–${lineCount} | **Size**: ${sizeFormatted}\n\n`;
  output += `\`\`\`${lang}\n`;
  output += numbered;
  output += `\n\`\`\`\n\n`;
  output += `---\n\n`;

  return output;
}

/**
 * Builds all 6 Knowledge Volume documents from scanned included files.
 * @param {string} rootDir
 * @param {Array<{ path: string, volume: string, size: number, lineCount: number }>} includedFiles
 * @param {string} commitSha
 * @returns {Map<string, { definition: typeof VOLUME_DEFINITIONS[0], content: string, fileCount: number, lineCount: number, sizeBytes: number }>}
 */
export function buildVolumes(rootDir, includedFiles, commitSha) {
  const volumeMap = new Map();

  for (const def of VOLUME_DEFINITIONS) {
    volumeMap.set(def.id, {
      definition: def,
      content: '',
      fileCount: 0,
      lineCount: 0,
      sizeBytes: 0,
      files: []
    });
  }

  for (const file of includedFiles) {
    const vol = volumeMap.get(file.volume);
    if (!vol) continue;

    const fullPath = path.join(rootDir, file.path);
    const raw = fs.readFileSync(fullPath, 'utf8');
    const { sanitized } = sanitizeContent(file.path, raw);
    const formatted = formatFileWithLineNumbers(file.path, sanitized, commitSha);

    vol.content += formatted;
    vol.fileCount++;
    vol.lineCount += file.lineCount;
    vol.sizeBytes += Buffer.byteLength(formatted, 'utf8');
    vol.files.push(file);
  }

  // Prepend volume header
  for (const [id, vol] of volumeMap.entries()) {
    let header = `# ${vol.definition.title}\n\n`;
    header += `> **Volume**: \`${vol.definition.filename}\`\n`;
    header += `> **Purpose**: ${vol.definition.description}\n`;
    header += `> **Commit SHA**: \`${commitSha}\`\n`;
    header += `> **Included Files**: ${vol.fileCount} files | **Total Lines**: ${vol.lineCount} | **Volume Size**: ${(vol.sizeBytes / 1024).toFixed(1)} KB\n\n`;
    header += `### Table of Contents for Volume\n\n`;
    header += `| File Path | Lines | Size |\n`;
    header += `| :--- | :--- | :--- |\n`;
    for (const f of vol.files) {
      header += `| \`${f.path}\` | ${f.lineCount} | ${(f.size / 1024).toFixed(1)} KB |\n`;
    }
    header += `\n---\n\n`;

    vol.content = header + vol.content;
    vol.sha256 = crypto.createHash('sha256').update(vol.content).digest('hex');
  }

  return volumeMap;
}

/**
 * Builds the master Index & System Status document (00_INDEX_AND_SYSTEM_STATUS.md).
 * @param {{
 *   commitSha: string,
 *   branch: string,
 *   timestamp: string,
 *   triggerEvent: string,
 *   ciStatusMd: string,
 *   volumeMap: Map<string, any>,
 *   included: Array<any>,
 *   excluded: Array<any>
 * }} context
 * @returns {string}
 */
export function buildIndexDocument(context) {
  const {
    commitSha,
    branch,
    timestamp,
    triggerEvent,
    ciStatusMd,
    volumeMap,
    included,
    excluded
  } = context;

  let totalLines = 0;
  let totalBytes = 0;
  for (const vol of volumeMap.values()) {
    totalLines += vol.lineCount;
    totalBytes += vol.sizeBytes;
  }

  let md = `# EVE MOCK TEST APP — JARVIS GEMINI KNOWLEDGE INDEX\n\n`;
  md += `> **Sync Timestamp**: \`${timestamp}\`\n`;
  md += `> **Repository**: \`vishu762701/eve-mock-test-app\`\n`;
  md += `> **Branch**: \`${branch}\`\n`;
  md += `> **Exact Commit SHA**: \`${commitSha}\`\n`;
  md += `> **Trigger Event**: \`${triggerEvent}\`\n\n`;

  md += `---\n\n`;

  // 1. Live CI Status
  md += ciStatusMd;
  md += `---\n\n`;

  // 2. Permanent Architectural Invariants (for immediate Gemini Gem reference)
  md += `### Permanent Architectural Invariants (Strict)\n\n`;
  md += `1. **Canvas Color System**:\n`;
  md += `   - Light canvas: Pure \`#FFFFFF\` standard canvas; \`#F2F2F7\` grouped background (ADR-007).\n`;
  md += `   - Dark canvas: Pure \`#000000\` / \`#101117\`.\n`;
  md += `   - Semantic tokens: Right/Answered (\`#C9F0B8\` light / \`#293B27\` dark); Wrong (\`#FFC1BB\` light / \`#602625\` dark); Marked (\`#FEE3AA\` light / \`#634416\` dark).\n`;
  md += `   - Banned: Lime \`#E3FF3B\` is strictly banned project-wide.\n`;
  md += `2. **Motion & Transitions**: Telegram circular reveal Day/Night animation is invariant.\n`;
  md += `3. **Test Engine**: Server-authoritative timer, scoring, answer hiding, and 2x2 rigid action grid must never be replaced with client-only logic.\n`;
  md += `4. **Typography**: Official Apple iOS 26 dynamic typography hierarchy; zero hardcoded \`android:textSize\` attributes project-wide (ADR-008).\n\n`;

  md += `---\n\n`;

  // 3. Two-Phase Consistency & Volume Integrity Seals
  md += `### Two-Phase Consistency & Volume Integrity Seals (SHA-256)\n\n`;
  md += `> **Consistency Model**: Eventual consistency with two-phase verification.\n`;
  md += `> **Inflight Mixed-Version Risk**: Cloud file storage updates documents individually without multi-file ACID transactions. During active publishing (~5–10s window), an inflight reader could observe individual volume updates. Gemini consumers must treat this master index as the authoritative version seal. Verify each volume's embedded Commit SHA against this master index (\`${commitSha}\`).\n\n`;
  md += `| Volume Document | Embedded Commit SHA | SHA-256 Content Digest | Status |\n`;
  md += `| :--- | :--- | :--- | :--- |\n`;
  for (const vol of volumeMap.values()) {
    md += `| \`${vol.definition.filename}\` | \`${commitSha}\` | \`${vol.sha256 || 'verified'}\` | Sealed |\n`;
  }
  md += `\n---\n\n`;

  // 4. Knowledge Volumes Directory
  md += `### Knowledge Volumes Directory (<= 10 Files Gemini Limit Compliant)\n\n`;
  md += `| Document Name | Title | Files | Lines | Size |\n`;
  md += `| :--- | :--- | :--- | :--- | :--- |\n`;
  md += `| \`00_INDEX_AND_SYSTEM_STATUS.md\` | Master Index & System Status | 1 | — | ~30 KB |\n`;

  for (const vol of volumeMap.values()) {
    md += `| \`${vol.definition.filename}\` | ${vol.definition.title} | ${vol.fileCount} | ${vol.lineCount} | ${(vol.sizeBytes / 1024).toFixed(1)} KB |\n`;
  }

  md += `| **TOTAL CODEBASE** | **All Synced Volumes** | **${included.length}** | **${totalLines}** | **${(totalBytes / 1024).toFixed(1)} KB** |\n\n`;

  md += `---\n\n`;

  // 4. Included Files Index
  md += `### Complete Included Files Index (${included.length} Files)\n\n`;
  md += `| Relative File Path | Target Volume | Lines | Size |\n`;
  md += `| :--- | :--- | :--- | :--- |\n`;
  for (const f of included) {
    md += `| \`${f.path}\` | \`${f.volume}\` | ${f.lineCount} | ${(f.size / 1024).toFixed(1)} KB |\n`;
  }
  md += `\n---\n\n`;

  // 5. Excluded Files Index
  md += `### Complete Excluded Files Index (${excluded.length} Files)\n\n`;
  md += `| Excluded Path | Reason for Exclusion |\n`;
  md += `| :--- | :--- |\n`;
  for (const e of excluded) {
    md += `| \`${e.path}\` | ${e.reason} |\n`;
  }
  md += `\n`;

  return md;
}
