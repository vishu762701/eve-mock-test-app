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
