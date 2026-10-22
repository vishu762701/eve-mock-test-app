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
  const isAdmin = c.get("user")?.isAdmin === true;
  const eligible = isAdmin ? (results || []) : (results || []).filter(
    (r: any) => !r.target_category || r.target_category === "All Users"
  );
  const list = eligible.map((r: any) => ({
    id: r.id,
    title: r.title,
    message: r.message,
    sentAt: r.sent_at,
    ...(isAdmin ? { sentBy: r.sent_by } : {}),
    type: r.type,
    targetCategory: r.target_category || "All Users",
  }));

  return c.json({ success: true, data: list });
});

// POST /api/broadcasts - Create broadcast (Admin)
broadcastRoutes.post("/", requireAdmin, async (c) => {
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
    await db
      .prepare(
        "INSERT INTO notifications (id, title, message, sent_at, sent_by, type, target_category) VALUES (?, ?, ?, ?, ?, ?, ?)"
      )
      .bind(id, title, message, now, user.email, type, targetCategory)
      .run();
  } catch {
    try {
      await db.prepare("ALTER TABLE notifications ADD COLUMN target_category TEXT DEFAULT 'All Users'").run();
      await db
        .prepare(
          "INSERT INTO notifications (id, title, message, sent_at, sent_by, type, target_category) VALUES (?, ?, ?, ?, ?, ?, ?)"
        )
        .bind(id, title, message, now, user.email, type, targetCategory)
        .run();
    } catch {
      await db
        .prepare(
          "INSERT INTO notifications (id, title, message, sent_at, sent_by, type) VALUES (?, ?, ?, ?, ?, ?)"
        )
        .bind(id, title, message, now, user.email, type)
        .run();
    }
  }

  return c.json({ success: true, data: { id, title, message, sentAt: now, targetCategory } }, 201);
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
  const ids: unknown[] = Array.isArray(body.ids) ? body.ids : [];
  if (ids.length < 1 || ids.length > 100 ||
      ids.some(id => typeof id !== "string" || id.length < 1 || id.length > 128) ||
      new Set(ids).size !== ids.length) {
    return c.json({ success: false, error: "Provide 1 to 100 distinct valid broadcast IDs" }, 400);
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
