import { MiddlewareHandler } from 'hono';
import { AuthUser, Env } from '../types';

export function errorCodeForStatus(status: number): string {
  return ({ 400: 'INVALID_REQUEST', 401: 'AUTH_REQUIRED', 403: 'FORBIDDEN',
    404: 'NOT_FOUND', 409: 'CONFLICT', 413: 'PAYLOAD_TOO_LARGE', 415: 'UNSUPPORTED_MEDIA_TYPE',
    422: 'VALIDATION_FAILED', 429: 'RATE_LIMITED', 500: 'INTERNAL_ERROR',
    502: 'UPSTREAM_UNAVAILABLE', 503: 'SERVICE_UNAVAILABLE', 504: 'UPSTREAM_TIMEOUT'
  } as Record<number, string>)[status] || 'REQUEST_FAILED';
}

/** Keep legacy error text while giving every API failure a machine-readable code. */
export const apiErrorContract: MiddlewareHandler = async (c, next) => {
  await next();
  if (!c.req.path.startsWith('/api/') || c.res.status < 400) return;
  let original: Record<string, unknown> = {};
  try {
    const parsed = await c.res.clone().json();
    if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) original = parsed as Record<string, unknown>;
  } catch { /* Non-JSON framework errors receive the same public contract. */ }
  const status = c.res.status;
  const body = { ...original, success: false,
    error: typeof original.error === 'string' && original.error.trim() ? original.error : 'The request could not be completed. Please retry.',
    code: typeof original.code === 'string' && original.code.trim() ? original.code : errorCodeForStatus(status),
    requestId: c.res.headers.get('X-Request-ID') || undefined,
  };
  const headers = new Headers(c.res.headers);
  headers.delete('Content-Length');
  headers.set('Content-Type', 'application/json; charset=UTF-8');
  c.res = new Response(JSON.stringify(body), { status, headers });
};

const textFields = new Set(['email', 'examId', 'questionId', 'examName', 'title', 'message',
  'question', 'questionText', 'optionA', 'optionB', 'optionC', 'optionD', 'explanation',
  'category', 'topic', 'pyqPaper', 'generationPrompt', 'customPromptNotes', 'autoGenTime',
  'timezone', 'testNumber', 'negativeMarkingText', 'syllabus', 'syllabusFileName', 'imageUrl',
  'linkUrl', 'linkLabel', 'customTitle', 'packName', 'packTitle', 'altEmoji', 'url', 'label', 'planName']);
const booleanFields = new Set(['enabled', 'active', 'isLifetime', 'confirmLifetime', 'isPyq', 'autoGenerationEnabled']);
const numericFields = new Set(['timeLimitMinutes', 'questionCount', 'durationDays', 'pyqYear']);

/** Authenticate first; validate admin JSON without consuming the route's request body. */
export const adminJsonContract: MiddlewareHandler<{ Bindings: Env; Variables: { user: AuthUser } }> = async (c, next) => {
  if (!c.get('user')?.isAdmin || !['POST', 'PUT', 'PATCH'].includes(c.req.method) ||
      !c.req.header('Content-Type')?.toLowerCase().includes('application/json') || c.req.raw.body === null) return next();
  let body: unknown;
  try { body = await c.req.raw.clone().json(); }
  catch { return c.json({ success: false, code: 'INVALID_JSON', error: 'Provide a valid JSON object.' }, 400); }
  if (!body || typeof body !== 'object' || Array.isArray(body)) {
    return c.json({ success: false, code: 'INVALID_BODY', error: 'Provide a JSON object.' }, 400);
  }
  for (const [key, value] of Object.entries(body)) {
    if (value === undefined || value === null) continue; // Route-specific required/clear semantics remain authoritative.
    const invalidText = textFields.has(key) && typeof value !== 'string';
    const invalidBoolean = booleanFields.has(key) && typeof value !== 'boolean';
    const invalidNumber = numericFields.has(key) &&
      !((typeof value === 'number' || (typeof value === 'string' && value.trim() !== '')) && Number.isFinite(Number(value)));
    if (invalidText || invalidBoolean || invalidNumber) {
      return c.json({ success: false, code: 'INVALID_FIELD_TYPE', error: `Invalid value type for ${key}.` }, 400);
    }
  }
  return next();
};
