import { deletionInfo, deleteUnused } from '../services/deletion';
// ============================================================================
// AI Generated Tests Management Routes
// ============================================================================

import { runGeneration, GenerationBusyError } from "../services/generation";
import { parseAndValidateQuestions } from "../ai/generator";
import { hideAnswers } from "../util/clientProtocol";
import { Hono } from "hono";
import { GeminiProviderError } from "../ai/generator";
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

    let validationError = "";
    if (user.isAdmin) {
      try { if (parseAndValidateQuestions(r.questions_json).length !== questionCount) throw new Error(); }
      catch { validationError = "Invalid questions, duplicate options, answer keys or question count"; }
    }
    return {
      validationError,
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
  const id = c.req.param("id") || "";
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

  if (!user.isAdmin && (row.available_from || 0) > serverNow) return c.json({ success: false, error: "Test not open yet" }, 403);
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

  let validationError = "";
  try { if (parseAndValidateQuestions(row.questions_json).length !== questionCount) throw new Error(); }
  catch { validationError = "Invalid questions or answer keys"; }
  if (!Array.isArray(questions)) questions = [];
  if (!user.isAdmin && validationError) return c.json({ success: false, error: "This test needs administrator repair before it can be attempted" }, 409);

  return c.json({
    success: true,
    serverNow,
    data: {
      validationError,
      id: row.id,
      examId: row.exam_id,
      examName: row.exam_name,
      testNumber: row.test_number,
      title: row.title,
      generatedAt: row.generated_at,
      status: row.status,
      questionCount: questionCount || 0,
      availableFrom: row.available_from || 0,
      questions: hideAnswers(c) ? questions.map((q) => ({ ...q, correctAnswer: "", correct_answer: "", answer: "", explanation: "", explanationHi: "", explanation_hi: "" })) : questions,
    },
  });
});

// PUT /api/generated-tests/:id/schedule - Schedule test (Admin)
generatedTestRoutes.put("/:id/schedule", requireAdmin, async (c) => {
  const id = c.req.param("id") || "";
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
  const id = c.req.param("id") || "";
  const body = await c.req.json().catch(() => ({}));
  const status = String(body.status || "paused").trim().toLowerCase();
  const db = c.env.DB;

  if (!["draft", "paused", "live", "published", "unpublished"].includes(status)) return c.json({ success: false, error: "Invalid test status" }, 400);
  const test = await db.prepare("SELECT * FROM generated_tests WHERE id = ?").bind(id).first<GeneratedTestRow>();
  if (!test) return c.json({ success: false, error: "Test not found" }, 404);
  if (["live", "published"].includes(status)) {
    try {
      const validated = parseAndValidateQuestions(test.questions_json);
      if (validated.length !== test.question_count) throw new Error("Question count mismatch");
    } catch { return c.json({ success: false, error: "Test has invalid questions or answer keys; repair it before publishing" }, 400); }
  }
  await db.prepare("UPDATE generated_tests SET status = ? WHERE id = ?").bind(status, id).run();
  return c.json({ success: true });
});

// DELETE /api/generated-tests/:id - Delete generated test (Admin)
generatedTestRoutes.get("/:id/deletion-info", requireAdmin, async (c) => {
  const id = c.req.param("id") || "";
  if (!await c.env.DB.prepare("SELECT id FROM generated_tests WHERE id = ?").bind(id).first()) return c.json({ success: false, error: "Record not found" }, 404);
  return c.json({ success: true, data: await deletionInfo(c.env.DB, id, 'test') });
});

generatedTestRoutes.delete("/:id", requireAdmin, async (c) => {
  const id = c.req.param("id") || "";
  const db = c.env.DB;
  if (!await db.prepare("SELECT id FROM generated_tests WHERE id = ?").bind(id).first()) return c.json({ success: false, error: "Record not found" }, 404);
  const dependencies = await deletionInfo(db, id, 'test');
  if (!dependencies.canDelete) return c.json({ success: false, error: dependencies.message, code: dependencies.code, dependencies, requestId: c.res.headers.get('X-Request-ID') }, 409);

  if (!await deleteUnused(db, id, 'test')) {
    const current = await deletionInfo(db, id, 'test');
    return c.json({ success: false, error: current.message || 'Record changed. Refresh before retrying.', code: current.code || 'DELETE_RECORD_CHANGED', dependencies: current, requestId: c.res.headers.get('X-Request-ID') }, 409);
  }
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
  const targetCount = Number(body.questionCount ?? exam.question_count ?? 20);
  if (!Number.isInteger(targetCount) || targetCount < 1 || targetCount > 200) return c.json({ success: false, error: "questionCount must be an integer between 1 and 200" }, 400);
  const testNumber = String(body.testNumber || exam.test_number || "Test 1").trim();
  const customPrompt = String(body.customPromptNotes || exam.generation_prompt || exam.custom_prompt_notes || "").trim();

  try {
    const data = await runGeneration(c.env, exam, targetCount, testNumber, customPrompt, "manual", body.requestId ? String(body.requestId).slice(0, 100) : undefined);
    return c.json({ success: true, data });
  } catch (err: any) {
    if (err instanceof GenerationBusyError) return c.json({ success: false, error: err.message, code: "GENERATION_BUSY" }, 409);
    const provider = err instanceof GeminiProviderError;
    return c.json({ success: false, error: provider ? err.userFacingMessage : "Generation could not be completed. Check the system monitor and retry.", code: provider ? err.code : "GENERATION_FAILED" }, provider ? 502 : 500);
  }
});
