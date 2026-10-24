// ============================================================================
// Attempt Submission, Results History, Mistakes, Streak, and Anti-Cheat Locking
// ============================================================================

import { parseAndValidateQuestions } from "../ai/generator";
import { Hono } from "hono";
import { AttemptAnswerRow, AttemptRow, AttemptSessionRow, AuthUser, Env, ExamRow, GeneratedTestRow, QuestionRow } from "../types";
import { hideAnswers } from "../util/clientProtocol";
import { isUserPremium } from "./premium";

export const attemptRoutes = new Hono<{ Bindings: Env; Variables: { user: AuthUser } }>();

export const ATTEMPT_KEY_SEP = "__";

export function parseAttemptKey(key: string): { sourceExamId: string; generatedTestId: string | null } {
  const trimmed = (key || "").trim();
  if (trimmed.includes(ATTEMPT_KEY_SEP)) {
    const parts = trimmed.split(ATTEMPT_KEY_SEP);
    const sourceExamId = parts[0] || trimmed;
    const generatedTestId = parts.slice(1).join(ATTEMPT_KEY_SEP).trim();
    return { sourceExamId, generatedTestId: generatedTestId || null };
  }
  return { sourceExamId: trimmed, generatedTestId: null };
}

export function canonicalGeneratedQuestionId(testId: string, index: number): string {
  return `${testId.trim()}_${index}`;
}

export function parseGeneratedQuestionId(questionId: string): { testId: string; index: number } | null {
  const trimmed = (questionId || "").trim();
  const lastUnderscore = trimmed.lastIndexOf("_");
  if (lastUnderscore <= 0) return null;
  const testId = trimmed.substring(0, lastUnderscore);
  const idxStr = trimmed.substring(lastUnderscore + 1);
  const index = parseInt(idxStr, 10);
  if (!/^\d+$/.test(idxStr) || !Number.isSafeInteger(index) || index < 0) return null;
  return { testId, index };
}

const VALID_OPTIONS = new Set(["A", "B", "C", "D"]);

function questionOptionText(q: any, letter: string): string {
  switch (letter) {
    case "A": return q.option_a || q.optionA || "";
    case "B": return q.option_b || q.optionB || "";
    case "C": return q.option_c || q.optionC || "";
    case "D": return q.option_d || q.optionD || "";
    default: return "";
  }
}

function questionOptionTextHi(q: any, letter: string): string {
  switch (letter) {
    case "A": return q.option_a_hi || q.optionAHi || "";
    case "B": return q.option_b_hi || q.optionBHi || "";
    case "C": return q.option_c_hi || q.optionCHi || "";
    case "D": return q.option_d_hi || q.optionDHi || "";
    default: return "";
  }
}

// POST /api/attempts/start - Begin test attempt session & fetch generated questions
attemptRoutes.post("/start", async (c) => {
  const user = c.get("user");
  const uid = user.uid;
  const db = c.env.DB;
  const body = await c.req.json().catch(() => ({}));
  let examId = String(body.examId || "").trim();

  if (!examId) {
    return c.json({ success: false, error: "examId is required" }, 400);
  }

  if (!examId.includes(ATTEMPT_KEY_SEP)) {
    const direct = await db.prepare("SELECT exam_id FROM generated_tests WHERE id = ?").bind(examId).first<{ exam_id: string }>();
    if (direct) examId = `${direct.exam_id}${ATTEMPT_KEY_SEP}${examId}`;
  }
  let { sourceExamId, generatedTestId } = parseAttemptKey(examId);
  const resumeAttemptId = String(body.resumeAttemptId || '').trim();
  if (resumeAttemptId) {
    const confirmed = await db.prepare("SELECT id, exam_id, timestamp FROM attempts WHERE user_id = ? AND (client_attempt_id = ? OR server_session_id = ?)").bind(uid, resumeAttemptId, resumeAttemptId).first<{ id: string; exam_id: string; timestamp: number }>();
    if (confirmed) {
      if (confirmed.exam_id !== examId) return c.json({ success: false, error: "Saved session belongs to another test" }, 409);
      return c.json({ success: true, data: { alreadySubmitted: true, completedAttemptId: confirmed.id, startedAt: confirmed.timestamp, serverNow: Date.now(), timeLimitSeconds: 0, clientAttemptId: resumeAttemptId, questions: [] } });
    }
  }


  // Read exam row for time limit
  const examRow = await db
    .prepare("SELECT time_limit_minutes, negative_marking_value FROM exams WHERE id = ?")
    .bind(sourceExamId)
    .first<{ time_limit_minutes: number; negative_marking_value: number }>();

  if (!examRow) return c.json({ success: false, error: "Exam not found" }, 404);
  const limitSeconds = examRow && typeof examRow.time_limit_minutes === "number" ? examRow.time_limit_minutes * 60 : 0;

  // Non-admin with maximum attempts (3) reached -> 409
  // Premium users have unlimited eligible reattempts
  const lockKey = `${uid}_${examId}`;
  if (!user.isAdmin) {
    const isPremium = await isUserPremium(db, uid);
    if (!isPremium) {
      const countRow = await db
        .prepare("SELECT COUNT(*) as count FROM attempts WHERE user_id = ? AND exam_id = ?")
        .bind(uid, examId)
        .first<{ count: number }>();
      const completedCount = countRow ? countRow.count : 0;
      if (completedCount >= 3) {
        return c.json({ success: false, error: "You have reached the maximum limit of 3 attempts for this test." }, 409);
      }
    }
  }

  const activeSession = await db.prepare("SELECT * FROM attempt_sessions WHERE id = ?").bind(`${uid}_${examId}`).first<AttemptSessionRow>();
  let generatedTestRow: GeneratedTestRow | null = null;
  if (generatedTestId) {
    generatedTestRow = await db
      .prepare("SELECT * FROM generated_tests WHERE id = ?")
      .bind(generatedTestId)
      .first<GeneratedTestRow>();

    if (!generatedTestRow) {
      return c.json({ success: false, error: "Test not found" }, 404);
    }

    if (generatedTestRow.exam_id !== sourceExamId) {
      return c.json({ success: false, error: "Test does not belong to specified exam" }, 400);
    }

    if (!user.isAdmin && !activeSession && generatedTestRow.status !== "live" && generatedTestRow.status !== "published") {
      return c.json({ success: false, error: "Test is not available" }, 403);
    }

    const availableFrom = generatedTestRow.available_from || 0;
    if (!activeSession && availableFrom > Date.now()) {
      return c.json({ success: false, error: "Test not open yet", availableFrom }, 403);
    }
  } else {
    // Check if sourceExamId is directly a generated test ID
    const directTest = await db
      .prepare("SELECT * FROM generated_tests WHERE id = ?")
      .bind(sourceExamId)
      .first<GeneratedTestRow>();
    if (directTest) {
      generatedTestRow = directTest;
      generatedTestId = directTest.id;
    }
  }

  let snapshotQuestions: any[];
  if (activeSession?.questions_json && activeSession.questions_json !== '[]') {
    try {
      snapshotQuestions = JSON.parse(activeSession.questions_json);
      if (!Array.isArray(snapshotQuestions)) throw new Error();
    } catch { return c.json({ success: false, error: "Saved test session requires administrator repair" }, 409); }
  } else if (generatedTestRow) {
    try {
      const valid = parseAndValidateQuestions(generatedTestRow.questions_json);
      if (valid.length !== generatedTestRow.question_count) throw new Error();
      const stored = JSON.parse(generatedTestRow.questions_json);
      const list = Array.isArray(stored) ? stored : stored.questions;
      snapshotQuestions = list.map((q: any, index: number) => ({ ...q, ...valid[index], id: canonicalGeneratedQuestionId(generatedTestId!, index) }));
    } catch { return c.json({ success: false, error: "Test questions require administrator repair" }, 409); }
  } else {
    snapshotQuestions = (await db.prepare("SELECT * FROM questions WHERE exam_id = ? AND (is_pyq = 0 OR is_pyq IS NULL) ORDER BY RANDOM()").bind(sourceExamId).all<QuestionRow>()).results || [];
  }

  // Idempotent: INSERT OR IGNORE then SELECT
  const sessionId = `${uid}_${examId}`;
  const now = Date.now();

  await db
    .prepare(
      `INSERT OR IGNORE INTO attempt_sessions (
         id, user_id, exam_key, started_at, time_limit_seconds, accumulated_active_seconds, status, last_resumed_at, questions_json, negative_marking_value, session_instance_id
       ) SELECT ?, ?, ?, ?, ?, 0, 'RUNNING', ?, ?, ?, ? WHERE EXISTS (SELECT 1 FROM exams WHERE id = ?) AND (? = '' OR EXISTS (SELECT 1 FROM generated_tests WHERE id = ? AND exam_id = ?))`
    )
    .bind(sessionId, uid, examId, now, limitSeconds, now, JSON.stringify(snapshotQuestions), examRow.negative_marking_value ?? 0, crypto.randomUUID(), sourceExamId, generatedTestId || '', generatedTestId || '', sourceExamId)
    .run();

  await db.prepare("UPDATE attempt_sessions SET session_instance_id = ? WHERE id = ? AND session_instance_id = ''").bind(crypto.randomUUID(), sessionId).run();

  const session = await db
    .prepare("SELECT * FROM attempt_sessions WHERE id = ?")
    .bind(sessionId)
    .first<AttemptSessionRow>();

  if (!session) return c.json({ success: false, code: 'TEST_REMOVED', error: 'This test was removed before the attempt could start. Refresh the exam list.' }, 404);

  let startedAt = now;
  let timeLimitSeconds = limitSeconds;
  let accumulatedActiveSeconds = 0;
  let status = "RUNNING";
  let lastResumedAt = now;

  if (session) {
    startedAt = session.started_at;
    timeLimitSeconds = session.time_limit_seconds;
    accumulatedActiveSeconds = session.accumulated_active_seconds || 0;
    status = session.status || "RUNNING";
    lastResumedAt = session.last_resumed_at || 0;

    // If session was paused, auto-resume on start
    if (status === "PAUSED") {
      status = "RUNNING";
      lastResumedAt = now;
      await db
        .prepare("UPDATE attempt_sessions SET status = 'RUNNING', last_resumed_at = ? WHERE id = ?")
        .bind(now, sessionId)
        .run();
    } else if (lastResumedAt === 0) {
      lastResumedAt = startedAt;
      await db
        .prepare("UPDATE attempt_sessions SET last_resumed_at = ? WHERE id = ?")
        .bind(lastResumedAt, sessionId)
        .run();
    }
  }

  const activeSeconds =
    accumulatedActiveSeconds +
    (status === "RUNNING" && lastResumedAt > 0 ? Math.max(0, Math.floor((now - lastResumedAt) / 1000)) : 0);
  const remainingSeconds =
    timeLimitSeconds > 0 ? Math.max(0, timeLimitSeconds - activeSeconds) : 0;

  let questions: any[] | undefined = undefined;
  if (session?.questions_json && session.questions_json !== "[]") {
    let parsedQuestions: any[] = [];
    try {
      parsedQuestions = JSON.parse(session.questions_json);
    } catch (_e) {}

    const shouldHide = hideAnswers(c);
    questions = parsedQuestions.map((q, idx) => ({
      id: q.id || canonicalGeneratedQuestionId(generatedTestId!, idx),
      examId: sourceExamId,
      questionText: q.question_text || q.questionText || "",
      optionA: q.option_a || q.optionA || "",
      optionB: q.option_b || q.optionB || "",
      optionC: q.option_c || q.optionC || "",
      optionD: q.option_d || q.optionD || "",
      correctAnswer: shouldHide ? "" : String(q.correct_answer || q.correctAnswer || "").toUpperCase(),
      explanation: shouldHide ? "" : String(q.explanation || ""),
      topic: String(q.topic || ""),
      questionTextHi: q.question_text_hi || q.questionTextHi || "",
      optionAHi: q.option_a_hi || q.optionAHi || "",
      optionBHi: q.option_b_hi || q.optionBHi || "",
      optionCHi: q.option_c_hi || q.optionCHi || "",
      optionDHi: q.option_d_hi || q.optionDHi || "",
      explanationHi: shouldHide ? "" : String(q.explanation_hi || q.explanationHi || ""),
    }));
  }

  return c.json({
    success: true,
    data: {
      startedAt,
      clientAttemptId: session?.session_instance_id || "",
      serverNow: Date.now(),
      timeLimitSeconds,
      remainingSeconds,
      activeSeconds,
      ...(questions !== undefined ? { questions } : {}),
    },
  });
});

// POST /api/attempts/pause - Pause an active test session
attemptRoutes.post("/pause", async (c) => {
  const user = c.get("user");
  const uid = user.uid;
  const db = c.env.DB;
  const body = await c.req.json().catch(() => ({}));
  const examId = String(body.examId || "").trim();

  if (!examId) {
    return c.json({ success: false, error: "Missing examId" }, 400);
  }

  const sessionId = `${uid}_${examId}`;
  const session = await db
    .prepare("SELECT * FROM attempt_sessions WHERE id = ?")
    .bind(sessionId)
    .first<AttemptSessionRow>();

  if (!session) {
    return c.json({ success: true, data: { status: "PAUSED", accumulatedActiveSeconds: 0 } });
  }

  const now = Date.now();
  let accumulated = session.accumulated_active_seconds || 0;
  if (session.status !== "PAUSED") {
    const lastResumed = session.last_resumed_at || session.started_at || now;
    accumulated += Math.max(0, Math.floor((now - lastResumed) / 1000));
    await db
      .prepare(
        "UPDATE attempt_sessions SET status = 'PAUSED', accumulated_active_seconds = ?, last_resumed_at = 0 WHERE id = ?"
      )
      .bind(accumulated, sessionId)
      .run();
  }

  return c.json({
    success: true,
    data: {
      status: "PAUSED",
      accumulatedActiveSeconds: accumulated,
    },
  });
});

// POST /api/attempts/resume - Resume a paused test session
attemptRoutes.post("/resume", async (c) => {
  const user = c.get("user");
  const uid = user.uid;
  const db = c.env.DB;
  const body = await c.req.json().catch(() => ({}));
  const examId = String(body.examId || "").trim();

  if (!examId) {
    return c.json({ success: false, error: "Missing examId" }, 400);
  }

  const sessionId = `${uid}_${examId}`;
  const session = await db
    .prepare("SELECT * FROM attempt_sessions WHERE id = ?")
    .bind(sessionId)
    .first<AttemptSessionRow>();

  if (!session) {
    return c.json({ success: false, error: "Session not found" }, 404);
  }

  const now = Date.now();
  if (session.status === "PAUSED") {
    await db
      .prepare("UPDATE attempt_sessions SET status = 'RUNNING', last_resumed_at = ? WHERE id = ?")
      .bind(now, sessionId)
      .run();
  }

  const accumulated = session.accumulated_active_seconds || 0;
  const remainingSeconds =
    session.time_limit_seconds > 0 ? Math.max(0, session.time_limit_seconds - accumulated) : 0;

  return c.json({
    success: true,
    data: {
      status: "RUNNING",
      activeSeconds: accumulated,
      remainingSeconds,
    },
  });
});

// POST /api/attempts/submit - Server-side trusted test grading & atomic submission
attemptRoutes.post("/submit", async (c) => {
  const user = c.get("user");
  const uid = user.uid;
  const db = c.env.DB;
  const body = await c.req.json().catch(() => ({}));

  const examId = String(body.examId || "").trim();
  const examName = String(body.examName || "Test").trim();
  const category = String(body.category || "").trim();
  const displayName = String(body.displayName || user.displayName || "Student").trim();
  const clientAttemptId = String(body.clientAttemptId || "").trim();
  const rawAnswers: any[] = Array.isArray(body.answers) ? body.answers : [];

  if (!examId || !Array.isArray(body.answers) || rawAnswers.length > 300 || clientAttemptId.length > 100 || (c.req.header("X-Eve-Client") === "2" && !clientAttemptId)) {
    return c.json({ success: false, error: "Invalid attempt payload" }, 400);
  }

  if (body.expectedUid && body.expectedUid !== uid) return c.json({ success: false, error: "Sign in as the student who started this attempt" }, 403);

  let serverSessionId = "";
  // 1. Idempotency Check: if clientAttemptId exists, return cached result
  const cachedResult = async () => {
  if (clientAttemptId) {
    const existing = await db
      .prepare("SELECT * FROM attempts WHERE user_id = ? AND (client_attempt_id = ? OR server_session_id = ?)")
      .bind(uid, clientAttemptId, serverSessionId || clientAttemptId)
      .first<AttemptRow>();

    if (existing) {
      if (existing.exam_id !== examId) return c.json({ success: false, error: "Attempt ID belongs to another test" }, 409);
      const { results: answers } = await db
        .prepare("SELECT * FROM attempt_answers WHERE attempt_id = ? ORDER BY question_number ASC")
        .bind(existing.id)
        .all<AttemptAnswerRow>();

      return c.json({
        success: true,
        data: {
          attemptId: existing.id,
          score: existing.score,
          total: existing.total,
          correct: existing.correct,
          wrong: existing.wrong,
          unattempted: existing.unattempted,
          timeTakenSeconds: existing.time_taken_seconds || 0,
          counted: existing.counted !== undefined ? existing.counted : 1,
          answers: (answers || []).map((a) => ({
            questionId: a.question_id,
            number: a.question_number,
            questionText: a.question_text,
            selected: a.selected,
            selectedText: a.selected_text,
            correct: a.correct,
            correctText: a.correct_text,
            explanation: a.explanation,
            isBookmarked: Boolean(a.is_bookmarked),
            topic: a.topic,
            questionTextHi: a.question_text_hi,
            selectedTextHi: a.selected_text_hi,
            correctTextHi: a.correct_text_hi,
            explanationHi: a.explanation_hi,
            timeTakenSeconds: a.time_taken_seconds || 0,
          })),
        },
      });
    }
  }

    return null;
  };
  const cached = await cachedResult();
  if (cached) return cached;

  const isPractice = Boolean(body.practice || body.topic || body.pyqYear);
  const lockKey = `${uid}_${examId}`;

  // 2. Anti-cheat lock check: Max 3 attempts for non-admins on non-practice tests
  // Premium users have unlimited eligible reattempts
  if (!isPractice && !user.isAdmin) {
    const isPremium = await isUserPremium(db, uid);
    if (!isPremium) {
      const countRow = await db
        .prepare("SELECT COUNT(*) as count FROM attempts WHERE user_id = ? AND exam_id = ?")
        .bind(uid, examId)
        .first<{ count: number }>();
      const completedCount = countRow ? countRow.count : 0;
      if (completedCount >= 3) {
        return c.json({ success: false, error: "You have reached the maximum limit of 3 attempts for this test." }, 409);
      }
    }
  }

  const { sourceExamId, generatedTestId } = parseAttemptKey(examId);

  const sessionId = `${uid}_${examId}`;
  const session = await db.prepare("SELECT * FROM attempt_sessions WHERE id = ?").bind(sessionId).first<AttemptSessionRow>();
  serverSessionId = session?.session_instance_id || "";
  if (serverSessionId) {
    const confirmed = await cachedResult();
    if (confirmed) return confirmed;
  }


  // 3. Build EXPECTED question set
  type ExpectedQ = {
    id: string;
    questionText: string;
    questionTextHi: string;
    optionA: string;
    optionB: string;
    optionC: string;
    optionD: string;
    optionAHi: string;
    optionBHi: string;
    optionCHi: string;
    optionDHi: string;
    correctAnswer: string;
    explanation: string;
    explanationHi: string;
    topic: string;
    defaultNumber: number;
  };

  const expectedQuestions: ExpectedQ[] = [];
  const expectedMap = new Map<string, ExpectedQ>();

  if (!isPractice && session?.questions_json && session.questions_json !== '[]') {
    let snapshot: any[];
    try { snapshot = JSON.parse(session.questions_json); if (!Array.isArray(snapshot)) throw new Error(); }
    catch { return c.json({ success: false, error: "The saved test session needs administrator repair" }, 409); }
    snapshot.forEach((q, index) => {
      const entry: ExpectedQ = {
        id: q.id, defaultNumber: index + 1,
        questionText: q.questionText || q.question_text || '', questionTextHi: q.questionTextHi || q.question_text_hi || '',
        optionA: q.optionA || q.option_a || '', optionB: q.optionB || q.option_b || '',
        optionC: q.optionC || q.option_c || '', optionD: q.optionD || q.option_d || '',
        optionAHi: q.optionAHi || q.option_a_hi || '', optionBHi: q.optionBHi || q.option_b_hi || '',
        optionCHi: q.optionCHi || q.option_c_hi || '', optionDHi: q.optionDHi || q.option_d_hi || '',
        correctAnswer: String(q.correctAnswer || q.correct_answer || '').toUpperCase(),
        explanation: q.explanation || '', explanationHi: q.explanationHi || q.explanation_hi || '', topic: q.topic || '',
      };
      expectedQuestions.push(entry); expectedMap.set(entry.id, entry);
    });
  } else if (generatedTestId) {
    const gRow = await db
      .prepare("SELECT id, exam_id, questions_json, status, available_from FROM generated_tests WHERE id = ?")
      .bind(generatedTestId)
      .first<{ id: string; exam_id: string; questions_json: string; status: string; available_from: number }>();

    if (!gRow) {
      return c.json({ success: false, error: "Generated test not found" }, 400);
    }

    if (gRow.exam_id !== sourceExamId) {
      return c.json({ success: false, error: "Test does not belong to specified exam" }, 400);
    }

    if (!user.isAdmin && !session && (!['live', 'published'].includes(gRow.status) || (gRow.available_from || 0) > Date.now())) {
      return c.json({ success: false, error: "Test is not available" }, 403);
    }

    let parsedQuestions: any[] = [];
    try {
      parseAndValidateQuestions(gRow.questions_json);
      parsedQuestions = JSON.parse(gRow.questions_json);
      if (!Array.isArray(parsedQuestions)) parsedQuestions = JSON.parse(gRow.questions_json).questions;
    } catch { return c.json({ success: false, error: "Test questions require administrator repair" }, 409); }

    parsedQuestions.forEach((q, idx) => {
      const qId = canonicalGeneratedQuestionId(generatedTestId!, idx);
      const entry: ExpectedQ = {
        id: qId,
        questionText: q.question_text || q.questionText || "",
        questionTextHi: q.question_text_hi || q.questionTextHi || "",
        optionA: q.option_a || q.optionA || "",
        optionB: q.option_b || q.optionB || "",
        optionC: q.option_c || q.optionC || "",
        optionD: q.option_d || q.optionD || "",
        optionAHi: q.option_a_hi || q.optionAHi || "",
        optionBHi: q.option_b_hi || q.optionBHi || "",
        optionCHi: q.option_c_hi || q.optionCHi || "",
        optionDHi: q.option_d_hi || q.optionDHi || "",
        correctAnswer: String(q.correct_answer || q.correctAnswer || "").toUpperCase(),
        explanation: String(q.explanation || ""),
        explanationHi: String(q.explanation_hi || q.explanationHi || ""),
        topic: String(q.topic || ""),
        defaultNumber: idx + 1,
      };
      expectedQuestions.push(entry);
      expectedMap.set(qId, entry);
    });
  } else if (isPractice) {
    let qSql = "SELECT * FROM questions WHERE exam_id = ?";
    const qParams: any[] = [sourceExamId];

    if (body.topic) {
      qSql += " AND (is_pyq = 0 OR is_pyq IS NULL) AND LOWER(TRIM(topic)) = LOWER(TRIM(?))";
      qParams.push(String(body.topic).trim());
    } else if (body.pyqYear) {
      qSql += " AND is_pyq = 1 AND pyq_year = ?";
      qParams.push(parseInt(body.pyqYear, 10));
      if (body.pyqPaper) {
        qSql += " AND LOWER(TRIM(pyq_paper)) = LOWER(TRIM(?))";
        qParams.push(String(body.pyqPaper).trim());
      }
    }

    const { results: dbQs } = await db.prepare(qSql).bind(...qParams).all<QuestionRow>();
    (dbQs || []).forEach((q, idx) => {
      const entry: ExpectedQ = {
        id: q.id,
        questionText: q.question_text,
        questionTextHi: q.question_text_hi || "",
        optionA: q.option_a,
        optionB: q.option_b,
        optionC: q.option_c,
        optionD: q.option_d,
        optionAHi: q.option_a_hi || "",
        optionBHi: q.option_b_hi || "",
        optionCHi: q.option_c_hi || "",
        optionDHi: q.option_d_hi || "",
        correctAnswer: String(q.correct_answer || "").toUpperCase(),
        explanation: q.explanation || "",
        explanationHi: q.explanation_hi || "",
        topic: q.topic || "",
        defaultNumber: idx + 1,
      };
      expectedQuestions.push(entry);
      expectedMap.set(q.id, entry);
    });
  } else {
    // Plain non-practice mock test: load mock questions (is_pyq = 0 or null)
    const { results: dbQs } = await db
      .prepare("SELECT * FROM questions WHERE exam_id = ? AND (is_pyq = 0 OR is_pyq IS NULL)")
      .bind(sourceExamId)
      .all<QuestionRow>();

    const candidateGenTestId = rawAnswers
      .map((a) => String(a?.questionId || "").trim())
      .map((id) => parseGeneratedQuestionId(id)?.testId)
      .find((id) => Boolean(id));

    if ((!dbQs || dbQs.length === 0 || candidateGenTestId) && expectedMap.size === 0) {
      let gRow: { questions_json: string; id: string } | null = null;
      if (candidateGenTestId) {
        gRow = await db
          .prepare("SELECT id, questions_json FROM generated_tests WHERE id = ? AND exam_id = ?")
          .bind(candidateGenTestId, sourceExamId)
          .first<{ id: string; questions_json: string }>();
      }
      if (!gRow) {
        gRow = await db
          .prepare(
            "SELECT id, questions_json FROM generated_tests WHERE exam_id = ? AND status IN ('live', 'published') ORDER BY generated_at DESC LIMIT 1"
          )
          .bind(sourceExamId)
          .first<{ id: string; questions_json: string }>();
      }

      if (gRow) {
        let parsedQuestions: any[] = [];
        try {
          parseAndValidateQuestions(gRow.questions_json);
          parsedQuestions = JSON.parse(gRow.questions_json);
          if (!Array.isArray(parsedQuestions)) parsedQuestions = JSON.parse(gRow.questions_json).questions;
        } catch { return c.json({ success: false, error: "Test questions require administrator repair" }, 409); }

        parsedQuestions.forEach((q, idx) => {
          const qId = canonicalGeneratedQuestionId(gRow!.id, idx);
          const entry: ExpectedQ = {
            id: qId,
            questionText: q.question_text || q.questionText || "",
            questionTextHi: q.question_text_hi || q.questionTextHi || "",
            optionA: q.option_a || q.optionA || "",
            optionB: q.option_b || q.optionB || "",
            optionC: q.option_c || q.optionC || "",
            optionD: q.option_d || q.optionD || "",
            optionAHi: q.option_a_hi || q.optionAHi || "",
            optionBHi: q.option_b_hi || q.optionBHi || "",
            optionCHi: q.option_c_hi || q.optionCHi || "",
            optionDHi: q.option_d_hi || q.optionDHi || "",
            correctAnswer: String(q.correct_answer || q.correctAnswer || "").toUpperCase(),
            explanation: String(q.explanation || ""),
            explanationHi: String(q.explanation_hi || q.explanationHi || ""),
            topic: String(q.topic || ""),
            defaultNumber: idx + 1,
          };
          expectedQuestions.push(entry);
          expectedMap.set(qId, entry);
        });
      }
    }

    if (expectedMap.size === 0 && dbQs) {
      (dbQs || []).forEach((q, idx) => {
        const entry: ExpectedQ = {
          id: q.id,
          questionText: q.question_text,
          questionTextHi: q.question_text_hi || "",
          optionA: q.option_a,
          optionB: q.option_b,
          optionC: q.option_c,
          optionD: q.option_d,
          optionAHi: q.option_a_hi || "",
          optionBHi: q.option_b_hi || "",
          optionCHi: q.option_c_hi || "",
          optionDHi: q.option_d_hi || "",
          correctAnswer: String(q.correct_answer || "").toUpperCase(),
          explanation: q.explanation || "",
          explanationHi: q.explanation_hi || "",
          topic: q.topic || "",
          defaultNumber: idx + 1,
        };
        expectedQuestions.push(entry);
        expectedMap.set(q.id, entry);
      });
    }
  }

  if (expectedQuestions.some((q) => !VALID_OPTIONS.has(q.correctAnswer))) return c.json({ success: false, error: "Test has an invalid answer key. Contact an administrator." }, 409);

  // Filter student picks against expected question set
  const submittedPicksMap = new Map<
    string,
    { number: number; selected: string; isBookmarked: boolean; timeTakenSeconds: number }
  >();

  for (const a of rawAnswers) {
    const qId = String(a?.questionId || "").trim();
    if (!qId || !expectedMap.has(qId) || submittedPicksMap.has(qId)) return c.json({ success: false, error: "Unknown or duplicate question ID" }, 400);

    const selectedRaw = String(a?.selected || "").trim().toUpperCase();
    if (selectedRaw && !VALID_OPTIONS.has(selectedRaw)) return c.json({ success: false, error: "Invalid answer option" }, 400);
    const timeTaken = Math.max(0, Math.min(7200, parseInt(a?.timeTakenSeconds, 10) || 0));

    submittedPicksMap.set(qId, {
      number: expectedMap.get(qId)!.defaultNumber,
      selected: VALID_OPTIONS.has(selectedRaw) ? selectedRaw : "",
      isBookmarked: Boolean(a?.isBookmarked),
      timeTakenSeconds: timeTaken,
    });
  }

  if (expectedMap.size === 0) {
    return c.json({ success: false, error: "No questions found for this exam" }, 400);
  }

  if (rawAnswers.length > 0 && submittedPicksMap.size === 0) {
    return c.json({ success: false, error: "No valid answers in payload" }, 400);
  }

  if (!isPractice && !user.isAdmin && !session) return c.json({ success: false, error: "No active session. Reopen the test before submitting." }, 409);

  // 4. Timing & Counted logic

  const examRow = await db
    .prepare("SELECT time_limit_minutes, negative_marking_value FROM exams WHERE id = ?")
    .bind(sourceExamId)
    .first<{ time_limit_minutes: number; negative_marking_value: number }>();

  const limitSeconds = session
    ? session.time_limit_seconds
    : examRow && typeof examRow.time_limit_minutes === "number"
    ? examRow.time_limit_minutes * 60
    : 0;

  const clampTimeMax = limitSeconds > 0 ? limitSeconds : 7200;

  let counted = 1;
  let attemptTimeTakenSeconds = 0;
  const now = Date.now();

  if (isPractice) {
    counted = 0;
    // Practice: sum of individual pick times clamped
    let totalPicksTime = 0;
    for (const pick of submittedPicksMap.values()) {
      totalPicksTime += Math.min(clampTimeMax, pick.timeTakenSeconds);
    }
    attemptTimeTakenSeconds = totalPicksTime;
  } else if (user.isAdmin) {
    counted = 1;
    if (session) {
      let activeElapsedSeconds = session.accumulated_active_seconds || 0;
      const lastResumedAt = session.last_resumed_at || 0;
      if (session.status === "RUNNING" && lastResumedAt > 0) {
        activeElapsedSeconds += Math.max(0, Math.floor((now - lastResumedAt) / 1000));
      } else if (!lastResumedAt && session.started_at > 0 && !session.accumulated_active_seconds) {
        activeElapsedSeconds = Math.max(0, Math.floor((now - session.started_at) / 1000));
      }
      attemptTimeTakenSeconds = Math.max(0, Math.min(clampTimeMax, activeElapsedSeconds));
    } else {
      let totalPicksTime = 0;
      for (const pick of submittedPicksMap.values()) {
        totalPicksTime += Math.min(clampTimeMax, pick.timeTakenSeconds);
      }
      attemptTimeTakenSeconds = totalPicksTime;
    }
  } else {
    // Non-practice student
    if (!session) {
      counted = 0;
      let totalPicksTime = 0;
      for (const pick of submittedPicksMap.values()) {
        totalPicksTime += Math.min(clampTimeMax, pick.timeTakenSeconds);
      }
      attemptTimeTakenSeconds = totalPicksTime;
    } else {
      let activeElapsedSeconds = session.accumulated_active_seconds || 0;
      const lastResumedAt = session.last_resumed_at || 0;
      if (session.status === "RUNNING" && lastResumedAt > 0) {
        activeElapsedSeconds += Math.max(0, Math.floor((now - lastResumedAt) / 1000));
      } else if (!lastResumedAt && session.started_at > 0 && !session.accumulated_active_seconds) {
        activeElapsedSeconds = Math.max(0, Math.floor((now - session.started_at) / 1000));
      }
      attemptTimeTakenSeconds = Math.max(0, Math.min(clampTimeMax, activeElapsedSeconds));

      if (session.time_limit_seconds > 0 && activeElapsedSeconds > session.time_limit_seconds + 60) {
        counted = 0; // Exceeded grace period on active test time
      } else {
        counted = 1;
      }
    }
  }

  // 5. Evaluate answers
  // For non-practice: total = size of EXPECTED set (missing questions count as unattempted)
  // For practice: total = number of valid picks submitted
  const questionsToEvaluate: Array<{ q: ExpectedQ; pick?: { number: number; selected: string; isBookmarked: boolean; timeTakenSeconds: number } }> = [];

  if (isPractice) {
    for (const [qId, pick] of submittedPicksMap.entries()) {
      const q = expectedMap.get(qId)!;
      questionsToEvaluate.push({ q, pick });
    }
  } else {
    for (const q of expectedQuestions) {
      const pick = submittedPicksMap.get(q.id);
      questionsToEvaluate.push({ q, pick });
    }
  }

  let correct = 0;
  let wrong = 0;
  let unattempted = 0;

  const answersData: Array<{
    questionId: string;
    number: number;
    questionText: string;
    selected: string;
    selectedText: string;
    correct: string;
    correctText: string;
    explanation: string;
    isBookmarked: boolean;
    topic: string;
    questionTextHi: string;
    selectedTextHi: string;
    correctTextHi: string;
    explanationHi: string;
    timeTakenSeconds: number;
  }> = [];

  for (let i = 0; i < questionsToEvaluate.length; i++) {
    const { q, pick } = questionsToEvaluate[i];
    const selected = pick ? pick.selected : "";
    const isBookmarked = pick ? pick.isBookmarked : false;
    const timeTaken = pick ? Math.min(clampTimeMax, pick.timeTakenSeconds) : 0;
    const number = pick && pick.number > 0 ? pick.number : i + 1;

    const attempted = selected.length > 0;
    const isCorrect = attempted && selected === q.correctAnswer;
    const isWrong = attempted && !isCorrect;

    if (!attempted) unattempted++;
    else if (isCorrect) correct++;
    else wrong++;

    answersData.push({
      questionId: q.id,
      number,
      questionText: q.questionText,
      selected,
      selectedText: selected ? questionOptionText(q, selected) : "",
      correct: q.correctAnswer,
      correctText: questionOptionText(q, q.correctAnswer),
      explanation: q.explanation,
      isBookmarked,
      topic: q.topic,
      questionTextHi: q.questionTextHi,
      selectedTextHi: selected ? questionOptionTextHi(q, selected) : "",
      correctTextHi: questionOptionTextHi(q, q.correctAnswer),
      explanationHi: q.explanationHi,
      timeTakenSeconds: timeTaken,
    });
  }

  const total = answersData.length;
  const negativeMarking = session?.negative_marking_value ?? (examRow && typeof examRow.negative_marking_value === "number" ? examRow.negative_marking_value : 0.0);
  const score = Math.round((correct - wrong * negativeMarking) * 100) / 100;
  const attemptId = crypto.randomUUID();

  const batchStatements: D1PreparedStatement[] = [];

  // 1. Lock test for non-practice ONLY when reaching maximum attempts (>= 3)
  if (!isPractice && !user.isAdmin) {
    const countRow = await db
      .prepare("SELECT COUNT(*) as count FROM attempts WHERE user_id = ? AND exam_id = ?")
      .bind(uid, examId)
      .first<{ count: number }>();
    const completedCount = countRow ? countRow.count : 0;
    if (completedCount + 1 >= 3) {
      batchStatements.push(
        db
          .prepare(
            "INSERT INTO attempt_locks (id, user_id, exam_id, timestamp, source) VALUES (?, ?, ?, ?, ?) ON CONFLICT(id) DO NOTHING"
          )
          .bind(lockKey, uid, examId, now, "submit")
      );
    }
  }

  // 2. Remove session row
  if (session) {
    batchStatements.push(db.prepare("DELETE FROM attempt_sessions WHERE id = ?").bind(sessionId));
  }

  // 3. Insert attempt
  batchStatements.push(
    db
      .prepare(
        `INSERT INTO attempts (id, user_id, display_name, exam_id, exam_name, category, score, total, correct, wrong, unattempted, timestamp, time_taken_seconds, client_attempt_id, counted, server_session_id)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
      )
      .bind(
        attemptId,
        uid,
        displayName,
        examId,
        examName,
        category,
        score,
        total,
        correct,
        wrong,
        unattempted,
        now,
        attemptTimeTakenSeconds,
        clientAttemptId,
        counted,
        serverSessionId
      )
  );

  // One JSON set insert avoids one query per answer and stays within free D1 query limits.
  const storedAnswers = answersData.map((answer) => ({ ...answer, id: crypto.randomUUID(), isBookmarked: answer.isBookmarked ? 1 : 0 }));
  batchStatements.push(db.prepare(`INSERT INTO attempt_answers (
    id, attempt_id, question_id, question_number, question_text, selected, selected_text,
    correct, correct_text, explanation, is_bookmarked, topic,
    question_text_hi, selected_text_hi, correct_text_hi, explanation_hi, time_taken_seconds
  ) SELECT json_extract(value, '$.id'), ?, json_extract(value, '$.questionId'),
    json_extract(value, '$.number'), json_extract(value, '$.questionText'), json_extract(value, '$.selected'),
    json_extract(value, '$.selectedText'), json_extract(value, '$.correct'), json_extract(value, '$.correctText'),
    json_extract(value, '$.explanation'), json_extract(value, '$.isBookmarked'), json_extract(value, '$.topic'),
    json_extract(value, '$.questionTextHi'), json_extract(value, '$.selectedTextHi'), json_extract(value, '$.correctTextHi'),
    json_extract(value, '$.explanationHi'), json_extract(value, '$.timeTakenSeconds') FROM json_each(?)`).bind(attemptId, JSON.stringify(storedAnswers)));

  // 5. Leaderboard, overall, and analytics ONLY when counted = 1
  if (counted === 1) {
    const lbKey = `${examId}_${uid}`;
    batchStatements.push(
      db
        .prepare(
          `INSERT INTO leaderboard (id, user_id, exam_id, exam_name, category, display_name, score, total, timestamp, time_taken_seconds)
           VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
           ON CONFLICT(id) DO UPDATE SET
             time_taken_seconds = CASE
               WHEN excluded.score > leaderboard.score THEN excluded.time_taken_seconds
               WHEN excluded.score = leaderboard.score AND (leaderboard.time_taken_seconds = 0 OR excluded.time_taken_seconds < leaderboard.time_taken_seconds) THEN excluded.time_taken_seconds
               ELSE leaderboard.time_taken_seconds END,
             score = CASE WHEN excluded.score > leaderboard.score THEN excluded.score ELSE leaderboard.score END,
             total = CASE
               WHEN excluded.score > leaderboard.score THEN excluded.total
               WHEN excluded.score = leaderboard.score AND (leaderboard.time_taken_seconds = 0 OR excluded.time_taken_seconds < leaderboard.time_taken_seconds) THEN excluded.total
               ELSE leaderboard.total END,
             display_name = excluded.display_name,
             timestamp = CASE
               WHEN excluded.score > leaderboard.score THEN excluded.timestamp
               WHEN excluded.score = leaderboard.score AND (leaderboard.time_taken_seconds = 0 OR excluded.time_taken_seconds < leaderboard.time_taken_seconds) THEN excluded.timestamp
               ELSE leaderboard.timestamp END`
        )
        .bind(lbKey, uid, examId, examName, category, displayName, score, total, now, attemptTimeTakenSeconds)
    );

    batchStatements.push(
      db
        .prepare(
          `INSERT INTO overall_leaderboard (user_id, display_name, score, total_score, total, tests_taken, total_correct, accuracy, timestamp)
           VALUES (?, ?, ?, ?, ?, 1, ?, ?, ?)
           ON CONFLICT(user_id) DO UPDATE SET
             display_name = excluded.display_name,
             score = ROUND(overall_leaderboard.score + excluded.score, 2),
             total_score = ROUND(overall_leaderboard.total_score + excluded.score, 2),
             total = overall_leaderboard.total + excluded.total,
             tests_taken = overall_leaderboard.tests_taken + 1,
             total_correct = overall_leaderboard.total_correct + excluded.total_correct,
             accuracy = CASE WHEN (overall_leaderboard.total + excluded.total) > 0
                             THEN ROUND(((overall_leaderboard.total_correct + excluded.total_correct) * 100.0) / (overall_leaderboard.total + excluded.total))
                             ELSE 0 END,
             timestamp = excluded.timestamp`
        )
        .bind(uid, displayName, score, score, total, correct, total > 0 ? Math.round((correct * 100) / total) : 0, now)
    );

    const examUserMarker = `${examId}_${uid}`;
    batchStatements.push(
      db
        .prepare(
          `INSERT INTO admin_analytics_exams (exam_id, exam_name, category, attempt_count, unique_users, last_attempt_at)
           VALUES (?, ?, ?, 1, 1, ?)
           ON CONFLICT(exam_id) DO UPDATE SET
             attempt_count = admin_analytics_exams.attempt_count + 1,
             last_attempt_at = excluded.last_attempt_at`
        )
        .bind(examId, examName, category, now)
    );

    batchStatements.push(
      db
        .prepare(
          `INSERT INTO admin_analytics_exam_users (id, exam_id, user_id, created_at)
           VALUES (?, ?, ?, ?)
           ON CONFLICT(id) DO NOTHING`
        )
        .bind(examUserMarker, examId, uid, now)
    );

    // Upsert admin_analytics_questions per evaluated answer
    const questionStats = answersData.map((answer) => ({
      ...answer, key: `${examId}_${answer.questionId}`,
      isCorrect: answer.selected !== '' && answer.selected === answer.correct ? 1 : 0,
      isWrong: answer.selected !== '' && answer.selected !== answer.correct ? 1 : 0,
      isUnattempted: answer.selected === '' ? 1 : 0,
    }));
    batchStatements.push(db.prepare(`INSERT INTO admin_analytics_questions (
      id, exam_id, exam_name, question_id, question_number, question_text, topic,
      attempts, correct, wrong, unattempted, total_time_seconds
    ) SELECT json_extract(value, '$.key'), ?, ?, json_extract(value, '$.questionId'),
      json_extract(value, '$.number'), json_extract(value, '$.questionText'), json_extract(value, '$.topic'),
      1, json_extract(value, '$.isCorrect'), json_extract(value, '$.isWrong'), json_extract(value, '$.isUnattempted'),
      json_extract(value, '$.timeTakenSeconds') FROM json_each(?) WHERE 1
    ON CONFLICT(id) DO UPDATE SET
      exam_name = excluded.exam_name, question_number = excluded.question_number,
      question_text = excluded.question_text, topic = excluded.topic,
      attempts = admin_analytics_questions.attempts + 1,
      correct = admin_analytics_questions.correct + excluded.correct,
      wrong = admin_analytics_questions.wrong + excluded.wrong,
      unattempted = admin_analytics_questions.unattempted + excluded.unattempted,
      total_time_seconds = admin_analytics_questions.total_time_seconds + excluded.total_time_seconds
    `).bind(examId, examName, JSON.stringify(questionStats)));
  }

  // D1 batch is one transaction: a failed answer/stat write rolls back the attempt too.
  // The existing unique (user_id, client_attempt_id) index serializes racing retries.
  try {
    await db.batch(batchStatements);
  } catch (err) {
    const winner = await cachedResult();
    if (winner) return winner;
    throw err;
  }

  return c.json({
    success: true,
    data: {
      attemptId,
      score,
      total,
      correct,
      wrong,
      unattempted,
      timeTakenSeconds: attemptTimeTakenSeconds,
      counted,
      answers: answersData,
    },
  });
});

// GET /api/attempts - List test attempts for user
attemptRoutes.get("/", async (c) => {
  const user = c.get("user");
  const db = c.env.DB;

  const { results: attempts } = await db
    .prepare("SELECT * FROM attempts WHERE user_id = ? ORDER BY timestamp DESC")
    .bind(user.uid)
    .all<AttemptRow>();

  if (!attempts || attempts.length === 0) {
    return c.json({ success: true, data: [] });
  }

  const { results: allAnswers } = await db
    .prepare(`SELECT aa.* FROM attempt_answers aa JOIN attempts a ON a.id = aa.attempt_id WHERE a.user_id = ? ORDER BY aa.question_number ASC`)
    .bind(user.uid)
    .all<AttemptAnswerRow>();

  const answersByAttempt = new Map<string, any[]>();
  for (const ans of allAnswers || []) {
    if (!answersByAttempt.has(ans.attempt_id)) {
      answersByAttempt.set(ans.attempt_id, []);
    }
    answersByAttempt.get(ans.attempt_id)!.push({
      questionId: ans.question_id || "",
      number: ans.question_number || 0,
      questionText: ans.question_text || "",
      selected: ans.selected || "",
      selectedText: ans.selected_text || "",
      correct: ans.correct || "",
      correctText: ans.correct_text || "",
      explanation: ans.explanation || "",
      isBookmarked: Boolean(ans.is_bookmarked),
      topic: ans.topic || "",
      questionTextHi: ans.question_text_hi || "",
      selectedTextHi: ans.selected_text_hi || "",
      correctTextHi: ans.correct_text_hi || "",
      explanationHi: ans.explanation_hi || "",
      timeTakenSeconds: ans.time_taken_seconds || 0,
      optionA: "",
      optionB: "",
      optionC: "",
      optionD: "",
      optionAHi: "",
      optionBHi: "",
      optionCHi: "",
      optionDHi: "",
    });
  }

  const list = attempts.map((a) => ({
    id: a.id,
    userId: a.user_id,
    displayName: a.display_name,
    examId: a.exam_id,
    examName: a.exam_name,
    category: a.category,
    score: a.score,
    total: a.total,
    correct: a.correct,
    wrong: a.wrong,
    unattempted: a.unattempted,
    timestamp: a.timestamp,
    timeTakenSeconds: a.time_taken_seconds || 0,
    counted: a.counted !== undefined ? a.counted : 1,
    answers: answersByAttempt.get(a.id) || [],
  }));

  return c.json({ success: true, data: list });
});

// GET /api/attempts/locks - Get all completed exam IDs for user
attemptRoutes.get("/locks", async (c) => {
  const user = c.get("user");
  const db = c.env.DB;

  if (user.isAdmin) {
    return c.json({ success: true, data: [] });
  }

  const { results } = await db
    .prepare("SELECT exam_id FROM attempts WHERE user_id = ? GROUP BY exam_id HAVING COUNT(*) >= 3")
    .bind(user.uid)
    .all<{ exam_id: string }>();

  const examIds = (results || []).map((r) => r.exam_id);
  return c.json({ success: true, data: examIds });
});

// GET /api/attempts/locks/:examId - Check lock for single exam
attemptRoutes.get("/locks/:examId", async (c) => {
  const user = c.get("user");
  const examId = c.req.param("examId");
  const db = c.env.DB;

  if (user.isAdmin) {
    return c.json({
      success: true,
      data: {
        hasLock: false,
        timestamp: null,
      },
    });
  }

  const countRow = await db
    .prepare("SELECT COUNT(*) as count, MAX(timestamp) as lastTimestamp FROM attempts WHERE user_id = ? AND exam_id = ?")
    .bind(user.uid, examId)
    .first<{ count: number; lastTimestamp: number | null }>();

  const count = countRow ? countRow.count : 0;
  const hasLock = count >= 3;

  return c.json({
    success: true,
    data: {
      hasLock,
      timestamp: hasLock && countRow ? countRow.lastTimestamp : null,
    },
  });
});

// GET /api/attempts/mistakes - Mistake Notebook
attemptRoutes.get("/mistakes", async (c) => {
  const user = c.get("user");
  const db = c.env.DB;

  const limit = Math.min(200, Math.max(1, parseInt(c.req.query("limit") || "100", 10)));
  const filter = (c.req.query("filter") || "all").trim().toLowerCase();

  // Retrieve most recent answer per question_id for this user
  const { results } = await db
    .prepare(
      `WITH ranked_answers AS (
        SELECT
          aa.question_id as questionId,
          aa.question_text as questionText,
          aa.question_text_hi as questionTextHi,
          aa.selected as selected,
          aa.selected_text as selectedText,
          aa.selected_text_hi as selectedTextHi,
          aa.correct as correct,
          aa.correct_text as correctText,
          aa.correct_text_hi as correctTextHi,
          aa.explanation as explanation,
          aa.explanation_hi as explanationHi,
          aa.topic as topic,
          a.exam_id as examId,
          a.exam_name as examName,
          a.timestamp as timestamp,
          ROW_NUMBER() OVER (PARTITION BY aa.question_id ORDER BY a.timestamp DESC) as rn
        FROM attempt_answers aa
        JOIN attempts a ON aa.attempt_id = a.id
        WHERE a.user_id = ?
      )
      SELECT questionId, questionText, questionTextHi, selected, selectedText, selectedTextHi,
             correct, correctText, correctTextHi, explanation, explanationHi, topic, examId, examName, timestamp
      FROM ranked_answers
      WHERE rn = 1
        AND (
          (? = 'all' AND (selected = '' OR selected != correct))
          OR (? = 'wrong' AND selected != '' AND selected != correct)
          OR (? = 'skipped' AND selected = '')
        )
      ORDER BY timestamp DESC
      LIMIT ?`
    )
    .bind(user.uid, filter, filter, filter, limit)
    .all<any>();

  return c.json({ success: true, data: results || [] });
});

// GET /api/attempts/streak - Study streak & daily goal calculation
attemptRoutes.get("/streak", async (c) => {
  const user = c.get("user");
  const db = c.env.DB;

  const rawTz = parseInt(c.req.query("tzOffsetMinutes") || "0", 10);
  const tz = isNaN(rawTz) ? 0 : Math.max(-840, Math.min(840, rawTz));

  // Get distinct local dates from attempts (all attempts, practice included)
  const { results } = await db
    .prepare(
      `SELECT date((timestamp / 1000) + ? * 60, 'unixepoch') AS local_date, COUNT(*) AS count
       FROM attempts
       WHERE user_id = ?
       GROUP BY local_date
       ORDER BY local_date DESC`
    )
    .bind(tz, user.uid)
    .all<{ local_date: string; count: number }>();

  const dateList = (results || []).map((r) => ({
    date: r.local_date,
    count: r.count,
  }));

  const now = Date.now();
  const todayDateStr = new Date(now + tz * 60000).toISOString().slice(0, 10);
  const yesterdayDateStr = new Date(now + tz * 60000 - 86400000).toISOString().slice(0, 10);

  let todayCount = 0;
  const todayRow = dateList.find((d) => d.date === todayDateStr);
  if (todayRow) {
    todayCount = todayRow.count;
  }

  const lastActiveDate = dateList.length > 0 ? dateList[0].date : null;

  // Compute currentStreak: counts consecutive days ending today OR yesterday
  let currentStreak = 0;
  if (dateList.length > 0) {
    const firstDate = dateList[0].date;
    if (firstDate === todayDateStr || firstDate === yesterdayDateStr) {
      let expectedEpochDay = Math.floor(new Date(firstDate + "T00:00:00Z").getTime() / 86400000);
      for (const entry of dateList) {
        const entryEpochDay = Math.floor(new Date(entry.date + "T00:00:00Z").getTime() / 86400000);
        if (entryEpochDay === expectedEpochDay) {
          currentStreak++;
          expectedEpochDay--;
        } else {
          break;
        }
      }
    }
  }

  // Compute longestStreak across the entire history
  let longestStreak = 0;
  let tempStreak = 0;
  let prevEpochDay: number | null = null;

  for (const entry of dateList) {
    const entryEpochDay = Math.floor(new Date(entry.date + "T00:00:00Z").getTime() / 86400000);
    if (prevEpochDay === null || entryEpochDay === prevEpochDay - 1) {
      tempStreak++;
    } else {
      tempStreak = 1;
    }
    prevEpochDay = entryEpochDay;
    if (tempStreak > longestStreak) {
      longestStreak = tempStreak;
    }
  }

  return c.json({
    success: true,
    data: {
      currentStreak,
      longestStreak,
      todayCount,
      lastActiveDate,
    },
  });
});

// Reattempt releases only the legacy completion lock. Confirmed results and
// derived statistics stay intact; /start enforces limits and creates a new UUID.
async function prepareReattempt(c: any, examId: string) {
  const uid = c.get('user').uid;
  if (!examId) return c.json({ success: false, error: 'Exam ID is required' }, 400);
  const db = c.env.DB;
  const session = await db.prepare('SELECT 1 FROM attempt_sessions WHERE user_id = ? AND exam_key = ?').bind(uid, examId).first();
  if (session) return c.json({ success: false, code: 'REATTEMPT_ACTIVE_SESSION', error: 'An unfinished attempt exists. Resume it before starting another.' }, 409);
  await db.prepare('DELETE FROM attempt_locks WHERE user_id = ? AND exam_id = ?').bind(uid, examId).run();
  return c.json({ success: true, data: { historyPreserved: true } });
}
attemptRoutes.post('/reset', async (c) => {
  const body = await c.req.json().catch(() => ({}));
  return prepareReattempt(c, String(body.examId || '').trim());
});
// Legacy clients receive the same non-destructive semantics.
attemptRoutes.delete('/exam/:examId', async (c) => prepareReattempt(c, c.req.param('examId').trim()));
