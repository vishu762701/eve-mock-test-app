// ============================================================================
// Admin Dashboard & Analytics Routes
// ============================================================================

import { Hono } from "hono";
import { requireAdmin } from "../middleware/authMiddleware";
import { AuthUser, Env, PremiumConfigRow, PremiumEntitlementRow, PremiumOrderRow } from "../types";
import { activateUserPremium, getPremiumConfig } from "./premium";
import { getGeminiApiKeys, getModelChain, classifyGeminiError } from "../ai/generator";
import { checkRateLimit } from "../middleware/rateLimiter";

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

  let query = "SELECT id, eve_id, email, display_name, dob, category, created_at, last_active, COALESCE(disabled, 0) as disabled FROM users";
  const params: any[] = [];
  if (q) {
    query += " WHERE LOWER(email) LIKE ? OR LOWER(display_name) LIKE ? OR LOWER(COALESCE(eve_id, '')) LIKE ?";
    params.push(`%${q}%`, `%${q}%`, `%${q}%`);
  }
  query += " ORDER BY last_active DESC, created_at DESC LIMIT 200";

  try {
    const { results } = await db.prepare(query).bind(...params).all<any>();
    const users = (results || []).map((u) => ({
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
    return c.json({ success: true, data: users });
  } catch (_: any) {
    // If disabled column does not exist yet
    try {
      const fallbackQuery = q
        ? "SELECT id, eve_id, email, display_name, dob, category, created_at, last_active FROM users WHERE LOWER(email) LIKE ? OR LOWER(display_name) LIKE ? OR LOWER(COALESCE(eve_id, '')) LIKE ? ORDER BY last_active DESC, created_at DESC LIMIT 200"
        : "SELECT id, eve_id, email, display_name, dob, category, created_at, last_active FROM users ORDER BY last_active DESC, created_at DESC LIMIT 200";
      const { results } = await db.prepare(fallbackQuery).bind(...params).all<any>();
      const users = (results || []).map((u) => ({
        id: u.id,
        eveId: u.eve_id || null,
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

  const minVersion = body.minimum_supported_version_code !== undefined
    ? Number(body.minimum_supported_version_code)
    : (prevConfig.minimum_supported_version_code !== undefined ? Number(prevConfig.minimum_supported_version_code) : 1);
  const maintenanceMode = body.maintenance_mode !== undefined
    ? Boolean(body.maintenance_mode)
    : (prevConfig.maintenance_mode !== undefined ? Boolean(prevConfig.maintenance_mode) : false);
  const maintenanceMessage = body.maintenance_message !== undefined
    ? String(body.maintenance_message)
    : (prevConfig.maintenance_message !== undefined
        ? String(prevConfig.maintenance_message)
        : "Eve Mock Test is currently undergoing scheduled maintenance. Please check back shortly.");

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

// ============================================================================
// Admin Premium Management Routes
// ============================================================================

// GET /api/admin/premium/config - Get current premium plan & payment configuration
adminRoutes.get("/premium/config", async (c) => {
  const db = c.env.DB;
  const config = await getPremiumConfig(db);
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
      ...config,
      isEnabled: Boolean(config.is_enabled),
      isLifetime: Boolean(config.is_lifetime),
      qrEnabled: Boolean(config.qr_enabled),
      upiEnabled: Boolean(config.upi_enabled),
      benefits
    }
  });
});

// PUT /api/admin/premium/config - Update premium plan & payment configuration
adminRoutes.put("/premium/config", async (c) => {
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
  const webhookSecret = String(body.webhookSecret || "eve_whsec_dev").trim();

  await db
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
    )
    .run();

  return c.json({ success: true });
});

// GET /api/admin/premium/transactions - List all payment orders / transactions
adminRoutes.get("/premium/transactions", async (c) => {
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
adminRoutes.get("/premium/users", async (c) => {
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
adminRoutes.post("/premium/users/grant", async (c) => {
  const db = c.env.DB;
  const user = c.get("user");
  const body = await c.req.json().catch(() => ({}));
  const userIdOrEmail = String(body.userIdOrEmail || body.userId || "").trim();
  const durationDays = parseInt(String(body.durationDays || "30"), 10) || 30;
  const isLifetime = Boolean(body.isLifetime);
  const planName = String(body.planName || "Admin Premium Access").trim();

  if (!userIdOrEmail) {
    return c.json({ success: false, error: "userId or email is required" }, 400);
  }

  // Resolve user UID if email was passed
  let targetUid = userIdOrEmail;
  if (userIdOrEmail.includes("@")) {
    const userRow = await db.prepare("SELECT id FROM users WHERE LOWER(email) = LOWER(?)").bind(userIdOrEmail).first<{ id: string }>();
    if (!userRow) {
      return c.json({ success: false, error: `User with email ${userIdOrEmail} not found` }, 404);
    }
    targetUid = userRow.id;
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

  return c.json({ success: true, data: entitlement });
});

// POST /api/admin/premium/users/extend - Extend existing Premium expiry
adminRoutes.post("/premium/users/extend", async (c) => {
  const db = c.env.DB;
  const body = await c.req.json().catch(() => ({}));
  const userId = String(body.userId || "").trim();
  const additionalDays = parseInt(String(body.additionalDays || "30"), 10) || 30;

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

  if (existing.is_lifetime === 1) {
    return c.json({ success: true, message: "User already has Lifetime Premium" });
  }

  const now = Date.now();
  const baseTime = (existing.expires_at && existing.expires_at > now) ? existing.expires_at : now;
  const newExpiresAt = baseTime + additionalDays * 86400000;

  await db
    .prepare("UPDATE premium_entitlements SET expires_at = ?, status = 'ACTIVE', updated_at = ? WHERE user_id = ?")
    .bind(newExpiresAt, now, userId)
    .run();

  return c.json({ success: true, data: { userId, expiresAt: newExpiresAt } });
});

// POST /api/admin/premium/users/revoke - Revoke Premium
adminRoutes.post("/premium/users/revoke", async (c) => {
  const db = c.env.DB;
  const body = await c.req.json().catch(() => ({}));
  const userId = String(body.userId || "").trim();

  if (!userId) {
    return c.json({ success: false, error: "userId is required" }, 400);
  }

  const now = Date.now();
  await db
    .prepare("UPDATE premium_entitlements SET status = 'REVOKED', updated_at = ? WHERE user_id = ?")
    .bind(now, userId)
    .run();

  return c.json({ success: true, data: { userId, status: "REVOKED" } });
});

// POST /api/admin/ai-health - Test AI connection per key slot and model chain (Rate-limited: 1 call per 10s)
adminRoutes.post("/ai-health", async (c) => {
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
            break fallbackSearch;
          }
        } catch {}
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
