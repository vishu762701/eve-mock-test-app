// ============================================================================
// Pinned Exams Routes
// ============================================================================

import { Hono } from "hono";
import { AuthUser, Env } from "../types";

export const pinRoutes = new Hono<{ Bindings: Env; Variables: { user: AuthUser } }>();

// GET /api/pins - Get pinned exam IDs for current user
pinRoutes.get("/", async (c) => {
  const user = c.get("user");
  const db = c.env.DB;

  const { results } = await db
    .prepare("SELECT exam_id FROM pinned_exams WHERE user_id = ?")
    .bind(user.uid)
    .all<{ exam_id: string }>();

  const list = (results || []).map((r) => r.exam_id);
  return c.json({ success: true, data: list });
});

// POST /api/pins/:examId - Pin an exam
pinRoutes.post("/:examId", async (c) => {
  const user = c.get("user");
  const examId = c.req.param("examId");
  const db = c.env.DB;
  const now = Date.now();

  await db
    .prepare(
      "INSERT INTO pinned_exams (user_id, exam_id, pinned_at) VALUES (?, ?, ?) ON CONFLICT(user_id, exam_id) DO UPDATE SET pinned_at = excluded.pinned_at"
    )
    .bind(user.uid, examId, now)
    .run();

  return c.json({ success: true });
});

// DELETE /api/pins/:examId - Unpin an exam
pinRoutes.delete("/:examId", async (c) => {
  const user = c.get("user");
  const examId = c.req.param("examId");
  const db = c.env.DB;

  await db.prepare("DELETE FROM pinned_exams WHERE user_id = ? AND exam_id = ?").bind(user.uid, examId).run();
  return c.json({ success: true });
});

// POST /api/pins/:examId/toggle - Toggle pin state
pinRoutes.post("/:examId/toggle", async (c) => {
  const user = c.get("user");
  const examId = c.req.param("examId");
  const db = c.env.DB;

  const existing = await db
    .prepare("SELECT 1 FROM pinned_exams WHERE user_id = ? AND exam_id = ?")
    .bind(user.uid, examId)
    .first();

  if (existing) {
    await db.prepare("DELETE FROM pinned_exams WHERE user_id = ? AND exam_id = ?").bind(user.uid, examId).run();
    return c.json({ success: true, data: { isPinned: false } });
  } else {
    await db
      .prepare("INSERT INTO pinned_exams (user_id, exam_id, pinned_at) VALUES (?, ?, ?)")
      .bind(user.uid, examId, Date.now())
      .run();
    return c.json({ success: true, data: { isPinned: true } });
  }
});
