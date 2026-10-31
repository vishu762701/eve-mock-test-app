// ============================================================================
// Student Feedback & Community Discussion Posts / Replies
// ============================================================================

import { Hono } from "hono";
import { requireAdmin, requirePermission } from "../middleware/authMiddleware";
import {
  AuthUser,
  Env,
  FeedbackMessageRow,
  FeedbackPostRow,
  FeedbackReplyRow,
} from "../types";
import { prepareAdminAuditLog, recordAdminAction } from "../util/auditLog";

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
  });
  await db.batch([updateStmt, auditStmt]);
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
  });
  await db.batch([deleteStmt, auditStmt]);
  return c.json({ success: true });
});

// POST /api/feedback/messages/:id/reply - Admin reply to student feedback
feedbackRoutes.post("/messages/:id/reply", requirePermission("moderate_feedback"), async (c) => {
  const user = c.get("user");
  const id = c.req.param("id");
  const body = await c.req.json().catch(() => ({}));
  const reply = String(body.reply || "").trim();

  if (!reply) {
    return c.json({ success: false, error: "Reply cannot be empty" }, 400);
  }

  const db = c.env.DB;
  try {
    await db.prepare("UPDATE feedback_messages SET read = 1 WHERE id = ?").bind(id).run();
  } catch {}

  try {
    await db
      .prepare("UPDATE feedback_messages SET reply_text = ?, replied_at = ? WHERE id = ?")
      .bind(reply, Date.now(), id)
      .run();
  } catch {
    try {
      await db.prepare("ALTER TABLE feedback_messages ADD COLUMN reply_text TEXT DEFAULT ''").run();
      await db.prepare("ALTER TABLE feedback_messages ADD COLUMN replied_at INTEGER DEFAULT 0").run();
      await db
        .prepare("UPDATE feedback_messages SET reply_text = ?, replied_at = ? WHERE id = ?")
        .bind(reply, Date.now(), id)
        .run();
    } catch {}
  }

  await recordAdminAction(db, user, {
    actionType: "FEEDBACK_REPLIED",
    description: `Replied to feedback message '${id}'`,
    targetType: "feedback_message",
    targetId: id,
  });

  return c.json({ success: true, data: { id, reply } });
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
  });

  await db.batch([updateStmt, auditStmt]);

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
  });

  await db.batch([
    db.prepare("DELETE FROM feedback_post_replies WHERE post_id = ?").bind(id),
    db.prepare("DELETE FROM feedback_posts WHERE id = ?").bind(id),
    auditStmt,
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
feedbackRoutes.put("/posts/:postId/replies/:replyId/read", requirePermission("moderate_feedback"), async (c) => {
  const user = c.get("user");
  const replyId = c.req.param("replyId");
  const db = c.env.DB;
  const updateStmt = db.prepare("UPDATE feedback_post_replies SET read = 1 WHERE id = ?").bind(replyId);
  const { stmt: auditStmt } = prepareAdminAuditLog(db, user, {
    actionType: "FEEDBACK_REPLY_READ",
    description: `Marked reply '${replyId}' as read`,
    targetType: "feedback_reply",
    targetId: replyId,
  });
  await db.batch([updateStmt, auditStmt]);
  return c.json({ success: true });
});

// DELETE /api/feedback/posts/:postId/replies/:replyId - Delete single reply (Admin)
feedbackRoutes.delete("/posts/:postId/replies/:replyId", requirePermission("moderate_feedback"), async (c) => {
  const user = c.get("user");
  const replyId = c.req.param("replyId");
  const db = c.env.DB;
  const deleteStmt = db.prepare("DELETE FROM feedback_post_replies WHERE id = ?").bind(replyId);
  const { stmt: auditStmt } = prepareAdminAuditLog(db, user, {
    actionType: "FEEDBACK_REPLY_DELETED",
    description: `Deleted feedback reply '${replyId}'`,
    targetType: "feedback_reply",
    targetId: replyId,
  });
  await db.batch([deleteStmt, auditStmt]);
  return c.json({ success: true });
});
