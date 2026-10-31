// ============================================================================
// Authentication & Admin Authorization Middleware
// ============================================================================

import { Context, Next } from "hono";
import { AdminRight, getAdminPermissions, isOwnerEmail, isUserAdmin, verifyFirebaseIdToken } from "../auth";
import { AuthUser, Env } from "../types";

export async function authMiddleware(c: Context<{ Bindings: Env; Variables: { user: AuthUser } }>, next: Next) {
  const path = c.req.path;
  const method = c.req.method;

  // Public allowlist - read-only / diagnostic endpoints handled with their own auth
  if (
    (path === "/api/health" && method === "GET") ||
    path.startsWith("/api/health/") ||
    path.startsWith("/api/diag/") ||
    path === "/api/app-config" ||
    path === "/api/floating-link" ||
    (path === "/api/premium/plan" && method === "GET") ||
    path === "/api/premium/webhook" ||
    (path.startsWith("/api/app-content/") && method === "GET") ||
    (path === "/api/banners" && method === "GET") ||
    (path === "/api/typography/published" && method === "GET")
  ) {
    return next();
  }

  const authHeader = c.req.header("Authorization");
  if (!authHeader || !authHeader.startsWith("Bearer ")) {
    return c.json({ success: false, error: "Missing or malformed Authorization header" }, 401);
  }

  const token = authHeader.substring(7).trim();
  if (!token) {
    return c.json({ success: false, error: "Empty Bearer token" }, 401);
  }

  let user: AuthUser;
  try { user = await verifyFirebaseIdToken(token, c.env.FIREBASE_PROJECT_ID); }
  catch { return c.json({ success: false, error: "Invalid or expired authentication. Please sign in again." }, 401); }
  try {
    // Double check dynamic admin list from D1
    const isAdmin = user.isAdmin || (user.emailVerified === true && await isUserAdmin(c.env.DB, user.email));
    user.isAdmin = isAdmin;

    const profile = await c.env.DB.prepare("SELECT disabled FROM users WHERE id = ?").bind(user.uid).first<{ disabled?: number }>();
    if (profile?.disabled === 1) return c.json({ success: false, error: "This account is disabled. Contact an administrator." }, 403);
    c.set("user", user);

    // Keep 'Online Now' truthful by updating last_active on authenticated activity (throttled to at most once per 2 minutes)
    try {
      const now = Date.now();
      const updateStmt = c.env.DB.prepare(
        "UPDATE users SET last_active = ?1 WHERE id = ?2 AND (?1 - COALESCE(last_active, 0)) > 120000"
      ).bind(now, user.uid);
      if (typeof updateStmt.run === "function") {
        updateStmt.run().catch(() => {});
      }
    } catch {}

    return next();
  } catch (err: any) {
    return c.json({ success: false, error: "Authentication services are temporarily unavailable. Please retry." }, 503);
  }
}

export async function requireAdmin(c: Context<{ Bindings: Env; Variables: { user: AuthUser } }>, next: Next) {
  const user = c.get("user");
  if (!user || !user.isAdmin) {
    return c.json({ success: false, error: "Admin privileges required" }, 403);
  }
  return next();
}

export function requirePermission(permission: AdminRight) {
  return async (c: Context<{ Bindings: Env; Variables: { user: AuthUser } }>, next: Next) => {
    const user = c.get("user");
    if (!user || !user.isAdmin) {
      return c.json({ success: false, error: "Admin privileges required" }, 403);
    }

    if (isOwnerEmail(user.email)) {
      return next();
    }

    const perms = await getAdminPermissions(c.env.DB, user.email);
    if (perms) {
      if (!perms.rights.includes(permission)) {
        return c.json({ success: false, error: `Missing required permission: ${permission}` }, 403);
      }
      return next();
    }

    // Missing membership or an unavailable permission store must fail closed.
    // Tests must provide a real owner or explicit permissions, just like callers.
    return c.json({ success: false, error: "Admin permissions could not be verified" }, 403);
  };
}
