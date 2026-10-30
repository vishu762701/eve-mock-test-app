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

export function validateUsername(name: string): { valid: boolean; error?: string } {
  const trimmed = name.trim();
  if (trimmed.length < 5) {
    return { valid: false, error: "A username must have at least 5 characters." };
  }
  if (trimmed.length > 32) {
    return { valid: false, error: "The username must not exceed 32 characters." };
  }
  if (/^[0-9]/.test(trimmed)) {
    return { valid: false, error: "Sorry, a username can't start with a number." };
  }
  if (trimmed.startsWith("_") || trimmed.endsWith("_")) {
    return { valid: false, error: "Sorry, this username is invalid." };
  }
  if (!/^[a-zA-Z0-9_]+$/.test(trimmed)) {
    return { valid: false, error: "Sorry, this username is invalid." };
  }
  return { valid: true };
}

export function deriveDefaultUsername(eveId: string): string {
  const cleaned = eveId.trim().toLowerCase().replace(/[^a-z0-9_]/g, "_");
  if (cleaned.length >= 5 && cleaned.length <= 32 && !/^[0-9_]/.test(cleaned) && !cleaned.endsWith("_")) {
    return cleaned;
  }
  return `ev_${cleaned.replace(/^[^a-z]+/, "")}`.slice(0, 32);
}

export async function getOrAssignUsername(
  db: D1Database,
  userId: string,
  eveId: string
): Promise<string> {
  const base = deriveDefaultUsername(eveId);
  let candidate = base;
  for (let attempt = 0; attempt < 10; attempt++) {
    if (attempt > 0) {
      candidate = `${base.slice(0, 28)}_${attempt}`;
    }
    try {
      const updateRes = await db
        .prepare("UPDATE users SET username = ? WHERE id = ? AND (username IS NULL OR username = '')")
        .bind(candidate, userId)
        .run();
      if (updateRes.meta?.changes && updateRes.meta.changes > 0) {
        return candidate;
      }
      const existing = await db
        .prepare("SELECT username FROM users WHERE id = ?")
        .bind(userId)
        .first<{ username: string | null }>();
      if (existing?.username) {
        return existing.username;
      }
    } catch (e: any) {
      if (String(e).includes("UNIQUE constraint failed") || String(e).includes("idx_users_username_lower")) {
        continue;
      }
      if (String(e).includes("no such column: username")) {
        await db.prepare("ALTER TABLE users ADD COLUMN username TEXT").run().catch(() => {});
        await db.prepare("CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username_lower ON users (lower(username))").run().catch(() => {});
        continue;
      }
      throw e;
    }
  }
  return base;
}

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

// GET /api/users/username/check?u=... - Check username availability
authRoutes.get("/username/check", async (c) => {
  const user = c.get("user");
  const db = c.env.DB;
  const raw = String(c.req.query("u") || "").trim();
  const target = raw.startsWith("@") ? raw.substring(1).trim() : raw;

  const validation = validateUsername(target);
  if (!validation.valid) {
    return c.json({
      success: true,
      data: {
        available: false,
        code: "invalid",
        error: validation.error,
      },
    });
  }

  // Check if it belongs to current user
  const currentUserRow = await db
    .prepare("SELECT username FROM users WHERE id = ?")
    .bind(user.uid)
    .first<{ username: string | null }>()
    .catch(() => null);

  if (currentUserRow?.username && currentUserRow.username.toLowerCase() === target.toLowerCase()) {
    return c.json({
      success: true,
      data: {
        available: true,
        code: "current",
      },
    });
  }

  const existing = await db
    .prepare("SELECT id FROM users WHERE lower(username) = lower(?) AND id != ?")
    .bind(target, user.uid)
    .first<{ id: string }>()
    .catch(() => null);

  if (existing) {
    return c.json({
      success: true,
      data: {
        available: false,
        code: "taken",
        error: "Sorry, this username is already taken.",
      },
    });
  }

  return c.json({
    success: true,
    data: {
      available: true,
      code: "available",
    },
  });
});

// GET /api/users/profile - Get profile data
authRoutes.get("/profile", async (c) => {
  const user = c.get("user");
  const db = c.env.DB;

  let row: UserRow | null = null;
  try {
    row = await db
      .prepare("SELECT id, eve_id, email, display_name, dob, category, username, phone, created_at, last_active FROM users WHERE id = ?")
      .bind(user.uid)
      .first<UserRow>();
  } catch (e: any) {
    if (String(e).includes("no such column: eve_id") || String(e).includes("no such column: username") || String(e).includes("no such column: phone")) {
      await db.prepare("ALTER TABLE users ADD COLUMN eve_id TEXT").run().catch(() => {});
      await db.prepare("CREATE UNIQUE INDEX IF NOT EXISTS idx_users_eve_id ON users (eve_id)").run().catch(() => {});
      await db.prepare("ALTER TABLE users ADD COLUMN username TEXT").run().catch(() => {});
      await db.prepare("ALTER TABLE users ADD COLUMN phone TEXT").run().catch(() => {});
      await db.prepare("CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username_lower ON users (lower(username))").run().catch(() => {});
      row = await db
        .prepare("SELECT id, eve_id, email, display_name, dob, category, username, phone, created_at, last_active FROM users WHERE id = ?")
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

  let username = row?.username;
  if (!username) {
    username = await getOrAssignUsername(db, user.uid, eveId);
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
        username: username,
        phone: "",
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
      username: username,
      phone: row.phone || "",
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
  const rawUsername = body.username !== undefined ? String(body.username).trim() : undefined;
  const rawPhone = body.phone !== undefined ? String(body.phone).trim() : undefined;

  if (!displayName) {
    return c.json({ success: false, error: "Display name cannot be empty" }, 400);
  }

  let newUsername: string | undefined = undefined;
  if (rawUsername !== undefined) {
    const cleanUsername = rawUsername.startsWith("@") ? rawUsername.substring(1).trim() : rawUsername;
    if (cleanUsername !== "") {
      const validation = validateUsername(cleanUsername);
      if (!validation.valid) {
        return c.json({ success: false, error: validation.error }, 400);
      }
      newUsername = cleanUsername.toLowerCase();
    }
  }

  let newPhone: string | undefined = undefined;
  if (rawPhone !== undefined) {
    if (rawPhone !== "") {
      // Light validation: allow optional leading +, digits, spaces, hyphens; length 7-20
      if (!/^\+?[0-9\s\-]{7,20}$/.test(rawPhone)) {
        return c.json({ success: false, error: "Please enter a valid phone number" }, 400);
      }
      newPhone = rawPhone;
    } else {
      newPhone = "";
    }
  }

  try {
    if (newUsername !== undefined && newPhone !== undefined) {
      await db
        .prepare(
          `INSERT INTO users (id, email, display_name, dob, category, username, phone, created_at, last_active, last_updated)
           VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
           ON CONFLICT(id) DO UPDATE SET
             display_name = excluded.display_name,
             dob = excluded.dob,
             category = excluded.category,
             username = excluded.username,
             phone = excluded.phone,
             last_updated = excluded.last_updated`
        )
        .bind(user.uid, user.email, displayName, dob, category, newUsername, newPhone, now, now, now)
        .run();
    } else if (newUsername !== undefined) {
      await db
        .prepare(
          `INSERT INTO users (id, email, display_name, dob, category, username, created_at, last_active, last_updated)
           VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
           ON CONFLICT(id) DO UPDATE SET
             display_name = excluded.display_name,
             dob = excluded.dob,
             category = excluded.category,
             username = excluded.username,
             last_updated = excluded.last_updated`
        )
        .bind(user.uid, user.email, displayName, dob, category, newUsername, now, now, now)
        .run();
    } else if (newPhone !== undefined) {
      await db
        .prepare(
          `INSERT INTO users (id, email, display_name, dob, category, phone, created_at, last_active, last_updated)
           VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
           ON CONFLICT(id) DO UPDATE SET
             display_name = excluded.display_name,
             dob = excluded.dob,
             category = excluded.category,
             phone = excluded.phone,
             last_updated = excluded.last_updated`
        )
        .bind(user.uid, user.email, displayName, dob, category, newPhone, now, now, now)
        .run();
    } else {
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
    }
  } catch (e: any) {
    if (String(e).includes("UNIQUE constraint failed") || String(e).includes("idx_users_username_lower")) {
      return c.json({ success: false, error: "Sorry, this username is already taken." }, 409);
    }
    throw e;
  }

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
    db.prepare("DELETE FROM attempt_sessions WHERE user_id = ?").bind(uid),
    db.prepare("DELETE FROM admin_analytics_exam_users WHERE user_id = ?").bind(uid),
    db.prepare("DELETE FROM premium_entitlements WHERE user_id = ?").bind(uid),
    db.prepare("DELETE FROM rate_limits WHERE key = ?").bind(`uid:${uid}`),
    // Financial premium_orders and immutable audit records are intentionally retained.
    db.prepare("DELETE FROM users WHERE id = ?").bind(uid),
  ]);

  return c.json({ success: true, message: "Account and associated data deleted successfully." });
});
