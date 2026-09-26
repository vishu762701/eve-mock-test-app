// ============================================================================
// Auth & User Profile Routes
// ============================================================================

import { Hono } from "hono";
import { AuthUser, Env, UserRow } from "../types";

export const authRoutes = new Hono<{ Bindings: Env; Variables: { user: AuthUser } }>();

// GET /api/auth/me - Current user auth info
authRoutes.get("/me", async (c) => {
  const user = c.get("user");
  return c.json({ success: true, data: user });
});

// POST /api/users/sync - On login / foreground heartbeat
authRoutes.post("/sync", async (c) => {
  const user = c.get("user");
  const body = await c.req.json().catch(() => ({}));
  const db = c.env.DB;
  const now = Date.now();

  const displayName = String(body.displayName || user.displayName || "Student").trim();
  const email = String(body.email || user.email || "").trim().toLowerCase();

  await db
    .prepare(
      `INSERT INTO users (id, email, display_name, created_at, last_active)
       VALUES (?, ?, ?, ?, ?)
       ON CONFLICT(id) DO UPDATE SET
         email = excluded.email,
         display_name = CASE WHEN excluded.display_name != '' THEN excluded.display_name ELSE users.display_name END,
         last_active = excluded.last_active`
    )
    .bind(user.uid, email, displayName, now, now)
    .run();

  return c.json({ success: true });
});

// GET /api/users/profile - Get profile data
authRoutes.get("/profile", async (c) => {
  const user = c.get("user");
  const db = c.env.DB;

  const row = await db
    .prepare("SELECT id, email, display_name, dob, category, created_at, last_active FROM users WHERE id = ?")
    .bind(user.uid)
    .first<UserRow>();

  if (!row) {
    return c.json({
      success: true,
      data: {
        id: user.uid,
        email: user.email,
        displayName: user.displayName,
        dob: "",
        category: "General",
      },
    });
  }

  return c.json({
    success: true,
    data: {
      id: row.id,
      email: row.email,
      displayName: row.display_name,
      dob: row.dob,
      category: row.category,
    },
  });
});

// PUT /api/users/profile - Update profile details
authRoutes.put("/profile", async (c) => {
  const user = c.get("user");
  const body = await c.req.json().catch(() => ({}));
  const db = c.env.DB;
  const now = Date.now();

  const displayName = String(body.displayName || "").trim();
  const dob = String(body.dob || "").trim();
  const category = String(body.category || "General").trim();

  if (!displayName) {
    return c.json({ success: false, error: "Display name cannot be empty" }, 400);
  }

  await db
    .prepare(
      `INSERT INTO users (id, email, display_name, dob, category, created_at, last_active, last_updated)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?)
       ON CONFLICT(id) DO UPDATE SET
         display_name = excluded.display_name,
         dob = excluded.dob,
         category = excluded.category,
         last_updated = excluded.last_updated`
    )
    .bind(user.uid, user.email, displayName, dob, category, now, now, now)
    .run();

  return c.json({ success: true });
});

// POST /api/users/fcm-token - Save FCM token
authRoutes.post("/fcm-token", async (c) => {
  const user = c.get("user");
  const body = await c.req.json().catch(() => ({}));
  const token = String(body.fcmToken || "").trim();
  const db = c.env.DB;
  const now = Date.now();

  if (!token) {
    return c.json({ success: false, error: "Token cannot be empty" }, 400);
  }

  await db
    .prepare(
      `INSERT INTO users (id, email, display_name, created_at, last_active, fcm_token)
       VALUES (?, ?, ?, ?, ?, ?)
       ON CONFLICT(id) DO UPDATE SET fcm_token = excluded.fcm_token`
    )
    .bind(user.uid, user.email, user.displayName, now, now, token)
    .run();

  return c.json({ success: true });
});

// DELETE /api/users/account - User Account Deletion (GDPR/Privacy)
authRoutes.delete("/account", async (c) => {
  const user = c.get("user");
  const db = c.env.DB;
  const uid = user.uid;

  // Batch delete user records across all tables
  await db.batch([
    db.prepare("DELETE FROM pinned_exams WHERE user_id = ?").bind(uid),
    db.prepare("DELETE FROM bookmarks WHERE user_id = ?").bind(uid),
    db.prepare("DELETE FROM attempt_answers WHERE attempt_id IN (SELECT id FROM attempts WHERE user_id = ?)").bind(uid),
    db.prepare("DELETE FROM attempts WHERE user_id = ?").bind(uid),
    db.prepare("DELETE FROM attempt_locks WHERE user_id = ?").bind(uid),
    db.prepare("DELETE FROM leaderboard WHERE user_id = ?").bind(uid),
    db.prepare("DELETE FROM overall_leaderboard WHERE user_id = ?").bind(uid),
    db.prepare("DELETE FROM feedback_messages WHERE user_id = ?").bind(uid),
    db.prepare("DELETE FROM feedback_post_replies WHERE user_id = ?").bind(uid),
    db.prepare("DELETE FROM poll_votes WHERE user_id = ?").bind(uid),
    db.prepare("DELETE FROM users WHERE id = ?").bind(uid),
  ]);

  return c.json({ success: true, message: "Account and associated data deleted successfully." });
});
