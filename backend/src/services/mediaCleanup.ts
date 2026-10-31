import { Env } from '../types';
import { SupabaseStorage } from '../supabase';

/** Run only AFTER the record mutation is confirmed. A cleanup failure must not
 * remove media from a still-live record or be silently reported as complete. */
export async function cleanupMedia(env: Env, path: string, requestId: string | null | undefined): Promise<boolean> {
  try { await new SupabaseStorage(env).deleteFile(path); return true; }
  catch {
    try {
      await env.DB.prepare('INSERT INTO operation_events (id,operation,timestamp,category,status,retryable,correlation_id) VALUES (?,?,?,?,?,?,?)')
        .bind(crypto.randomUUID(), 'media_cleanup', Date.now(), 'MEDIA_CLEANUP_INCOMPLETE', 502, 0, requestId || crypto.randomUUID()).run();
    } catch { /* Diagnostic storage cannot reverse a confirmed record mutation. */ }
    return false;
  }
}


/** Keep shared assets alive and batch storage requests after the DB transaction. */
export async function cleanupEmojiMedia(env: Env, paths: string[], requestId?: string | null): Promise<boolean> {
  const candidates = [...new Set(paths.map(path => path.replace(/^\/+/, ""))
    .filter(path => path && !path.startsWith("res:raw/")))];
  if (!candidates.length) return true;
  try {
    const shared = await env.DB.prepare(
      "SELECT DISTINCT ltrim(storage_key, '/') AS storage_key FROM emoji_items WHERE ltrim(storage_key, '/') IN (SELECT value FROM json_each(?))"
    ).bind(JSON.stringify(candidates)).all<{ storage_key: string }>();
    const keep = new Set((shared.results || []).map(item => item.storage_key));
    await new SupabaseStorage(env).deleteFiles(candidates.filter(path => !keep.has(path)));
    return true;
  } catch {
    try {
      await env.DB.prepare('INSERT INTO operation_events (id,operation,timestamp,category,status,retryable,correlation_id) VALUES (?,?,?,?,?,?,?)')
        .bind(crypto.randomUUID(), 'emoji_media_cleanup', Date.now(), 'MEDIA_CLEANUP_INCOMPLETE', 502, 0, requestId || crypto.randomUUID()).run();
    } catch { /* A diagnostic outage never rolls back a confirmed deletion. */ }
    return false;
  }
}
