// ============================================================================
// Question Reports & Admin Audit Log Routes
// ============================================================================

import { Hono } from "hono";
import { requireAdmin, requirePermission } from "../middleware/authMiddleware";
import { checkRateLimit } from "../middleware/rateLimiter";
import { AuthUser, Env, QuestionReportRow, AdminAuditLogRow } from "../types";
import { prepareAdminAuditLog, recordAdminAction, runBatch } from "../util/auditLog";

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
reportRoutes.get("/", requirePermission("moderate_feedback"), async (c) => {
  const typeParam = c.req.query("type")?.trim().toLowerCase();
  const list = await fetchPendingReports(c.env.DB, typeParam);
  return c.json({ success: true, data: list });
});

// ----------------------------------------------------------------------------
// 2. Admin Question Reports
adminReportRoutes.use("*", requirePermission("moderate_feedback"));

adminReportRoutes.get("/", async (c) => {
  const typeParam = c.req.query("type")?.trim().toLowerCase();
  const list = await fetchPendingReports(c.env.DB, typeParam);
  return c.json({ success: true, data: list });
});

function validReportId(value: unknown): value is string {
  return typeof value === "string" && value.length > 0 && value.length <= 128 &&
    value === value.trim();
}

adminReportRoutes.put("/:id/dismiss", async (c) => {
  const id = c.req.param("id");
  if (!validReportId(id)) return c.json({ success: false, error: "Invalid report ID" }, 400);
  const db = c.env.DB;
  const pending = await db.prepare("SELECT id FROM question_reports WHERE id = ? AND status = 'pending'")
    .bind(id).first<{ id: string }>();
  if (!pending) return c.json({ success: false, error: "Pending report not found" }, 404);

  const update = db.prepare("UPDATE question_reports SET status = 'dismissed' WHERE id = ? AND status = 'pending'").bind(id);
  // D1 executes the ordered batch on one SQLite connection. changes() reflects
  // the immediately preceding UPDATE, so a racing dismissal cannot forge an audit.
  const { stmt: audit } = prepareAdminAuditLog(db, c.get("user"), {
    actionType: "REPORT_DISMISSED",
    description: `Dismissed question report '${id}'`,
    targetType: "question_report",
    targetId: id,
    condition: { sql: "changes() = 1", bindings: [] },
  });
  const results = await runBatch(db, [update, audit]);
  if (results[0]?.meta?.changes !== 1 || results[1]?.meta?.changes !== 1) {
    return c.json({ success: false, error: "Report dismissal not confirmed; reload before retrying" }, 409);
  }
  return c.json({ success: true });
});

adminReportRoutes.post("/dismiss-batch", async (c) => {
  const body = await c.req.json().catch(() => null);
  const rawIds: unknown = body && typeof body === "object" && !Array.isArray(body) ? body.ids : null;
  if (!Array.isArray(rawIds) || rawIds.length < 1 || rawIds.length > 100 ||
      !rawIds.every(validReportId) || new Set(rawIds).size !== rawIds.length) {
    return c.json({ success: false, error: "Provide 1 to 100 unique valid report IDs" }, 400);
  }
  const ids: string[] = rawIds;
  const db = c.env.DB;
  const placeholders = ids.map(() => "?").join(", ");
  const found = await db.prepare(
    `SELECT COUNT(*) AS count FROM question_reports WHERE status = 'pending' AND id IN (${placeholders})`
  ).bind(...ids).first<{ count: number }>();
  if (found?.count !== ids.length) {
    return c.json({ success: false, error: "Some pending reports are unavailable; reload before retrying" }, 409);
  }

  // One conditional UPDATE, rather than N independent writes, prevents partial
  // dismissal if even one report changes between preflight and the D1 transaction.
  const update = db.prepare(
    `UPDATE question_reports SET status = 'dismissed'
     WHERE status = 'pending' AND id IN (${placeholders})
       AND (SELECT COUNT(*) FROM question_reports
            WHERE status = 'pending' AND id IN (${placeholders})) = ?`
  ).bind(...ids, ...ids, ids.length);
  const { stmt: audit } = prepareAdminAuditLog(db, c.get("user"), {
    actionType: "REPORT_BATCH_DISMISSED",
    description: `Batch dismissed ${ids.length} question reports`,
    targetType: "question_report",
    targetId: "batch",
    metadata: { count: ids.length },
    condition: { sql: "changes() = ?", bindings: [ids.length] },
  });
  const results = await runBatch(db, [update, audit]);
  if (results[0]?.meta?.changes !== ids.length || results[1]?.meta?.changes !== 1) {
    return c.json({ success: false, error: "Batch dismissal not confirmed; reload before retrying" }, 409);
  }
  return c.json({ success: true });
});

adminReportRoutes.delete("/:id", async (c) => {
  const id = c.req.param("id");
  if (!validReportId(id)) return c.json({ success: false, error: "Invalid report ID" }, 400);
  const db = c.env.DB;
  const row = await db.prepare("SELECT id FROM question_reports WHERE id = ?").bind(id).first<{ id: string }>();
  if (!row) return c.json({ success: false, error: "Report not found" }, 404);

  const removal = db.prepare("DELETE FROM question_reports WHERE id = ?").bind(id);
  const { stmt: audit } = prepareAdminAuditLog(db, c.get("user"), {
    actionType: "REPORT_DELETED",
    description: `Deleted question report '${id}'`,
    targetType: "question_report",
    targetId: id,
    condition: { sql: "changes() = 1", bindings: [] },
  });
  const results = await runBatch(db, [removal, audit]);
  if (results[0]?.meta?.changes !== 1 || results[1]?.meta?.changes !== 1) {
    return c.json({ success: false, error: "Report deletion not confirmed; reload before retrying" }, 409);
  }
  return c.json({ success: true });
});

// ----------------------------------------------------------------------------
// 3. Admin Audit Log Routes
// POST /api/admin/audit-log
// GET /api/admin/audit-log?range=today|7d|all&before=123456&admin=email&type=TYPE&q=search&limit=50
// GET /api/admin/audit-log/types
// ----------------------------------------------------------------------------
adminAuditLogRoutes.use("*", requirePermission("view_analytics_logs"));

const FORBIDDEN_METADATA_KEY_REGEX = /(token|secret|password|auth|answer|credential|phone|username|api_?key|secret_?key)/i;

function safeFormatMetadata(raw: any): Record<string, any> | null {
  if (!raw) return null;
  let obj = raw;
  if (typeof raw === "string") {
    try {
      obj = JSON.parse(raw);
    } catch {
      return null;
    }
  }
  if (typeof obj !== "object" || obj === null) return null;
  const safe: Record<string, any> = {};
  for (const [k, v] of Object.entries(obj)) {
    if (FORBIDDEN_METADATA_KEY_REGEX.test(k)) continue;
    if (typeof v === "string") {
      safe[k] = v.length > 200 ? v.substring(0, 200) + "..." : v;
    } else if (typeof v === "number" || typeof v === "boolean" || v === null) {
      safe[k] = v;
    } else if (Array.isArray(v)) {
      safe[k] = v.slice(0, 20).filter((item) => typeof item === "string" || typeof item === "number" || typeof item === "boolean");
    } else if (typeof v === "object") {
      safe[k] = safeFormatMetadata(v);
    }
  }
  return safe;
}

adminAuditLogRoutes.get("/types", async (c) => {
  const db = c.env.DB;
  const { results } = await db
    .prepare("SELECT DISTINCT action_type FROM admin_audit_log ORDER BY action_type ASC")
    .all<{ action_type: string }>();
  const types = (results || []).map((r) => r.action_type).filter(Boolean);
  return c.json({ success: true, data: types });
});

adminAuditLogRoutes.post("/", async (c) => {
  const user = c.get("user");
  const body = await c.req.json().catch(() => ({}));
  const actionType = String(body.actionType || "").trim();
  const description = String(body.description || "").trim().substring(0, 500);

  if (!actionType || !/^[A-Z0-9_]{3,64}$/.test(actionType)) {
    return c.json({ success: false, error: "Valid actionType is required" }, 400);
  }

  const db = c.env.DB;
  const { id, stmt } = prepareAdminAuditLog(db, user, {
    actionType,
    description,
    source: "client",
  });
  await stmt.run();

  return c.json({ success: true, data: { id } }, 201);
});

adminAuditLogRoutes.get("/", async (c) => {
  const range = c.req.query("range")?.trim().toLowerCase() || "all";
  const beforeRaw = c.req.query("before")?.trim();
  const adminFilter = c.req.query("admin")?.trim().toLowerCase();
  const typeFilter = c.req.query("type")?.trim().toUpperCase();
  const searchQuery = c.req.query("q")?.trim();
  const limitParam = parseInt(c.req.query("limit") || "50", 10);
  const limit = Math.max(1, Math.min(isNaN(limitParam) ? 50 : limitParam, 100));

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

  const conditions: string[] = [];
  const bindings: any[] = [];

  if (cutoff > 0) {
    conditions.push("timestamp >= ?");
    bindings.push(cutoff);
  }

  if (beforeRaw) {
    const beforeTs = parseInt(beforeRaw, 10);
    if (!isNaN(beforeTs) && beforeTs > 0) {
      conditions.push("timestamp < ?");
      bindings.push(beforeTs);
    }
  }

  if (adminFilter && adminFilter !== "all") {
    conditions.push("LOWER(admin_email) = ?");
    bindings.push(adminFilter);
  }

  if (typeFilter && typeFilter !== "ALL") {
    conditions.push("action_type = ?");
    bindings.push(typeFilter);
  }

  if (searchQuery) {
    const escaped = `%${searchQuery.replace(/[%_\\]/g, "\\$&")}%`;
    conditions.push("(description LIKE ? ESCAPE '\\' OR target_id LIKE ? ESCAPE '\\' OR admin_email LIKE ? ESCAPE '\\')");
    bindings.push(escaped, escaped, escaped);
  }

  const whereClause = conditions.length > 0 ? "WHERE " + conditions.join(" AND ") : "";
  const query = `SELECT * FROM admin_audit_log ${whereClause} ORDER BY timestamp DESC, id DESC LIMIT ?`;
  bindings.push(limit + 1);

  const db = c.env.DB;
  const { results } = await db.prepare(query).bind(...bindings).all<AdminAuditLogRow>();
  const rows = results || [];
  const hasMore = rows.length > limit;
  const listRows = hasMore ? rows.slice(0, limit) : rows;

  const list = listRows.map((r) => {
    return {
      id: r.id,
      actionType: r.action_type,
      description: r.description,
      adminEmail: r.admin_email,
      timestamp: r.timestamp,
      actorUid: r.actor_uid || null,
      targetType: r.target_type || null,
      targetId: r.target_id || null,
      metadata: safeFormatMetadata(r.metadata),
      source: r.source || "server",
    };
  });

  const nextCursor = hasMore && list.length > 0 ? list[list.length - 1].timestamp : null;

  return c.json({
    success: true,
    data: list,
    nextCursor,
    hasMore,
  });
});
