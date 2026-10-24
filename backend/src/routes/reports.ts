// ============================================================================
// Question Reports & Admin Audit Log Routes
// ============================================================================

import { Hono } from "hono";
import { requireAdmin } from "../middleware/authMiddleware";
import { checkRateLimit } from "../middleware/rateLimiter";
import { AuthUser, Env, QuestionReportRow, AdminAuditLogRow } from "../types";

export const reportRoutes = new Hono<{ Bindings: Env; Variables: { user: AuthUser } }>();
export const adminReportRoutes = new Hono<{ Bindings: Env; Variables: { user: AuthUser } }>();
export const adminAuditLogRoutes = new Hono<{ Bindings: Env; Variables: { user: AuthUser } }>();

export const CONTENT_ISSUES = [
  "Wrong Question",
  "No Solution",
  "Wrong Translation",
  "Out of Syllabus",
];

export const TECHNICAL_ISSUES = [
  "Question and Options not visible",
  "Blinking Screen Issue",
  "Formatting Issues",
  "Scroll Not Working",
  "Dark Mode Issue",
  "Question not visible but Options visible",
];

export const VALID_REASONS = new Set([
  ...CONTENT_ISSUES,
  ...TECHNICAL_ISSUES,
  "Other",
]);

export function isContentIssue(reason: string): boolean {
  return CONTENT_ISSUES.includes(reason) || reason === "Other" || reason.startsWith("Wrong");
}

// ----------------------------------------------------------------------------
// 1. Submit Question Report (Student)
// POST /api/reports
// ----------------------------------------------------------------------------
reportRoutes.post("/", async (c) => {
  const user = c.get("user");
  if (!user || !user.uid) {
    return c.json({ success: false, error: "Unauthorized" }, 401);
  }

  // Rate limit: 15 reports / minute per user
  const { limited, retryAfter } = await checkRateLimit(c.env.DB, `uid:${user.uid}:reports`, 15, 60);
  if (limited) {
    c.header("Retry-After", String(retryAfter));
    return c.json({ success: false, error: "Too many reports submitted. Please wait before retrying." }, 429);
  }

  const body = await c.req.json().catch(() => ({}));
  const questionId = String(body.questionId || "").trim();
  const examId = String(body.examId || "").trim();
  const examName = String(body.examName || "").trim();
  const rawQuestionText = String(body.questionText || "").trim();
  const reason = String(body.reason || "").trim();
  const rawComment = String(body.comment || "").trim();

  if (!questionId) {
    return c.json({ success: false, error: "questionId is required" }, 400);
  }
  if (!reason) {
    return c.json({ success: false, error: "reason is required" }, 400);
  }
  if (!VALID_REASONS.has(reason) && !reason.startsWith("Wrong")) {
    return c.json({ success: false, error: "Invalid report reason" }, 400);
  }
  if (rawComment.length > 500) {
    return c.json({ success: false, error: "Comment cannot exceed 500 characters" }, 400);
  }

  const comment = rawComment.slice(0, 500);
  const questionText = rawQuestionText.slice(0, 1000);
  const reportType = isContentIssue(reason) ? "content" : "technical";
  const studentId = user.uid;
  const studentEmail = user.email || "Student";
  const db = c.env.DB;

  // De-duplicate: same user + same question + same reason still pending -> return success without duplicate
  const existing = await db
    .prepare("SELECT id FROM question_reports WHERE student_id = ? AND question_id = ? AND reason = ? AND status = 'pending'")
    .bind(studentId, questionId, reason)
    .first<{ id: string }>();

  if (existing) {
    return c.json({ success: true, message: "Report already submitted", data: { id: existing.id } });
  }

  const id = crypto.randomUUID();
  const now = Date.now();

  await db
    .prepare(
      `INSERT INTO question_reports (
        id, question_id, exam_id, exam_name, question_text, reason, comment,
        student_id, student_email, timestamp, status, report_type
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'pending', ?)`
    )
    .bind(
      id,
      questionId,
      examId,
      examName,
      questionText,
      reason,
      comment,
      studentId,
      studentEmail,
      now,
      reportType
    )
    .run();

  return c.json({ success: true, data: { id } }, 201);
});

async function fetchPendingReports(db: any, typeParam?: string) {
  let query = "SELECT * FROM question_reports WHERE status = 'pending'";
  const params: any[] = [];

  if (typeParam === "content" || typeParam === "technical") {
    query += " AND report_type = ?";
    params.push(typeParam);
  }
  query += " ORDER BY timestamp DESC";

  const stmt = params.length > 0 ? db.prepare(query).bind(...params) : db.prepare(query);
  const { results } = await stmt.all();

  return (results || []).map((r: any) => ({
    id: r.id,
    questionId: r.question_id,
    examId: r.exam_id,
    examName: r.exam_name,
    questionText: r.question_text,
    reason: r.reason,
    comment: r.comment || "",
    studentId: r.student_id,
    studentEmail: r.student_email,
    timestamp: r.timestamp,
    status: r.status,
    reportType: r.report_type,
  }));
}

// Also allow GET /api/reports for convenience (admin only)
reportRoutes.get("/", requireAdmin, async (c) => {
  const typeParam = c.req.query("type")?.trim().toLowerCase();
  const list = await fetchPendingReports(c.env.DB, typeParam);
  return c.json({ success: true, data: list });
});

// ----------------------------------------------------------------------------
// 2. Admin Question Reports
adminReportRoutes.use("*", requireAdmin);

adminReportRoutes.get("/", async (c) => {
  const typeParam = c.req.query("type")?.trim().toLowerCase();
  const list = await fetchPendingReports(c.env.DB, typeParam);
  return c.json({ success: true, data: list });
});

adminReportRoutes.put("/:id/dismiss", async (c) => {
  const id = c.req.param("id");
  const db = c.env.DB;
  await db.prepare("UPDATE question_reports SET status = 'dismissed' WHERE id = ?").bind(id).run();
  return c.json({ success: true });
});

adminReportRoutes.post("/dismiss-batch", async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const ids: string[] = Array.isArray(body.ids) ? body.ids : [];
  if (ids.length === 0) {
    return c.json({ success: true });
  }

  const db = c.env.DB;
  const stmts = ids.map((id) =>
    db.prepare("UPDATE question_reports SET status = 'dismissed' WHERE id = ?").bind(id)
  );
  await db.batch(stmts);
  return c.json({ success: true });
});

adminReportRoutes.delete("/:id", async (c) => {
  const id = c.req.param("id");
  const db = c.env.DB;
  await db.prepare("DELETE FROM question_reports WHERE id = ?").bind(id).run();
  return c.json({ success: true });
});

// ----------------------------------------------------------------------------
// 3. Admin Audit Log Routes
// POST /api/admin/audit-log
// GET /api/admin/audit-log?range=today|7d|all
// ----------------------------------------------------------------------------
adminAuditLogRoutes.use("*", requireAdmin);

adminAuditLogRoutes.post("/", async (c) => {
  const user = c.get("user");
  const body = await c.req.json().catch(() => ({}));
  const actionType = String(body.actionType || "").trim();
  const description = String(body.description || "").trim();
  const adminEmail = String(body.adminEmail || user?.email || "admin@eve.app").trim();

  if (!actionType) {
    return c.json({ success: false, error: "actionType is required" }, 400);
  }

  const id = crypto.randomUUID();
  const now = Date.now();
  const db = c.env.DB;

  await db
    .prepare(
      "INSERT INTO admin_audit_log (id, action_type, description, admin_email, timestamp) VALUES (?, ?, ?, ?, ?)"
    )
    .bind(id, actionType, description, adminEmail, now)
    .run();

  return c.json({ success: true, data: { id } }, 201);
});

adminAuditLogRoutes.get("/", async (c) => {
  const range = c.req.query("range")?.trim().toLowerCase() || "all";
  const now = Date.now();

  let cutoff = 0;
  if (range === "today") {
    const istOffset = 5.5 * 3600 * 1000;
    const istNow = new Date(now + istOffset);
    istNow.setUTCHours(0, 0, 0, 0);
    cutoff = istNow.getTime() - istOffset;
  } else if (range === "7d") {
    cutoff = now - 7 * 86400 * 1000;
  }

  const db = c.env.DB;
  const { results } = await db
    .prepare("SELECT * FROM admin_audit_log WHERE timestamp >= ? ORDER BY timestamp DESC LIMIT 200")
    .bind(cutoff)
    .all<AdminAuditLogRow>();

  const list = (results || []).map((r) => ({
    id: r.id,
    actionType: r.action_type,
    description: r.description,
    adminEmail: r.admin_email,
    timestamp: r.timestamp,
  }));

  return c.json({ success: true, data: list });
});
