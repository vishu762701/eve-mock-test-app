// ============================================================================
// AI Generated Tests Management Routes
// ============================================================================

import { Hono } from "hono";
import type { ContentfulStatusCode } from "hono/utils/http-status";
import { generateQuestions, getIstTimeAndDate, incrementTestNumber, GeminiProviderError } from "../ai/generator";
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
  const serverNow = Date.now();

  const list = (results || []).map((r) => {
    let questionCount = r.question_count;
    if (!questionCount && r.questions_json) {
      try {
        questionCount = JSON.parse(r.questions_json).length;
      } catch (_e) {}
    }

    return {
      id: r.id,
      examId: r.exam_id,
      examName: r.exam_name,
      testNumber: r.test_number,
      title: r.title,
      generatedAt: r.generated_at,
      status: r.status,
      questionCount: questionCount || 0,
      availableFrom: r.available_from || 0,
      questions: [],
    };
  });

  return c.json({ success: true, serverNow, data: list });
});

// GET /api/generated-tests/:id - Single generated test details
generatedTestRoutes.get("/:id", async (c) => {
  const id = c.req.param("id");
  const user = c.get("user");
  const db = c.env.DB;
  const serverNow = Date.now();

  const row = await db.prepare("SELECT * FROM generated_tests WHERE id = ?").bind(id).first<GeneratedTestRow>();
  if (!row) {
    return c.json({ success: false, error: "Test not found" }, 404);
  }

  if (!user.isAdmin && row.status !== "live" && row.status !== "published") {
    return c.json({ success: false, error: "Test is not available" }, 403);
  }

  let questionCount = row.question_count;
  let questions: any[] = [];
  if (row.questions_json) {
    try {
      questions = JSON.parse(row.questions_json);
      if (!questionCount) {
        questionCount = questions.length;
      }
    } catch (_e) {}
  }

  return c.json({
    success: true,
    serverNow,
    data: {
      id: row.id,
      examId: row.exam_id,
      examName: row.exam_name,
      testNumber: row.test_number,
      title: row.title,
      generatedAt: row.generated_at,
      status: row.status,
      questionCount: questionCount || 0,
      availableFrom: row.available_from || 0,
      questions,
    },
  });
});

// PUT /api/generated-tests/:id/schedule - Schedule test (Admin)
generatedTestRoutes.put("/:id/schedule", requireAdmin, async (c) => {
  const id = c.req.param("id");
  const body = await c.req.json().catch(() => ({}));
  const availableFrom = Number(body.availableFrom);

  if (isNaN(availableFrom) || availableFrom < 0 || !Number.isInteger(availableFrom)) {
    return c.json({ success: false, error: "availableFrom must be a non-negative integer" }, 400);
  }

  const db = c.env.DB;
  await db.prepare("UPDATE generated_tests SET available_from = ? WHERE id = ?").bind(availableFrom, id).run();
  return c.json({ success: true, data: { id, availableFrom } });
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
    const isGeminiErr = err instanceof GeminiProviderError;
    const userMsg = isGeminiErr ? err.userFacingMessage : (err.userFacingMessage || err.message || "Generation failed");
    const httpStatus: ContentfulStatusCode =
      isGeminiErr && err.httpStatus >= 400 && err.httpStatus < 600
        ? (err.httpStatus as ContentfulStatusCode)
        : 500;
    const errorCode = isGeminiErr ? err.code : "GENERATION_FAILED";

    await db
      .prepare(
        `UPDATE exams SET
          generating_lock_until = 0,
          last_generation_status = 'failed',
          last_generation_error = ?,
          last_generation_time = ?
         WHERE id = ?`
      )
      .bind(userMsg.slice(0, 200), Date.now(), examId)
      .run();

    return c.json({ success: false, error: userMsg, code: errorCode }, httpStatus);
  }
});
