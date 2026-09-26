// ============================================================================
// Student Feedback & Community Discussion Posts / Replies
// ============================================================================

import { Hono } from "hono";
import { requireAdmin } from "../middleware/authMiddleware";
import {
  AuthUser,
  Env,
  FeedbackMessageRow,
  FeedbackPostRow,
  FeedbackReplyRow,
} from "../types";

export const feedbackRoutes = new Hono<{ Bindings: Env; Variables: { user: AuthUser } }>();

// ----------------------------------------------------------------------------
// Part 1: Student Feedback Messages (Private to Admin)
// ----------------------------------------------------------------------------

// POST /api/feedback/messages - Submit student feedback
feedbackRoutes.post("/messages", async (c) => {
  const user = c.get("user");
  const body = await c.req.json().catch(() => ({}));
  const message = String(body.message || "").trim();
  const userName = String(body.userName || user.displayName || "Student").trim();
  const userEmail = String(body.userEmail || user.email || "").trim();
  const postId = body.postId ? String(body.postId) : null;
  const postTitle = body.postTitle ? String(body.postTitle) : null;

  if (!message) {
    return c.json({ success: false, error: "Message cannot be empty" }, 400);
  }
  if (message.length > 1000) {
    return c.json({ success: false, error: "Message cannot exceed 1000 characters" }, 400);
  }

  const id = crypto.randomUUID();
  const now = Date.now();
  const db = c.env.DB;

  await db
    .prepare(
      `INSERT INTO feedback_messages (id, message, user_id, user_name, user_email, timestamp, read, post_id, post_title)
       VALUES (?, ?, ?, ?, ?, ?, 0, ?, ?)`
    )
    .bind(id, message, user.uid, userName, userEmail, now, postId, postTitle)
    .run();

  return c.json({ success: true, data: { id } }, 201);
});

// GET /api/feedback/messages - List feedback messages (Admin)
feedbackRoutes.get("/messages", requireAdmin, async (c) => {
  const db = c.env.DB;
  const { results } = await db
    .prepare("SELECT * FROM feedback_messages ORDER BY timestamp DESC")
    .all<FeedbackMessageRow>();

  const list = (results || []).map((r) => ({
    id: r.id,
    message: r.message,
    userId: r.user_id,
    userName: r.user_name,
    userEmail: r.user_email,
    timestamp: r.timestamp,
    read: Boolean(r.read),
    postId: r.post_id,
    postTitle: r.post_title,
  }));

  return c.json({ success: true, data: list });
});

// PUT /api/feedback/messages/:id/read - Mark feedback message read (Admin)
feedbackRoutes.put("/messages/:id/read", requireAdmin, async (c) => {
  const id = c.req.param("id");
  const db = c.env.DB;
  await db.prepare("UPDATE feedback_messages SET read = 1 WHERE id = ?").bind(id).run();
  return c.json({ success: true });
});

// DELETE /api/feedback/messages/:id - Delete feedback message (Admin)
feedbackRoutes.delete("/messages/:id", requireAdmin, async (c) => {
  const id = c.req.param("id");
  const db = c.env.DB;
  await db.prepare("DELETE FROM feedback_messages WHERE id = ?").bind(id).run();
  return c.json({ success: true });
});

// ----------------------------------------------------------------------------
// Part 2: Feedback Discussion Posts (Admin Announcements)
// ----------------------------------------------------------------------------

// GET /api/feedback/posts - List discussion posts
feedbackRoutes.get("/posts", async (c) => {
  const db = c.env.DB;
  const { results } = await db
    .prepare("SELECT * FROM feedback_posts ORDER BY timestamp DESC")
    .all<FeedbackPostRow>();

  const list = (results || []).map((r) => ({
    id: r.id,
    title: r.title,
    message: r.message,
    authorId: r.author_id,
    authorEmail: r.author_email,
    timestamp: r.timestamp,
  }));

  return c.json({ success: true, data: list });
});

// POST /api/feedback/posts - Create post (Admin)
feedbackRoutes.post("/posts", requireAdmin, async (c) => {
  const user = c.get("user");
  const body = await c.req.json().catch(() => ({}));
  const title = String(body.title || "").trim();
  const message = String(body.message || "").trim();

  if (!title || !message) {
    return c.json({ success: false, error: "Title and message are required" }, 400);
  }

  const id = crypto.randomUUID();
  const now = Date.now();
  const db = c.env.DB;

  await db
    .prepare(
      "INSERT INTO feedback_posts (id, title, message, author_id, author_email, timestamp) VALUES (?, ?, ?, ?, ?, ?)"
    )
    .bind(id, title, message, user.uid, user.email, now)
    .run();

  return c.json({ success: true, data: { id } }, 201);
});

// DELETE /api/feedback/posts/:id - Delete post & cascade replies (Admin)
feedbackRoutes.delete("/posts/:id", requireAdmin, async (c) => {
  const id = c.req.param("id");
  const db = c.env.DB;

  await db.batch([
    db.prepare("DELETE FROM feedback_post_replies WHERE post_id = ?").bind(id),
    db.prepare("DELETE FROM feedback_posts WHERE id = ?").bind(id),
  ]);

  return c.json({ success: true });
});

// ----------------------------------------------------------------------------
// Part 3: Post Replies (Subcollection feedback_posts/{postId}/replies)
// ----------------------------------------------------------------------------

// GET /api/feedback/posts/:postId/replies - List replies for post
feedbackRoutes.get("/posts/:postId/replies", async (c) => {
  const postId = c.req.param("postId");
  const db = c.env.DB;

  const { results } = await db
    .prepare("SELECT * FROM feedback_post_replies WHERE post_id = ? ORDER BY timestamp DESC")
    .bind(postId)
    .all<FeedbackReplyRow>();

  const list = (results || []).map((r) => ({
    id: r.id,
    postId: r.post_id,
    uid: r.user_id,
    name: r.name,
    email: r.email,
    text: r.text,
    timestamp: r.timestamp,
    read: Boolean(r.read),
  }));

  return c.json({ success: true, data: list });
});

// POST /api/feedback/posts/:postId/replies - Submit student reply
feedbackRoutes.post("/posts/:postId/replies", async (c) => {
  const user = c.get("user");
  const postId = c.req.param("postId");
  const body = await c.req.json().catch(() => ({}));
  const text = String(body.text || "").trim();
  const name = String(body.name || user.displayName || "Student").trim();
  const email = String(body.email || user.email || "").trim();

  if (!text) {
    return c.json({ success: false, error: "Reply text cannot be empty" }, 400);
  }
  if (text.length > 1000) {
    return c.json({ success: false, error: "Reply text cannot exceed 1000 characters" }, 400);
  }

  const id = crypto.randomUUID();
  const now = Date.now();
  const db = c.env.DB;

  await db
    .prepare(
      `INSERT INTO feedback_post_replies (id, post_id, user_id, name, email, text, timestamp, read)
       VALUES (?, ?, ?, ?, ?, ?, ?, 0)`
    )
    .bind(id, postId, user.uid, name, email, text, now)
    .run();

  return c.json({ success: true, data: { id } }, 201);
});

// PUT /api/feedback/posts/:postId/replies/:replyId/read - Mark reply read (Admin)
feedbackRoutes.put("/posts/:postId/replies/:replyId/read", requireAdmin, async (c) => {
  const replyId = c.req.param("replyId");
  const db = c.env.DB;
  await db.prepare("UPDATE feedback_post_replies SET read = 1 WHERE id = ?").bind(replyId).run();
  return c.json({ success: true });
});

// DELETE /api/feedback/posts/:postId/replies/:replyId - Delete single reply (Admin)
feedbackRoutes.delete("/posts/:postId/replies/:replyId", requireAdmin, async (c) => {
  const replyId = c.req.param("replyId");
  const db = c.env.DB;
  await db.prepare("DELETE FROM feedback_post_replies WHERE id = ?").bind(replyId).run();
  return c.json({ success: true });
});
