// ============================================================================
// Community Polls & Single-Vote Logic
// ============================================================================

import { Hono } from "hono";
import { requireAdmin } from "../middleware/authMiddleware";
import { AuthUser, Env, PollRow } from "../types";

export const pollRoutes = new Hono<{ Bindings: Env; Variables: { user: AuthUser } }>();

async function getVoteCountsForPoll(db: D1Database, pollId: string, optionsCount: number): Promise<Record<string, number>> {
  const counts: Record<string, number> = {};
  for (let i = 0; i < optionsCount; i++) {
    counts[String(i)] = 0;
  }

  const { results } = await db
    .prepare("SELECT option_index, COUNT(*) as vote_count FROM poll_votes WHERE poll_id = ? GROUP BY option_index")
    .bind(pollId)
    .all<{ option_index: number; vote_count: number }>();

  for (const r of results || []) {
    counts[String(r.option_index)] = r.vote_count;
  }

  return counts;
}

// GET /api/polls - List all polls
pollRoutes.get("/", async (c) => {
  const db = c.env.DB;
  const { results: polls } = await db
    .prepare("SELECT * FROM polls ORDER BY created_at DESC")
    .all<PollRow>();

  const list: any[] = [];
  for (const p of polls || []) {
    let options: string[] = [];
    try {
      options = JSON.parse(p.options_json);
    } catch (_e) {}

    const voteCounts = await getVoteCountsForPoll(db, p.id, options.length);
    list.push({
      id: p.id,
      question: p.question,
      options,
      createdAt: p.created_at,
      endsAt: p.ends_at,
      active: Boolean(p.active),
      createdBy: p.created_by,
      voteCounts,
    });
  }

  return c.json({ success: true, data: list });
});

// GET /api/polls/active - Get current active poll
pollRoutes.get("/active", async (c) => {
  const db = c.env.DB;
  const now = Date.now();

  const { results: polls } = await db
    .prepare("SELECT * FROM polls WHERE active = 1 ORDER BY created_at DESC LIMIT 5")
    .all<PollRow>();

  for (const p of polls || []) {
    if (p.ends_at > 0 && now > p.ends_at) continue;

    let options: string[] = [];
    try {
      options = JSON.parse(p.options_json);
    } catch (_e) {}

    const voteCounts = await getVoteCountsForPoll(db, p.id, options.length);
    return c.json({
      success: true,
      data: {
        id: p.id,
        question: p.question,
        options,
        createdAt: p.created_at,
        endsAt: p.ends_at,
        active: Boolean(p.active),
        createdBy: p.created_by,
        voteCounts,
      },
    });
  }

  return c.json({ success: true, data: null });
});

// POST /api/polls - Create poll (Admin)
pollRoutes.post("/", requireAdmin, async (c) => {
  const user = c.get("user");
  const body = await c.req.json().catch(() => ({}));
  const question = String(body.question || "").trim();
  const optionsRaw: string[] = Array.isArray(body.options) ? body.options : [];
  const options = optionsRaw.map((o) => String(o).trim()).filter(Boolean);
  const endsAt = Number(body.endsAt) || 0;
  const active = body.active !== false ? 1 : 0;

  if (!question || options.length < 2) {
    return c.json({ success: false, error: "Question and at least 2 options are required" }, 400);
  }

  const id = crypto.randomUUID();
  const now = Date.now();
  const db = c.env.DB;

  await db
    .prepare(
      "INSERT INTO polls (id, question, options_json, created_at, ends_at, active, created_by) VALUES (?, ?, ?, ?, ?, ?, ?)"
    )
    .bind(id, question, JSON.stringify(options), now, endsAt, active, user.email)
    .run();

  return c.json({ success: true, data: { id } }, 201);
});

// PUT /api/polls/:id/status - Toggle poll active status (Admin)
pollRoutes.put("/:id/status", requireAdmin, async (c) => {
  const id = c.req.param("id");
  const body = await c.req.json().catch(() => ({}));
  const active = body.active ? 1 : 0;
  const db = c.env.DB;

  await db.prepare("UPDATE polls SET active = ? WHERE id = ?").bind(active, id).run();
  return c.json({ success: true });
});

// DELETE /api/polls/:id - Delete poll & votes (Admin)
pollRoutes.delete("/:id", requireAdmin, async (c) => {
  const id = c.req.param("id");
  const db = c.env.DB;

  await db.batch([
    db.prepare("DELETE FROM poll_votes WHERE poll_id = ?").bind(id),
    db.prepare("DELETE FROM polls WHERE id = ?").bind(id),
  ]);

  return c.json({ success: true });
});

// GET /api/polls/:id/vote - Get current user vote
pollRoutes.get("/:id/vote", async (c) => {
  const user = c.get("user");
  const pollId = c.req.param("id");
  const db = c.env.DB;

  const row = await db
    .prepare("SELECT option_index FROM poll_votes WHERE poll_id = ? AND user_id = ?")
    .bind(pollId, user.uid)
    .first<{ option_index: number }>();

  return c.json({ success: true, data: { optionIndex: row ? row.option_index : null } });
});

// POST /api/polls/:id/vote - Submit vote (1 vote per user)
pollRoutes.post("/:id/vote", async (c) => {
  const user = c.get("user");
  const pollId = c.req.param("id");
  const body = await c.req.json().catch(() => ({}));
  const optionIndex = Number(body.optionIndex);
  const db = c.env.DB;
  const now = Date.now();

  if (isNaN(optionIndex) || optionIndex < 0) {
    return c.json({ success: false, error: "Valid optionIndex required" }, 400);
  }

  // Check poll exists and is active
  const poll = await db.prepare("SELECT * FROM polls WHERE id = ?").bind(pollId).first<PollRow>();
  if (!poll) {
    return c.json({ success: false, error: "Poll not found" }, 404);
  }
  if (!poll.active || (poll.ends_at > 0 && now > poll.ends_at)) {
    return c.json({ success: false, error: "This poll is closed or has expired." }, 400);
  }

  // Check single vote constraint
  const existing = await db
    .prepare("SELECT 1 FROM poll_votes WHERE poll_id = ? AND user_id = ?")
    .bind(pollId, user.uid)
    .first();

  if (existing) {
    return c.json({ success: false, error: "You have already voted in this poll." }, 409);
  }

  await db
    .prepare(
      "INSERT INTO poll_votes (poll_id, user_id, option_index, voted_at) VALUES (?, ?, ?, ?)"
    )
    .bind(pollId, user.uid, optionIndex, now)
    .run();

  return c.json({ success: true });
});
