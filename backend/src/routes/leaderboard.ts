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
        "SELECT user_id, display_name, score, total, timestamp, accuracy FROM overall_leaderboard ORDER BY score DESC, accuracy DESC, timestamp ASC LIMIT ?"
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
      accuracy: r.accuracy || 0,
      timeTakenSeconds: 0,
    }));

    return c.json({ success: true, data: list });
  }

  const { results } = await db
    .prepare(
      "SELECT * FROM leaderboard WHERE exam_id = ? ORDER BY score DESC, time_taken_seconds ASC, timestamp ASC LIMIT ?"
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
    timeTakenSeconds: r.time_taken_seconds || 0,
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
      .prepare("SELECT score, total, accuracy, timestamp FROM overall_leaderboard WHERE user_id = ?")
      .bind(user.uid)
      .first<{ score: number; total: number; accuracy: number; timestamp: number }>();

    if (!myDoc) {
      return c.json({ success: true, data: null });
    }

    const myAccuracy = myDoc.accuracy || 0;
    const higher = await db
      .prepare(
        `SELECT COUNT(*) as count FROM overall_leaderboard
         WHERE score > ?
            OR (score = ? AND accuracy > ?)
            OR (score = ? AND accuracy = ? AND timestamp < ?)`
      )
      .bind(myDoc.score, myDoc.score, myAccuracy, myDoc.score, myAccuracy, myDoc.timestamp)
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
    .prepare("SELECT score, total, time_taken_seconds, timestamp FROM leaderboard WHERE exam_id = ? AND user_id = ?")
    .bind(examId, user.uid)
    .first<{ score: number; total: number; time_taken_seconds: number; timestamp: number }>();

  if (!myDoc) {
    return c.json({ success: true, data: null });
  }

  const myTime = myDoc.time_taken_seconds || 0;
  const higher = await db
    .prepare(
      `SELECT COUNT(*) as count FROM leaderboard
       WHERE exam_id = ?
         AND (score > ?
              OR (score = ? AND time_taken_seconds < ?)
              OR (score = ? AND time_taken_seconds = ? AND timestamp < ?))`
    )
    .bind(examId, myDoc.score, myDoc.score, myTime, myDoc.score, myTime, myDoc.timestamp)
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
      timeTakenSeconds: myTime,
    },
  });
});

// GET /api/leaderboard/stats?examId=<key>
leaderboardRoutes.get("/stats", async (c) => {
  const user = c.get("user");
  const examId = c.req.query("examId");
  if (!examId) {
    return c.json({ success: false, error: "examId is required" }, 400);
  }

  const db = c.env.DB;

  const agg = await db
    .prepare(
      "SELECT COUNT(*) as participants, MAX(score) as topperScore, AVG(score) as avgScore FROM leaderboard WHERE exam_id = ?"
    )
    .bind(examId)
    .first<{ participants: number; topperScore: number | null; avgScore: number | null }>();

  const participants = agg?.participants || 0;
  const topperScore = agg?.topperScore !== null && agg?.topperScore !== undefined ? agg.topperScore : 0;
  const averageScore = agg?.avgScore !== null && agg?.avgScore !== undefined ? Math.round(agg.avgScore * 10) / 10 : 0;

  const myDoc = await db
    .prepare("SELECT score, time_taken_seconds, timestamp FROM leaderboard WHERE exam_id = ? AND user_id = ?")
    .bind(examId, user.uid)
    .first<{ score: number; time_taken_seconds: number; timestamp: number }>();

  if (!myDoc) {
    return c.json({
      success: true,
      data: {
        participants,
        topperScore,
        averageScore,
        myRank: null,
        myScore: null,
        myTimeSeconds: null,
        myPercentile: null,
      },
    });
  }

  const myTime = myDoc.time_taken_seconds || 0;
  const higher = await db
    .prepare(
      `SELECT COUNT(*) as count FROM leaderboard
       WHERE exam_id = ?
         AND (score > ?
              OR (score = ? AND time_taken_seconds < ?)
              OR (score = ? AND time_taken_seconds = ? AND timestamp < ?))`
    )
    .bind(examId, myDoc.score, myDoc.score, myTime, myDoc.score, myTime, myDoc.timestamp)
    .first<{ count: number }>();

  const myRank = (higher?.count || 0) + 1;
  const myPercentile =
    participants <= 1 ? 100 : Math.round((((participants - myRank) / (participants - 1)) * 100) * 10) / 10;

  return c.json({
    success: true,
    data: {
      participants,
      topperScore,
      averageScore,
      myRank,
      myScore: myDoc.score,
      myTimeSeconds: myTime,
      myPercentile,
    },
  });
});
