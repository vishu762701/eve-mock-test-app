// ============================================================================
// Admin Dashboard & Analytics Routes
// ============================================================================

import { Hono } from "hono";
import { requireAdmin, requirePermission } from "../middleware/authMiddleware";
import { AuthUser, Env, ExamRow, PremiumConfigRow, PremiumEntitlementRow, PremiumOrderRow, QuestionRow } from "../types";
import { activateUserPremium, getPremiumConfig } from "./premium";
import { getGeminiApiKeys, getModelChain, classifyGeminiError } from "../ai/generator";
import { checkRateLimit } from "../middleware/rateLimiter";
import { isUserAdmin, HARDCODED_ADMIN_EMAILS, ALL_ADMIN_RIGHTS, AdminRight, isOwnerEmail, getAdminPermissions } from "../auth";
import { prepareAdminAuditLog, recordAdminAction, runBatch } from "../util/auditLog";
import { getMonthlyApiUsage, trackGeminiCall } from "../util/usageTracker";
import { validateReformatInput, reformatExplanationWithAi } from "../ai/explanationReformat";

const EMAIL_REGEX = /^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)+$/;

export const adminRoutes = new Hono<{ Bindings: Env; Variables: { user: AuthUser } }>();

// All routes here strictly require admin authorization
adminRoutes.use("*", requireAdmin);

// GET /api/admin/me - Returns caller's owner flag, rights set and custom title
adminRoutes.get("/me", async (c) => {
  const user = c.get("user");
  const db = c.env.DB;
  const perms = await getAdminPermissions(db, user?.email);
  if (!perms) return c.json({ success: false, error: "Admin permissions could not be verified" }, 403);
  return c.json({
    success: true,
    data: {
      email: user?.email || "",
      isOwner: perms?.isOwner ?? false,
      rights: perms?.rights ?? [],
      customTitle: perms?.customTitle ?? null,
    },
  });
});

// GET /api/admin/admins - List dynamic admins with rights and titles
adminRoutes.get("/admins", async (c) => {
  const db = c.env.DB;
  const { results: admins } = await db.prepare("SELECT email, created_at FROM admins ORDER BY email ASC").all<{ email: string; created_at?: number }>();
  const { results: rightsList } = await db.prepare("SELECT email, rights, custom_title, updated_at FROM admin_rights").all<{ email: string; rights: string; custom_title?: string | null; updated_at: number }>();

  const rightsMap = new Map<string, { rights: string[]; customTitle: string | null }>();
  for (const r of rightsList || []) {
    let parsed: string[] = [];
    try {
      parsed = JSON.parse(r.rights);
      if (!Array.isArray(parsed)) parsed = [];
    } catch {
      parsed = [];
    }
    rightsMap.set(r.email.toLowerCase(), {
      rights: parsed,
      customTitle: r.custom_title || "Admin",
    });
  }

  const data = (admins || []).map((a) => {
    const norm = a.email.toLowerCase();
    const isOwner = isOwnerEmail(norm);
    const rightsInfo = rightsMap.get(norm);
    return {
      email: a.email,
      isOwner,
      rights: isOwner ? [...ALL_ADMIN_RIGHTS] : (rightsInfo?.rights || []),
      customTitle: isOwner ? "Owner" : (rightsInfo?.customTitle || "Admin"),
      createdAt: a.created_at || 0,
    };
  });

  return c.json({ success: true, data });
});

// POST /api/admin/admins - Add dynamic admin with explicit rights selection
adminRoutes.post("/admins", requirePermission("manage_admins_rights"), async (c) => {
  const user = c.get("user");
  const body = await c.req.json().catch(() => ({}));
  if (!body || typeof body !== "object" || Array.isArray(body)) {
    return c.json({ success: false, error: "An admin object is required" }, 400);
  }
  const email = String(body.email || "").trim().toLowerCase();
  const db = c.env.DB;

  if (!email || email.length > 254 || !EMAIL_REGEX.test(email)) {
    return c.json({ success: false, error: "A valid email address (max 254 characters) is required" }, 400);
  }

  const callerPerms = await getAdminPermissions(db, user?.email);
  if (!callerPerms) return c.json({ success: false, error: "Admin permissions could not be verified" }, 403);
  if (body.rights !== undefined && (!Array.isArray(body.rights) ||
      body.rights.some((right: unknown) => typeof right !== "string" || !ALL_ADMIN_RIGHTS.includes(right as AdminRight)))) {
    return c.json({ success: false, error: "Rights must be an array of recognized admin permissions" }, 400);
  }
  // Old Android clients omit rights. Preserve owner defaults while limiting
  // delegated creation to the caller's actual rights. An explicit [] means none.
  const requestedRights: AdminRight[] = body.rights === undefined ? [...callerPerms.rights] : body.rights;
  const customTitle = body.customTitle !== undefined ? String(body.customTitle).trim().substring(0, 64) : "Admin";

  // Caller cannot grant rights they do not hold unless owner
  if (!callerPerms?.isOwner) {
    const callerRightsSet = new Set<string>(callerPerms?.rights || []);
    for (const r of requestedRights) {
      if (!callerRightsSet.has(r)) {
        return c.json({ success: false, error: `Cannot grant permission '${r}' which you do not possess` }, 403);
      }
    }
  }

  const validRights = [...new Set(requestedRights)];
  const rightsJson = JSON.stringify(validRights);
  const now = Date.now();

  const insertStmt = db
    .prepare("INSERT INTO admins (email, created_at) VALUES (?, ?) ON CONFLICT(email) DO NOTHING")
    .bind(email, now);

  const insertRightsStmt = db
    .prepare("INSERT INTO admin_rights (email, rights, custom_title, updated_at) VALUES (?, ?, ?, ?) ON CONFLICT(email) DO NOTHING")
    .bind(email, rightsJson, customTitle, now);

  const { stmt: auditStmt } = prepareAdminAuditLog(db, user, {
    actionType: "ADMIN_ADDED",
    description: `Added dynamic admin '${email}'`,
    targetType: "admin",
    targetId: email,
    metadata: { rights: validRights, customTitle },
  });

  await runBatch(db, [insertStmt, insertRightsStmt, auditStmt]);

  return c.json({ success: true, data: { email, rights: validRights, customTitle } }, 201);
});

// PUT /api/admin/admins/:email/rights - Update admin rights and custom title
adminRoutes.put("/admins/:email/rights", requirePermission("manage_admins_rights"), async (c) => {
  const user = c.get("user");
  const targetEmail = (c.req.param("email") || "").trim().toLowerCase();
  const db = c.env.DB;
  const callerEmail = (user?.email || "").trim().toLowerCase();

  const body = await c.req.json().catch(() => ({}));
  const requestedRights: string[] = Array.isArray(body.rights) ? body.rights.map(String) : [];
  const customTitle = body.customTitle !== undefined && body.customTitle !== null ? String(body.customTitle).trim().substring(0, 64) : null;

  // 1. Built-in owner rights cannot be modified by anyone
  if (isOwnerEmail(targetEmail)) {
    return c.json({ success: false, error: "Built-in owner permissions and role cannot be modified" }, 403);
  }

  // 2. Check target exists in admins table
  const targetAdmin = await db.prepare("SELECT email FROM admins WHERE LOWER(TRIM(email)) = ?").bind(targetEmail).first<{ email: string }>();
  if (!targetAdmin) {
    return c.json({ success: false, error: "Admin not found" }, 404);
  }

  // 3. Resolve caller permissions
  const callerPerms = await getAdminPermissions(db, callerEmail);
  const targetPerms = await getAdminPermissions(db, targetEmail);
  if (!callerPerms || !targetPerms) {
    return c.json({ success: false, error: "Admin permissions could not be verified" }, 403);
  }

  // 4. Caller cannot self-escalate
  if (callerEmail === targetEmail && !callerPerms?.isOwner) {
    const currentRights = new Set<string>(callerPerms?.rights || []);
    for (const r of requestedRights) {
      if (!currentRights.has(r)) {
        return c.json({ success: false, error: "Self-escalation is not allowed: you cannot grant yourself rights you do not currently hold" }, 403);
      }
    }
  }

  // 5. Caller cannot edit an admin whose rights exceed caller's (Telegram rule)
  if (!callerPerms?.isOwner) {
    const callerRightsSet = new Set<string>(callerPerms?.rights || []);
    const targetRights = targetPerms?.rights || [];
    const targetHasExceeding = targetRights.some((r) => !callerRightsSet.has(r));
    if (targetHasExceeding) {
      return c.json({ success: false, error: "Cannot modify permissions of an administrator whose rights exceed yours" }, 403);
    }

    // 6. Caller cannot grant rights they do not hold
    for (const r of requestedRights) {
      if (!callerRightsSet.has(r)) {
        return c.json({ success: false, error: `Cannot grant permission '${r}' which you do not possess` }, 403);
      }
    }
  }

  // 7. Last holder of manage_admins_rights protection
  const targetHadManageAdmins = targetPerms?.rights.includes("manage_admins_rights");
  const targetWillHaveManageAdmins = requestedRights.includes("manage_admins_rights");
  if (targetHadManageAdmins && !targetWillHaveManageAdmins) {
    const ownersCount = HARDCODED_ADMIN_EMAILS.size;
    const otherAdmins = await db.prepare("SELECT email, rights FROM admin_rights WHERE LOWER(TRIM(email)) != ?").bind(targetEmail).all<{ email: string; rights: string }>();
    const otherDynamicHolders = (otherAdmins.results || []).filter((r) => {
      try {
        const rights = JSON.parse(r.rights);
        return Array.isArray(rights) && rights.includes("manage_admins_rights");
      } catch {
        return false;
      }
    });

    if (ownersCount === 0 && otherDynamicHolders.length === 0) {
      return c.json({ success: false, error: "The last holder of manage_admins_rights cannot lose this permission" }, 400);
    }
  }

  // Filter requestedRights to valid canonical rights
  const validRights = requestedRights.filter((r) => ALL_ADMIN_RIGHTS.includes(r as any));
  const rightsJson = JSON.stringify(validRights);
  const now = Date.now();

  const updateRightsStmt = db
    .prepare("INSERT INTO admin_rights (email, rights, custom_title, updated_at) VALUES (?1, ?2, ?3, ?4) ON CONFLICT(email) DO UPDATE SET rights = ?2, custom_title = COALESCE(?3, custom_title), updated_at = ?4")
    .bind(targetEmail, rightsJson, customTitle, now);

  const { stmt: auditStmt } = prepareAdminAuditLog(db, user, {
    actionType: "ADMIN_RIGHTS_CHANGED",
    description: `Updated permissions for '${targetEmail}' (${validRights.length} rights, title: ${customTitle || 'unchanged'})`,
    targetType: "admin",
    targetId: targetEmail,
    metadata: { newRights: validRights, customTitle },
  });

  await runBatch(db, [updateRightsStmt, auditStmt]);

  return c.json({
    success: true,
    data: {
      email: targetEmail,
      rights: validRights,
      customTitle: customTitle || targetPerms?.customTitle || "Admin",
    },
  });
});

// DELETE /api/admin/admins/:email - Remove dynamic admin
adminRoutes.delete("/admins/:email", requirePermission("manage_admins_rights"), async (c) => {
  const user = c.get("user");
  const email = (c.req.param("email") || "").trim().toLowerCase();
  const db = c.env.DB;

  // 1. Reject removing yourself
  const actingEmail = (user?.email || "").trim().toLowerCase();
  if (actingEmail && actingEmail === email) {
    return c.json({ success: false, error: "Cannot remove yourself from the admin list" }, 409);
  }

  // 2. Reject removing hardcoded built-in admin
  if (HARDCODED_ADMIN_EMAILS.has(email)) {
    return c.json({ success: false, error: "Cannot remove built-in system administrator" }, 409);
  }

  // 3. Keep at least one effective admin at all times
  const dynamicRow = await db.prepare("SELECT email FROM admins WHERE LOWER(TRIM(email)) = ?").bind(email).first<{ email: string }>();
  if (!dynamicRow) {
    return c.json({ success: false, error: "Admin not found" }, 404);
  }

  const totalDynamicAdmins = await db.prepare("SELECT COUNT(*) as count FROM admins").first<{ count: number }>();
  const totalEffectiveAdmins = HARDCODED_ADMIN_EMAILS.size + (totalDynamicAdmins?.count ?? 0);
  if (totalEffectiveAdmins <= 1) {
    return c.json({ success: false, error: "Cannot remove the last remaining administrator" }, 409);
  }

  // 4. Last holder of manage_admins_rights check
  const callerPerms = await getAdminPermissions(db, actingEmail);
  const targetPerms = await getAdminPermissions(db, email);
  if (!callerPerms || !targetPerms) {
    return c.json({ success: false, error: "Admin permissions could not be verified" }, 403);
  }
  if (!callerPerms.isOwner && targetPerms.rights.some(right => !callerPerms.rights.includes(right))) {
    return c.json({ success: false, error: "Cannot remove an administrator whose rights exceed yours" }, 403);
  }
  if (targetPerms?.rights.includes("manage_admins_rights")) {
    const ownersCount = HARDCODED_ADMIN_EMAILS.size;
    const otherAdmins = await db.prepare("SELECT email, rights FROM admin_rights WHERE LOWER(TRIM(email)) != ?").bind(email).all<{ email: string; rights: string }>();
    const otherDynamicHolders = (otherAdmins.results || []).filter((r) => {
      try {
        const rights = JSON.parse(r.rights);
        return Array.isArray(rights) && rights.includes("manage_admins_rights");
      } catch {
        return false;
      }
    });
    if (ownersCount === 0 && otherDynamicHolders.length === 0) {
      return c.json({ success: false, error: "Cannot remove the last holder of manage_admins_rights" }, 409);
    }
  }

  const deleteAdminStmt = db.prepare("DELETE FROM admins WHERE LOWER(TRIM(email)) = ?").bind(email);
  const deleteRightsStmt = db.prepare("DELETE FROM admin_rights WHERE LOWER(TRIM(email)) = ?").bind(email);
  const { stmt: auditStmt } = prepareAdminAuditLog(db, user, {
    actionType: "ADMIN_REMOVED",
    description: `Removed dynamic admin '${email}'`,
    targetType: "admin",
    targetId: email,
  });

  const batchRes = await runBatch(db, [deleteAdminStmt, deleteRightsStmt, auditStmt]);
  if ((batchRes[0]?.meta?.changes ?? 0) === 0) {
    return c.json({ success: false, error: "Admin not found" }, 404);
  }
  return c.json({ success: true, message: `Admin ${email} removed successfully` });
});

// GET /api/admin/analytics/exams - Pre-aggregated exam analytics
adminRoutes.get("/analytics/exams", requirePermission("view_analytics_logs"), async (c) => {
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
        LEFT JOIN attempts a ON (e.id = a.exam_id OR substr(a.exam_id, 1, length(e.id) + 2) = e.id || '__') AND a.counted = 1 AND a.timestamp >= ?
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
        LEFT JOIN attempts a ON (e.id = a.exam_id OR substr(a.exam_id, 1, length(e.id) + 2) = e.id || '__') AND a.counted = 1
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
adminRoutes.get("/analytics/questions", requirePermission("view_analytics_logs"), async (c) => {
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
adminRoutes.get("/stats/users", requirePermission("manage_users"), async (c) => {
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

// GET /api/admin/users - Full-dataset search and bounded, deterministic pagination.
// Backward compatibility: callers that omit pagination parameters retain 200 rows.
adminRoutes.get("/users", requirePermission("manage_users"), async (c) => {
  const parsePositiveInt = (raw: string | undefined, fallback: number, maximum: number): number | null => {
    if (raw === undefined) return fallback;
    if (!/^[1-9][0-9]*$/.test(raw) || raw.length > 7) return null;
    const value = Number(raw);
    return Number.isSafeInteger(value) && value <= maximum ? value : null;
  };
  const legacy = c.req.query("page") === undefined && c.req.query("pageSize") === undefined;
  const page = parsePositiveInt(c.req.query("page"), 1, 1000000);
  const pageSize = parsePositiveInt(c.req.query("pageSize"), legacy ? 200 : 50, 200);
  if (page === null || pageSize === null) {
    return c.json({ success: false, error: "Invalid page or pageSize" }, 400);
  }
  const raw = (c.req.query("q") || "").trim();
  if (raw.length > 160) return c.json({ success: false, error: "Search query too long" }, 400);
  // ! is the SQL LIKE escape character. Literal %, _ and ! in search
  // must never become wildcards or change the predicate.
  const q = raw.toLowerCase().replace(/[!%_]/g, (ch) => "!" + ch);
  const where = q
    ? " WHERE LOWER(COALESCE(email, '')) LIKE ? ESCAPE '!' OR LOWER(COALESCE(display_name, '')) LIKE ? ESCAPE '!' OR LOWER(COALESCE(eve_id, '')) LIKE ? ESCAPE '!'"
    : "";
  const params = q ? Array(3).fill(`%${q}%`) : [];
  const db = c.env.DB;
  const offset = (page - 1) * pageSize;
  const order = " ORDER BY last_active DESC, created_at DESC, id ASC LIMIT ? OFFSET ?";

  try {
    const totalRow = await db.prepare("SELECT COUNT(*) AS total FROM users" + where)
      .bind(...params).first<{ total: number }>();
    const total = Number(totalRow?.total ?? 0);
    if (!Number.isSafeInteger(total) || total < 0) throw new Error("Invalid user count");

    const fetchPage = async (includeDisabled: boolean) => {
      const columns = "id, eve_id, email, display_name, dob, category, created_at, last_active" +
        (includeDisabled ? ", COALESCE(disabled, 0) AS disabled" : "");
      return db.prepare("SELECT " + columns + " FROM users" + where + order)
        .bind(...params, pageSize, offset).all<any>();
    };
    let rows: any[];
    try {
      rows = (await fetchPage(true)).results || [];
    } catch (error: any) {
      // Compatibility for legacy DB deployments missing the disabled migration;
      // do not conceal arbitrary database failures with a second query.
      if (!/no such column:\s*disabled/i.test(String(error?.message || error))) throw error;
      rows = (await fetchPage(false)).results || [];
    }
    const users = rows.map((u) => ({
      id: u.id,
      eveId: u.eve_id || null,
      email: u.email,
      displayName: u.display_name || "Student",
      dob: u.dob || "",
      category: u.category || "General",
      createdAt: u.created_at || 0,
      lastActive: u.last_active || 0,
      disabled: Boolean(u.disabled),
    }));
    const totalPages = Math.ceil(total / pageSize);
    return c.json({
      success: true,
      data: users,
      pagination: {
        page, pageSize, total, totalPages,
        hasNext: page < totalPages,
        hasPrevious: page > 1 && totalPages > 0,
      },
    });
  } catch (_error: any) {
    return c.json({ success: false, error: "Unable to load users" }, 500);
  }
});

// POST /api/admin/users/:id/status - Toggle user disabled/ban status
adminRoutes.post("/users/:id/status", requirePermission("manage_users"), async (c) => {
  const user = c.get("user");
  const id = c.req.param("id");
  const body = await c.req.json().catch(() => ({}));
  const disabled = body.disabled === true ? 1 : 0;
  const db = c.env.DB;

  // 1. Fetch user to check existence and email
  const targetUser = await db
    .prepare("SELECT id, email FROM users WHERE id = ?")
    .bind(id)
    .first<{ id: string; email: string }>();

  if (!targetUser) {
    return c.json({ success: false, error: "User not found" }, 404);
  }

  // 2. If attempting to disable, enforce admin self/peer protection
  if (disabled === 1) {
    const actingUid = user?.uid || "";
    const actingEmail = (user?.email || "").trim().toLowerCase();
    const targetEmail = (targetUser.email || "").trim().toLowerCase();

    if (id === actingUid || (actingEmail && targetEmail && actingEmail === targetEmail)) {
      return c.json({ success: false, error: "Cannot disable your own administrator account" }, 409);
    }

    if (targetEmail) {
      const isTargetAdmin = await isUserAdmin(db, targetEmail);
      if (isTargetAdmin) {
        return c.json({ success: false, error: "Cannot disable an administrator account" }, 409);
      }
    }
  }

  // 3. Update status (no request-time ALTER)
  const updateStmt = db.prepare("UPDATE users SET disabled = ? WHERE id = ?").bind(disabled, id);
  const { stmt: auditStmt } = prepareAdminAuditLog(db, user, {
    actionType: "USER_STATUS_TOGGLED",
    description: `${disabled ? "Disabled" : "Enabled"} user account '${id}'`,
    targetType: "user",
    targetId: id,
    metadata: { disabled: Boolean(disabled), targetEmail: targetUser.email },
  });

  await runBatch(db, [updateStmt, auditStmt]);
  return c.json({ success: true, data: { id, disabled: Boolean(disabled) } });
});

// GET /api/admin/users/:id/attempts - Get user test attempts
adminRoutes.get("/users/:id/attempts", requirePermission("manage_users"), async (c) => {
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
adminRoutes.put("/config", requirePermission("manage_config_maintenance"), async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const db = c.env.DB;

  let prevConfig: any = {};
  try {
    const existing = await db
      .prepare("SELECT body FROM app_content WHERE id = 'app_config'")
      .first<{ body: string }>();
    if (existing && existing.body) {
      prevConfig = JSON.parse(existing.body);
    }
  } catch {}

  // 1. Validate minimum_supported_version_code
  let minVersion = prevConfig.minimum_supported_version_code !== undefined ? Number(prevConfig.minimum_supported_version_code) : 1;
  if (body.minimum_supported_version_code !== undefined && body.minimum_supported_version_code !== null) {
    const rawVer = body.minimum_supported_version_code;
    const v = Number(rawVer);
    if (typeof rawVer === "boolean" || !Number.isInteger(v) || isNaN(v) || v < 0) {
      return c.json({ success: false, error: "minimum_supported_version_code must be a non-negative integer" }, 400);
    }
    minVersion = v;
  }

  // 2. Validate maintenance_mode
  let maintenanceMode = prevConfig.maintenance_mode !== undefined ? Boolean(prevConfig.maintenance_mode) : false;
  if (body.maintenance_mode !== undefined && body.maintenance_mode !== null) {
    if (typeof body.maintenance_mode !== "boolean") {
      return c.json({ success: false, error: "maintenance_mode must be a boolean" }, 400);
    }
    maintenanceMode = body.maintenance_mode;
  }

  // 3. Validate maintenance_message
  let maintenanceMessage = prevConfig.maintenance_message !== undefined
    ? String(prevConfig.maintenance_message)
    : "Eve Mock Test is currently undergoing scheduled maintenance. Please check back shortly.";
  if (body.maintenance_message !== undefined && body.maintenance_message !== null) {
    if (typeof body.maintenance_message !== "string" || body.maintenance_message.length > 1000) {
      return c.json({ success: false, error: "maintenance_message must be a string up to 1000 characters" }, 400);
    }
    maintenanceMessage = body.maintenance_message.trim();
  }

  // 4. Validate default_publish_mode
  let defaultPublishMode = prevConfig.default_publish_mode || "paused";
  if (body.default_publish_mode !== undefined && body.default_publish_mode !== null) {
    const mode = String(body.default_publish_mode).trim().toLowerCase();
    if (mode !== "paused" && mode !== "live") {
      return c.json({ success: false, error: "default_publish_mode must be 'paused' or 'live'" }, 400);
    }
    defaultPublishMode = mode;
  }

  const configObj = {
    ...prevConfig,
    minimum_supported_version_code: minVersion,
    maintenance_mode: maintenanceMode,
    maintenance_message: maintenanceMessage,
    default_publish_mode: defaultPublishMode,
  };

  try {
    const user = c.get("user");
    const saveStmt = db
      .prepare(
        "INSERT INTO app_content (id, title, body, updated_at, updated_by) VALUES ('app_config', 'App Configuration', ?, ?, ?) ON CONFLICT(id) DO UPDATE SET body = excluded.body, updated_at = excluded.updated_at, updated_by = excluded.updated_by"
      )
      .bind(JSON.stringify(configObj), Date.now(), user?.email || "admin");

    const actionType = prevConfig.maintenance_mode !== maintenanceMode ? "MAINTENANCE_TOGGLED" : "CONFIG_UPDATED";
    const { stmt: auditStmt } = prepareAdminAuditLog(db, user, {
      actionType,
      description: `Updated app configuration (maintenance=${maintenanceMode}, minVersion=${minVersion})`,
      targetType: "app_content",
      targetId: "app_config",
      metadata: { maintenanceMode, minVersion, defaultPublishMode },
    });

    await runBatch(db, [saveStmt, auditStmt]);

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
adminRoutes.get("/floating-link", requirePermission("manage_content"), async (c) => {
  const db = c.env.DB;
  try {
    const row = await db.prepare("SELECT body FROM app_content WHERE id = 'floating_link'").first<{ body: string }>();
    if (!row || !row.body) {
      return c.json({ success: true, data: { url: "", x: null, y: null }, url: "" });
    }
    let url = "";
    let x: number | null = null;
    let y: number | null = null;
    const trimmed = row.body.trim();
    if (trimmed.startsWith("{")) {
      try {
        const parsed = JSON.parse(trimmed);
        url = typeof parsed.url === "string" ? parsed.url.trim() : "";
        x = typeof parsed.x === "number" && !isNaN(parsed.x) ? parsed.x : null;
        y = typeof parsed.y === "number" && !isNaN(parsed.y) ? parsed.y : null;
      } catch {
        url = trimmed;
      }
    } else {
      url = trimmed;
    }
    return c.json({ success: true, data: { url, x, y }, url, x, y });
  } catch (err: any) {
    return c.json({ success: false, error: err.message }, 500);
  }
});

// PUT /api/admin/floating-link
adminRoutes.put("/floating-link", requirePermission("manage_content"), async (c) => {
  const body = await c.req.json().catch(() => ({}));

  const db = c.env.DB;
  let existingUrl = "";
  let existingX: number | null = null;
  let existingY: number | null = null;
  try {
    const existingRow = await db.prepare("SELECT body FROM app_content WHERE id = 'floating_link'").first<{ body: string }>();
    if (existingRow?.body) {
      const trimmed = existingRow.body.trim();
      if (trimmed.startsWith("{")) {
        try {
          const parsed = JSON.parse(trimmed);
          existingUrl = typeof parsed.url === "string" ? parsed.url.trim() : "";
          existingX = typeof parsed.x === "number" && !isNaN(parsed.x) ? parsed.x : null;
          existingY = typeof parsed.y === "number" && !isNaN(parsed.y) ? parsed.y : null;
        } catch {
          existingUrl = trimmed;
        }
      } else {
        existingUrl = trimmed;
      }
    }
  } catch {}

  let rawUrl = body.url !== undefined ? String(body.url).trim() : (body.link !== undefined ? String(body.link).trim() : existingUrl);
  if (rawUrl && !rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
    rawUrl = `https://${rawUrl}`;
  }
  const validation = validateFloatingLinkUrl(rawUrl);
  if (!validation.valid) {
    return c.json({ success: false, error: validation.error }, 400);
  }

  let newX: number | null = existingX;
  if (typeof body.x === "number" && !isNaN(body.x)) {
    newX = Math.max(0, Math.min(1, body.x));
  } else if (body.x === null) {
    newX = null;
  }

  let newY: number | null = existingY;
  if (typeof body.y === "number" && !isNaN(body.y)) {
    newY = Math.max(0, Math.min(1, body.y));
  } else if (body.y === null) {
    newY = null;
  }

  const cleanUrl = validation.cleanUrl || "";
  if (!cleanUrl && newX === null && newY === null) {
    const user = c.get("user");
    const deleteStmt = db.prepare("DELETE FROM app_content WHERE id = 'floating_link'");
    const { stmt: auditStmt } = prepareAdminAuditLog(db, user, {
      actionType: "FLOATING_LINK_UPDATED",
      description: "Cleared floating link",
      targetType: "app_content",
      targetId: "floating_link",
    });
    await runBatch(db, [deleteStmt, auditStmt]);
    return c.json({ success: true, data: { url: "", x: null, y: null }, url: "", x: null, y: null });
  }

  const user = c.get("user");
  const updatedBy = user?.email || "admin";
  const now = Date.now();
  const payload = JSON.stringify({ url: cleanUrl, x: newX, y: newY });

  try {
    const saveStmt = db
      .prepare(
        "INSERT INTO app_content (id, title, body, updated_at, updated_by) VALUES ('floating_link', 'Floating Link', ?, ?, ?) ON CONFLICT(id) DO UPDATE SET body = excluded.body, updated_at = excluded.updated_at, updated_by = excluded.updated_by"
      )
      .bind(payload, now, updatedBy);

    const { stmt: auditStmt } = prepareAdminAuditLog(db, user, {
      actionType: "FLOATING_LINK_UPDATED",
      description: `Updated floating link to '${cleanUrl}'`,
      targetType: "app_content",
      targetId: "floating_link",
      metadata: { url: cleanUrl, x: newX, y: newY },
    });

    await runBatch(db, [saveStmt, auditStmt]);

    return c.json({ success: true, data: { url: cleanUrl, x: newX, y: newY }, url: cleanUrl, x: newX, y: newY });
  } catch (err: any) {
    return c.json({ success: false, error: err.message }, 500);
  }
});

// ============================================================================
// Admin Premium Management Routes
// ============================================================================

// GET /api/admin/premium/config - Get current premium plan & payment configuration
adminRoutes.get("/premium/config", requirePermission("manage_premium"), async (c) => {
  const db = c.env.DB;
  const config = await getPremiumConfig(db);
  // Legacy D1 signing values are retired and must not be sent to clients.
  const { webhook_secret: _retiredWebhookSecret, ...safeConfig } = config;
  let benefits: string[] = [];
  try {
    benefits = JSON.parse(config.benefits_json);
  } catch (_e) {
    benefits = [
      "Access to all eligible tests",
      "Unlimited eligible reattempts",
      "No normal 3-attempt restriction while Premium is active"
    ];
  }
  return c.json({
    success: true,
    data: {
      ...safeConfig,
      isEnabled: Boolean(config.is_enabled),
      isLifetime: Boolean(config.is_lifetime),
      qrEnabled: Boolean(config.qr_enabled),
      upiEnabled: Boolean(config.upi_enabled),
      benefits
    }
  });
});

// PUT /api/admin/premium/config - Update premium plan & payment configuration
adminRoutes.put("/premium/config", requirePermission("manage_premium"), async (c) => {
  const db = c.env.DB;
  const user = c.get("user");
  const body = await c.req.json().catch(() => ({}));
  const now = Date.now();
  const updatedBy = user?.email || "admin";

  const isEnabled = body.isEnabled !== undefined ? (body.isEnabled ? 1 : 0) : 1;
  const planName = String(body.planName || "Premium Pro").trim();
  const priceInr = parseInt(String(body.priceInr || "99"), 10) || 99;
  const currency = String(body.currency || "INR").trim().toUpperCase();
  const durationDays = parseInt(String(body.durationDays || "30"), 10) || 30;
  const isLifetime = body.isLifetime ? 1 : 0;
  const description = String(body.description || "Unlock all eligible tests & unlimited reattempts.").trim();
  const rawBenefits = Array.isArray(body.benefits) ? body.benefits : ["Access to all eligible tests", "Unlimited eligible reattempts"];
  const benefitsJson = JSON.stringify(rawBenefits);
  const qrEnabled = body.qrEnabled !== undefined ? (body.qrEnabled ? 1 : 0) : 1;
  const upiEnabled = body.upiEnabled !== undefined ? (body.upiEnabled ? 1 : 0) : 1;
  const sessionExpiryMinutes = parseInt(String(body.sessionExpiryMinutes || "10"), 10) || 10;
  const merchantVpa = String(body.merchantVpa || "evemocktest@upi").trim();
  const merchantName = String(body.merchantName || "Eve Mock Test").trim();
  // The webhook signing key is managed only as a Cloudflare Worker secret.
  // The legacy database column stays empty for migration compatibility.
  const webhookSecret = "";

  const configStmt = db
    .prepare(
      `INSERT INTO premium_config (
         id, is_enabled, plan_name, price_inr, currency, duration_days, is_lifetime,
         description, benefits_json, qr_enabled, upi_enabled, session_expiry_minutes,
         merchant_vpa, merchant_name, webhook_secret, updated_at, updated_by
       ) VALUES ('default', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
       ON CONFLICT(id) DO UPDATE SET
         is_enabled = excluded.is_enabled,
         plan_name = excluded.plan_name,
         price_inr = excluded.price_inr,
         currency = excluded.currency,
         duration_days = excluded.duration_days,
         is_lifetime = excluded.is_lifetime,
         description = excluded.description,
         benefits_json = excluded.benefits_json,
         qr_enabled = excluded.qr_enabled,
         upi_enabled = excluded.upi_enabled,
         session_expiry_minutes = excluded.session_expiry_minutes,
         merchant_vpa = excluded.merchant_vpa,
         merchant_name = excluded.merchant_name,
         webhook_secret = excluded.webhook_secret,
         updated_at = excluded.updated_at,
         updated_by = excluded.updated_by`
    )
    .bind(
      isEnabled,
      planName,
      priceInr,
      currency,
      durationDays,
      isLifetime,
      description,
      benefitsJson,
      qrEnabled,
      upiEnabled,
      sessionExpiryMinutes,
      merchantVpa,
      merchantName,
      webhookSecret,
      now,
      updatedBy
    );

  const { stmt: auditStmt } = prepareAdminAuditLog(db, user, {
    actionType: "PREMIUM_CONFIG_UPDATED",
    description: `Updated premium configuration (${planName}, ₹${priceInr})`,
    targetType: "premium_config",
    targetId: "default",
    metadata: { planName, priceInr, currency, durationDays, isLifetime: Boolean(isLifetime) },
  });

  await runBatch(db, [configStmt, auditStmt]);

  return c.json({ success: true });
});

// GET /api/admin/premium/transactions - List all payment orders / transactions
adminRoutes.get("/premium/transactions", requirePermission("manage_premium"), async (c) => {
  const db = c.env.DB;
  const limit = parseInt(c.req.query("limit") || "100", 10);
  const statusFilter = c.req.query("status") || "";

  let query = "SELECT * FROM premium_orders";
  const params: any[] = [];
  if (statusFilter) {
    query += " WHERE status = ?";
    params.push(statusFilter.toUpperCase());
  }
  query += " ORDER BY created_at DESC LIMIT ?";
  params.push(limit);

  const stmt = db.prepare(query);
  const { results } = await (params.length === 1 ? stmt.bind(params[0]) : stmt.bind(params[0], params[1])).all<PremiumOrderRow>();

  return c.json({
    success: true,
    data: (results || []).map((o) => ({
      orderId: o.id,
      userId: o.user_id,
      userEmail: o.user_email,
      planId: o.plan_id,
      planName: o.plan_name,
      amount: o.amount,
      currency: o.currency,
      durationDays: o.duration_days,
      isLifetime: Boolean(o.is_lifetime),
      paymentMethod: o.payment_method,
      providerOrderId: o.provider_order_id,
      providerPaymentId: o.provider_payment_id,
      status: o.status,
      createdAt: o.created_at,
      expiresAt: o.expires_at,
      paidAt: o.paid_at
    }))
  });
});

// GET /api/admin/premium/users - List premium users
adminRoutes.get("/premium/users", requirePermission("manage_premium"), async (c) => {
  const db = c.env.DB;
  const now = Date.now();

  const { results } = await db
    .prepare(
      `SELECT pe.*, u.email as user_email, u.display_name
       FROM premium_entitlements pe
       LEFT JOIN users u ON pe.user_id = u.id
       ORDER BY pe.updated_at DESC LIMIT 200`
    )
    .all<any>();

  const list = (results || []).map((r) => {
    const isExpired = r.is_lifetime === 0 && r.expires_at !== null && r.expires_at <= now;
    return {
      userId: r.user_id,
      email: r.user_email || "",
      displayName: r.display_name || "Student",
      planId: r.plan_id,
      planName: r.plan_name,
      paymentOrderId: r.payment_order_id,
      providerPaymentId: r.payment_provider_id,
      activatedAt: r.activated_at,
      expiresAt: r.expires_at,
      isLifetime: Boolean(r.is_lifetime),
      status: isExpired ? "EXPIRED" : r.status,
      source: r.source,
      createdAt: r.created_at,
      updatedAt: r.updated_at
    };
  });

  return c.json({ success: true, data: list });
});

// POST /api/admin/premium/users/grant - Manually grant Premium (marked ADMIN_GRANTED)
adminRoutes.post("/premium/users/grant", requirePermission("manage_premium"), async (c) => {
  const db = c.env.DB;
  const user = c.get("user");
  const body = await c.req.json().catch(() => ({}));
  const userIdOrEmail = String(body.userIdOrEmail || body.userId || "").trim();
  const isLifetime = Boolean(body.isLifetime);

  if (!userIdOrEmail) {
    return c.json({ success: false, error: "userId or email is required" }, 400);
  }

  // 1. Validate planName
  let planName = typeof body.planName === "string" ? body.planName.trim() : "Admin Premium Access";
  if (!planName) planName = "Admin Premium Access";
  if (planName.length > 100) {
    return c.json({ success: false, error: "planName cannot exceed 100 characters" }, 400);
  }

  // 2. Validate durationDays and lifetime confirmation
  let durationDays = 30;
  if (isLifetime) {
    if (body.confirmLifetime !== true) {
      return c.json(
        {
          success: false,
          error: "Explicit confirmation (confirmLifetime: true) is required to grant lifetime premium access",
        },
        400
      );
    }
    durationDays = 0;
  } else {
    const rawDays = body.durationDays;
    if (rawDays === undefined || rawDays === null || rawDays === "") {
      return c.json({ success: false, error: "durationDays is required when isLifetime is false" }, 400);
    }
    const parsedDays = Number(rawDays);
    if (!Number.isInteger(parsedDays) || isNaN(parsedDays) || parsedDays < 1 || parsedDays > 3650) {
      return c.json({ success: false, error: "durationDays must be an integer between 1 and 3650" }, 400);
    }
    durationDays = parsedDays;
  }

  // Resolve user UID if email was passed
  let targetUid = userIdOrEmail;
  if (userIdOrEmail.includes("@")) {
    const userRow = await db.prepare("SELECT id FROM users WHERE LOWER(email) = LOWER(?)").bind(userIdOrEmail).first<{ id: string }>();
    if (!userRow) {
      return c.json({ success: false, error: `User with email ${userIdOrEmail} not found` }, 404);
    }
    targetUid = userRow.id;
  } else {
    const userRow = await db.prepare("SELECT id FROM users WHERE id = ?").bind(targetUid).first<{ id: string }>();
    if (!userRow) {
      return c.json({ success: false, error: `User '${targetUid}' not found` }, 404);
    }
  }

  const entitlement = await activateUserPremium(
    db,
    targetUid,
    "admin_grant",
    planName,
    `ADMIN_${Date.now()}`,
    user.email || "admin",
    durationDays,
    isLifetime,
    "ADMIN_GRANTED"
  );

  await recordAdminAction(db, user, {
    actionType: "PREMIUM_GRANTED",
    description: `Granted ${isLifetime ? "Lifetime" : `${durationDays}d`} premium to user '${targetUid}'`,
    targetType: "user",
    targetId: targetUid,
    metadata: { planName, durationDays, isLifetime },
  });

  return c.json({ success: true, data: entitlement });
});

// POST /api/admin/premium/users/extend - Extend existing Premium expiry
adminRoutes.post("/premium/users/extend", requirePermission("manage_premium"), async (c) => {
  const db = c.env.DB;
  const user = c.get("user");
  const body = await c.req.json().catch(() => ({}));
  const userId = String(body.userId || "").trim();

  if (!userId) {
    return c.json({ success: false, error: "userId is required" }, 400);
  }

  const rawAdditionalDays = body.additionalDays;
  if (rawAdditionalDays === undefined || rawAdditionalDays === null || rawAdditionalDays === "") {
    return c.json({ success: false, error: "additionalDays is required" }, 400);
  }
  const parsedDays = Number(rawAdditionalDays);
  if (!Number.isInteger(parsedDays) || isNaN(parsedDays) || parsedDays < 1 || parsedDays > 3650) {
    return c.json({ success: false, error: "additionalDays must be an integer between 1 and 3650" }, 400);
  }
  const additionalDays = parsedDays;

  const existing = await db
    .prepare("SELECT * FROM premium_entitlements WHERE user_id = ?")
    .bind(userId)
    .first<PremiumEntitlementRow>();

  if (!existing) {
    return c.json({ success: false, error: "User has no existing premium entitlement" }, 404);
  }

  if (existing.is_lifetime === 1) {
    return c.json({ success: false, error: "User already has Lifetime Premium" }, 409);
  }

  const now = Date.now();
  const baseTime = (existing.expires_at && existing.expires_at > now) ? existing.expires_at : now;
  const newExpiresAt = baseTime + additionalDays * 86400000;

  const updateStmt = db
    .prepare("UPDATE premium_entitlements SET expires_at = ?, status = 'ACTIVE', updated_at = ? WHERE user_id = ?")
    .bind(newExpiresAt, now, userId);

  const { stmt: auditStmt } = prepareAdminAuditLog(db, user, {
    actionType: "PREMIUM_EXTENDED",
    description: `Extended premium for user '${userId}' by ${additionalDays} days`,
    targetType: "user",
    targetId: userId,
    metadata: { additionalDays, newExpiresAt },
  });

  await runBatch(db, [updateStmt, auditStmt]);

  return c.json({ success: true, data: { userId, expiresAt: newExpiresAt } });
});

// POST /api/admin/premium/users/revoke - Revoke Premium
adminRoutes.post("/premium/users/revoke", requirePermission("manage_premium"), async (c) => {
  const db = c.env.DB;
  const user = c.get("user");
  const body = await c.req.json().catch(() => ({}));
  const userId = String(body.userId || "").trim();

  if (!userId) {
    return c.json({ success: false, error: "userId is required" }, 400);
  }

  const existing = await db
    .prepare("SELECT * FROM premium_entitlements WHERE user_id = ?")
    .bind(userId)
    .first<PremiumEntitlementRow>();

  if (!existing) {
    return c.json({ success: false, error: "User has no existing premium entitlement" }, 404);
  }

  const now = Date.now();
  const revokeStmt = db
    .prepare("UPDATE premium_entitlements SET status = 'REVOKED', updated_at = ? WHERE user_id = ?")
    .bind(now, userId);

  const { stmt: auditStmt } = prepareAdminAuditLog(db, user, {
    actionType: "PREMIUM_REVOKED",
    description: `Revoked premium for user '${userId}'`,
    targetType: "user",
    targetId: userId,
  });

  await runBatch(db, [revokeStmt, auditStmt]);

  return c.json({ success: true, data: { userId, status: "REVOKED" } });
});

// POST /api/admin/ai-health - Test AI connection per key slot and model chain (Rate-limited: 1 call per 10s)
adminRoutes.post("/ai-health", requirePermission("manage_questions"), async (c) => {
  const db = c.env.DB;
  const { limited, retryAfter } = await checkRateLimit(db, "rate:admin:ai-health", 1, 10);
  if (limited) {
    c.header("Retry-After", String(retryAfter));
    return c.json(
      {
        success: false,
        error: `AI health check rate limit exceeded. Please wait ${retryAfter}s before testing again.`,
      },
      429
    );
  }

  const candidateKeys = getGeminiApiKeys(c.env);
  const modelChain = getModelChain(c.env);
  const primaryModel = modelChain[0] || "gemini-3.5-flash-lite";

  if (candidateKeys.length === 0) {
    return c.json({
      success: true,
      data: {
        primaryModel,
        workingModel: null,
        slots: [],
      },
    });
  }

  const slots: Array<{
    slot: string;
    ok: boolean;
    latencyMs: number;
    providerStatus: string;
    message: string;
  }> = [];

  let primaryWorked = false;

  for (let i = 0; i < candidateKeys.length; i++) {
    const key = candidateKeys[i];
    const slotLabel = `Key ${i + 1}`;
    const endpoint = `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(primaryModel)}:generateContent`;
    const start = Date.now();
    let ok = false;
    let latencyMs = 0;
    let providerStatus = "UNKNOWN";
    let message = "";

    try {
      const res = await fetch(endpoint, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "x-goog-api-key": key,
        },
        body: JSON.stringify({
          contents: [{ parts: [{ text: "ping" }] }],
        }),
      });
      latencyMs = Date.now() - start;

      if (res.ok) {
        ok = true;
        providerStatus = "OK";
        message = "Healthy";
        primaryWorked = true;
      } else {
        const errText = await res.text();
        const classified = classifyGeminiError(res.status, errText, { keysToRedact: candidateKeys });
        providerStatus = classified.providerStatus || (res.status === 429 ? "RESOURCE_EXHAUSTED" : res.status === 404 ? "NOT_FOUND" : `HTTP_${res.status}`);
        message = classified.userFacingMessage;
      }
    } catch (e: any) {
      latencyMs = Date.now() - start;
      providerStatus = "NETWORK_ERROR";
      message = "Network connection failed";
    }

    slots.push({
      slot: slotLabel,
      ok,
      latencyMs,
      providerStatus,
      message,
    });
    if (db) {
      await trackGeminiCall(db, ok).catch(() => {});
    }
  }

  let workingModel: string | null = null;
  if (primaryWorked) {
    workingModel = primaryModel;
  } else {
    // If primary model failed across all slots, test fallback models in the chain
    fallbackSearch: for (let m = 1; m < modelChain.length; m++) {
      const testModel = modelChain[m];
      for (const key of candidateKeys) {
        try {
          const endpoint = `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(testModel)}:generateContent`;
          const res = await fetch(endpoint, {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              "x-goog-api-key": key,
            },
            body: JSON.stringify({
              contents: [{ parts: [{ text: "ping" }] }],
            }),
          });
          if (res.ok) {
            workingModel = testModel;
            if (db) {
              await trackGeminiCall(db, true).catch(() => {});
            }
            break fallbackSearch;
          } else if (db) {
            await trackGeminiCall(db, false).catch(() => {});
          }
        } catch {
          if (db) {
            await trackGeminiCall(db, false).catch(() => {});
          }
        }
      }
    }
  }

  return c.json({
    success: true,
    data: {
      primaryModel,
      workingModel,
      slots,
    },
  });
});

// GET /api/admin/api-usage - Truthful server-side API usage & cost monitor metrics
adminRoutes.get("/api-usage", requirePermission("view_analytics_logs"), async (c) => {
  const monthParam = c.req.query("month")?.trim();
  if (monthParam && !/^(\d{4})-(0[1-9]|1[0-2])$/.test(monthParam)) {
    return c.json({ success: false, error: "Query parameter 'month' must be in YYYY-MM format" }, 400);
  }

  try {
    const usage = await getMonthlyApiUsage(c.env.DB, monthParam);
    return c.json({ success: true, data: usage });
  } catch (err: any) {
    return c.json({ success: false, error: err.message || "Failed to retrieve monthly API usage" }, 500);
  }
});

// POST /api/admin/questions/reformat-explanation - Reformat question explanation with Gemini AI (rate-limited, sanitized)
adminRoutes.post("/questions/reformat-explanation", requirePermission("manage_questions"), async (c) => {
  const user = c.get("user");
  const ip = c.req.header("cf-connecting-ip") || c.req.header("x-forwarded-for") || "unknown";

  // Rate limit: 20 calls per minute per user/IP
  const rl = await checkRateLimit(c.env.DB, `reformat:${user.uid || ip}`, 20, 60);
  if (rl.limited) {
    return c.json({ success: false, error: "Too many reformat requests. Please try again in a few moments." }, 429);
  }

  const rawBody = await c.req.json().catch(() => ({}));
  const validation = validateReformatInput(rawBody);
  if (!validation.valid || !validation.input) {
    return c.json({ success: false, error: validation.error || "Invalid reformat input" }, 400);
  }

  try {
    const reformattedExplanation = await reformatExplanationWithAi(c.env, validation.input);
    return c.json({
      success: true,
      data: { reformattedExplanation },
    });
  } catch (err: any) {
    const status = typeof err.statusCode === "number" ? err.statusCode : 502;
    return c.json({
      success: false,
      error: err.userFacingMessage || err.message || "AI reformat service is temporarily unavailable",
      errorCode: err.code || "REFORMAT_FAILED",
    }, status);
  }
});

// GET /api/admin/export/exams - Safe, chunked one-call export of exam content & questions (NO user/student PII)
adminRoutes.get("/export/exams", requirePermission("export_data"), async (c) => {
  const db = c.env.DB;
  const user = c.get("user");

  const offsetParam = c.req.query("offset");
  const limitParam = c.req.query("limit");

  let offset = 0;
  if (offsetParam !== undefined) {
    if (!/^\d+$/.test(offsetParam)) {
      return c.json({ success: false, error: "Query parameter 'offset' must be a non-negative integer" }, 400);
    }
    offset = parseInt(offsetParam, 10);
  }

  let limit = 50;
  if (limitParam !== undefined) {
    if (!/^[1-9]\d*$/.test(limitParam)) {
      return c.json({ success: false, error: "Query parameter 'limit' must be a positive integer between 1 and 100" }, 400);
    }
    limit = parseInt(limitParam, 10);
    if (limit > 100) limit = 100;
  }

  try {
    // 1. Get total exams and questions count
    const totalExamsRow = await db.prepare("SELECT COUNT(*) as count FROM exams").first<{ count: number }>();
    const totalQuestionsRow = await db.prepare("SELECT COUNT(*) as count FROM questions").first<{ count: number }>();
    const totalExams = totalExamsRow?.count || 0;
    const totalQuestions = totalQuestionsRow?.count || 0;

    // 2. Fetch exams for this chunk
    const { results: examRows } = await db
      .prepare("SELECT * FROM exams ORDER BY id ASC LIMIT ? OFFSET ?")
      .bind(limit, offset)
      .all<ExamRow>();

    const exams: ExamRow[] = examRows || [];

    // 3. Fetch questions for these exams
    const questionsByExam: Record<string, QuestionRow[]> = {};
    if (exams.length > 0) {
      const examIds = exams.map((e) => e.id);
      const placeholders = examIds.map(() => "?").join(",");
      const { results: questionRows } = await db
        .prepare(`SELECT * FROM questions WHERE exam_id IN (${placeholders}) ORDER BY exam_id ASC, id ASC`)
        .bind(...examIds)
        .all<QuestionRow>();

      for (const q of questionRows || []) {
        if (!questionsByExam[q.exam_id]) {
          questionsByExam[q.exam_id] = [];
        }
        questionsByExam[q.exam_id].push(q);
      }
    }

    // 4. Map to clean export structure containing NO student PII
    let chunkQuestionCount = 0;
    const mappedExams = exams.map((exam) => {
      const qList = questionsByExam[exam.id] || [];
      chunkQuestionCount += qList.length;

      return {
        id: exam.id,
        examName: exam.exam_name,
        category: exam.category || "Other",
        timeLimitMinutes: exam.time_limit_minutes,
        questionCount: qList.length,
        autoGenerationEnabled: Boolean(exam.auto_generation_enabled),
        autoGenTime: exam.auto_gen_time || "00:00",
        generationPrompt: exam.generation_prompt || "",
        syllabusFileName: exam.syllabus_file_name || "",
        syllabusUrl: exam.syllabus_url || "",
        questions: qList.map((q) => ({
          id: q.id,
          examId: q.exam_id,
          questionText: q.question_text,
          optionA: q.option_a,
          optionB: q.option_b,
          optionC: q.option_c,
          optionD: q.option_d,
          correctAnswer: q.correct_answer,
          explanation: q.explanation || "",
          topic: q.topic || "",
          questionTextHi: q.question_text_hi || "",
          optionAHi: q.option_a_hi || "",
          optionBHi: q.option_b_hi || "",
          optionCHi: q.option_c_hi || "",
          optionDHi: q.option_d_hi || "",
          explanationHi: q.explanation_hi || "",
        })),
      };
    });

    // 5. Audit log recorded server-side
    await recordAdminAction(db, user, {
      actionType: "EXAM_EXPORTED",
      description: `Exported backup of ${mappedExams.length} exams and ${chunkQuestionCount} questions (offset ${offset}, limit ${limit})`,
      targetType: "system",
      targetId: "export_exams",
      metadata: {
        offset,
        limit,
        examCount: mappedExams.length,
        questionCount: chunkQuestionCount,
        totalExams,
        totalQuestions,
      },
    });

    const hasMore = offset + mappedExams.length < totalExams;

    return c.json({
      success: true,
      data: {
        exams: mappedExams,
        pagination: {
          offset,
          limit,
          totalExams,
          totalQuestions,
          hasMore,
        },
      },
    });
  } catch (err: any) {
    return c.json({ success: false, error: err.message || "Failed to assemble exam export" }, 500);
  }
});
