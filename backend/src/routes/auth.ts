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
  const body = await c.req.json().catch(() => null);
  if (!body || typeof body !== "object" || Array.isArray(body)) {
    return c.json({ success: false, error: "A profile object is required" }, 400);
  }
  if (body.displayName !== undefined && (typeof body.displayName !== "string" ||
      !body.displayName.trim() || body.displayName.length > 100)) {
    return c.json({ success: false, error: "Display name must contain 1-100 characters" }, 400);
  }
  const db = c.env.DB;
  const now = Date.now();

  const displayName = String(body.displayName || user.displayName || "Student").trim();
  // Identity comes from the verified token, never from editable profile input.
  const email = user.email.trim().toLowerCase();

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
      .prepare("SELECT id, eve_id, email, display_name, dob, category, username, phone, bio, created_at, last_active FROM users WHERE id = ?")
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
        .prepare("SELECT id, eve_id, email, display_name, dob, category, username, phone, bio, created_at, last_active FROM users WHERE id = ?")
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
        bio: "",
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
      bio: row.bio || "",
    },
  });
});

// PUT /api/users/profile - Android editors send one changed field at a time.
authRoutes.put("/profile", async (c) => {
  const user = c.get("user");
  const body = await c.req.json().catch(() => null);
  if (!body || typeof body !== "object" || Array.isArray(body)) {
    return c.json({ success: false, error: "A profile object is required" }, 400);
  }

  const fields = {
    displayName: { column: "display_name", max: 100 },
    dob: { column: "dob", max: 32 },
    category: { column: "category", max: 100 },
    username: { column: "username", max: 33 },
    phone: { column: "phone", max: 32 },
    bio: { column: "bio", max: 160 },
  } as const;
  const supplied: Record<string, string> = {};
  const updatedColumns: string[] = [];
  for (const [field, rule] of Object.entries(fields)) {
    if (!Object.hasOwn(body, field)) continue;
    if (typeof body[field] !== "string" || body[field].length > rule.max) {
      return c.json({ success: false, error: `${field} must be text of at most ${rule.max} characters` }, 400);
    }
    supplied[field] = body[field].trim();
    updatedColumns.push(rule.column);
  }
  if (updatedColumns.length === 0) {
    return c.json({ success: false, error: "Provide at least one profile field to update" }, 400);
  }
  if (supplied.displayName === "") {
    return c.json({ success: false, error: "Display name cannot be empty" }, 400);
  }
  if (supplied.username !== undefined) {
    const clean = supplied.username.replace(/^@/, "").trim();
    const validation = validateUsername(clean);
    if (!validation.valid) return c.json({ success: false, error: validation.error }, 400);
    supplied.username = clean.toLowerCase();
  }
  if (supplied.phone) {
    const digits = supplied.phone.replace(/[^0-9]/g, "");
    if (!/^\+?[0-9 -]+$/.test(supplied.phone) || digits.length < 7 || digits.length > 15) {
      return c.json({ success: false, error: "Please enter a valid phone number (7-15 digits)" }, 400);
    }
  }

  const now = Date.now();
  try {
    // Columns come only from the fixed allowlist above; values remain bound.
    // Update only supplied fields, atomically preserving other concurrent edits.
    await c.env.DB.prepare(
      `INSERT INTO users (id, email, display_name, dob, category, username, phone, bio, created_at, last_active, last_updated)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
       ON CONFLICT(id) DO UPDATE SET
         ${updatedColumns.map(column => `${column} = excluded.${column}`).join(", ")},
         last_updated = excluded.last_updated`
    ).bind(
      user.uid, user.email, supplied.displayName ?? user.displayName ?? "Student",
      supplied.dob ?? "", supplied.category ?? "General", supplied.username ?? null,
      supplied.phone ?? "", supplied.bio ?? "", now, now, now
    ).run();
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
