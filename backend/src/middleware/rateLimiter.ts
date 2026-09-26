// ============================================================================
// D1 Rate Limiting Middleware
// ============================================================================

import { Context, Next } from "hono";
import { AuthUser, Env } from "../types";

export function rateLimit(limit: number = 100, windowSeconds: number = 60) {
  return async (c: Context<{ Bindings: Env; Variables: { user?: AuthUser } }>, next: Next) => {
    const user = c.get("user");
    const ip = c.req.header("cf-connecting-ip") || c.req.header("x-forwarded-for") || "unknown";
    const key = user?.uid ? `uid:${user.uid}` : `ip:${ip}`;

    const now = Math.floor(Date.now() / 1000);
    const db = c.env.DB;

    try {
      const record = await db
        .prepare("SELECT count, reset_at FROM rate_limits WHERE key = ?")
        .bind(key)
        .first<{ count: number; reset_at: number }>();

      if (!record || record.reset_at <= now) {
        // Window expired or new entry
        await db
          .prepare(
            "INSERT INTO rate_limits (key, count, reset_at) VALUES (?, 1, ?) ON CONFLICT(key) DO UPDATE SET count = 1, reset_at = ?"
          )
          .bind(key, now + windowSeconds, now + windowSeconds)
          .run();
      } else {
        if (record.count >= limit) {
          c.header("Retry-After", String(record.reset_at - now));
          return c.json(
            {
              success: false,
              error: "Rate limit exceeded. Please wait before retrying.",
            },
            429
          );
        }

        await db
          .prepare("UPDATE rate_limits SET count = count + 1 WHERE key = ?")
          .bind(key)
          .run();
      }
    } catch (_e) {
      // Non-blocking fallback if rate_limits table fails transiently
    }

    return next();
  };
}
