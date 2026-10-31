// ============================================================================
// Broadcast Notifications & Push Records
// ============================================================================

import { Hono } from "hono";
import { requireAdmin, requirePermission } from "../middleware/authMiddleware";
import { AuthUser, EmojiPlacementPublic, Env, NotificationRow } from "../types";
import { prepareAdminAuditLog } from "../util/auditLog";
import { getAdminPermissions } from "../auth";
import { getActiveEmojiPlacements } from "./emoji";

export const broadcastRoutes = new Hono<{ Bindings: Env; Variables: { user: AuthUser } }>();

// GET /api/broadcasts - List broadcast notifications
broadcastRoutes.get("/", async (c) => {
  const db = c.env.DB;
  const rawLimit = c.req.query("limit");
  if (rawLimit !== undefined && !/^[1-9][0-9]{0,5}$/.test(rawLimit)) {
    return c.json({ success: false, error: "limit must be a positive integer" }, 400);
  }
  const limit = Math.min(100, rawLimit === undefined ? 100 : Number(rawLimit));

  const { results } = await db
    .prepare("SELECT * FROM notifications ORDER BY sent_at DESC LIMIT ?")
    .bind(limit)
    .all<NotificationRow>();

  // A category-limited notice has no verified server-side recipient membership
  // mapping. Fail closed for students rather than showing another audience's post.
  const user = c.get("user");
  const canSendBroadcasts = user?.isAdmin === true &&
    (await getAdminPermissions(db, user.email))?.rights.includes("send_broadcasts") === true;
  const eligible = canSendBroadcasts ? (results || []) : (results || []).filter(
    (r: any) => !r.target_category || r.target_category === "All Users"
  );

  let broadcastPlacements = new Map<string, EmojiPlacementPublic>();
  try {
    const placements = await getActiveEmojiPlacements(db, c.env);
    broadcastPlacements = placements.broadcastPlacements;
  } catch {}

  const list = eligible.map((r: any) => ({
    id: r.id,
    title: r.title,
    message: r.message,
    sentAt: r.sent_at,
    ...(canSendBroadcasts ? { sentBy: r.sent_by } : {}),
    type: r.type,
    targetCategory: r.target_category || "All Users",
    ...(broadcastPlacements.has(r.id) ? { emoji: broadcastPlacements.get(r.id) } : {}),
  }));

  return c.json({ success: true, data: list });
});

// POST /api/broadcasts - Create broadcast (Admin)
broadcastRoutes.post("/", requirePermission("send_broadcasts"), async (c) => {
  const user = c.get("user");
  const body = await c.req.json().catch(() => ({}));
  const title = typeof body.title === "string" ? body.title.trim() : "";
  const rawMessage = body.message ?? body.body;
  const message = typeof rawMessage === "string" ? rawMessage.trim() : "";
  const rawType = body.type ?? "general";
  const rawTarget = body.targetCategory ?? body.target_category ?? "All Users";
  if (!title || !message || title.length > 120 || message.length > 4000 ||
      typeof rawType !== "string" || !rawType.trim() || rawType.length > 40 ||
      typeof rawTarget !== "string" || !rawTarget.trim() || rawTarget.length > 100) {
    return c.json({ success: false, error: "Invalid broadcast title, message, type or audience" }, 400);
  }
  const type = rawType.trim();
  const targetCategory = rawTarget.trim();
  if (targetCategory !== "All Users") {
    return c.json({ success: false, error: "Category-targeted delivery is unavailable. Only All Users is supported." }, 422);
  }
  const db = c.env.DB;

  const id = crypto.randomUUID();
  const now = Date.now();

  try {
    const insertStmt = db
      .prepare(
        "INSERT INTO notifications (id, title, message, sent_at, sent_by, type, target_category) VALUES (?, ?, ?, ?, ?, ?, ?)"
      )
      .bind(id, title, message, now, user.email, type, targetCategory);

    const { stmt: auditStmt } = prepareAdminAuditLog(db, user, {
      actionType: "BROADCAST_SENT",
      description: `Sent broadcast '${title}'`,
      targetType: "broadcast",
      targetId: id,
      metadata: { title, type, targetCategory },
    });

    await db.batch([insertStmt, auditStmt]);
  } catch {
    try {
      await db.prepare("ALTER TABLE notifications ADD COLUMN target_category TEXT DEFAULT 'All Users'").run();
      const insertStmt = db
        .prepare(
          "INSERT INTO notifications (id, title, message, sent_at, sent_by, type, target_category) VALUES (?, ?, ?, ?, ?, ?, ?)"
        )
        .bind(id, title, message, now, user.email, type, targetCategory);

      const { stmt: auditStmt } = prepareAdminAuditLog(db, user, {
        actionType: "BROADCAST_SENT",
        description: `Sent broadcast '${title}'`,
        targetType: "broadcast",
        targetId: id,
        metadata: { title, type, targetCategory },
      });

      await db.batch([insertStmt, auditStmt]);
    } catch {
      const insertStmt = db
        .prepare(
          "INSERT INTO notifications (id, title, message, sent_at, sent_by, type) VALUES (?, ?, ?, ?, ?, ?)"
        )
        .bind(id, title, message, now, user.email, type);

      const { stmt: auditStmt } = prepareAdminAuditLog(db, user, {
        actionType: "BROADCAST_SENT",
        description: `Sent broadcast '${title}'`,
        targetType: "broadcast",
        targetId: id,
        metadata: { title, type },
      });

      await db.batch([insertStmt, auditStmt]);
    }
  }

  return c.json({ success: true, data: { id, title, message, sentAt: now, targetCategory } }, 201);
});

// DELETE /api/broadcasts/:id - Delete single broadcast (Admin)
broadcastRoutes.delete("/:id", requirePermission("send_broadcasts"), async (c) => {
  const user = c.get("user");
  const id = c.req.param("id");
  const db = c.env.DB;

  if (!id || id.length > 128) {
    return c.json({ success: false, error: "Invalid broadcast ID" }, 400);
  }
  // Insert an audit entry only if the row exists, then delete it in the same
  // D1 transaction. Stale IDs produce neither a deletion nor a fake log.
  const { stmt: auditStmt } = prepareAdminAuditLog(db, user, {
    actionType: "BROADCAST_DELETED",
    description: `Deleted broadcast '${id}'`,
    targetType: "broadcast",
    targetId: id,
    condition: { sql: "EXISTS(SELECT 1 FROM notifications WHERE id = ?)", bindings: [id] },
  });
  const deleteStmt = db.prepare("DELETE FROM notifications WHERE id = ?").bind(id);
  const results = await db.batch([auditStmt, deleteStmt]);
  if ((results[1]?.meta?.changes ?? 0) < 1) {
    return c.json({ success: false, error: "Broadcast not found or already deleted" }, 404);
  }
  return c.json({ success: true });
});

// POST /api/broadcasts/bulk-delete - Multi-select bulk delete (Admin)
broadcastRoutes.post("/bulk-delete", requirePermission("send_broadcasts"), async (c) => {
  const user = c.get("user");
  const body = await c.req.json().catch(() => ({}));
  const ids: unknown[] = Array.isArray(body.ids) ? body.ids : [];
  if (ids.length < 1 || ids.length > 100 ||
      ids.some(id => typeof id !== "string" || id.length < 1 || id.length > 128) ||
      new Set(ids).size !== ids.length) {
    return c.json({ success: false, error: "Provide 1 to 100 distinct valid broadcast IDs" }, 400);
  }

  const db = c.env.DB;
  const statements = ids.map((id) => db.prepare("DELETE FROM notifications WHERE id = ?").bind(id));

  // A single D1 batch makes the entire request atomic: no partially deleted
  // chunks, and no successful destructive mutation without a Recent Actions log.
  // The audit describes requested IDs; only the response reports actual changes.
  const placeholders = ids.map(() => "?").join(",");
  const { stmt: auditStmt } = prepareAdminAuditLog(db, user, {
    actionType: "BROADCAST_BULK_DELETED",
    description: `Bulk broadcast deletion requested for ${ids.length} IDs`,
    targetType: "broadcast",
    targetId: "batch",
    metadata: { requested: ids.length },
    condition: {
      sql: `EXISTS(SELECT 1 FROM notifications WHERE id IN (${placeholders}))`,
      bindings: ids as string[],
    },
  });
  const results = await db.batch([auditStmt, ...statements]);
  const deletedCount = results.slice(1).reduce(
    (sum, result) => sum + (result.meta?.changes ?? 0), 0
  );

  if (deletedCount < 1) {
    return c.json({ success: false, error: "No matching broadcasts remain" }, 404);
  }
  return c.json({ success: true, count: deletedCount, data: { count: deletedCount, requested: ids.length } });
});
