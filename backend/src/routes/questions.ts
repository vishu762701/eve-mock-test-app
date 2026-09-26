// ============================================================================
// Question Bank Routes
// ============================================================================

import { Hono } from "hono";
import { requireAdmin } from "../middleware/authMiddleware";
import { AuthUser, Env, QuestionRow } from "../types";

export const questionRoutes = new Hono<{ Bindings: Env; Variables: { user: AuthUser } }>();

function mapQuestionRow(row: QuestionRow) {
  return {
    id: row.id,
    examId: row.exam_id,
    questionText: row.question_text,
    optionA: row.option_a,
    optionB: row.option_b,
    optionC: row.option_c,
    optionD: row.option_d,
    correctAnswer: row.correct_answer,
    explanation: row.explanation || "",
    topic: row.topic || "",
    isPyq: Boolean(row.is_pyq),
    pyqYear: row.pyq_year || 0,
    pyqPaper: row.pyq_paper || "",
    questionTextHi: row.question_text_hi || "",
    optionAHi: row.option_a_hi || "",
    optionBHi: row.option_b_hi || "",
    optionCHi: row.option_c_hi || "",
    optionDHi: row.option_d_hi || "",
    explanationHi: row.explanation_hi || "",
  };
}

// GET /api/exams/:examId/questions - Query questions with optional filters
questionRoutes.get("/exam/:examId", async (c) => {
  const examId = c.req.param("examId");
  const isPyqParam = c.req.query("isPyq");
  const topic = c.req.query("topic");
  const pyqYear = c.req.query("pyqYear");
  const pyqPaper = c.req.query("pyqPaper");

  const db = c.env.DB;
  let query = "SELECT * FROM questions WHERE exam_id = ?";
  const params: any[] = [examId];

  if (isPyqParam !== undefined) {
    const isPyq = isPyqParam === "true" || isPyqParam === "1" ? 1 : 0;
    query += " AND is_pyq = ?";
    params.push(isPyq);
  }

  if (topic) {
    query += " AND LOWER(topic) = LOWER(?)";
    params.push(topic.trim());
  }

  if (pyqYear) {
    query += " AND pyq_year = ?";
    params.push(parseInt(pyqYear, 10));
  }

  if (pyqPaper) {
    query += " AND LOWER(pyq_paper) = LOWER(?)";
    params.push(pyqPaper.trim());
  }

  const { results } = await db.prepare(query).bind(...params).all<QuestionRow>();
  const list = (results || []).map(mapQuestionRow);
  return c.json({ success: true, data: list });
});

// POST /api/questions - Add single question (Admin)
questionRoutes.post("/", requireAdmin, async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const examId = String(body.examId || "").trim();
  const questionText = String(body.questionText || "").trim();
  const optionA = String(body.optionA || "").trim();
  const optionB = String(body.optionB || "").trim();
  const optionC = String(body.optionC || "").trim();
  const optionD = String(body.optionD || "").trim();
  const correctAnswer = String(body.correctAnswer || "A").trim().toUpperCase();

  if (!examId || !questionText || !optionA || !optionB || !optionC || !optionD) {
    return c.json({ success: false, error: "Missing required question fields" }, 400);
  }

  const id = crypto.randomUUID();
  const db = c.env.DB;

  await db
    .prepare(
      `INSERT INTO questions (
        id, exam_id, question_text, option_a, option_b, option_c, option_d,
        correct_answer, explanation, topic, is_pyq, pyq_year, pyq_paper,
        question_text_hi, option_a_hi, option_b_hi, option_c_hi, option_d_hi, explanation_hi
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
    )
    .bind(
      id,
      examId,
      questionText,
      optionA,
      optionB,
      optionC,
      optionD,
      correctAnswer,
      String(body.explanation || ""),
      String(body.topic || ""),
      body.isPyq ? 1 : 0,
      Number(body.pyqYear) || 0,
      String(body.pyqPaper || ""),
      String(body.questionTextHi || ""),
      String(body.optionAHi || ""),
      String(body.optionBHi || ""),
      String(body.optionCHi || ""),
      String(body.optionDHi || ""),
      String(body.explanationHi || "")
    )
    .run();

  return c.json({ success: true, data: { id } }, 201);
});

// POST /api/questions/batch - Batch add questions (Admin)
questionRoutes.post("/batch", requireAdmin, async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const questions: any[] = Array.isArray(body.questions) ? body.questions : [];

  if (questions.length === 0) {
    return c.json({ success: false, error: "Empty questions array" }, 400);
  }

  const db = c.env.DB;
  const statements = questions.map((q) => {
    const id = q.id || crypto.randomUUID();
    return db
      .prepare(
        `INSERT INTO questions (
          id, exam_id, question_text, option_a, option_b, option_c, option_d,
          correct_answer, explanation, topic, is_pyq, pyq_year, pyq_paper,
          question_text_hi, option_a_hi, option_b_hi, option_c_hi, option_d_hi, explanation_hi
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
      )
      .bind(
        id,
        String(q.examId || ""),
        String(q.questionText || ""),
        String(q.optionA || ""),
        String(q.optionB || ""),
        String(q.optionC || ""),
        String(q.optionD || ""),
        String(q.correctAnswer || "A").toUpperCase(),
        String(q.explanation || ""),
        String(q.topic || ""),
        q.isPyq ? 1 : 0,
        Number(q.pyqYear) || 0,
        String(q.pyqPaper || ""),
        String(q.questionTextHi || ""),
        String(q.optionAHi || ""),
        String(q.optionBHi || ""),
        String(q.optionCHi || ""),
        String(q.optionDHi || ""),
        String(q.explanationHi || "")
      );
  });

  // Execute in chunks of 50 to respect D1 batch limits
  for (let i = 0; i < statements.length; i += 50) {
    const chunk = statements.slice(i, i + 50);
    await db.batch(chunk);
  }

  return c.json({ success: true, count: questions.length }, 201);
});

// PUT /api/questions/:id - Update single question (Admin)
questionRoutes.put("/:id", requireAdmin, async (c) => {
  const id = c.req.param("id");
  const body = await c.req.json().catch(() => ({}));
  const db = c.env.DB;

  await db
    .prepare(
      `UPDATE questions SET
        exam_id = ?,
        question_text = ?,
        option_a = ?,
        option_b = ?,
        option_c = ?,
        option_d = ?,
        correct_answer = ?,
        explanation = ?,
        topic = ?,
        is_pyq = ?,
        pyq_year = ?,
        pyq_paper = ?,
        question_text_hi = ?,
        option_a_hi = ?,
        option_b_hi = ?,
        option_c_hi = ?,
        option_d_hi = ?,
        explanation_hi = ?
       WHERE id = ?`
    )
    .bind(
      String(body.examId || ""),
      String(body.questionText || ""),
      String(body.optionA || ""),
      String(body.optionB || ""),
      String(body.optionC || ""),
      String(body.optionD || ""),
      String(body.correctAnswer || "A").toUpperCase(),
      String(body.explanation || ""),
      String(body.topic || ""),
      body.isPyq ? 1 : 0,
      Number(body.pyqYear) || 0,
      String(body.pyqPaper || ""),
      String(body.questionTextHi || ""),
      String(body.optionAHi || ""),
      String(body.optionBHi || ""),
      String(body.optionCHi || ""),
      String(body.optionDHi || ""),
      String(body.explanationHi || ""),
      id
    )
    .run();

  return c.json({ success: true });
});

// DELETE /api/questions/:id - Delete question (Admin)
questionRoutes.delete("/:id", requireAdmin, async (c) => {
  const id = c.req.param("id");
  const db = c.env.DB;
  await db.prepare("DELETE FROM questions WHERE id = ?").bind(id).run();
  return c.json({ success: true });
});
