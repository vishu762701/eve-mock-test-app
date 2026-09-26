// ============================================================================
// Bookmarked Questions Routes
// ============================================================================

import { Hono } from "hono";
import { AuthUser, Env } from "../types";

export const bookmarkRoutes = new Hono<{ Bindings: Env; Variables: { user: AuthUser } }>();

// GET /api/bookmarks/ids - Get all bookmarked question IDs
bookmarkRoutes.get("/ids", async (c) => {
  const user = c.get("user");
  const db = c.env.DB;

  const { results } = await db
    .prepare("SELECT id FROM bookmarks WHERE user_id = ?")
    .bind(user.uid)
    .all<{ id: string }>();

  const list = (results || []).map((r) => r.id);
  return c.json({ success: true, data: list });
});

// GET /api/bookmarks - Get full bookmarked question details
bookmarkRoutes.get("/", async (c) => {
  const user = c.get("user");
  const db = c.env.DB;

  const { results } = await db
    .prepare("SELECT * FROM bookmarks WHERE user_id = ? ORDER BY bookmarked_at DESC")
    .bind(user.uid)
    .all<any>();

  const list = (results || []).map((r) => ({
    questionId: r.id,
    examId: r.exam_id,
    examName: r.exam_name,
    questionNumber: r.question_number,
    questionText: r.question_text,
    questionTextHi: r.question_text_hi || "",
    optionA: r.option_a,
    optionB: r.option_b,
    optionC: r.option_c,
    optionD: r.option_d,
    optionAHi: r.option_a_hi || "",
    optionBHi: r.option_b_hi || "",
    optionCHi: r.option_c_hi || "",
    optionDHi: r.option_d_hi || "",
    correctAnswer: r.correct_answer,
    explanation: r.explanation || "",
    explanationHi: r.explanation_hi || "",
    topic: r.topic || "",
    isPyq: Boolean(r.is_pyq),
    pyqYear: r.pyq_year || 0,
    pyqPaper: r.pyq_paper || "",
    bookmarkedAt: r.bookmarked_at,
  }));

  return c.json({ success: true, data: list });
});

// POST /api/bookmarks - Add bookmark
bookmarkRoutes.post("/", async (c) => {
  const user = c.get("user");
  const body = await c.req.json().catch(() => ({}));
  const db = c.env.DB;

  const questionId = String(body.questionId || "").trim();
  if (!questionId) {
    return c.json({ success: false, error: "questionId is required" }, 400);
  }

  const now = Date.now();

  await db
    .prepare(
      `INSERT INTO bookmarks (
        id, user_id, question_id, exam_id, exam_name, question_number,
        question_text, question_text_hi, option_a, option_b, option_c, option_d,
        option_a_hi, option_b_hi, option_c_hi, option_d_hi, correct_answer,
        explanation, explanation_hi, topic, is_pyq, pyq_year, pyq_paper, bookmarked_at
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      ON CONFLICT(id) DO UPDATE SET
        bookmarked_at = excluded.bookmarked_at`
    )
    .bind(
      questionId,
      user.uid,
      questionId,
      String(body.examId || ""),
      String(body.examName || ""),
      Number(body.questionNumber || 1),
      String(body.questionText || ""),
      String(body.questionTextHi || ""),
      String(body.optionA || ""),
      String(body.optionB || ""),
      String(body.optionC || ""),
      String(body.optionD || ""),
      String(body.optionAHi || ""),
      String(body.optionBHi || ""),
      String(body.optionCHi || ""),
      String(body.optionDHi || ""),
      String(body.correctAnswer || "A"),
      String(body.explanation || ""),
      String(body.explanationHi || ""),
      String(body.topic || ""),
      body.isPyq ? 1 : 0,
      Number(body.pyqYear || 0),
      String(body.pyqPaper || ""),
      now
    )
    .run();

  return c.json({ success: true }, 201);
});

// DELETE /api/bookmarks/:id - Remove bookmark
bookmarkRoutes.delete("/:id", async (c) => {
  const user = c.get("user");
  const id = c.req.param("id");
  const db = c.env.DB;

  await db.prepare("DELETE FROM bookmarks WHERE id = ? AND user_id = ?").bind(id, user.uid).run();
  return c.json({ success: true });
});

// POST /api/bookmarks/toggle - Toggle bookmark state
bookmarkRoutes.post("/toggle", async (c) => {
  const user = c.get("user");
  const body = await c.req.json().catch(() => ({}));
  const id = String(body.questionId || "").trim();
  const db = c.env.DB;

  if (!id) {
    return c.json({ success: false, error: "questionId is required" }, 400);
  }

  const existing = await db
    .prepare("SELECT 1 FROM bookmarks WHERE id = ? AND user_id = ?")
    .bind(id, user.uid)
    .first();

  if (existing) {
    await db.prepare("DELETE FROM bookmarks WHERE id = ? AND user_id = ?").bind(id, user.uid).run();
    return c.json({ success: true, data: { isBookmarked: false } });
  } else {
    const now = Date.now();
    await db
      .prepare(
        `INSERT INTO bookmarks (
          id, user_id, question_id, exam_id, exam_name, question_number,
          question_text, question_text_hi, option_a, option_b, option_c, option_d,
          option_a_hi, option_b_hi, option_c_hi, option_d_hi, correct_answer,
          explanation, explanation_hi, topic, is_pyq, pyq_year, pyq_paper, bookmarked_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
      )
      .bind(
        id,
        user.uid,
        id,
        String(body.examId || ""),
        String(body.examName || ""),
        Number(body.questionNumber || 1),
        String(body.questionText || ""),
        String(body.questionTextHi || ""),
        String(body.optionA || ""),
        String(body.optionB || ""),
        String(body.optionC || ""),
        String(body.optionD || ""),
        String(body.optionAHi || ""),
        String(body.optionBHi || ""),
        String(body.optionCHi || ""),
        String(body.optionDHi || ""),
        String(body.correctAnswer || "A"),
        String(body.explanation || ""),
        String(body.explanationHi || ""),
        String(body.topic || ""),
        body.isPyq ? 1 : 0,
        Number(body.pyqYear || 0),
        String(body.pyqPaper || ""),
        now
      )
      .run();

    return c.json({ success: true, data: { isBookmarked: true } });
  }
});
