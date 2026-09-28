// ============================================================================
// Admin Dashboard & Analytics Routes
// ============================================================================

import { Hono } from "hono";
import { requireAdmin } from "../middleware/authMiddleware";
import { AuthUser, Env } from "../types";

export const adminRoutes = new Hono<{ Bindings: Env; Variables: { user: AuthUser } }>();

// All routes here strictly require admin authorization
adminRoutes.use("*", requireAdmin);

// GET /api/admin/admins - List dynamic admins
adminRoutes.get("/admins", async (c) => {
  const db = c.env.DB;
  const { results } = await db.prepare("SELECT email FROM admins ORDER BY email ASC").all<{ email: string }>();
  const list = (results || []).map((r) => r.email);
  return c.json({ success: true, data: list });
});

// POST /api/admin/admins - Add dynamic admin
adminRoutes.post("/admins", async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const email = String(body.email || "").trim().toLowerCase();
  const db = c.env.DB;

  if (!email || !email.includes("@")) {
    return c.json({ success: false, error: "Valid email required" }, 400);
  }

  await db
    .prepare("INSERT INTO admins (email, created_at) VALUES (?, ?) ON CONFLICT(email) DO NOTHING")
    .bind(email, Date.now())
    .run();

  return c.json({ success: true, data: { email } }, 201);
});

// DELETE /api/admin/admins/:email - Remove dynamic admin
adminRoutes.delete("/admins/:email", async (c) => {
  const email = c.req.param("email").trim().toLowerCase();
  const db = c.env.DB;

  await db.prepare("DELETE FROM admins WHERE email = ?").bind(email).run();
  return c.json({ success: true });
});

// GET /api/admin/analytics/exams - Pre-aggregated exam analytics
adminRoutes.get("/analytics/exams", async (c) => {
  const db = c.env.DB;
  const days = parseInt(c.req.query("days") || "0", 10);
  const cutoff = days > 0 ? Date.now() - days * 24 * 60 * 60 * 1000 : 0;
  try {
    const query = cutoff > 0
      ? `
        SELECT 
          e.id as exam_id,
          e.exam_name,
          e.category,
          COUNT(a.id) as attempt_count,
          COUNT(DISTINCT a.user_id) as unique_users,
          COALESCE(MAX(a.timestamp), 0) as last_attempt_at,
          COALESCE(AVG(a.score), 0.0) as average_score
        FROM exams e
        LEFT JOIN attempts a ON e.id = a.exam_id AND a.timestamp >= ?
        GROUP BY e.id, e.exam_name, e.category
        ORDER BY attempt_count DESC, e.exam_name ASC
      `
      : `
        SELECT 
          e.id as exam_id,
          e.exam_name,
          e.category,
          COUNT(a.id) as attempt_count,
          COUNT(DISTINCT a.user_id) as unique_users,
          COALESCE(MAX(a.timestamp), 0) as last_attempt_at,
          COALESCE(AVG(a.score), 0.0) as average_score
        FROM exams e
        LEFT JOIN attempts a ON e.id = a.exam_id
        GROUP BY e.id, e.exam_name, e.category
        ORDER BY attempt_count DESC, e.exam_name ASC
      `;

    const stmt = cutoff > 0 ? db.prepare(query).bind(cutoff) : db.prepare(query);
    const { results } = await stmt.all<any>();

    const list = (results || []).map((r) => ({
      examId: r.exam_id,
      examName: r.exam_name,
      category: r.category,
      attemptCount: r.attempt_count || 0,
      uniqueUsers: r.unique_users || 0,
      lastAttemptAt: r.last_attempt_at || 0,
      averageScore: Math.round((r.average_score || 0.0) * 10.0) / 10.0,
    }));

    return c.json({ success: true, data: list });
  } catch (_: any) {
    const { results } = await db
      .prepare("SELECT * FROM admin_analytics_exams ORDER BY attempt_count DESC, exam_name ASC")
      .all<any>();

    const list = (results || []).map((r) => ({
      examId: r.exam_id,
      examName: r.exam_name,
      category: r.category,
      attemptCount: r.attempt_count,
      uniqueUsers: r.unique_users,
      lastAttemptAt: r.last_attempt_at,
      averageScore: 0.0,
    }));

    return c.json({ success: true, data: list });
  }
});

// GET /api/admin/analytics/questions - Pre-aggregated question analytics
adminRoutes.get("/analytics/questions", async (c) => {
  const examId = c.req.query("examId");
  const db = c.env.DB;

  let query = "SELECT * FROM admin_analytics_questions";
  const params: any[] = [];
  if (examId) {
    query += " WHERE exam_id = ?";
    params.push(examId.trim());
  }

  const { results } = await db.prepare(query).bind(...params).all<any>();

  const list = (results || []).map((r) => {
    const attempts = r.attempts || 0;
    const wrong = r.wrong || 0;
    const correct = r.correct || 0;
    const wrongRate = attempts > 0 ? (wrong * 100.0) / attempts : 0.0;
    const accuracy = attempts > 0 ? (correct * 100.0) / attempts : 0.0;

    return {
      id: r.id,
      examId: r.exam_id,
      examName: r.exam_name,
      questionId: r.question_id,
      questionNumber: r.question_number,
      questionText: r.question_text,
      topic: r.topic,
      attempts,
      correct,
      wrong,
      unattempted: r.unattempted,
      wrongRate,
      accuracy,
    };
  });

  list.sort((a, b) => b.wrongRate - a.wrongRate || b.wrong - a.wrong || a.questionNumber - b.questionNumber);
  return c.json({ success: true, data: list });
});

// GET /api/admin/stats/users - Total and online user counts
adminRoutes.get("/stats/users", async (c) => {
  const db = c.env.DB;
  const ONLINE_WINDOW_MS = 5 * 60 * 1000;
  const cutoff = Date.now() - ONLINE_WINDOW_MS;

  const total = await db.prepare("SELECT COUNT(*) as count FROM users").first<{ count: number }>();
  const online = await db
    .prepare("SELECT COUNT(*) as count FROM users WHERE last_active >= ?")
    .bind(cutoff)
    .first<{ count: number }>();

  return c.json({
    success: true,
    data: {
      totalUsers: total?.count || 0,
      onlineUsers: online?.count || 0,
    },
  });
});

// GET /api/admin/users - List users with optional search
adminRoutes.get("/users", async (c) => {
  const db = c.env.DB;
  const q = c.req.query("q")?.trim()?.toLowerCase();

  let query = "SELECT id, email, display_name, dob, category, created_at, last_active, COALESCE(disabled, 0) as disabled FROM users";
  const params: any[] = [];
  if (q) {
    query += " WHERE LOWER(email) LIKE ? OR LOWER(display_name) LIKE ?";
    params.push(`%${q}%`, `%${q}%`);
  }
  query += " ORDER BY last_active DESC, created_at DESC LIMIT 200";

  try {
    const { results } = await db.prepare(query).bind(...params).all<any>();
    const users = (results || []).map((u) => ({
      id: u.id,
      email: u.email,
      displayName: u.display_name || "Student",
      dob: u.dob || "",
      category: u.category || "General",
      createdAt: u.created_at || 0,
      lastActive: u.last_active || 0,
      disabled: Boolean(u.disabled),
    }));
    return c.json({ success: true, data: users });
  } catch (_: any) {
    // If disabled column does not exist yet
    try {
      const fallbackQuery = q
        ? "SELECT id, email, display_name, dob, category, created_at, last_active FROM users WHERE LOWER(email) LIKE ? OR LOWER(display_name) LIKE ? ORDER BY last_active DESC, created_at DESC LIMIT 200"
        : "SELECT id, email, display_name, dob, category, created_at, last_active FROM users ORDER BY last_active DESC, created_at DESC LIMIT 200";
      const { results } = await db.prepare(fallbackQuery).bind(...params).all<any>();
      const users = (results || []).map((u) => ({
        id: u.id,
        email: u.email,
        displayName: u.display_name || "Student",
        dob: u.dob || "",
        category: u.category || "General",
        createdAt: u.created_at || 0,
        lastActive: u.last_active || 0,
        disabled: false,
      }));
      return c.json({ success: true, data: users });
    } catch (e: any) {
      return c.json({ success: false, error: e.message }, 500);
    }
  }
});

// POST /api/admin/users/:id/status - Toggle user disabled/ban status
adminRoutes.post("/users/:id/status", async (c) => {
  const id = c.req.param("id");
  const body = await c.req.json().catch(() => ({}));
  const disabled = body.disabled === true ? 1 : 0;
  const db = c.env.DB;

  try {
    await db.prepare("UPDATE users SET disabled = ? WHERE id = ?").bind(disabled, id).run();
    return c.json({ success: true, data: { id, disabled: Boolean(disabled) } });
  } catch (_: any) {
    try {
      await db.prepare("ALTER TABLE users ADD COLUMN disabled INTEGER DEFAULT 0").run();
      await db.prepare("UPDATE users SET disabled = ? WHERE id = ?").bind(disabled, id).run();
      return c.json({ success: true, data: { id, disabled: Boolean(disabled) } });
    } catch (e: any) {
      return c.json({ success: false, error: e.message }, 500);
    }
  }
});

// GET /api/admin/users/:id/attempts - Get user test attempts
adminRoutes.get("/users/:id/attempts", async (c) => {
  const userId = c.req.param("id");
  const db = c.env.DB;
  try {
    const { results } = await db
      .prepare("SELECT * FROM attempts WHERE user_id = ? ORDER BY timestamp DESC LIMIT 50")
      .bind(userId)
      .all<any>();
    const attempts = (results || []).map((a) => ({
      id: a.id,
      userId: a.user_id,
      displayName: a.display_name,
      examId: a.exam_id,
      examName: a.exam_name,
      category: a.category,
      score: a.score,
      total: a.total,
      correct: a.correct,
      wrong: a.wrong,
      unattempted: a.unattempted,
      timestamp: a.timestamp,
    }));
    return c.json({ success: true, data: attempts });
  } catch (err: any) {
    return c.json({ success: false, error: err.message }, 500);
  }
});

// PUT /api/admin/config - Update app configuration
adminRoutes.put("/config", async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const minVersion = Number(body.minimum_supported_version_code || 1);
  const maintenanceMode = Boolean(body.maintenance_mode);
  const maintenanceMessage = String(
    body.maintenance_message ||
      "Eve Mock Test is currently undergoing scheduled maintenance. Please check back shortly."
  );
  const db = c.env.DB;

  const configObj = {
    minimum_supported_version_code: minVersion,
    maintenance_mode: maintenanceMode,
    maintenance_message: maintenanceMessage,
  };

  try {
    await db
      .prepare(
        "INSERT INTO app_content (id, title, body, updated_at, updated_by) VALUES ('app_config', 'App Configuration', ?, ?, 'admin') ON CONFLICT(id) DO UPDATE SET body = excluded.body, updated_at = excluded.updated_at"
      )
      .bind(JSON.stringify(configObj), Date.now())
      .run();

    return c.json({ success: true, data: configObj });
  } catch (err: any) {
    return c.json({ success: false, error: err.message }, 500);
  }
});

function validateFloatingLinkUrl(rawUrl: string): { valid: boolean; error?: string; cleanUrl: string } {
  const trimmed = rawUrl.trim();
  if (!trimmed) {
    return { valid: true, cleanUrl: "" };
  }

  if (trimmed.length > 500) {
    return { valid: false, error: "URL exceeds maximum length of 500 characters", cleanUrl: "" };
  }

  let parsed: URL;
  try {
    parsed = new URL(trimmed);
  } catch {
    return { valid: false, error: "Invalid URL format", cleanUrl: "" };
  }

  if (parsed.protocol !== "https:") {
    return { valid: false, error: "Only HTTPS URLs are allowed", cleanUrl: "" };
  }

  if (parsed.username || parsed.password) {
    return { valid: false, error: "Embedded credentials are not allowed", cleanUrl: "" };
  }

  return { valid: true, cleanUrl: parsed.toString() };
}

// GET /api/admin/floating-link
adminRoutes.get("/floating-link", async (c) => {
  const db = c.env.DB;
  try {
    const row = await db.prepare("SELECT body FROM app_content WHERE id = 'floating_link'").first<{ body: string }>();
    const url = row?.body?.trim() || "";
    return c.json({ success: true, data: { url }, url });
  } catch (err: any) {
    return c.json({ success: false, error: err.message }, 500);
  }
});

// PUT /api/admin/floating-link
adminRoutes.put("/floating-link", async (c) => {
  const body = await c.req.json().catch(() => ({}));
  let rawUrl = String(body.url ?? body.link ?? "").trim();
  if (rawUrl && !rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
    rawUrl = `https://${rawUrl}`;
  }
  const validation = validateFloatingLinkUrl(rawUrl);
  if (!validation.valid) {
    return c.json({ success: false, error: validation.error }, 400);
  }

  const db = c.env.DB;
  const user = c.get("user");
  const updatedBy = user?.email || "admin";
  const now = Date.now();

  try {
    if (!validation.cleanUrl) {
      await db.prepare("DELETE FROM app_content WHERE id = 'floating_link'").run();
      return c.json({ success: true, data: { url: "" }, url: "" });
    }

    await db
      .prepare(
        "INSERT INTO app_content (id, title, body, updated_at, updated_by) VALUES ('floating_link', 'Floating Link', ?, ?, ?) ON CONFLICT(id) DO UPDATE SET body = excluded.body, updated_at = excluded.updated_at, updated_by = excluded.updated_by"
      )
      .bind(validation.cleanUrl, now, updatedBy)
      .run();

    return c.json({ success: true, data: { url: validation.cleanUrl }, url: validation.cleanUrl });
  } catch (err: any) {
    return c.json({ success: false, error: err.message }, 500);
  }
});

