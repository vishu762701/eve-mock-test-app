// ============================================================================
// D1 Rate Limiting Middleware (Two-Layer Architecture)
// ============================================================================

import { Context, Next } from "hono";
import { AuthUser, Env } from "../types";

/**
 * Executes atomic upsert on rate_limits table using SQLite RETURNING.
 * Fails open on D1 database error.
 */
export async function checkRateLimit(
  db: D1Database,
  key: string,
  limit: number,
  windowSeconds: number = 60
): Promise<{ limited: boolean; retryAfter: number }> {
  const now = Math.floor(Date.now() / 1000);
  const resetAt = now + windowSeconds;

  try {
    const res = await db
      .prepare(
        `INSERT INTO rate_limits (key, count, reset_at)
         VALUES (?, 1, ?)
         ON CONFLICT(key) DO UPDATE SET
           count = CASE WHEN reset_at <= ? THEN 1 ELSE count + 1 END,
           reset_at = CASE WHEN reset_at <= ? THEN ? ELSE reset_at END
         RETURNING count, reset_at`
      )
      .bind(key, resetAt, now, now, resetAt)
      .first<{ count: number; reset_at: number }>();

    if (res && res.count > limit) {
      const retryAfter = Math.max(1, (res.reset_at || resetAt) - now);
      return { limited: true, retryAfter };
    }
  } catch (_e) {
    // Fail open on D1 error
  }

  return { limited: false, retryAfter: 0 };
}

/**
 * Layer 1 (BEFORE authMiddleware):
 * Per-IP rate limiting: 300 requests / 60 seconds.
 */
export function ipRateLimit() {
  return async (c: Context<{ Bindings: Env; Variables: { user?: AuthUser } }>, next: Next) => {
    const ip = c.req.header("cf-connecting-ip") || c.req.header("x-forwarded-for") || "unknown";
    const key = `ip:${ip.trim()}`;

    const { limited, retryAfter } = await checkRateLimit(c.env.DB, key, 300, 60);
    if (limited) {
      c.header("Retry-After", String(retryAfter));
      return c.json(
        {
          success: false,
          error: "Too many requests. Please wait before retrying.",
        },
        429
      );
    }

    return next();
  };
}

/**
 * Layer 2 (AFTER authMiddleware):
 * Per-user rate limiting:
 * - General: 120 / 60 s
 * - POST /api/attempts/submit: 10 / 60 s
 * - POST /api/attempts/start: 20 / 60 s
 * - POST /api/generated-tests/generate-now: 5 / 60 s
 */
export function userRateLimit() {
  return async (c: Context<{ Bindings: Env; Variables: { user?: AuthUser } }>, next: Next) => {
    const user = c.get("user");
    if (!user || !user.uid) {
      return next();
    }

    const uid = user.uid;
    const path = c.req.path;
    const method = c.req.method.toUpperCase();

    // Check endpoint-specific bucket if applicable
    let endpointBucket: { name: string; limit: number } | null = null;
    if (method === "POST" && path === "/api/attempts/submit") {
      endpointBucket = { name: "submit", limit: 10 };
    } else if (method === "POST" && path === "/api/attempts/start") {
      endpointBucket = { name: "start", limit: 20 };
    } else if (method === "POST" && (path === "/api/generated-tests/generate-now" || /\/system\/jobs\/.*\/retry/.test(path))) {
      endpointBucket = { name: "generate-now", limit: 5 };
    }

    if (endpointBucket) {
      const endpointKey = `uid:${uid}:${endpointBucket.name}`;
      const { limited, retryAfter } = await checkRateLimit(c.env.DB, endpointKey, endpointBucket.limit, 60);
      if (limited) {
        c.header("Retry-After", String(retryAfter));
        return c.json(
          {
            success: false,
            error: `Rate limit exceeded for ${endpointBucket.name}. Please wait before retrying.`,
          },
          429
        );
      }
    }

    // Always check general bucket
    const generalKey = `uid:${uid}:general`;
    const { limited: generalLimited, retryAfter: generalRetryAfter } = await checkRateLimit(c.env.DB, generalKey, 120, 60);
    if (generalLimited) {
      c.header("Retry-After", String(generalRetryAfter));
      return c.json(
        {
          success: false,
          error: "Rate limit exceeded. Please wait before retrying.",
        },
        429
      );
    }

    return next();
  };
}

// Backwards-compatibility export if needed
export const rateLimit = (limit: number = 100, windowSeconds: number = 60) => ipRateLimit();
