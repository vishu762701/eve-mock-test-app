import { execFileSync } from 'node:child_process';
import { readdirSync, appendFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { resolve } from 'node:path';

// Schema metadata only: never export users, tokens, answers or account records.
export const schemaSql = `SELECT m.name AS table_name, p.name AS column_name,
  p.type AS column_type, p."notnull" AS required, p.dflt_value AS default_value, p.pk AS primary_key
  FROM sqlite_master m JOIN pragma_table_info(m.name) p
  WHERE m.type = 'table' AND m.name IN
  ('users','admins','admin_rights','app_content','admin_audit_log',
   'user_presence_sessions','emoji_packs','emoji_items','feedback_admin_replies')`;

const requiredColumns = {
  users: ['id', 'email', 'eve_id', 'username', 'phone', 'dob', 'category'],
  admins: ['email'], admin_rights: ['email'], app_content: ['id', 'body'],
  admin_audit_log: ['id'], emoji_packs: ['id'], emoji_items: ['id', 'pack_id'],
};

export function assessPreflight({ local, applied, schema, bookmark, postMigration = false }) {
  if (typeof bookmark !== 'string' || !/^[a-zA-Z0-9-]+$/.test(bookmark)) {
    throw new Error('A valid current D1 Time Travel bookmark is required before migration.');
  }
  if (!local.length || new Set(local).size !== local.length || new Set(applied).size !== applied.length) {
    throw new Error('Invalid or duplicate migration inventory.');
  }
  const sorted = [...local].sort();
  if (sorted.some((name, index) => !/^\d{4}_[\w-]+\.sql$/.test(name) || Number(name.slice(0, 4)) !== index + 1)) {
    throw new Error('Local migration history must be contiguous and ordered.');
  }
  const unknown = applied.filter(name => !local.includes(name));
  if (unknown.length) throw new Error(`Remote migrations absent from this revision: ${unknown.join(', ')}`);
  const pending = sorted.filter(name => !applied.includes(name));
  if (applied.some(name => pending.some(missing => missing < name))) {
    throw new Error('Remote migration history contains a gap; do not apply blindly.');
  }
  if (pending.some(name => Number(name.slice(0, 4)) < 25)) {
    throw new Error('Pre-0025 baseline migration history requires separate reconciliation before this rollout.');
  }
  const columns = table => new Set(schema.filter(row => row.table_name === table).map(row => row.column_name));
  for (const [table, required] of Object.entries(requiredColumns)) {
    for (const column of required) {
      if (!columns(table).has(column)) throw new Error(`Missing baseline schema ${table}.${column}`);
    }
  }
  const bioApplied = applied.includes('0025_user_bio.sql');
  if (bioApplied !== columns('users').has('bio')) {
    throw new Error('users.bio / migration 0025 drift: reconcile add-column history safely before rollout.');
  }
  if (bioApplied) {
    const bio = schema.find(row => row.table_name === 'users' && row.column_name === 'bio');
    if (bio.column_type.toUpperCase() !== 'TEXT' || Number(bio.required) !== 1 || bio.default_value !== "''") {
      throw new Error('users.bio does not match the reviewed non-null empty-text contract.');
    }
  }
  for (const [migration, table, fields] of [
    ['0026_user_presence_sessions.sql', 'user_presence_sessions', ['user_id', 'session_id', 'expires_at']],
    ['0029_feedback_reply_persistence.sql', 'feedback_admin_replies', ['message_id', 'reply_text', 'replied_at']],
  ]) {
    if (applied.includes(migration) || columns(table).size) {
      for (const field of fields) {
        if (!columns(table).has(field)) throw new Error(`Incompatible schema ${table}.${field}`);
      }
      if (table === 'user_presence_sessions') {
        for (const [field, type, key] of [['user_id', 'TEXT', 1], ['session_id', 'TEXT', 2], ['expires_at', 'INTEGER', 0]]) {
          const column = schema.find(row => row.table_name === table && row.column_name === field);
          if (column.column_type.toUpperCase() !== type || Number(column.required) !== 1 || Number(column.primary_key) !== key) {
            throw new Error(`Incompatible per-device lease constraint ${table}.${field}`);
          }
        }
      }
    }
  }
  if (postMigration && pending.length) throw new Error(`Migrations remain pending: ${pending.join(', ')}`);
  return { pending, appliedCount: applied.length, bookmark };
}

function wranglerJson(args) {
  const output = execFileSync(process.execPath, ['node_modules/wrangler/bin/wrangler.js', ...args], {
    encoding: 'utf8', timeout: 120_000, maxBuffer: 8 * 1024 * 1024,
    env: { ...process.env, WRANGLER_SEND_METRICS: 'false', NO_COLOR: '1' },
    stdio: ['ignore', 'pipe', 'pipe'],
  });
  return JSON.parse(output);
}

function readRows(sql) {
  const response = wranglerJson(['d1', 'execute', 'eve-database', '--remote', '--json', '--command', sql]);
  if (!Array.isArray(response) || response.length !== 1 || response[0].success !== true || !Array.isArray(response[0].results)) {
    throw new Error('D1 did not confirm a successful schema read.');
  }
  return response[0].results;
}

function main() {
  if (!process.env.CLOUDFLARE_API_TOKEN) throw new Error('CLOUDFLARE_API_TOKEN is unavailable.');
  const local = readdirSync('migrations').filter(name => name.endsWith('.sql')).sort();
  // Unlike `migrations list`, these SELECTs cannot create a migration table.
  const applied = readRows('SELECT name FROM d1_migrations ORDER BY id').map(row => row.name);
  const schema = readRows(schemaSql);
  const recovery = wranglerJson(['d1', 'time-travel', 'info', 'eve-database', '--json']);
  const report = assessPreflight({ local, applied, schema, bookmark: recovery.bookmark,
    postMigration: process.argv.includes('--post-migration') });
  const summary = { ...report, revision: process.env.GITHUB_SHA ?? 'local', checkedAt: new Date().toISOString(),
    mode: process.argv.includes('--post-migration') ? 'post-migration' : 'read-only-preflight', schema };
  console.log(JSON.stringify(summary, null, 2));
  if (process.env.GITHUB_STEP_SUMMARY) {
    appendFileSync(process.env.GITHUB_STEP_SUMMARY, `\n### D1 ${summary.mode}\nRevision: ${summary.revision}\n\nApplied: ${report.appliedCount}; pending: ${report.pending.join(', ') || 'none'}\n\nTime Travel recovery bookmark: ${report.bookmark}\n\nNo account records or database dump exported. Bookmark recovery is retention-limited; do not restore without assessing writes since this point.\n`);
  }
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try { main(); } catch (error) {
    console.error(`Production preflight failed: ${error.message}`);
    process.exitCode = 1;
  }
}
