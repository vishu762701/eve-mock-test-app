// ============================================================================
// Broadcast Notifications & Push Records
// ============================================================================

import { Hono } from "hono";
import { requireAdmin } from "../middleware/authMiddleware";
import { AuthUser, Env, NotificationRow } from "../types";

export const broadcastRoutes = new Hono<{ Bindings: Env; Variables: { user: AuthUser } }>();

// GET /api/broadcasts - List broadcast notifications
broadcastRoutes.get("/", async (c) => {
  const db = c.env.DB;
  const limit = Math.min(100, Math.max(1, parseInt(c.req.query("limit") || "100", 10)));

  const { results } = await db
    .prepare("SELECT * FROM notifications ORDER BY sent_at DESC LIMIT ?")
    .bind(limit)
    .all<NotificationRow>();

  const list = (results || []).map((r) => ({
    id: r.id,
    title: r.title,
    message: r.message,
    sentAt: r.sent_at,
    sentBy: r.sent_by,
    type: r.type,
  }));

  return c.json({ success: true, data: list });
});

// POST /api/broadcasts - Create broadcast (Admin)
broadcastRoutes.post("/", requireAdmin, async (c) => {
  const user = c.get("user");
  const body = await c.req.json().catch(() => ({}));
  const title = String(body.title || "").trim();
  const message = String(body.message || body.body || "").trim();
  const type = String(body.type || "general").trim();
  const db = c.env.DB;

  if (!title && !message) {
    return c.json({ success: false, error: "Title or message required" }, 400);
  }

  const id = crypto.randomUUID();
  const now = Date.now();

  await db
    .prepare(
      "INSERT INTO notifications (id, title, message, sent_at, sent_by, type) VALUES (?, ?, ?, ?, ?, ?)"
    )
    .bind(id, title, message, now, user.email, type)
    .run();

  return c.json({ success: true, data: { id, title, message, sentAt: now } }, 201);
});

// DELETE /api/broadcasts/:id - Delete single broadcast (Admin)
broadcastRoutes.delete("/:id", requireAdmin, async (c) => {
  const id = c.req.param("id");
  const db = c.env.DB;

  await db.prepare("DELETE FROM notifications WHERE id = ?").bind(id).run();
  return c.json({ success: true });
});

// POST /api/broadcasts/bulk-delete - Multi-select bulk delete (Admin)
broadcastRoutes.post("/bulk-delete", requireAdmin, async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const ids: string[] = Array.isArray(body.ids) ? body.ids : [];

  if (ids.length === 0) {
    return c.json({ success: false, error: "Empty IDs array" }, 400);
  }

  const db = c.env.DB;
  const statements = ids.map((id) => db.prepare("DELETE FROM notifications WHERE id = ?").bind(id));

  // Chunk in batches of 50
  for (let i = 0; i < statements.length; i += 50) {
    const chunk = statements.slice(i, i + 50);
    await db.batch(chunk);
  }

  return c.json({ success: true, count: ids.length });
});
