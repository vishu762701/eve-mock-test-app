// ============================================================================
// Student Feedback & Community Discussion Posts / Replies
// ============================================================================

import { Hono } from "hono";
import { requirePermission } from "../middleware/authMiddleware";
import {
  AuthUser,
  Env,
  FeedbackMessageRow,
  FeedbackPostRow,
  FeedbackReplyRow,
} from "../types";
import { prepareAdminAuditLog } from "../util/auditLog";

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
  const userEmail = user.email || "";
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
feedbackRoutes.get("/messages", requirePermission("moderate_feedback"), async (c) => {
  const db = c.env.DB;
  const { results } = await db
    .prepare("SELECT fm.*, fr.reply_text AS saved_reply_text, fr.replied_at AS saved_replied_at FROM feedback_messages fm LEFT JOIN feedback_admin_replies fr ON fr.message_id = fm.id ORDER BY fm.timestamp DESC")
    .all<FeedbackMessageRow & { saved_reply_text?: string; saved_replied_at?: number; reply_text?: string; replied_at?: number }>();

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
    replyText: r.saved_reply_text ?? r.reply_text ?? "",
    repliedAt: r.saved_replied_at ?? r.replied_at ?? 0,
  }));

  return c.json({ success: true, data: list });
});

// PUT /api/feedback/messages/:id/read - Mark feedback message read (Admin)
feedbackRoutes.put("/messages/:id/read", requirePermission("moderate_feedback"), async (c) => {
  const user = c.get("user");
  const id = c.req.param("id");
  const db = c.env.DB;
  const updateStmt = db.prepare("UPDATE feedback_messages SET read = 1 WHERE id = ?").bind(id);
  const { stmt: auditStmt } = prepareAdminAuditLog(db, user, {
    actionType: "FEEDBACK_MESSAGE_READ",
    description: `Marked feedback message '${id}' as read`,
    targetType: "feedback_message",
    targetId: id,
    condition: { sql: "changes() > 0", bindings: [] },
  });
  const result = await db.batch([updateStmt, auditStmt]);
  if (!result[0].meta.changes) return c.json({ success: false, error: "Feedback record not found." }, 404);
  return c.json({ success: true });
});

// DELETE /api/feedback/messages/:id - Delete feedback message (Admin)
feedbackRoutes.delete("/messages/:id", requirePermission("moderate_feedback"), async (c) => {
  const user = c.get("user");
  const id = c.req.param("id");
  const db = c.env.DB;
  const deleteStmt = db.prepare("DELETE FROM feedback_messages WHERE id = ?").bind(id);
  const { stmt: auditStmt } = prepareAdminAuditLog(db, user, {
    actionType: "FEEDBACK_MESSAGE_DELETED",
    description: `Deleted feedback message '${id}'`,
    targetType: "feedback_message",
    targetId: id,
    condition: { sql: "changes() > 0", bindings: [] },
  });
  const result = await db.batch([
    db.prepare("DELETE FROM feedback_admin_replies WHERE message_id = ?").bind(id), deleteStmt, auditStmt]);
  if (!result[1].meta.changes) return c.json({ success: false, error: "Feedback record not found." }, 404);
  return c.json({ success: true });
});

// POST /api/feedback/messages/:id/reply - Admin reply to student feedback
feedbackRoutes.post("/messages/:id/reply", requirePermission("moderate_feedback"), async (c) => {
  const user = c.get("user");
  const id = c.req.param("id");
  const body = await c.req.json().catch(() => ({}));
  const reply = String(body.reply || "").trim();

  if (typeof body.reply !== "string" || !reply || reply.length > 1000) {
    return c.json({ success: false, error: "Reply must contain 1-1000 characters." }, 400);
  }
  const db = c.env.DB;
  if (!await db.prepare("SELECT id FROM feedback_messages WHERE id = ?").bind(id).first()) {
    return c.json({ success: false, error: "Feedback message not found." }, 404);
  }
  const now = Date.now();
  const { stmt: auditStmt } = prepareAdminAuditLog(db, user, {
    actionType: "FEEDBACK_REPLIED", description: `Saved reply to feedback message '${id}'`,
    targetType: "feedback_message", targetId: id,
    condition: { sql: "changes() > 0", bindings: [] },
  });
  const result = await db.batch([
    db.prepare("INSERT INTO feedback_admin_replies(message_id,reply_text,replied_at) SELECT id,?,? FROM feedback_messages WHERE id = ? ON CONFLICT(message_id) DO UPDATE SET reply_text=excluded.reply_text,replied_at=excluded.replied_at").bind(reply, now, id),
    db.prepare("UPDATE feedback_messages SET read = 1 WHERE id = ?").bind(id), auditStmt,
  ]);
  if (!result[0].meta.changes) return c.json({ success: false, error: "Feedback message not found." }, 404);
  return c.json({ success: true, data: { id, reply, repliedAt: now } });
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
feedbackRoutes.post("/posts", requirePermission("moderate_feedback"), async (c) => {
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

  const insertStmt = db
    .prepare(
      "INSERT INTO feedback_posts (id, title, message, author_id, author_email, timestamp) VALUES (?, ?, ?, ?, ?, ?)"
    )
    .bind(id, title, message, user.uid, user.email, now);

  const { stmt: auditStmt } = prepareAdminAuditLog(db, user, {
    actionType: "FEEDBACK_POST_CREATED",
    description: `Created feedback post '${title}'`,
    targetType: "feedback_post",
    targetId: id,
    metadata: { title },
  });

  await db.batch([insertStmt, auditStmt]);

  return c.json({ success: true, data: { id } }, 201);
});

// PUT /api/feedback/posts/:id - Edit post (Admin)
feedbackRoutes.put("/posts/:id", requirePermission("moderate_feedback"), async (c) => {
  const user = c.get("user");
  const id = c.req.param("id");
  const body = await c.req.json().catch(() => ({}));
  const title = String(body.title || "").trim();
  const message = String(body.message || "").trim();

  if (!title || !message) {
    return c.json({ success: false, error: "Title and message are required" }, 400);
  }

  const db = c.env.DB;
  const updateStmt = db
    .prepare("UPDATE feedback_posts SET title = ?, message = ? WHERE id = ?")
    .bind(title, message, id);

  const { stmt: auditStmt } = prepareAdminAuditLog(db, user, {
    actionType: "FEEDBACK_POST_EDITED",
    description: `Edited feedback post '${title}'`,
    targetType: "feedback_post",
    targetId: id,
    metadata: { title },
    condition: { sql: "changes() > 0", bindings: [] },
  });

  const result = await db.batch([updateStmt, auditStmt]);
  if (!result[0].meta.changes) return c.json({ success: false, error: "Feedback post not found." }, 404);
  return c.json({ success: true });
});

// DELETE /api/feedback/posts/:id - Delete post & cascade replies (Admin)
feedbackRoutes.delete("/posts/:id", requirePermission("moderate_feedback"), async (c) => {
  const user = c.get("user");
  const id = c.req.param("id");
  const db = c.env.DB;

  const { stmt: auditStmt } = prepareAdminAuditLog(db, user, {
    actionType: "FEEDBACK_POST_DELETED",
    description: `Deleted feedback post '${id}'`,
    targetType: "feedback_post",
    targetId: id,
    condition: { sql: "changes() > 0", bindings: [] },
  });

  const result = await db.batch([
    db.prepare("DELETE FROM feedback_post_replies WHERE post_id = ?").bind(id),
    db.prepare("DELETE FROM feedback_posts WHERE id = ?").bind(id),
    auditStmt,
  ]);
  if (!result[1].meta.changes) return c.json({ success: false, error: "Feedback post not found." }, 404);
  return c.json({ success: true });
});

// ----------------------------------------------------------------------------
// Part 3: Post Replies (Subcollection feedback_posts/{postId}/replies)
// ----------------------------------------------------------------------------

// GET /api/feedback/posts/:postId/replies - List replies for post
feedbackRoutes.get("/posts/:postId/replies", requirePermission("moderate_feedback"), async (c) => {
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
  const email = user.email || "";

  if (!text) {
    return c.json({ success: false, error: "Reply text cannot be empty" }, 400);
  }
  if (text.length > 1000) {
    return c.json({ success: false, error: "Reply text cannot exceed 1000 characters" }, 400);
  }

  const id = crypto.randomUUID();
  const now = Date.now();
  const db = c.env.DB;
  if (!await db.prepare("SELECT id FROM feedback_posts WHERE id = ?").bind(postId).first()) return c.json({ success: false, error: "Feedback post not found." }, 404);

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
feedbackRoutes.put("/posts/:postId/replies/:replyId/read", requirePermission("moderate_feedback"), async (c) => {
  const user = c.get("user");
  const replyId = c.req.param("replyId");
  const postId = c.req.param("postId");
  const db = c.env.DB;
  const updateStmt = db.prepare("UPDATE feedback_post_replies SET read = 1 WHERE id = ? AND post_id = ?").bind(replyId, postId);
  const { stmt: auditStmt } = prepareAdminAuditLog(db, user, {
    actionType: "FEEDBACK_REPLY_READ",
    description: `Marked reply '${replyId}' as read`,
    targetType: "feedback_reply",
    targetId: replyId,
    condition: { sql: "changes() > 0", bindings: [] },
  });
  const result = await db.batch([updateStmt, auditStmt]);
  if (!result[0].meta.changes) return c.json({ success: false, error: "Feedback record not found." }, 404);
  return c.json({ success: true });
});

// DELETE /api/feedback/posts/:postId/replies/:replyId - Delete single reply (Admin)
feedbackRoutes.delete("/posts/:postId/replies/:replyId", requirePermission("moderate_feedback"), async (c) => {
  const user = c.get("user");
  const replyId = c.req.param("replyId");
  const postId = c.req.param("postId");
  const db = c.env.DB;
  const deleteStmt = db.prepare("DELETE FROM feedback_post_replies WHERE id = ? AND post_id = ?").bind(replyId, postId);
  const { stmt: auditStmt } = prepareAdminAuditLog(db, user, {
    actionType: "FEEDBACK_REPLY_DELETED",
    description: `Deleted feedback reply '${replyId}'`,
    targetType: "feedback_reply",
    targetId: replyId,
    condition: { sql: "changes() > 0", bindings: [] },
  });
  const result = await db.batch([deleteStmt, auditStmt]);
  if (!result[0].meta.changes) return c.json({ success: false, error: "Feedback record not found." }, 404);
  return c.json({ success: true });
});
