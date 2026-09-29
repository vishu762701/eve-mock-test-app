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
    .prepare("SELECT question_id, id FROM bookmarks WHERE user_id = ?")
    .bind(user.uid)
    .all<{ question_id: string; id: string }>();

  const list = (results || []).map((r) => r.question_id || r.id);
  return c.json({ success: true, data: list });
});

// GET /api/bookmarks - Get full bookmarked question details
// Only return correct answers and explanations for questions the user has already submitted
bookmarkRoutes.get("/", async (c) => {
  const user = c.get("user");
  const db = c.env.DB;

  const { results } = await db
    .prepare(
      `SELECT b.*,
              aa.correct as submitted_correct,
              aa.explanation as submitted_explanation,
              aa.explanation_hi as submitted_explanation_hi
       FROM bookmarks b
       LEFT JOIN (
         SELECT aa.question_id, aa.correct, aa.explanation, aa.explanation_hi
         FROM attempt_answers aa
         JOIN attempts a ON aa.attempt_id = a.id
         WHERE a.user_id = ?
         GROUP BY aa.question_id
       ) aa ON aa.question_id = b.question_id
       WHERE b.user_id = ?
       ORDER BY b.bookmarked_at DESC`
    )
    .bind(user.uid, user.uid)
    .all<any>();

  const list = (results || []).map((r) => ({
    questionId: r.question_id || r.id,
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
    correctAnswer: r.submitted_correct || "",
    explanation: r.submitted_explanation || "",
    explanationHi: r.submitted_explanation_hi || "",
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
  const bookmarkId = `${user.uid}_${questionId}`;

  // Store empty string for correct_answer, explanation, and explanation_hi to avoid leaks
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
      bookmarkId,
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
      "",
      "",
      "",
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
  const compositeId = `${user.uid}_${id}`;

  await db
    .prepare("DELETE FROM bookmarks WHERE (id = ? OR id = ? OR question_id = ?) AND user_id = ?")
    .bind(compositeId, id, id, user.uid)
    .run();
  return c.json({ success: true });
});

// POST /api/bookmarks/toggle - Toggle bookmark state
bookmarkRoutes.post("/toggle", async (c) => {
  const user = c.get("user");
  const body = await c.req.json().catch(() => ({}));
  const questionId = String(body.questionId || "").trim();
  const db = c.env.DB;

  if (!questionId) {
    return c.json({ success: false, error: "questionId is required" }, 400);
  }

  const compositeId = `${user.uid}_${questionId}`;
  const existing = await db
    .prepare("SELECT 1 FROM bookmarks WHERE (id = ? OR id = ? OR question_id = ?) AND user_id = ?")
    .bind(compositeId, questionId, questionId, user.uid)
    .first();

  if (existing) {
    await db
      .prepare("DELETE FROM bookmarks WHERE (id = ? OR id = ? OR question_id = ?) AND user_id = ?")
      .bind(compositeId, questionId, questionId, user.uid)
      .run();
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
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(id) DO UPDATE SET
          bookmarked_at = excluded.bookmarked_at`
      )
      .bind(
        compositeId,
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
        "",
        "",
        "",
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
