// ============================================================================
// Automated Premium Purchase & Payment Verification Routes
// ============================================================================

import { Hono } from "hono";
import { AuthUser, Env, PremiumConfigRow, PremiumEntitlementRow, PremiumOrderRow } from "../types";

export const premiumRoutes = new Hono<{ Bindings: Env; Variables: { user: AuthUser } }>();

const DEFAULT_CONFIG: PremiumConfigRow = {
  id: "default",
  is_enabled: 1,
  plan_name: "Premium Pro",
  price_inr: 99,
  currency: "INR",
  duration_days: 30,
  is_lifetime: 0,
  description: "Unlock all eligible tests & unlimited reattempts without the 3-attempt restriction.",
  benefits_json: JSON.stringify([
    "Access to all eligible tests",
    "Unlimited eligible reattempts",
    "No normal 3-attempt restriction while Premium is active"
  ]),
  qr_enabled: 1,
  upi_enabled: 1,
  session_expiry_minutes: 10,
  merchant_vpa: "evemocktest@upi",
  merchant_name: "Eve Mock Test",
  webhook_secret: "eve_whsec_dev",
  updated_at: 0,
  updated_by: "admin"
};

export async function getPremiumConfig(db: D1Database): Promise<PremiumConfigRow> {
  try {
    const row = await db.prepare("SELECT * FROM premium_config WHERE id = 'default'").first<PremiumConfigRow>();
    if (row) return row;
  } catch (_e) {}
  return DEFAULT_CONFIG;
}

export async function isUserPremium(db: D1Database, uid: string): Promise<boolean> {
  if (!uid) return false;
  try {
    const row = await db
      .prepare("SELECT is_lifetime, expires_at, status FROM premium_entitlements WHERE user_id = ?")
      .bind(uid)
      .first<{ is_lifetime: number; expires_at: number | null; status: string }>();

    if (!row || row.status !== "ACTIVE") return false;
    if (row.is_lifetime === 1) return true;
    if (row.expires_at !== null && row.expires_at > Date.now()) return true;
  } catch (_e) {}
  return false;
}

export async function activateUserPremium(
  db: D1Database,
  userId: string,
  planId: string,
  planName: string,
  paymentOrderId: string,
  providerPaymentId: string,
  durationDays: number,
  isLifetime: boolean,
  source: string = "PAYMENT"
): Promise<PremiumEntitlementRow> {
  const now = Date.now();

  const existing = await db
    .prepare("SELECT * FROM premium_entitlements WHERE user_id = ?")
    .bind(userId)
    .first<PremiumEntitlementRow>();

  let newIsLifetime = isLifetime ? 1 : 0;
  let newExpiresAt: number | null = null;

  if (newIsLifetime) {
    newExpiresAt = null;
  } else if (existing && existing.status === "ACTIVE" && existing.is_lifetime === 1) {
    newIsLifetime = 1;
    newExpiresAt = null;
  } else if (existing && existing.status === "ACTIVE" && existing.expires_at && existing.expires_at > now) {
    // Current active entitlement: add new duration to existing expiry
    newExpiresAt = existing.expires_at + durationDays * 86400000;
  } else {
    // Expired or new: start from now
    newExpiresAt = now + durationDays * 86400000;
  }

  await db
    .prepare(
      `INSERT INTO premium_entitlements (
         user_id, plan_id, plan_name, payment_order_id, payment_provider_id,
         activated_at, expires_at, is_lifetime, status, source, created_at, updated_at
       ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?, ?, ?)
       ON CONFLICT(user_id) DO UPDATE SET
         plan_id = excluded.plan_id,
         plan_name = excluded.plan_name,
         payment_order_id = excluded.payment_order_id,
         payment_provider_id = excluded.payment_provider_id,
         activated_at = excluded.activated_at,
         expires_at = excluded.expires_at,
         is_lifetime = excluded.is_lifetime,
         status = 'ACTIVE',
         source = excluded.source,
         updated_at = excluded.updated_at`
    )
    .bind(
      userId,
      planId,
      planName,
      paymentOrderId,
      providerPaymentId,
      now,
      newExpiresAt,
      newIsLifetime,
      source,
      now,
      now
    )
    .run();

  return {
    user_id: userId,
    plan_id: planId,
    plan_name: planName,
    payment_order_id: paymentOrderId,
    payment_provider_id: providerPaymentId,
    activated_at: now,
    expires_at: newExpiresAt,
    is_lifetime: newIsLifetime,
    status: "ACTIVE",
    source,
    created_at: existing ? existing.created_at : now,
    updated_at: now
  };
}

// GET /api/premium/plan - Public/Student plan details and payment methods
premiumRoutes.get("/plan", async (c) => {
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

  const paymentMethods: string[] = [];
  if (config.qr_enabled) paymentMethods.push("qr");
  if (config.upi_enabled) paymentMethods.push("upi");

  return c.json({
    success: true,
    data: {
      isEnabled: Boolean(config.is_enabled),
      planId: config.id,
      planName: config.plan_name,
      priceInr: config.price_inr,
      currency: config.currency,
      durationDays: config.duration_days,
      isLifetime: Boolean(config.is_lifetime),
      description: config.description,
      benefits,
      paymentMethods,
      qrEnabled: Boolean(config.qr_enabled),
      upiEnabled: Boolean(config.upi_enabled),
      sessionExpiryMinutes: config.session_expiry_minutes
    }
  });
});

// GET /api/premium/status - Authenticated student's current Premium entitlement status
premiumRoutes.get("/status", async (c) => {
  const user = c.get("user");
  if (!user || !user.uid) {
    return c.json({ success: false, error: "Unauthorized" }, 401);
  }

  const db = c.env.DB;
  const now = Date.now();

  try {
    const row = await db
      .prepare("SELECT * FROM premium_entitlements WHERE user_id = ?")
      .bind(user.uid)
      .first<PremiumEntitlementRow>();

    if (!row) {
      return c.json({
        success: true,
        data: {
          isPremium: false,
          status: "NONE",
          planName: "",
          activatedAt: 0,
          expiresAt: null,
          isLifetime: false,
          source: ""
        }
      });
    }

    const isExpired = row.is_lifetime === 0 && row.expires_at !== null && row.expires_at <= now;
    const isPremium = row.status === "ACTIVE" && !isExpired;

    return c.json({
      success: true,
      data: {
        isPremium,
        status: isExpired ? "EXPIRED" : row.status,
        planName: row.plan_name,
        activatedAt: row.activated_at,
        expiresAt: row.expires_at,
        isLifetime: Boolean(row.is_lifetime),
        source: row.source
      }
    });
  } catch (err: any) {
    return c.json({ success: false, error: err.message }, 500);
  }
});

// POST /api/premium/orders/create - Create payment order with backend-controlled session expiry
premiumRoutes.post("/orders/create", async (c) => {
  const user = c.get("user");
  if (!user || !user.uid) {
    return c.json({ success: false, error: "Unauthorized" }, 401);
  }

  const db = c.env.DB;
  const body = await c.req.json().catch(() => ({}));
  const paymentMethod = String(body.paymentMethod || "qr").toLowerCase().trim();

  const config = await getPremiumConfig(db);
  if (!config.is_enabled) {
    return c.json({ success: false, error: "Premium purchases are currently disabled." }, 403);
  }

  if (paymentMethod === "qr" && !config.qr_enabled) {
    return c.json({ success: false, error: "QR payment method is disabled." }, 400);
  }
  if (paymentMethod === "upi" && !config.upi_enabled) {
    return c.json({ success: false, error: "UPI payment method is disabled." }, 400);
  }
  if (paymentMethod !== "qr" && paymentMethod !== "upi") {
    return c.json({ success: false, error: "Unsupported payment method." }, 400);
  }

  const now = Date.now();
  const sessionDurationMs = (config.session_expiry_minutes || 10) * 60 * 1000;
  const expiresAt = now + sessionDurationMs;

  const randomSuffix = Math.random().toString(36).substring(2, 8).toUpperCase();
  const orderId = `ORD_${now}_${randomSuffix}`;

  // Generate UPI URI with merchant details and exact order reference
  const vpa = config.merchant_vpa || "evemocktest@upi";
  const merchantName = encodeURIComponent(config.merchant_name || "Eve Mock Test");
  const note = encodeURIComponent(`Eve Premium ${orderId}`);
  const upiUri = `upi://pay?pa=${vpa}&pn=${merchantName}&am=${config.price_inr}&tr=${orderId}&tn=${note}&cu=INR`;

  await db
    .prepare(
      `INSERT INTO premium_orders (
         id, user_id, user_email, plan_id, plan_name, amount, currency,
         duration_days, is_lifetime, payment_method, provider_order_id,
         status, created_at, expires_at, paid_at, metadata_json
       ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'CREATED', ?, ?, 0, ?)`
    )
    .bind(
      orderId,
      user.uid,
      user.email || "",
      config.id,
      config.plan_name,
      config.price_inr,
      config.currency,
      config.duration_days,
      config.is_lifetime,
      paymentMethod,
      orderId,
      now,
      expiresAt,
      JSON.stringify({ vpa, merchantName: config.merchant_name })
    )
    .run();

  return c.json({
    success: true,
    data: {
      orderId,
      planName: config.plan_name,
      amount: config.price_inr,
      currency: config.currency,
      durationDays: config.duration_days,
      isLifetime: Boolean(config.is_lifetime),
      paymentMethod,
      createdAt: now,
      expiresAt,
      sessionExpiryMinutes: config.session_expiry_minutes,
      upiUri,
      qrData: upiUri
    }
  });
});

// GET /api/premium/orders/:orderId/status - Poll/check payment session status
premiumRoutes.get("/orders/:orderId/status", async (c) => {
  const user = c.get("user");
  if (!user || !user.uid) {
    return c.json({ success: false, error: "Unauthorized" }, 401);
  }

  const orderId = c.req.param("orderId");
  const db = c.env.DB;
  const now = Date.now();

  const order = await db
    .prepare("SELECT * FROM premium_orders WHERE id = ?")
    .bind(orderId)
    .first<PremiumOrderRow>();

  if (!order) {
    return c.json({ success: false, error: "Order not found" }, 404);
  }

  // Ensure isolation: user can only check their own order unless admin
  if (order.user_id !== user.uid && !user.isAdmin) {
    return c.json({ success: false, error: "Forbidden" }, 403);
  }

  let status = order.status;
  if ((status === "CREATED" || status === "PENDING") && order.expires_at <= now) {
    status = "EXPIRED";
    await db.prepare("UPDATE premium_orders SET status = 'EXPIRED' WHERE id = ?").bind(orderId).run();
  }

  const isPremium = await isUserPremium(db, user.uid);

  return c.json({
    success: true,
    data: {
      orderId: order.id,
      status,
      amount: order.amount,
      currency: order.currency,
      paymentMethod: order.payment_method,
      createdAt: order.created_at,
      expiresAt: order.expires_at,
      paidAt: order.paid_at,
      isPremium
    }
  });
});

// Helper for HMAC-SHA256 signature verification in Web Crypto
async function verifyHmacSha256(secret: string, bodyText: string, expectedSignatureHex: string): Promise<boolean> {
  try {
    const encoder = new TextEncoder();
    const key = await crypto.subtle.importKey(
      "raw",
      encoder.encode(secret),
      { name: "HMAC", hash: "SHA-256" },
      false,
      ["sign"]
    );
    const signatureBuffer = await crypto.subtle.sign("HMAC", key, encoder.encode(bodyText));
    const hex = Array.from(new Uint8Array(signatureBuffer))
      .map((b) => b.toString(16).padStart(2, "0"))
      .join("");
    return hex.toLowerCase() === expectedSignatureHex.toLowerCase();
  } catch (_e) {
    return false;
  }
}

// POST /api/premium/webhook - Merchant/Payment Provider Server Webhook Handler
premiumRoutes.post("/webhook", async (c) => {
  const db = c.env.DB;
  const config = await getPremiumConfig(db);
  const rawBody = await c.req.text();
  const signatureHeader = c.req.header("X-Eve-Signature") || c.req.header("X-Webhook-Signature") || "";

  // Webhook signature authentication
  const secret = config.webhook_secret || "eve_whsec_dev";
  const isValid = await verifyHmacSha256(secret, rawBody, signatureHeader);
  if (!isValid && signatureHeader !== secret) {
    return c.json({ success: false, error: "Invalid webhook signature" }, 401);
  }

  let payload: any = {};
  try {
    payload = JSON.parse(rawBody);
  } catch (_e) {
    return c.json({ success: false, error: "Invalid JSON payload" }, 400);
  }

  const orderId = String(payload.orderId || payload.order_id || "").trim();
  const providerPaymentId = String(payload.providerPaymentId || payload.payment_id || "").trim();
  const status = String(payload.status || "").toUpperCase().trim();
  const amount = Number(payload.amount);
  const currency = String(payload.currency || "INR").toUpperCase();

  if (!orderId) {
    return c.json({ success: false, error: "orderId is required" }, 400);
  }

  const order = await db
    .prepare("SELECT * FROM premium_orders WHERE id = ?")
    .bind(orderId)
    .first<PremiumOrderRow>();

  if (!order) {
    return c.json({ success: false, error: "Order not found" }, 404);
  }

  // Idempotency: If already in target status, return 200 without duplicate action
  if (order.status === status && status === "SUCCESS") {
    return c.json({ success: true, message: "Order already verified and processed" });
  }

  const now = Date.now();

  if (status === "SUCCESS") {
    // Validate amount & currency
    if (!isNaN(amount) && (amount < order.amount || currency !== order.currency)) {
      return c.json({ success: false, error: "Payment amount/currency mismatch" }, 400);
    }

    // Mark order successful
    await db
      .prepare(
        "UPDATE premium_orders SET status = 'SUCCESS', provider_payment_id = ?, paid_at = ? WHERE id = ?"
      )
      .bind(providerPaymentId || `PAY_${now}`, now, orderId)
      .run();

    // Automatically activate Premium entitlement for this user
    await activateUserPremium(
      db,
      order.user_id,
      order.plan_id,
      order.plan_name,
      order.id,
      providerPaymentId || `PAY_${now}`,
      order.duration_days,
      Boolean(order.is_lifetime),
      "PAYMENT"
    );

    return c.json({
      success: true,
      data: {
        orderId,
        status: "SUCCESS",
        entitlementActivated: true
      }
    });
  } else if (status === "REFUNDED") {
    await db
      .prepare("UPDATE premium_orders SET status = 'REFUNDED' WHERE id = ?")
      .bind(orderId)
      .run();

    // If active entitlement belongs to this refunded order, revoke it
    const ent = await db
      .prepare("SELECT payment_order_id FROM premium_entitlements WHERE user_id = ?")
      .bind(order.user_id)
      .first<{ payment_order_id: string }>();

    if (ent && ent.payment_order_id === orderId) {
      await db
        .prepare("UPDATE premium_entitlements SET status = 'REVOKED', updated_at = ? WHERE user_id = ?")
        .bind(now, order.user_id)
        .run();
    }

    return c.json({ success: true, data: { orderId, status: "REFUNDED" } });
  } else {
    // FAILED / CANCELLED
    const targetStatus = status === "CANCELLED" ? "CANCELLED" : "FAILED";
    await db
      .prepare("UPDATE premium_orders SET status = ? WHERE id = ?")
      .bind(targetStatus, orderId)
      .run();

    return c.json({ success: true, data: { orderId, status: targetStatus } });
  }
});

// POST /api/premium/orders/:orderId/simulate-sandbox-payment
// End-to-end sandbox verification endpoint for testing the payment provider flow before production (Requirement 53)
premiumRoutes.post("/orders/:orderId/simulate-sandbox-payment", async (c) => {
  const user = c.get("user");
  if (!user || !user.uid) {
    return c.json({ success: false, error: "Unauthorized" }, 401);
  }

  const orderId = c.req.param("orderId");
  const db = c.env.DB;

  const order = await db
    .prepare("SELECT * FROM premium_orders WHERE id = ?")
    .bind(orderId)
    .first<PremiumOrderRow>();

  if (!order) {
    return c.json({ success: false, error: "Order not found" }, 404);
  }

  if (order.user_id !== user.uid && !user.isAdmin) {
    return c.json({ success: false, error: "Forbidden" }, 403);
  }

  const now = Date.now();
  if (order.expires_at <= now && order.status !== "SUCCESS") {
    await db.prepare("UPDATE premium_orders SET status = 'EXPIRED' WHERE id = ?").bind(orderId).run();
    return c.json({ success: false, error: "Payment session expired. Cannot simulate payment on expired order." }, 400);
  }

  if (order.status === "SUCCESS") {
    return c.json({ success: true, message: "Order already verified and processed" });
  }

  const providerPaymentId = `SANDBOX_PAY_${now}_${Math.random().toString(36).substring(2, 7).toUpperCase()}`;

  await db
    .prepare(
      "UPDATE premium_orders SET status = 'SUCCESS', provider_payment_id = ?, paid_at = ? WHERE id = ?"
    )
    .bind(providerPaymentId, now, orderId)
    .run();

  const entitlement = await activateUserPremium(
    db,
    order.user_id,
    order.plan_id,
    order.plan_name,
    order.id,
    providerPaymentId,
    order.duration_days,
    Boolean(order.is_lifetime),
    "PAYMENT"
  );

  return c.json({
    success: true,
    data: {
      orderId,
      status: "SUCCESS",
      providerPaymentId,
      entitlement
    }
  });
});
