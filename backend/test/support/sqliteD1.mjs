import { DatabaseSync } from 'node:sqlite';
import { readdirSync, readFileSync } from 'node:fs';

/** Executes real migrations/SQL. Batches are synchronous transactions like D1. */
export function isolatedD1() {
  const sqlite = new DatabaseSync(':memory:');
  sqlite.exec('PRAGMA foreign_keys=ON');
  const dir = new URL('../../migrations/', import.meta.url);
  for (const file of readdirSync(dir).filter(f=>f.endsWith('.sql')).sort()) sqlite.exec(readFileSync(new URL(file,dir),'utf8'));
  const d1 = {
    prepare(sql) {
      const make = (values=[]) => ({
        bind(...args) { return make(args); },
        execute() { const r=sqlite.prepare(sql).run(...values); return {meta:{changes:r.changes}}; },
        async run() { return this.execute(); },
        async first() { return sqlite.prepare(sql).get(...values) || null; },
        async all() { return {results:sqlite.prepare(sql).all(...values)}; }
      });
      return make();
    },
    async batch(statements) {
      sqlite.exec('BEGIN');
      try { const result=statements.map(s=>s.execute()); sqlite.exec('COMMIT'); return result; }
      catch(error) { sqlite.exec('ROLLBACK'); throw error; }
    }
  };
  return {sqlite,d1};
}
