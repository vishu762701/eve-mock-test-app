import { cleanupMedia } from '../services/mediaCleanup';
import { deletionInfo, deleteUnused } from '../services/deletion';
// ============================================================================
// Exam Management Routes
// ============================================================================

import { Hono } from "hono";
import { requireAdmin, requirePermission } from "../middleware/authMiddleware";
import { SupabaseStorage } from "../supabase";
import { AuthUser, EmojiPlacementPublic, Env, ExamRow } from "../types";
import { prepareAdminAuditLog, recordAdminAction, runBatch } from "../util/auditLog";
import { getActiveEmojiPlacements } from "./emoji";

export const examRoutes = new Hono<{ Bindings: Env; Variables: { user: AuthUser } }>();

/** Same bounded PDF header/trailer contract as the Android picker. */
export function validSyllabusPdf(bytes: Uint8Array): boolean {
  if (bytes.length < 12 || bytes.length > 20 * 1024 * 1024) return false;
  const headerLimit = Math.min(bytes.length - 4, 1024);
  let header = false;
  for (let i = 0; i < headerLimit; i++) {
    if (bytes[i] === 0x25 && bytes[i + 1] === 0x50 &&
        bytes[i + 2] === 0x44 && bytes[i + 3] === 0x46 &&
        bytes[i + 4] === 0x2d) { header = true; break; }
  }
  if (!header) return false;
  const footerStart = Math.max(0, bytes.length - 1024);
  for (let i = footerStart; i + 4 < bytes.length; i++) {
    if (bytes[i] === 0x25 && bytes[i + 1] === 0x25 &&
        bytes[i + 2] === 0x45 && bytes[i + 3] === 0x4f &&
        bytes[i + 4] === 0x46) return true;
  }
  return false;
}

export function safeSyllabusFilename(value: unknown): string {
  const clean = String(value || "syllabus.pdf").replace(/[\\/\x00-\x1f\x7f]/g, "_").trim().slice(0, 180);
  return clean || "syllabus.pdf";
}


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

const cutoffCategories = new Set(["General", "OBC", "SC", "ST", "EWS"]);
function normalizeCutoffs(value: unknown): Record<string, number> | null {
  if (!value || typeof value !== "object" || Array.isArray(value)) return null;
  const result: Record<string, number> = {};
  for (const [category, cutoff] of Object.entries(value)) {
    if (!cutoffCategories.has(category) || typeof cutoff !== "number" || !Number.isFinite(cutoff) || cutoff < 0) return null;
    result[category] = cutoff;
  }
  return result;
}
function storedCutoffs(raw: string | undefined): Record<string, number> {
  const value = normalizeCutoffs(JSON.parse(raw || "{}"));
  if (!value) throw new Error("Stored exam cutoff settings are invalid");
  return value;
}

/** Shared destination guard for both name-edit entry points. Values remain bound. */
function examDestinationGuard(name?: string, parent?: string): { sql: string; bindings: string[] } {
  if (name === undefined && parent === undefined) return { sql: "1", bindings: [] };
  const nameSql = name === undefined ? "exams.exam_name" : "?";
  const parentSql = parent === undefined ? "exams.parent_exam_id" : "?";
  const pair = [...(name === undefined ? [] : [name]), ...(parent === undefined ? [] : [parent])];
  return {
    sql: `((LOWER(exams.exam_name) = LOWER(${nameSql}) AND COALESCE(exams.parent_exam_id, '') = COALESCE(${parentSql}, ''))
      OR NOT EXISTS (SELECT 1 FROM exams AS sibling WHERE LOWER(sibling.exam_name) = LOWER(${nameSql})
      AND COALESCE(sibling.parent_exam_id, '') = COALESCE(${parentSql}, '') AND sibling.id != exams.id))`,
    bindings: [...pair, ...pair],
  };
}
/** A fresh audit witness gates the existing denormalized-name updates. */
function examNameCascades(db: D1Database, id: string, name: string, auditId: string): D1PreparedStatement[] {
  return [
    db.prepare("UPDATE generated_tests SET exam_name = ? WHERE exam_id = ? AND EXISTS (SELECT 1 FROM admin_audit_log WHERE id = ?)").bind(name, id, auditId),
    db.prepare("UPDATE attempts SET exam_name = ? WHERE exam_id = ? AND EXISTS (SELECT 1 FROM admin_audit_log WHERE id = ?)").bind(name, id, auditId),
    db.prepare("UPDATE leaderboard SET exam_name = ? WHERE exam_id = ? AND EXISTS (SELECT 1 FROM admin_audit_log WHERE id = ?)").bind(name, id, auditId),
    db.prepare("UPDATE bookmarks SET exam_name = ? WHERE exam_id = ? AND EXISTS (SELECT 1 FROM admin_audit_log WHERE id = ?)").bind(name, id, auditId),
  ];
}

function mapExamRow(row: ExamRow, emojiPlacement?: EmojiPlacementPublic) {
  return {
    id: row.id,
    examName: row.exam_name,
    timeLimitMinutes: row.time_limit_minutes,
    category: row.category,
    syllabus: row.syllabus || "",
    questionCount: row.question_count,
    cutoffs: storedCutoffs(row.cutoffs_json),
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
    publishMode: row.publish_mode || "inherit",
    ...(emojiPlacement ? { emoji: emojiPlacement } : {}),
  };
}

// GET /api/exams - List all exams
examRoutes.get("/", async (c) => {
  const db = c.env.DB;
  const { results } = await db
    .prepare("SELECT * FROM exams ORDER BY exam_name ASC")
    .all<ExamRow>();

  let examPlacements = new Map<string, EmojiPlacementPublic>();
  let categoryPlacements: Record<string, EmojiPlacementPublic> = {};
  try {
    const placements = await getActiveEmojiPlacements(db, c.env);
    examPlacements = placements.examPlacements;
    categoryPlacements = placements.categoryPlacements;
  } catch {}

  const list = (results || []).map((r) => mapExamRow(r, examPlacements.get(r.id)));
  return c.json({ success: true, data: list, categoryEmoji: categoryPlacements });
});

// GET /api/exams/:id - Single exam details
examRoutes.get("/:id", async (c) => {
  const id = c.req.param("id") || "";
  const db = c.env.DB;
  const row = await db.prepare("SELECT * FROM exams WHERE id = ?").bind(id).first<ExamRow>();

  if (!row) {
    return c.json({ success: false, error: "Exam not found" }, 404);
  }

  let emojiPlacement: EmojiPlacementPublic | undefined;
  try {
    const { examPlacements } = await getActiveEmojiPlacements(db, c.env);
    emojiPlacement = examPlacements.get(id);
  } catch {}

  return c.json({ success: true, data: mapExamRow(row, emojiPlacement) });
});

// POST /api/exams - Create exam (Admin)
examRoutes.post("/", requirePermission("manage_exams"), async (c) => {
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

  let publishMode = "inherit";
  if (body.publishMode !== undefined && body.publishMode !== null) {
    const mode = String(body.publishMode).trim().toLowerCase();
    if (mode !== "inherit" && mode !== "live" && mode !== "paused") {
      return c.json({ success: false, error: "publishMode must be 'inherit', 'live', or 'paused'" }, 400);
    }
    publishMode = mode;
  }

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

  const insertStmt = db
    .prepare(
      `INSERT INTO exams (
        id, exam_name, time_limit_minutes, category, test_number, question_count,
        auto_generation_enabled, auto_gen_time, timezone, generation_prompt, image_url,
        negative_marking_text, negative_marking_value, parent_exam_id, publish_mode
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
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
      parentExamId,
      publishMode
    );

  const user = c.get("user");
  const { stmt: auditStmt } = prepareAdminAuditLog(db, user, {
    actionType: "EXAM_CREATED",
    description: `Created exam '${name}' (${category})`,
    targetType: "exam",
    targetId: id,
    metadata: { examName: name, category, parentExamId },
  });

  await db.batch([insertStmt, auditStmt]);

  return c.json({ success: true, data: { id } }, 201);
});

// PUT /api/exams/:id - Full settings update (Admin)
examRoutes.put("/:id", requirePermission("manage_exams"), async (c) => {
  const id = c.req.param("id") || "";
  const body = await c.req.json().catch(() => null);
  if (!body || typeof body !== "object" || Array.isArray(body)) return c.json({ success: false, error: "Provide exam settings" }, 400);
  if (body.examName !== undefined && typeof body.examName !== "string") return c.json({ success: false, error: "Provide a nonempty exam name" }, 400);
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
  let cutoffsJson = existing.cutoffs_json || "{}";
  if (body.cutoffs !== undefined) {
    const cutoffs = normalizeCutoffs(body.cutoffs);
    if (!cutoffs) return c.json({ success: false, error: "Cutoffs must contain non-negative numeric General/OBC/SC/ST/EWS values" }, 400);
    cutoffsJson = JSON.stringify(cutoffs);
  }
  const autoGenEnabled = body.autoGenEnabled !== undefined
    ? (body.autoGenEnabled ? 1 : 0)
    : (body.autoGenerationEnabled !== undefined ? (body.autoGenerationEnabled ? 1 : 0) : existing.auto_generation_enabled);
  const autoGenTime = body.autoGenTime !== undefined ? String(body.autoGenTime).trim() : existing.auto_gen_time;
  const syllabusUrl = body.syllabusUrl !== undefined ? String(body.syllabusUrl).trim() : (existing.syllabus_url || "");
  const syllabusFileName = body.syllabusFileName !== undefined ? String(body.syllabusFileName).trim() : (existing.syllabus_file_name || "");
  const generationPrompt = body.generationPrompt !== undefined ? String(body.generationPrompt).trim() : (existing.generation_prompt || "");
  const category = body.category !== undefined ? String(body.category).trim() : existing.category;
  const parentExamId = body.parentExamId !== undefined ? String(body.parentExamId).trim() : (existing.parent_exam_id || "");

  let publishMode = existing.publish_mode || "inherit";
  if (body.publishMode !== undefined && body.publishMode !== null) {
    const mode = String(body.publishMode).trim().toLowerCase();
    if (mode !== "inherit" && mode !== "live" && mode !== "paused") {
      return c.json({ success: false, error: "publishMode must be 'inherit', 'live', or 'paused'" }, 400);
    }
    publishMode = mode;
  }

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

  // Omitted columns retain their WRITE-time values rather than an earlier snapshot.
  const assignments: string[] = [];
  const values: (string | number)[] = [];
  const add = (column: string, value: string | number, provided: boolean) => {
    if (provided) { assignments.push(`${column} = ?`); values.push(value); }
  };
  add("exam_name", examName, body.examName !== undefined);
  add("time_limit_minutes", minutes, body.timeLimitMinutes != null);
  add("category", category, body.category !== undefined);
  add("test_number", testNumber, body.testNumber !== undefined);
  add("question_count", questionCount, body.questionCount !== undefined);
  add("auto_generation_enabled", autoGenEnabled, body.autoGenEnabled !== undefined || body.autoGenerationEnabled !== undefined);
  add("auto_gen_time", autoGenTime, body.autoGenTime !== undefined);
  add("syllabus_url", syllabusUrl, body.syllabusUrl !== undefined);
  add("syllabus_file_name", syllabusFileName, body.syllabusFileName !== undefined);
  add("generation_prompt", generationPrompt, body.generationPrompt !== undefined);
  add("negative_marking_text", negText, body.negativeMarkingText != null);
  add("negative_marking_value", negValue, body.negativeMarkingText != null);
  add("parent_exam_id", parentExamId, body.parentExamId !== undefined);
  add("publish_mode", publishMode, body.publishMode != null);
  add("cutoffs_json", cutoffsJson, body.cutoffs !== undefined);
  if (!assignments.length) return c.json({ success: false, error: "No editable exam fields provided" }, 400);
  const destination = examDestinationGuard(body.examName !== undefined ? examName : undefined,
    body.parentExamId !== undefined ? parentExamId : undefined);
  const updateStmt = db.prepare(`UPDATE exams SET ${assignments.join(", ")} WHERE id = ? AND ${destination.sql}`)
    .bind(...values, id, ...destination.bindings);
  const user = c.get("user");
  const { id: auditId, stmt: auditStmt } = prepareAdminAuditLog(db, user, {
    actionType: "EXAM_EDITED",
    description: `Updated exam '${examName}' (${id})`,
    targetType: "exam", targetId: id,
    metadata: {
      ...(body.examName !== undefined ? { examName } : {}),
      ...(body.category !== undefined ? { category } : {}),
      ...(body.timeLimitMinutes != null ? { minutes } : {}),
      ...(body.publishMode != null ? { publishMode } : {}),
      updatedFields: assignments.map(x => x.split(" ")[0]),
    },
    condition: { sql: "changes() = 1", bindings: [] },
  });
  const results = await db.batch([updateStmt, auditStmt,
    ...(body.examName !== undefined ? examNameCascades(db, id, examName, auditId) : [])]);
  if (results[0]?.meta?.changes === 0) {
    const exists = await db.prepare("SELECT 1 FROM exams WHERE id = ?").bind(id).first();
    if (!exists) return c.json({ success: false, error: "Exam not found" }, 404);
    return c.json({ success: false, error: "An exam with this name already exists here" }, 409);
  }
  if (results[0]?.meta?.changes !== 1 || results[1]?.meta?.changes !== 1) throw new Error("Exam update confirmation unavailable");
  return c.json({ success: true });
});

// PUT /api/exams/:id/image - Update image URL or base64 (Admin)
examRoutes.put("/:id/image", requirePermission("manage_exams"), async (c) => {
  const user = c.get("user");
  const id = c.req.param("id") || "";
  const body = await c.req.json().catch(() => ({}));
  const imageUrl = String(body.imageUrl || "").trim();
  const db = c.env.DB;

  const updateStmt = db.prepare("UPDATE exams SET image_url = ? WHERE id = ?").bind(imageUrl, id);
  const { stmt: auditStmt } = prepareAdminAuditLog(db, user, {
    actionType: "EXAM_IMAGE_UPDATED",
    description: `Updated image for exam '${id}'`,
    targetType: "exam",
    targetId: id,
  });

  await db.batch([updateStmt, auditStmt]);
  return c.json({ success: true });
});

// PUT /api/exams/:id/rename - Rename exam with cascading updates (Admin)
examRoutes.put("/:id/rename", requirePermission("manage_exams"), async (c) => {
  const user = c.get("user");
  const id = c.req.param("id") || "";
  const body = await c.req.json().catch(() => ({}));
  // Canonical newName plus compatibility for already-installed Android clients.
  const rawName = body && !Array.isArray(body) && typeof body === "object"
    ? (body.newName !== undefined ? body.newName : body.examName) : undefined;
  if (typeof rawName !== "string" || !rawName.trim()) {
    return c.json({ success: false, error: "Provide a nonempty new exam name" }, 400);
  }
  const newName = rawName.trim();
  const db = c.env.DB;

  const { id: auditId, stmt: auditStmt } = prepareAdminAuditLog(db, user, {
    actionType: "EXAM_RENAMED",
    description: `Renamed exam '${id}' to '${newName}'`,
    targetType: "exam",
    targetId: id,
    metadata: { newName },
    // Immediately follows the target UPDATE in this atomic batch.
    condition: { sql: "changes() = 1", bindings: [] },
  });

  const destination = examDestinationGuard(newName);
  const results = await db.batch([
    db.prepare(`UPDATE exams SET exam_name = ? WHERE id = ? AND ${destination.sql}`)
      .bind(newName, id, ...destination.bindings),
    auditStmt,
    ...examNameCascades(db, id, newName, auditId),
  ]);
  if (results[0]?.meta?.changes === 0) {
    const exists = await db.prepare("SELECT 1 FROM exams WHERE id = ?").bind(id).first();
    if (!exists) return c.json({ success: false, error: "Exam not found" }, 404);
    return c.json({ success: false, error: "An exam with this name already exists here" }, 409);
  }
  if (results[0]?.meta?.changes !== 1 || results[1]?.meta?.changes !== 1) {
    throw new Error("Exam rename confirmation unavailable");
  }

  return c.json({ success: true });
});

// DELETE /api/exams/:id - Delete exam and associated test data (Admin)
examRoutes.get("/:id/deletion-info", requirePermission("manage_exams"), async (c) => {
  const id = c.req.param("id") || "";
  if (!await c.env.DB.prepare("SELECT id FROM exams WHERE id = ?").bind(id).first()) return c.json({ success: false, error: "Record not found" }, 404);
  return c.json({ success: true, data: await deletionInfo(c.env.DB, id, 'exam') });
});

examRoutes.post('/:id/unpublish-tests', requirePermission("manage_exams"), async (c) => {
  const db = c.env.DB;
  const id = c.req.param('id') || '';
  if (!id || !await db.prepare('SELECT id FROM exams WHERE id = ?').bind(id).first()) {
    return c.json({ success: false, error: 'Exam not found' }, 404);
  }
  // Keep the existing active-session and saved-attempt data unchanged.
  const pause = db.prepare("UPDATE generated_tests SET status = 'paused' WHERE exam_id = ? AND EXISTS (SELECT 1 FROM exams WHERE id = ?)").bind(id, id);
  const disable = db.prepare('UPDATE exams SET auto_generation_enabled = 0 WHERE id = ?').bind(id);
  const { stmt: audit } = prepareAdminAuditLog(db, c.get('user'), {
    actionType: 'EXAM_TESTS_UNPUBLISHED',
    description: `Unpublished tests and disabled auto generation for exam '${id}'`,
    targetType: 'exam',
    targetId: id,
    condition: { sql: 'changes() = 1', bindings: [] },
  });
  const results = await runBatch(db, [pause, disable, audit]);
  if (results[1]?.meta?.changes !== 1 || results[2]?.meta?.changes !== 1) {
    return c.json({ success: false, error: 'Exam changed during unpublication; reload and retry' }, 409);
  }
  return c.json({ success: true, message: 'Tests unpublished and automatic generation disabled. History and ongoing sessions preserved.' });
});

examRoutes.delete("/:id", requirePermission("manage_exams"), async (c) => {
  const user = c.get("user");
  const id = c.req.param("id") || "";
  const db = c.env.DB;

  if (!await db.prepare("SELECT id FROM exams WHERE id = ?").bind(id).first()) return c.json({ success: false, error: "Record not found" }, 404);
  const dependencies = await deletionInfo(db, id, 'exam');
  if (!dependencies.canDelete) return c.json({ success: false, error: dependencies.message, code: dependencies.code, dependencies, requestId: c.res.headers.get('X-Request-ID') }, 409);

  const exam = await db.prepare("SELECT syllabus_url FROM exams WHERE id = ?").bind(id).first<ExamRow>();
  if (!await deleteUnused(db, id, 'exam', user)) {
    const current = await deletionInfo(db, id, 'exam');
    return c.json({ success: false, error: current.message || 'Record changed. Refresh before retrying.', code: current.code || 'DELETE_RECORD_CHANGED', dependencies: current, requestId: c.res.headers.get('X-Request-ID') }, 409);
  }

  const path = exam?.syllabus_url?.split('/eve-media/')[1];
  if (path && !await cleanupMedia(c.env, path, c.res.headers.get('X-Request-ID'))) {
    return c.json({ success: true, message: 'Exam deleted. Media cleanup was not completed; check System Monitor.' });
  }

  return c.json({ success: true });
});

// POST /api/exams/:id/syllabus - Upload syllabus PDF (Admin)
examRoutes.post("/:id/syllabus", requirePermission("manage_syllabus"), async (c) => {
  const id = c.req.param("id") || "";
  const contentType = c.req.header("content-type") || "";
  const db = c.env.DB;
  if (!id || !await db.prepare("SELECT id FROM exams WHERE id = ?").bind(id).first()) {
    return c.json({ success: false, error: "Exam not found" }, 404);
  }

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
    if (typeof body.base64 !== "string" ||
        body.base64.length > Math.ceil(20 * 1024 * 1024 * 4 / 3) + 4) {
      return c.json({ success: false, error: "Invalid or oversized PDF payload" }, 400);
    }
    let binary: string;
    try { binary = atob(body.base64); }
    catch { return c.json({ success: false, error: "Invalid PDF encoding" }, 400); }
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
    pdfBytes = bytes.buffer;
  }

  // Reject malformed/oversized bytes before uploading any remote object.
  if (!validSyllabusPdf(new Uint8Array(pdfBytes))) {
    return c.json({ success: false, error: "Invalid PDF document or exceeds 20MB limit" }, 400);
  }
  fileName = safeSyllabusFilename(fileName);

  const storage = new SupabaseStorage(c.env);
  // Non-user-controlled keys avoid path injection and timestamp collisions.
  const path = `syllabi/${crypto.randomUUID()}.pdf`;
  const downloadUrl = await storage.uploadFile(path, pdfBytes, "application/pdf");

  const updateStmt = db
    .prepare("UPDATE exams SET syllabus_url = ?, syllabus_file_name = ? WHERE id = ?")
    .bind(downloadUrl, fileName, id);

  const user = c.get("user");
  const { stmt: auditStmt } = prepareAdminAuditLog(db, user, {
    actionType: "SYLLABUS_UPDATED",
    description: `Uploaded syllabus '${fileName}' for exam '${id}'`,
    targetType: "exam",
    targetId: id,
    metadata: { fileName },
  });

  try {
    const results = await db.batch([updateStmt, auditStmt]);
    if ((results[0]?.meta?.changes ?? 0) !== 1) {
      throw new Error("Exam changed before syllabus upload was confirmed");
    }
  } catch {
    // Remote upload can succeed before D1 rejects the mutation.
    // Existing cleanup monitor records any failed compensation.
    await cleanupMedia(c.env, path, c.res.headers.get("X-Request-ID"));
    return c.json({ success: false, error: "Syllabus not linked; verify exam state before retrying" }, 409);
  }

  return c.json({ success: true, data: { syllabusUrl: downloadUrl, syllabusFileName: fileName } });
});

// DELETE /api/exams/:id/syllabus - Remove syllabus PDF (Admin)
examRoutes.delete("/:id/syllabus", requirePermission("manage_syllabus"), async (c) => {
  const user = c.get("user");
  const id = c.req.param("id") || "";
  const db = c.env.DB;

  const exam = await db.prepare("SELECT syllabus_url FROM exams WHERE id = ?").bind(id).first<ExamRow>();
  if (!exam) return c.json({ success: false, error: 'Exam not found' }, 404);

  const updateStmt = db.prepare("UPDATE exams SET syllabus_url = '', syllabus_file_name = '' WHERE id = ?").bind(id);
  const { stmt: auditStmt } = prepareAdminAuditLog(db, user, {
    actionType: "SYLLABUS_DELETED",
    description: `Removed syllabus for exam '${id}'`,
    targetType: "exam",
    targetId: id,
  });

  await db.batch([updateStmt, auditStmt]);

  const path = exam.syllabus_url?.split('/eve-media/')[1];
  if (path && !await cleanupMedia(c.env, path, c.res.headers.get('X-Request-ID'))) {
    return c.json({ success: true, message: 'Syllabus removed from the exam. Media cleanup was not completed; check System Monitor.' });
  }
  return c.json({ success: true });
});
