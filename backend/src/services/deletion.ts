import { Env } from '../types';

/** Counts only: never disclose student identifiers, tokens or answers. All saved
 * sessions remain protected, including paused/elapsed sessions awaiting a retry.
 * There is no approved retention policy that permits purging them here. */
export async function deletionInfo(db: Env['DB'], id: string, kind: 'exam' | 'test') {
  const key = kind === 'exam'
    ? "exam_id = ?1 OR substr(exam_id, 1, length(?1) + 2) = ?1 || '__'"
    : "exam_id = ?1 OR substr(exam_id, -length(?1) - 2) = '__' || ?1";
  const row = await db.prepare(`SELECT
    (SELECT COUNT(*) FROM attempts WHERE ${key}) AS attempts,
    (SELECT COUNT(*) FROM attempt_sessions WHERE ${key.replaceAll('exam_id', 'exam_key')}) AS sessions,
    (SELECT COUNT(*) FROM exams WHERE parent_exam_id = ?1 AND ?2 = 'exam') AS subExams,
    (SELECT COUNT(*) FROM generation_jobs WHERE exam_id = ?1 AND status = 'running' AND ?2 = 'exam') AS generationJobs`)
    .bind(id, kind).first<{ attempts: number; sessions: number; subExams: number; generationJobs: number }>();
  const counts = row!;
  const code = counts.attempts ? 'DELETE_BLOCKED_ATTEMPTS' : counts.sessions ? 'DELETE_BLOCKED_SESSIONS'
    : counts.subExams ? 'DELETE_BLOCKED_SUB_EXAMS' : counts.generationJobs ? 'DELETE_BLOCKED_GENERATION' : null;
  const message = counts.attempts ? `${counts.attempts} saved student attempt(s) depend on this record. Unpublish tests to preserve their history.`
    : counts.sessions ? `${counts.sessions} saved student session(s) depend on this record. Unpublish tests instead; paused sessions and submission retries are protected.`
    : counts.subExams ? `${counts.subExams} sub-exam(s) depend on this exam. Manage those records first.`
    : counts.generationJobs ? 'Question generation is running for this exam. Wait for it to finish before deleting.' : null;
  return { ...counts, canDelete: code === null, code, message, safeAction: counts.attempts || counts.sessions ? 'unpublish' : null };
}

/** Recheck protections inside the atomic batch, closing the read/delete race. */
export async function deleteUnused(db: Env['DB'], id: string, kind: 'exam' | 'test') {
  const match = kind === 'exam'
    ? "exam_id = ?1 OR substr(exam_id, 1, length(?1) + 2) = ?1 || '__'"
    : "exam_id = ?1 OR substr(exam_id, -length(?1) - 2) = '__' || ?1";
  const guard = `NOT EXISTS (SELECT 1 FROM attempts WHERE ${match})
    AND NOT EXISTS (SELECT 1 FROM attempt_sessions WHERE ${match.replaceAll('exam_id', 'exam_key')})`
    + (kind === 'exam' ? " AND NOT EXISTS (SELECT 1 FROM exams WHERE parent_exam_id = ?1) AND NOT EXISTS (SELECT 1 FROM generation_jobs WHERE exam_id = ?1 AND status = 'running')" : '');
  const statements = kind === 'exam' ? [
    db.prepare(`DELETE FROM questions WHERE exam_id = ?1 AND ${guard}`).bind(id),
    db.prepare(`DELETE FROM generated_tests WHERE exam_id = ?1 AND ${guard}`).bind(id),
    db.prepare(`DELETE FROM exams WHERE id = ?1 AND ${guard}`).bind(id),
  ] : [db.prepare(`DELETE FROM generated_tests WHERE id = ?1 AND ${guard}`).bind(id)];
  const result = await db.batch(statements);
  return result[result.length - 1].meta.changes === 1;
}
