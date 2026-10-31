import type { Context } from "hono";

/**
 * Protect pre-submission question answer keys and explanations for all
 * non-admin callers. Only the server-verified admin principal can see them.
 *
 * Never rely on client-provided version headers or runtime feature flags for
 * answer disclosure: both can change outside the authorization boundary.
 */
export function hideAnswers(c: Context<any>): boolean {
  return c.get("user")?.isAdmin !== true;
}
