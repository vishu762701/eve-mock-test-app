import test from 'node:test';
import assert from 'node:assert/strict';
import { DatabaseSync } from 'node:sqlite';
import { readdirSync, readFileSync } from 'node:fs';
import { assessPreflight, schemaSql } from '../scripts/production-preflight.mjs';

const dir = new URL('../migrations/', import.meta.url);
const local = readdirSync(dir).filter(name => name.endsWith('.sql')).sort();
function fixture(count = 24) {
  const db = new DatabaseSync(':memory:');
  for (const name of local.slice(0, count)) db.exec(readFileSync(new URL(name, dir), 'utf8'));
  const input = { local, applied: local.slice(0, count), schema: db.prepare(schemaSql).all(), bookmark: '00000001-00000002-00000003-00000004' };
  return { db, input };
}

test('read-only preflight identifies exactly reviewed pending production migrations', () => {
  const { db, input } = fixture();
  try {
    const before = db.prepare('SELECT count(*) n FROM sqlite_master').get().n;
    assert.deepEqual(assessPreflight(input).pending, local.slice(24));
    assert.equal(db.prepare('SELECT count(*) n FROM sqlite_master').get().n, before);
    assert.deepEqual(Object.keys(input.schema[0]).sort(), ['column_name','column_type','default_value','primary_key','required','table_name']);
  } finally { db.close(); }
});

test('all real migrations satisfy post-migration identity/bio/presence contracts', () => {
  const { db, input } = fixture(local.length);
  try { assert.deepEqual(assessPreflight({ ...input, postMigration: true }).pending, []); }
  finally { db.close(); }
});

test('pre-existing runtime-added bio stops duplicate ALTER before any migration', () => {
  const { db, input } = fixture();
  try {
    db.exec("ALTER TABLE users ADD COLUMN bio TEXT NOT NULL DEFAULT ''");
    assert.throws(() => assessPreflight({ ...input, schema: db.prepare(schemaSql).all() }), /migration 0025 drift/);
  } finally { db.close(); }
});

test('unknown or gapped remote migration history fails closed', () => {
  const { db, input } = fixture();
  try {
    assert.throws(() => assessPreflight({ ...input, applied: [...input.applied, '0030_unreviewed.sql'] }), /absent from this revision/);
    assert.throws(() => assessPreflight({ ...input, applied: input.applied.filter((_, i) => i !== 10) }), /contains a gap/);
    assert.throws(() => assessPreflight({ ...input, applied: [...input.applied, input.applied[0]] }), /duplicate/);
    assert.throws(() => assessPreflight({ ...input, applied: input.applied.slice(0, -1) }), /baseline migration history/);
  } finally { db.close(); }
});

test('missing recovery bookmark and unconfirmed post-migration inventory fail closed', () => {
  const { db, input } = fixture();
  try {
    for (const bookmark of [undefined, '', 'invalid bookmark']) assert.throws(() => assessPreflight({ ...input, bookmark }), /Time Travel bookmark/);
    assert.throws(() => assessPreflight({ ...input, postMigration: true }), /remain pending/);
  } finally { db.close(); }
});

test('missing legacy identity and incompatible existing presence schema stop rollout', () => {
  const { db, input } = fixture();
  try {
    assert.throws(() => assessPreflight({ ...input, schema: input.schema.filter(r => r.column_name !== 'eve_id') }), /users.eve_id/);
    db.exec('CREATE TABLE user_presence_sessions(user_id TEXT)');
    assert.throws(() => assessPreflight({ ...input, schema: db.prepare(schemaSql).all() }), /user_presence_sessions.session_id/);
  } finally { db.close(); }
});

test('post-migration rejects changed bio null/default semantics', () => {
  const { db, input } = fixture(local.length);
  try {
    const schema = input.schema.map(r => r.table_name === 'users' && r.column_name === 'bio' ? { ...r, required: 0 } : r);
    assert.throws(() => assessPreflight({ ...input, schema, postMigration: true }), /non-null empty-text contract/);
  } finally { db.close(); }
});

test('presence requires composite per-device ownership key, not just matching field names', () => {
  const { db, input } = fixture(local.length);
  try {
    const schema = input.schema.map(r => r.table_name === 'user_presence_sessions' && r.column_name === 'session_id' ? { ...r, primary_key: 0 } : r);
    assert.throws(() => assessPreflight({ ...input, schema }), /per-device lease constraint/);
  } finally { db.close(); }
});
