import { MiddlewareHandler } from 'hono';

/** Diagnostic failures only; confirmed submissions come from the attempts table. */
export const operationMonitor: MiddlewareHandler = async (c, next) => {
  const requestId = crypto.randomUUID();
  c.header('X-Request-ID', requestId);
  await next();
  const monitored = c.req.path === '/api/attempts/submit' || c.req.path.includes('generate-now') || /\/api\/admin\/system\/jobs\/.*\/retry/.test(c.req.path);
  if (!monitored || c.res.status < 400 || !c.env.DB) return;
  const status = c.res.status;
  const category = status === 401 ? 'AUTHENTICATION' : status === 403 ? 'AUTHORIZATION' : status === 409 ? 'CONFLICT' : status === 429 ? 'RATE_LIMIT' : status < 500 ? 'VALIDATION' : 'BACKEND_FAILURE';
  try {
    await c.env.DB.prepare('INSERT INTO operation_events (id, operation, timestamp, category, status, retryable, correlation_id) VALUES (?, ?, ?, ?, ?, ?, ?)')
      .bind(crypto.randomUUID(), c.req.path === '/api/attempts/submit' ? 'submission' : 'generation', Date.now(), category, status, status >= 500 || status === 429 ? 1 : 0, requestId).run();
  } catch { /* Diagnostic failure must never change a student's response. */ }
};
