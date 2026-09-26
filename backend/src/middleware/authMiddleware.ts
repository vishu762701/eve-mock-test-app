// ============================================================================
// Authentication & Admin Authorization Middleware
// ============================================================================

import { Context, Next } from "hono";
import { verifyFirebaseIdToken, isUserAdmin } from "../auth";
import { AuthUser, Env } from "../types";

export async function authMiddleware(c: Context<{ Bindings: Env; Variables: { user: AuthUser } }>, next: Next) {
  const path = c.req.path;
  const method = c.req.method;

  // Public allowlist
  if (
    path === "/api/health" ||
    (path.startsWith("/api/app-content/") && method === "GET") ||
    (path === "/api/banners" && method === "GET")
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

  try {
    const user = await verifyFirebaseIdToken(token, c.env.FIREBASE_PROJECT_ID);
    // Double check dynamic admin list from D1
    const isAdmin = user.isAdmin || (await isUserAdmin(c.env.DB, user.email));
    user.isAdmin = isAdmin;

    c.set("user", user);
    return next();
  } catch (err: any) {
    return c.json({ success: false, error: err.message || "Invalid or expired token" }, 401);
  }
}

export async function requireAdmin(c: Context<{ Bindings: Env; Variables: { user: AuthUser } }>, next: Next) {
  const user = c.get("user");
  if (!user || !user.isAdmin) {
    return c.json({ success: false, error: "Admin privileges required" }, 403);
  }
  return next();
}
