// ============================================================================
// AI Generated Tests Management Routes
// ============================================================================

import { Hono } from "hono";
import { generateQuestions, getIstTimeAndDate, incrementTestNumber } from "../ai/generator";
import { requireAdmin } from "../middleware/authMiddleware";
import { AuthUser, Env, ExamRow, GeneratedTestRow } from "../types";

export const generatedTestRoutes = new Hono<{ Bindings: Env; Variables: { user: AuthUser } }>();

// GET /api/generated-tests - List generated tests
generatedTestRoutes.get("/", async (c) => {
  const user = c.get("user");
  const examId = c.req.query("examId");
  const db = c.env.DB;

  let query = "SELECT * FROM generated_tests";
  const params: any[] = [];
  const conditions: string[] = [];

  if (examId) {
    conditions.push("exam_id = ?");
    params.push(examId.trim());
  }

  // Non-admins only see 'live' or 'published' tests
  if (!user.isAdmin) {
    conditions.push("status IN ('live', 'published')");
  }

  if (conditions.length > 0) {
    query += " WHERE " + conditions.join(" AND ");
  }

  query += " ORDER BY generated_at DESC";

  const { results } = await db.prepare(query).bind(...params).all<GeneratedTestRow>();

  const list = (results || []).map((r) => {
    let questions: any[] = [];
    try {
      questions = JSON.parse(r.questions_json);
    } catch (_e) {}

    return {
      id: r.id,
      examId: r.exam_id,
      examName: r.exam_name,
      testNumber: r.test_number,
      title: r.title,
      generatedAt: r.generated_at,
      status: r.status,
      questionCount: r.question_count || questions.length,
      questions,
    };
  });

  return c.json({ success: true, data: list });
});

// PUT /api/generated-tests/:id/status - Update test status (Admin)
generatedTestRoutes.put("/:id/status", requireAdmin, async (c) => {
  const id = c.req.param("id");
  const body = await c.req.json().catch(() => ({}));
  const status = String(body.status || "paused").trim().toLowerCase();
  const db = c.env.DB;

  await db.prepare("UPDATE generated_tests SET status = ? WHERE id = ?").bind(status, id).run();
  return c.json({ success: true });
});

// DELETE /api/generated-tests/:id - Delete generated test (Admin)
generatedTestRoutes.delete("/:id", requireAdmin, async (c) => {
  const id = c.req.param("id");
  const db = c.env.DB;
  await db.prepare("DELETE FROM generated_tests WHERE id = ?").bind(id).run();
  return c.json({ success: true });
});

// POST /api/generated-tests/generate-now - On-demand AI test generation (Admin)
generatedTestRoutes.post("/generate-now", requireAdmin, async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const examId = String(body.examId || "").trim();
  const db = c.env.DB;

  if (!examId) {
    return c.json({ success: false, error: "examId is required" }, 400);
  }

  const exam = await db.prepare("SELECT * FROM exams WHERE id = ?").bind(examId).first<ExamRow>();
  if (!exam) {
    return c.json({ success: false, error: "Exam not found" }, 404);
  }

  const examName = exam.exam_name || "Mock Test";
  const targetCount = Math.max(1, Math.min(200, Number(body.questionCount || exam.question_count || 20)));
  const testNumber = String(body.testNumber || exam.test_number || "Test 1").trim();
  const customPrompt = String(body.customPromptNotes || exam.generation_prompt || exam.custom_prompt_notes || "").trim();
  const { todayDate } = getIstTimeAndDate();

  await db
    .prepare("UPDATE exams SET last_generation_status = 'running', last_generation_time = ? WHERE id = ?")
    .bind(Date.now(), examId)
    .run();

  try {
    const questions = await generateQuestions(
      c.env,
      examName,
      exam.syllabus || "",
      targetCount,
      customPrompt
    );

    const testId = crypto.randomUUID();
    const now = Date.now();
    const title = `${examName} - ${testNumber}`;
    const nextTestNumber = incrementTestNumber(testNumber);

    await db.batch([
      db
        .prepare(
          `INSERT INTO generated_tests (id, exam_id, exam_name, test_number, title, generated_at, status, question_count, syllabus_used, prompt_used, questions_json)
           VALUES (?, ?, ?, ?, ?, ?, 'paused', ?, ?, ?, ?)`
        )
        .bind(
          testId,
          examId,
          examName,
          testNumber,
          title,
          now,
          questions.length,
          exam.syllabus || "",
          customPrompt,
          JSON.stringify(questions)
        ),
      db
        .prepare(
          `UPDATE exams SET
            generating_lock_until = 0,
            last_generated_date = ?,
            last_generation_status = 'success',
            last_generation_error = '',
            last_generation_time = ?,
            test_number = ?
           WHERE id = ?`
        )
        .bind(todayDate, now, nextTestNumber, examId),
    ]);

    return c.json({
      success: true,
      data: {
        testId,
        questionCount: questions.length,
        testNumber,
        nextTestNumber,
      },
    });
  } catch (err: any) {
    await db
      .prepare(
        `UPDATE exams SET
          generating_lock_until = 0,
          last_generation_status = 'failed',
          last_generation_error = ?,
          last_generation_time = ?
         WHERE id = ?`
      )
      .bind(String(err.message || "Unknown error").slice(0, 200), Date.now(), examId)
      .run();

    return c.json({ success: false, error: err.message || "Generation failed" }, 500);
  }
});
