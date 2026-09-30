// ============================================================================
// Exam Management Routes
// ============================================================================

import { Hono } from "hono";
import { requireAdmin } from "../middleware/authMiddleware";
import { SupabaseStorage } from "../supabase";
import { AuthUser, Env, ExamRow } from "../types";

export const examRoutes = new Hono<{ Bindings: Env; Variables: { user: AuthUser } }>();

export function parseNegativeMarking(raw: string): { text: string; value: number } | null {
  const trimmed = (raw ?? "").toString().trim();
  if (!trimmed || trimmed.includes(" ")) return null;
  if (trimmed === "0" || trimmed === "0.0" || trimmed === "0.00") {
    return { text: trimmed, value: 0 };
  }
  if (trimmed.includes("/")) {
    const parts = trimmed.split("/");
    if (parts.length !== 2) return null;
    if (!/^\d+$/.test(parts[0]) || !/^\d+$/.test(parts[1])) return null;
    const a = parseInt(parts[0], 10);
    const b = parseInt(parts[1], 10);
    if (isNaN(a) || isNaN(b)) return null;
    if (a < 1 || b < 1 || a > b || b > 100) return null;
    return { text: trimmed, value: a / b };
  }
  if (!/^\d+(\.\d+)?$/.test(trimmed)) return null;
  const num = Number(trimmed);
  if (isNaN(num) || num < 0 || num > 1) return null;
  return { text: trimmed, value: num };
}

function mapExamRow(row: ExamRow) {
  return {
    id: row.id,
    examName: row.exam_name,
    timeLimitMinutes: row.time_limit_minutes,
    category: row.category,
    syllabus: row.syllabus || "",
    questionCount: row.question_count,
    customPromptNotes: row.custom_prompt_notes || "",
    autoGenerationEnabled: Boolean(row.auto_generation_enabled),
    autoGenTime: row.auto_gen_time,
    timezone: row.timezone,
    testNumber: row.test_number,
    syllabusUrl: row.syllabus_url || "",
    syllabusFileName: row.syllabus_file_name || "",
    generationPrompt: row.generation_prompt || "",
    imageUrl: row.image_url || "",
    lastGeneratedDate: row.last_generated_date || "",
    lastGenerationStatus: row.last_generation_status || "",
    lastGenerationError: row.last_generation_error || "",
    lastGenerationTime: row.last_generation_time || 0,
    negativeMarkingText: row.negative_marking_text || "0",
    negativeMarkingValue: row.negative_marking_value ?? 0,
    parentExamId: row.parent_exam_id || "",
  };
}

// GET /api/exams - List all exams
examRoutes.get("/", async (c) => {
  const db = c.env.DB;
  const { results } = await db
    .prepare("SELECT * FROM exams ORDER BY exam_name ASC")
    .all<ExamRow>();

  const list = (results || []).map(mapExamRow);
  return c.json({ success: true, data: list });
});

// GET /api/exams/:id - Single exam details
examRoutes.get("/:id", async (c) => {
  const id = c.req.param("id");
  const db = c.env.DB;
  const row = await db.prepare("SELECT * FROM exams WHERE id = ?").bind(id).first<ExamRow>();

  if (!row) {
    return c.json({ success: false, error: "Exam not found" }, 404);
  }

  return c.json({ success: true, data: mapExamRow(row) });
});

// POST /api/exams - Create exam (Admin)
examRoutes.post("/", requireAdmin, async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const name = String(body.examName || "").trim();
  const minutes = body.timeLimitMinutes !== undefined ? Number(body.timeLimitMinutes) : 30;
  const category = String(body.category || "Other").trim();
  const testNumber = String(body.testNumber || "Test 1").trim();
  const questionCount = Number(body.questionCount) || 20;
  const autoGenEnabled = body.autoGenEnabled !== false && body.autoGenerationEnabled !== false ? 1 : 0;
  const autoGenTime = String(body.autoGenTime || "00:00").trim();
  const timezone = String(body.timezone || "Asia/Kolkata").trim();
  const generationPrompt = String(body.generationPrompt || "").trim();
  const imageUrl = String(body.imageUrl || "").trim();
  const parentExamId = String(body.parentExamId || "").trim();

  if (!name) {
    return c.json({ success: false, error: "Exam name cannot be empty" }, 400);
  }
  if (isNaN(minutes) || minutes < 1 || minutes > 600) {
    return c.json({ success: false, error: "Enter minutes between 1 and 600" }, 400);
  }

  let negText = "0";
  let negValue = 0;
  if (body.negativeMarkingText !== undefined && body.negativeMarkingText !== null) {
    const parsed = parseNegativeMarking(String(body.negativeMarkingText));
    if (!parsed) {
      return c.json({ success: false, error: "Enter like 1/3, 1/4, 0.25 or 0" }, 400);
    }
    negText = parsed.text;
    negValue = parsed.value;
  }

  const db = c.env.DB;

  if (parentExamId) {
    const parent = await db.prepare("SELECT id, parent_exam_id FROM exams WHERE id = ?").bind(parentExamId).first<ExamRow>();
    if (!parent) {
      return c.json({ success: false, error: "Parent exam not found" }, 400);
    }
    if (parent.parent_exam_id && parent.parent_exam_id.trim() !== "") {
      return c.json({ success: false, error: "Parent exam cannot be a sub-exam (only one level of nesting allowed)" }, 400);
    }
  }

  const duplicate = await db
    .prepare("SELECT 1 FROM exams WHERE LOWER(exam_name) = LOWER(?) AND COALESCE(parent_exam_id, '') = ?")
    .bind(name, parentExamId)
    .first();
  if (duplicate) {
    return c.json({ success: false, error: "An exam with this name already exists here" }, 409);
  }

  const id = crypto.randomUUID();

  await db
    .prepare(
      `INSERT INTO exams (
        id, exam_name, time_limit_minutes, category, test_number, question_count,
        auto_generation_enabled, auto_gen_time, timezone, generation_prompt, image_url,
        negative_marking_text, negative_marking_value, parent_exam_id
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
    )
    .bind(
      id,
      name,
      minutes,
      category,
      testNumber,
      questionCount,
      autoGenEnabled,
      autoGenTime,
      timezone,
      generationPrompt,
      imageUrl,
      negText,
      negValue,
      parentExamId
    )
    .run();

  return c.json({ success: true, data: { id } }, 201);
});

// PUT /api/exams/:id - Full settings update (Admin)
examRoutes.put("/:id", requireAdmin, async (c) => {
  const id = c.req.param("id");
  const body = await c.req.json().catch(() => ({}));
  const db = c.env.DB;

  const existing = await db.prepare("SELECT * FROM exams WHERE id = ?").bind(id).first<ExamRow>();
  if (!existing) {
    return c.json({ success: false, error: "Exam not found" }, 404);
  }

  const examName = body.examName !== undefined ? String(body.examName).trim() : existing.exam_name;
  if (!examName) {
    return c.json({ success: false, error: "Exam name cannot be empty" }, 400);
  }

  let minutes = existing.time_limit_minutes;
  if (body.timeLimitMinutes !== undefined && body.timeLimitMinutes !== null) {
    const parsed = Number(body.timeLimitMinutes);
    if (isNaN(parsed) || parsed < 1 || parsed > 600) {
      return c.json({ success: false, error: "Enter minutes between 1 and 600" }, 400);
    }
    minutes = parsed;
  }

  let negText = existing.negative_marking_text || "0";
  let negValue = existing.negative_marking_value ?? 0;
  if (body.negativeMarkingText !== undefined && body.negativeMarkingText !== null) {
    const parsed = parseNegativeMarking(String(body.negativeMarkingText));
    if (!parsed) {
      return c.json({ success: false, error: "Enter like 1/3, 1/4, 0.25 or 0" }, 400);
    }
    negText = parsed.text;
    negValue = parsed.value;
  }

  const testNumber = body.testNumber !== undefined ? String(body.testNumber).trim() : existing.test_number;
  const questionCount = body.questionCount !== undefined ? Number(body.questionCount) : existing.question_count;
  const autoGenEnabled = body.autoGenEnabled !== undefined
    ? (body.autoGenEnabled ? 1 : 0)
    : (body.autoGenerationEnabled !== undefined ? (body.autoGenerationEnabled ? 1 : 0) : existing.auto_generation_enabled);
  const autoGenTime = body.autoGenTime !== undefined ? String(body.autoGenTime).trim() : existing.auto_gen_time;
  const syllabusUrl = body.syllabusUrl !== undefined ? String(body.syllabusUrl).trim() : (existing.syllabus_url || "");
  const syllabusFileName = body.syllabusFileName !== undefined ? String(body.syllabusFileName).trim() : (existing.syllabus_file_name || "");
  const generationPrompt = body.generationPrompt !== undefined ? String(body.generationPrompt).trim() : (existing.generation_prompt || "");
  const category = body.category !== undefined ? String(body.category).trim() : existing.category;
  const parentExamId = body.parentExamId !== undefined ? String(body.parentExamId).trim() : (existing.parent_exam_id || "");

  if (parentExamId) {
    if (parentExamId === id) {
      return c.json({ success: false, error: "An exam cannot be its own parent" }, 400);
    }
    const parent = await db.prepare("SELECT id, parent_exam_id FROM exams WHERE id = ?").bind(parentExamId).first<ExamRow>();
    if (!parent) {
      return c.json({ success: false, error: "Parent exam not found" }, 400);
    }
    if (parent.parent_exam_id && parent.parent_exam_id.trim() !== "") {
      return c.json({ success: false, error: "Parent exam cannot be a sub-exam (only one level of nesting allowed)" }, 400);
    }
    const hasChildren = await db.prepare("SELECT 1 FROM exams WHERE parent_exam_id = ? LIMIT 1").bind(id).first();
    if (hasChildren) {
      return c.json({ success: false, error: "This exam has sub-exams and cannot become a sub-exam" }, 400);
    }
  }

  const nameChanged = examName.toLowerCase() !== existing.exam_name.toLowerCase();
  const parentChanged = parentExamId !== (existing.parent_exam_id || "");
  if (nameChanged || parentChanged) {
    const duplicate = await db
      .prepare("SELECT 1 FROM exams WHERE LOWER(exam_name) = LOWER(?) AND COALESCE(parent_exam_id, '') = ? AND id != ?")
      .bind(examName, parentExamId, id)
      .first();
    if (duplicate) {
      return c.json({ success: false, error: "An exam with this name already exists here" }, 409);
    }
  }

  await db
    .prepare(
      `UPDATE exams SET
        exam_name = ?,
        time_limit_minutes = ?,
        category = ?,
        test_number = ?,
        question_count = ?,
        auto_generation_enabled = ?,
        auto_gen_time = ?,
        syllabus_url = ?,
        syllabus_file_name = ?,
        generation_prompt = ?,
        negative_marking_text = ?,
        negative_marking_value = ?,
        parent_exam_id = ?
       WHERE id = ?`
    )
    .bind(
      examName,
      minutes,
      category,
      testNumber,
      questionCount,
      autoGenEnabled,
      autoGenTime,
      syllabusUrl,
      syllabusFileName,
      generationPrompt,
      negText,
      negValue,
      parentExamId,
      id
    )
    .run();

  return c.json({ success: true });
});

// PUT /api/exams/:id/image - Update image URL or base64 (Admin)
examRoutes.put("/:id/image", requireAdmin, async (c) => {
  const id = c.req.param("id");
  const body = await c.req.json().catch(() => ({}));
  const imageUrl = String(body.imageUrl || "").trim();
  const db = c.env.DB;

  await db.prepare("UPDATE exams SET image_url = ? WHERE id = ?").bind(imageUrl, id).run();
  return c.json({ success: true });
});

// PUT /api/exams/:id/rename - Rename exam with cascading updates (Admin)
examRoutes.put("/:id/rename", requireAdmin, async (c) => {
  const id = c.req.param("id");
  const body = await c.req.json().catch(() => ({}));
  const newName = String(body.newName || "").trim();
  const db = c.env.DB;

  if (!newName) {
    return c.json({ success: false, error: "New exam name cannot be empty" }, 400);
  }

  await db.batch([
    db.prepare("UPDATE exams SET exam_name = ? WHERE id = ?").bind(newName, id),
    db.prepare("UPDATE generated_tests SET exam_name = ? WHERE exam_id = ?").bind(newName, id),
    db.prepare("UPDATE attempts SET exam_name = ? WHERE exam_id = ?").bind(newName, id),
    db.prepare("UPDATE leaderboard SET exam_name = ? WHERE exam_id = ?").bind(newName, id),
    db.prepare("UPDATE bookmarks SET exam_name = ? WHERE exam_id = ?").bind(newName, id),
  ]);

  return c.json({ success: true });
});

// DELETE /api/exams/:id - Delete exam and associated test data (Admin)
examRoutes.delete("/:id", requireAdmin, async (c) => {
  const id = c.req.param("id");
  const db = c.env.DB;

  const hasSubExams = await db.prepare("SELECT 1 FROM exams WHERE parent_exam_id = ? LIMIT 1").bind(id).first();
  if (hasSubExams) {
    return c.json({ success: false, error: "This exam has sub-exams. Delete them first." }, 409);
  }

  // Retrieve syllabus URL to cleanup Supabase Storage if present
  const exam = await db.prepare("SELECT syllabus_url FROM exams WHERE id = ?").bind(id).first<ExamRow>();
  if (exam && exam.syllabus_url) {
    try {
      const storage = new SupabaseStorage(c.env);
      const urlParts = exam.syllabus_url.split("/eve-media/");
      if (urlParts.length > 1) {
        await storage.deleteFile(urlParts[1]);
      }
    } catch (_e) {}
  }

  await db.batch([
    db.prepare("DELETE FROM questions WHERE exam_id = ?").bind(id),
    db.prepare("DELETE FROM generated_tests WHERE exam_id = ?").bind(id),
    db.prepare("DELETE FROM attempts WHERE exam_id = ?").bind(id),
    db.prepare("DELETE FROM leaderboard WHERE exam_id = ?").bind(id),
    db.prepare("DELETE FROM exams WHERE id = ?").bind(id),
  ]);

  return c.json({ success: true });
});

// POST /api/exams/:id/syllabus - Upload syllabus PDF (Admin)
examRoutes.post("/:id/syllabus", requireAdmin, async (c) => {
  const id = c.req.param("id");
  const contentType = c.req.header("content-type") || "";
  const db = c.env.DB;

  let fileName = "syllabus.pdf";
  let pdfBytes: ArrayBuffer;

  if (contentType.includes("multipart/form-data")) {
    const formData = await c.req.formData();
    const file = formData.get("file") as File | null;
    if (!file) {
      return c.json({ success: false, error: "No file provided in form-data" }, 400);
    }
    fileName = file.name || "syllabus.pdf";
    pdfBytes = await file.arrayBuffer();
  } else if (contentType.includes("application/pdf") || contentType.includes("application/octet-stream")) {
    fileName = c.req.header("x-file-name") || "syllabus.pdf";
    pdfBytes = await c.req.arrayBuffer();
  } else {
    // Alternatively accept base64 JSON payload
    const body = await c.req.json().catch(() => ({}));
    if (!body.base64) {
      return c.json({ success: false, error: "Expected PDF file upload or base64 payload" }, 400);
    }
    fileName = String(body.fileName || "syllabus.pdf");
    const binary = atob(body.base64);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
    pdfBytes = bytes.buffer;
  }

  // Validate size < 20MB
  if (pdfBytes.byteLength > 20 * 1024 * 1024) {
    return c.json({ success: false, error: "Syllabus PDF size exceeds 20MB limit" }, 400);
  }

  const storage = new SupabaseStorage(c.env);
  const path = `syllabi/${id}_${Date.now()}.pdf`;
  const downloadUrl = await storage.uploadFile(path, pdfBytes, "application/pdf");

  await db
    .prepare("UPDATE exams SET syllabus_url = ?, syllabus_file_name = ? WHERE id = ?")
    .bind(downloadUrl, fileName, id)
    .run();

  return c.json({ success: true, data: { syllabusUrl: downloadUrl, syllabusFileName: fileName } });
});

// DELETE /api/exams/:id/syllabus - Remove syllabus PDF (Admin)
examRoutes.delete("/:id/syllabus", requireAdmin, async (c) => {
  const id = c.req.param("id");
  const db = c.env.DB;

  const exam = await db.prepare("SELECT syllabus_url FROM exams WHERE id = ?").bind(id).first<ExamRow>();
  if (exam && exam.syllabus_url) {
    try {
      const storage = new SupabaseStorage(c.env);
      const urlParts = exam.syllabus_url.split("/eve-media/");
      if (urlParts.length > 1) {
        await storage.deleteFile(urlParts[1]);
      }
    } catch (_e) {}
  }

  await db
    .prepare("UPDATE exams SET syllabus_url = '', syllabus_file_name = '' WHERE id = ?")
    .bind(id)
    .run();

  return c.json({ success: true });
});
