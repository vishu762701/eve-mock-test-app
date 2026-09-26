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
  }));

  return c.json({ success: true, data: list });
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
