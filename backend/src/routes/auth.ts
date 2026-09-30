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

export function generateCandidateEveId(): string {
  const chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
  let code = "";
  for (let i = 0; i < 6; i++) {
    code += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return `EV-${code}`;
}

export async function getOrAssignEveId(
  db: D1Database,
  userId: string,
  initialEmail?: string,
  initialDisplayName?: string
): Promise<string> {
  try {
    const existing = await db
      .prepare("SELECT eve_id FROM users WHERE id = ?")
      .bind(userId)
      .first<{ eve_id: string | null }>();

    if (existing?.eve_id) {
      return existing.eve_id;
    }
  } catch (e: any) {
    if (String(e).includes("no such column: eve_id")) {
      await db.prepare("ALTER TABLE users ADD COLUMN eve_id TEXT").run().catch(() => {});
      await db.prepare("CREATE UNIQUE INDEX IF NOT EXISTS idx_users_eve_id ON users (eve_id)").run().catch(() => {});
    }
  }

  for (let attempt = 0; attempt < 10; attempt++) {
    const candidate = generateCandidateEveId();
    try {
      const updateRes = await db
        .prepare("UPDATE users SET eve_id = ? WHERE id = ? AND (eve_id IS NULL OR eve_id = '')")
        .bind(candidate, userId)
        .run();

      if (updateRes.meta?.changes && updateRes.meta.changes > 0) {
        return candidate;
      }

      // Check if user row was missing or set concurrently
      const check = await db
        .prepare("SELECT eve_id FROM users WHERE id = ?")
        .bind(userId)
        .first<{ eve_id: string | null }>();

      if (check?.eve_id) {
        return check.eve_id;
      }

      // User row does not exist yet: insert with candidate ID
      const now = Date.now();
      await db
        .prepare(
          `INSERT INTO users (id, email, display_name, created_at, last_active, eve_id)
           VALUES (?, ?, ?, ?, ?, ?)
           ON CONFLICT(id) DO UPDATE SET
             eve_id = CASE WHEN users.eve_id IS NULL OR users.eve_id = '' THEN excluded.eve_id ELSE users.eve_id END`
        )
        .bind(userId, initialEmail || "", initialDisplayName || "Student", now, now, candidate)
        .run();

      const recheck = await db
        .prepare("SELECT eve_id FROM users WHERE id = ?")
        .bind(userId)
        .first<{ eve_id: string | null }>();

      if (recheck?.eve_id) {
        return recheck.eve_id;
      }
    } catch (e: any) {
      if (String(e).includes("UNIQUE constraint failed") || String(e).includes("idx_users_eve_id")) {
        continue;
      }
      if (String(e).includes("no such column: eve_id")) {
        await db.prepare("ALTER TABLE users ADD COLUMN eve_id TEXT").run().catch(() => {});
        await db.prepare("CREATE UNIQUE INDEX IF NOT EXISTS idx_users_eve_id ON users (eve_id)").run().catch(() => {});
        continue;
      }
      throw e;
    }
  }

  throw new Error("Failed to allocate unique EVE ID after multiple attempts");
}

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

  const eveId = await getOrAssignEveId(db, user.uid, email, displayName);

  return c.json({ success: true, data: { eveId } });
});

// GET /api/users/profile - Get profile data
authRoutes.get("/profile", async (c) => {
  const user = c.get("user");
  const db = c.env.DB;

  let row: UserRow | null = null;
  try {
    row = await db
      .prepare("SELECT id, eve_id, email, display_name, dob, category, created_at, last_active FROM users WHERE id = ?")
      .bind(user.uid)
      .first<UserRow>();
  } catch (e: any) {
    if (String(e).includes("no such column: eve_id")) {
      await db.prepare("ALTER TABLE users ADD COLUMN eve_id TEXT").run().catch(() => {});
      await db.prepare("CREATE UNIQUE INDEX IF NOT EXISTS idx_users_eve_id ON users (eve_id)").run().catch(() => {});
      row = await db
        .prepare("SELECT id, eve_id, email, display_name, dob, category, created_at, last_active FROM users WHERE id = ?")
        .bind(user.uid)
        .first<UserRow>();
    } else {
      throw e;
    }
  }

  let eveId = row?.eve_id;
  if (!eveId) {
    eveId = await getOrAssignEveId(db, user.uid, user.email, user.displayName);
  }

  if (!row) {
    return c.json({
      success: true,
      data: {
        id: user.uid,
        eveId: eveId,
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
      eveId: eveId,
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
