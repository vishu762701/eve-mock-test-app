// ============================================================================
// Leaderboard & Rank Routes
// ============================================================================

import { Hono } from "hono";
import { AuthUser, Env, LeaderboardRow, OverallLeaderboardRow } from "../types";

export const leaderboardRoutes = new Hono<{ Bindings: Env; Variables: { user: AuthUser } }>();

// GET /api/leaderboard - Top scorers for an exam or overall
leaderboardRoutes.get("/", async (c) => {
  const examId = c.req.query("examId") || "overall";
  const limit = Math.min(100, Math.max(1, parseInt(c.req.query("limit") || "50", 10)));
  const db = c.env.DB;

  if (examId === "overall") {
    const { results } = await db
      .prepare(
        "SELECT user_id, display_name, score, total, timestamp FROM overall_leaderboard ORDER BY score DESC LIMIT ?"
      )
      .bind(limit)
      .all<OverallLeaderboardRow>();

    const list = (results || []).map((r) => ({
      userId: r.user_id,
      examId: "overall",
      examName: "Overall",
      displayName: r.display_name || "Student",
      score: r.score,
      total: r.total,
      timestamp: r.timestamp,
    }));

    return c.json({ success: true, data: list });
  }

  const { results } = await db
    .prepare(
      "SELECT * FROM leaderboard WHERE exam_id = ? ORDER BY score DESC LIMIT ?"
    )
    .bind(examId, limit)
    .all<LeaderboardRow>();

  const list = (results || []).map((r) => ({
    userId: r.user_id,
    examId: r.exam_id,
    examName: r.exam_name,
    displayName: r.display_name || "Student",
    score: r.score,
    total: r.total,
    timestamp: r.timestamp,
  }));

  return c.json({ success: true, data: list });
});

// GET /api/leaderboard/rank - User rank for an exam or overall
leaderboardRoutes.get("/rank", async (c) => {
  const user = c.get("user");
  const examId = c.req.query("examId") || "overall";
  const db = c.env.DB;

  if (examId === "overall") {
    const myDoc = await db
      .prepare("SELECT score, total FROM overall_leaderboard WHERE user_id = ?")
      .bind(user.uid)
      .first<{ score: number; total: number }>();

    if (!myDoc) {
      return c.json({ success: true, data: null });
    }

    const higher = await db
      .prepare("SELECT COUNT(*) as count FROM overall_leaderboard WHERE score > ?")
      .bind(myDoc.score)
      .first<{ count: number }>();

    const total = await db
      .prepare("SELECT COUNT(*) as count FROM overall_leaderboard")
      .first<{ count: number }>();

    const rank = (higher?.count || 0) + 1;
    const totalParticipants = total?.count || 1;

    return c.json({
      success: true,
      data: {
        rank,
        totalParticipants,
        score: myDoc.score,
        total: myDoc.total,
      },
    });
  }

  const myDoc = await db
    .prepare("SELECT score, total FROM leaderboard WHERE exam_id = ? AND user_id = ?")
    .bind(examId, user.uid)
    .first<{ score: number; total: number }>();

  if (!myDoc) {
    return c.json({ success: true, data: null });
  }

  const higher = await db
    .prepare("SELECT COUNT(*) as count FROM leaderboard WHERE exam_id = ? AND score > ?")
    .bind(examId, myDoc.score)
    .first<{ count: number }>();

  const total = await db
    .prepare("SELECT COUNT(*) as count FROM leaderboard WHERE exam_id = ?")
    .bind(examId)
    .first<{ count: number }>();

  const rank = (higher?.count || 0) + 1;
  const totalParticipants = total?.count || 1;

  return c.json({
    success: true,
    data: {
      rank,
      totalParticipants,
      score: myDoc.score,
      total: myDoc.total,
    },
  });
});
