// ============================================================================
// Attempt Submission, Results History, Mistakes, Streak, and Anti-Cheat Locking
// ============================================================================

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
  const examId = String(body.examId || "").trim();

  if (!examId) {
    return c.json({ success: false, error: "examId is required" }, 400);
  }

  const { sourceExamId, generatedTestId } = parseAttemptKey(examId);

  // Read exam row for time limit
  const examRow = await db
    .prepare("SELECT time_limit_minutes FROM exams WHERE id = ?")
    .bind(sourceExamId)
    .first<{ time_limit_minutes: number }>();

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

  let generatedTestRow: GeneratedTestRow | null = null;
  if (generatedTestId) {
    generatedTestRow = await db
      .prepare("SELECT * FROM generated_tests WHERE id = ?")
      .bind(generatedTestId)
      .first<GeneratedTestRow>();

    if (!generatedTestRow) {
      return c.json({ success: false, error: "Test not found" }, 404);
    }

    if (!user.isAdmin && generatedTestRow.status !== "live" && generatedTestRow.status !== "published") {
      return c.json({ success: false, error: "Test is not available" }, 403);
    }

    const availableFrom = generatedTestRow.available_from || 0;
    if (availableFrom > Date.now()) {
      return c.json({ success: false, error: "Test not open yet", availableFrom }, 403);
    }
  }

  // Idempotent: INSERT OR IGNORE then SELECT
  const sessionId = `${uid}_${examId}`;
  const now = Date.now();

  await db
    .prepare(
      `INSERT OR IGNORE INTO attempt_sessions (
         id, user_id, exam_key, started_at, time_limit_seconds, accumulated_active_seconds, status, last_resumed_at
       ) VALUES (?, ?, ?, ?, ?, 0, 'RUNNING', ?)`
    )
    .bind(sessionId, uid, examId, now, limitSeconds, now)
    .run();

  const session = await db
    .prepare("SELECT * FROM attempt_sessions WHERE id = ?")
    .bind(sessionId)
    .first<AttemptSessionRow>();

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
  if (generatedTestRow) {
    let parsedQuestions: any[] = [];
    try {
      parsedQuestions = JSON.parse(generatedTestRow.questions_json);
    } catch (_e) {}

    const shouldHide = hideAnswers(c);
    questions = parsedQuestions.map((q, idx) => ({
      id: `${generatedTestId}_${idx}`,
      examId: generatedTestRow!.exam_id,
      questionText: q.question_text || q.questionText || "",
      optionA: q.option_a || q.optionA || "",
      optionB: q.option_b || q.optionB || "",
      optionC: q.option_c || q.optionC || "",
      optionD: q.option_d || q.optionD || "",
      correctAnswer: shouldHide ? "" : String(q.correct_answer || q.correctAnswer || "A").toUpperCase(),
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

  if (!examId || rawAnswers.length > 300) {
    return c.json({ success: false, error: "Invalid attempt payload" }, 400);
  }

  // 1. Idempotency Check: if clientAttemptId exists, return cached result
  if (clientAttemptId) {
    const existing = await db
      .prepare("SELECT * FROM attempts WHERE user_id = ? AND client_attempt_id = ?")
      .bind(uid, clientAttemptId)
      .first<AttemptRow>();

    if (existing) {
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

  const isPractice = Boolean(body.topic || body.pyqYear);
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

  if (generatedTestId) {
    const gRow = await db
      .prepare("SELECT questions_json FROM generated_tests WHERE id = ?")
      .bind(generatedTestId)
      .first<{ questions_json: string }>();

    if (!gRow) {
      return c.json({ success: false, error: "Generated test not found" }, 400);
    }

    let parsedQuestions: any[] = [];
    try {
      parsedQuestions = JSON.parse(gRow.questions_json);
    } catch (_e) {}

    parsedQuestions.forEach((q, idx) => {
      const qId = `${generatedTestId}_${idx}`;
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
        correctAnswer: String(q.correct_answer || q.correctAnswer || "A").toUpperCase(),
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
        correctAnswer: String(q.correct_answer || "A").toUpperCase(),
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
      .find((id) => id.includes("_"))
      ?.split("_")?.[0];

    if ((!dbQs || dbQs.length === 0 || candidateGenTestId) && expectedMap.size === 0) {
      let gRow: { questions_json: string; id: string } | null = null;
      if (candidateGenTestId) {
        gRow = await db
          .prepare("SELECT id, questions_json FROM generated_tests WHERE id = ?")
          .bind(candidateGenTestId)
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
          parsedQuestions = JSON.parse(gRow.questions_json);
        } catch (_e) {}

        parsedQuestions.forEach((q, idx) => {
          const qId = `${gRow!.id}_${idx}`;
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
            correctAnswer: String(q.correct_answer || q.correctAnswer || "A").toUpperCase(),
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
          correctAnswer: String(q.correct_answer || "A").toUpperCase(),
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

  // Filter student picks against expected question set
  const submittedPicksMap = new Map<
    string,
    { number: number; selected: string; isBookmarked: boolean; timeTakenSeconds: number }
  >();

  for (const a of rawAnswers) {
    const qId = String(a?.questionId || "").trim();
    if (!qId || !expectedMap.has(qId) || submittedPicksMap.has(qId)) continue;

    const selectedRaw = String(a?.selected || "").trim().toUpperCase();
    const timeTaken = Math.max(0, parseInt(a?.timeTakenSeconds, 10) || 0);

    submittedPicksMap.set(qId, {
      number: Number(a?.number || 0),
      selected: VALID_OPTIONS.has(selectedRaw) ? selectedRaw : "",
      isBookmarked: Boolean(a?.isBookmarked),
      timeTakenSeconds: timeTaken,
    });
  }

  if (submittedPicksMap.size === 0) {
    return c.json({ success: false, error: "No valid answers in payload" }, 400);
  }

  // 4. Timing & Counted logic
  const sessionId = `${uid}_${examId}`;
  const session = await db
    .prepare("SELECT * FROM attempt_sessions WHERE id = ?")
    .bind(sessionId)
    .first<AttemptSessionRow>();

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
  const negativeMarking = examRow && typeof examRow.negative_marking_value === "number" ? examRow.negative_marking_value : 0.0;
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
        `INSERT INTO attempts (id, user_id, display_name, exam_id, exam_name, category, score, total, correct, wrong, unattempted, timestamp, time_taken_seconds, client_attempt_id, counted)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
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
        counted
      )
  );

  // 4. Insert evaluated answers
  for (const a of answersData) {
    const answerId = crypto.randomUUID();
    batchStatements.push(
      db
        .prepare(
          `INSERT INTO attempt_answers (
            id, attempt_id, question_id, question_number, question_text, selected, selected_text,
            correct, correct_text, explanation, is_bookmarked, topic,
            question_text_hi, selected_text_hi, correct_text_hi, explanation_hi, time_taken_seconds
          ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
        )
        .bind(
          answerId,
          attemptId,
          a.questionId,
          a.number,
          a.questionText,
          a.selected,
          a.selectedText,
          a.correct,
          a.correctText,
          a.explanation,
          a.isBookmarked ? 1 : 0,
          a.topic,
          a.questionTextHi,
          a.selectedTextHi,
          a.correctTextHi,
          a.explanationHi,
          a.timeTakenSeconds
        )
    );
  }

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
    for (const a of answersData) {
      const qKey = `${examId}_${a.questionId}`;
      const isAttempted = a.selected.length > 0;
      const isCorrect = isAttempted && a.selected === a.correct;
      const isWrong = isAttempted && a.selected !== a.correct;
      const isUnattempted = !isAttempted;

      batchStatements.push(
        db
          .prepare(
            `INSERT INTO admin_analytics_questions (
               id, exam_id, exam_name, question_id, question_number, question_text, topic,
               attempts, correct, wrong, unattempted, total_time_seconds
             )
             VALUES (?, ?, ?, ?, ?, ?, ?, 1, ?, ?, ?, ?)
             ON CONFLICT(id) DO UPDATE SET
               exam_name = excluded.exam_name,
               question_number = excluded.question_number,
               question_text = CASE WHEN excluded.question_text != '' THEN excluded.question_text ELSE admin_analytics_questions.question_text END,
               topic = CASE WHEN excluded.topic != '' THEN excluded.topic ELSE admin_analytics_questions.topic END,
               attempts = admin_analytics_questions.attempts + 1,
               correct = admin_analytics_questions.correct + excluded.correct,
               wrong = admin_analytics_questions.wrong + excluded.wrong,
               unattempted = admin_analytics_questions.unattempted + excluded.unattempted,
               total_time_seconds = admin_analytics_questions.total_time_seconds + excluded.total_time_seconds`
          )
          .bind(
            qKey,
            examId,
            examName,
            a.questionId,
            a.number,
            a.questionText || "",
            a.topic || "",
            isCorrect ? 1 : 0,
            isWrong ? 1 : 0,
            isUnattempted ? 1 : 0,
            a.timeTakenSeconds
          )
      );
    }
  }

  // Execute D1 batch in chunks of 50
  for (let i = 0; i < batchStatements.length; i += 50) {
    const chunk = batchStatements.slice(i, i + 50);
    await db.batch(chunk);
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

  const attemptIds = attempts.map((a) => a.id);
  const placeholders = attemptIds.map(() => "?").join(",");
  const { results: allAnswers } = await db
    .prepare(`SELECT * FROM attempt_answers WHERE attempt_id IN (${placeholders}) ORDER BY question_number ASC`)
    .bind(...attemptIds)
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

// POST /api/attempts/reset - Atomic mock attempt & lock reset for standard mock reattempt flow
attemptRoutes.post("/reset", async (c) => {
  const user = c.get("user");
  const uid = user.uid;
  const db = c.env.DB;
  const body = await c.req.json().catch(() => ({}));
  const examId = String(body.examId || "").trim();

  if (!examId) {
    return c.json({ success: false, error: "Exam ID is required" }, 400);
  }

  const lockKey = `${uid}_${examId}`;
  const lbKey = `${examId}_${uid}`;
  const sessionId = `${uid}_${examId}`;

  // 1. Read the lock
  const lock = await db
    .prepare("SELECT id, user_id, exam_id, timestamp FROM attempt_locks WHERE id = ?")
    .bind(lockKey)
    .first<{ id: string; user_id: string; exam_id: string; timestamp: number }>();

  if (!lock) {
    return c.json({ success: true, data: { cleared: false } });
  }

  const lockTimestamp = Number(lock.timestamp || 0);

  // 2. Identify the mock attempt matching lock.timestamp
  let targetAttempt = await db
    .prepare("SELECT id, score, total, correct, counted FROM attempts WHERE user_id = ? AND exam_id = ? AND timestamp = ?")
    .bind(uid, examId, lockTimestamp)
    .first<{ id: string; score: number; total: number; correct: number; counted: number }>();

  if (!targetAttempt && lockTimestamp > 0) {
    targetAttempt = await db
      .prepare(
        "SELECT id, score, total, correct, counted FROM attempts WHERE user_id = ? AND exam_id = ? AND timestamp >= ? AND timestamp <= ? ORDER BY ABS(timestamp - ?) ASC LIMIT 1"
      )
      .bind(uid, examId, lockTimestamp - 60000, lockTimestamp + 60000, lockTimestamp)
      .first<{ id: string; score: number; total: number; correct: number; counted: number }>();
  }

  if (!targetAttempt) {
    await db.prepare("DELETE FROM attempt_locks WHERE id = ?").bind(lockKey).run();
    await db.prepare("DELETE FROM attempt_sessions WHERE id = ?").bind(sessionId).run();
    return c.json({ success: true, data: { cleared: true, clearedAttempts: 0 } });
  }

  const batchStatements: D1PreparedStatement[] = [];

  // A. Decrement admin_analytics_questions only if counted = 1
  if (targetAttempt.counted === 1) {
    const { results: targetAnswers } = await db
      .prepare("SELECT question_id, selected, correct, time_taken_seconds FROM attempt_answers WHERE attempt_id = ?")
      .bind(targetAttempt.id)
      .all<{ question_id: string; selected: string; correct: string; time_taken_seconds: number }>();

    for (const ans of targetAnswers || []) {
      const qKey = `${examId}_${ans.question_id}`;
      const isAttempted = Boolean(ans.selected && ans.selected.length > 0);
      const isCorrect = isAttempted && ans.selected === ans.correct;
      const isWrong = isAttempted && ans.selected !== ans.correct;
      const isUnattempted = !isAttempted;
      const timeTaken = ans.time_taken_seconds || 0;

      batchStatements.push(
        db
          .prepare(
            `UPDATE admin_analytics_questions
             SET attempts = MAX(0, attempts - 1),
                 correct = MAX(0, correct - ?),
                 wrong = MAX(0, wrong - ?),
                 unattempted = MAX(0, unattempted - ?),
                 total_time_seconds = MAX(0, total_time_seconds - ?)
             WHERE id = ?`
          )
          .bind(
            isCorrect ? 1 : 0,
            isWrong ? 1 : 0,
            isUnattempted ? 1 : 0,
            timeTaken,
            qKey
          )
      );
    }
  }

  // B. Delete attempt_answers & attempt
  batchStatements.push(db.prepare("DELETE FROM attempt_answers WHERE attempt_id = ?").bind(targetAttempt.id));
  batchStatements.push(db.prepare("DELETE FROM attempts WHERE id = ?").bind(targetAttempt.id));

  // C. Recompute leaderboard row ${examId}_${uid} from remaining counted attempts (best score, lower time)
  const { results: remainingAttempts } = await db
    .prepare(
      `SELECT score, total, timestamp, display_name, category, exam_name, time_taken_seconds
       FROM attempts
       WHERE user_id = ? AND exam_id = ? AND id != ? AND counted = 1
       ORDER BY score DESC, time_taken_seconds ASC, timestamp ASC`
    )
    .bind(uid, examId, targetAttempt.id)
    .all<{
      score: number;
      total: number;
      timestamp: number;
      display_name: string;
      category: string;
      exam_name: string;
      time_taken_seconds: number;
    }>();

  if (remainingAttempts && remainingAttempts.length > 0) {
    const best = remainingAttempts[0];
    batchStatements.push(
      db
        .prepare(
          `INSERT INTO leaderboard (id, user_id, exam_id, exam_name, category, display_name, score, total, timestamp, time_taken_seconds)
           VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
           ON CONFLICT(id) DO UPDATE SET
             score = excluded.score,
             total = excluded.total,
             display_name = excluded.display_name,
             timestamp = excluded.timestamp,
             time_taken_seconds = excluded.time_taken_seconds`
        )
        .bind(
          lbKey,
          uid,
          examId,
          best.exam_name,
          best.category,
          best.display_name,
          best.score,
          best.total,
          best.timestamp,
          best.time_taken_seconds || 0
        )
    );
  } else {
    batchStatements.push(db.prepare("DELETE FROM leaderboard WHERE id = ?").bind(lbKey));
  }

  // D. Adjust overall_leaderboard only if targetAttempt.counted === 1
  if (targetAttempt.counted === 1) {
    const overallRow = await db
      .prepare("SELECT score, total_score, total, tests_taken, total_correct FROM overall_leaderboard WHERE user_id = ?")
      .bind(uid)
      .first<{ score: number; total_score: number; total: number; tests_taken: number; total_correct: number }>();

    if (overallRow) {
      const newTestsTaken = Math.max(0, overallRow.tests_taken - 1);
      if (newTestsTaken === 0) {
        batchStatements.push(db.prepare("DELETE FROM overall_leaderboard WHERE user_id = ?").bind(uid));
      } else {
        const newScore = Math.max(0, Math.round((overallRow.score - targetAttempt.score) * 100) / 100);
        const newTotal = Math.max(0, overallRow.total - targetAttempt.total);
        const newCorrect = Math.max(0, overallRow.total_correct - targetAttempt.correct);
        const newAccuracy = newTotal > 0 ? Math.round((newCorrect * 100.0) / newTotal) : 0;
        batchStatements.push(
          db
            .prepare(
              `UPDATE overall_leaderboard
               SET score = ?, total_score = ?, total = ?, tests_taken = ?, total_correct = ?, accuracy = ?, timestamp = ?
               WHERE user_id = ?`
            )
            .bind(newScore, newScore, newTotal, newTestsTaken, newCorrect, newAccuracy, Date.now(), uid)
        );
      }
    }
  }

  // E. Delete lock and session
  batchStatements.push(db.prepare("DELETE FROM attempt_locks WHERE id = ?").bind(lockKey));
  batchStatements.push(db.prepare("DELETE FROM attempt_sessions WHERE id = ?").bind(sessionId));

  await db.batch(batchStatements);

  return c.json({
    success: true,
    data: {
      cleared: true,
      clearedAttempts: 1,
      attemptId: targetAttempt.id,
      examId,
    },
  });
});

// DELETE /api/attempts/exam/:examId - Reset attempts & lock for an exam (reattempt flow)
attemptRoutes.delete("/exam/:examId", async (c) => {
  const user = c.get("user");
  const uid = user.uid;
  const examId = String(c.req.param("examId") || "").trim();
  const db = c.env.DB;

  if (!examId) {
    return c.json({ success: false, error: "Exam ID is required" }, 400);
  }

  const lockKey = `${uid}_${examId}`;
  const lbKey = `${examId}_${uid}`;
  const sessionId = `${uid}_${examId}`;

  // Fetch only COUNTED attempts for this user and exam to adjust metrics
  const { results: countedAttempts } = await db
    .prepare("SELECT id, score, total, correct FROM attempts WHERE user_id = ? AND exam_id = ? AND counted = 1")
    .bind(uid, examId)
    .all<{ id: string; score: number; total: number; correct: number }>();

  // Fetch evaluated answers for these counted attempts
  const { results: countedAnswers } = await db
    .prepare(
      `SELECT question_id, selected, correct, time_taken_seconds
       FROM attempt_answers
       WHERE attempt_id IN (SELECT id FROM attempts WHERE user_id = ? AND exam_id = ? AND counted = 1)`
    )
    .bind(uid, examId)
    .all<{ question_id: string; selected: string; correct: string; time_taken_seconds: number }>();

  const batchStatements: D1PreparedStatement[] = [];

  // 1. Delete lock and session
  batchStatements.push(db.prepare("DELETE FROM attempt_locks WHERE id = ?").bind(lockKey));
  batchStatements.push(db.prepare("DELETE FROM attempt_sessions WHERE id = ?").bind(sessionId));

  // 2. Decrement derived per-question metrics in admin_analytics_questions for counted answers
  if (countedAnswers && countedAnswers.length > 0) {
    for (const ans of countedAnswers) {
      const qKey = `${examId}_${ans.question_id}`;
      const isAttempted = Boolean(ans.selected && ans.selected.length > 0);
      const isCorrect = isAttempted && ans.selected === ans.correct;
      const isWrong = isAttempted && ans.selected !== ans.correct;
      const isUnattempted = !isAttempted;
      const timeTaken = ans.time_taken_seconds || 0;

      batchStatements.push(
        db
          .prepare(
            `UPDATE admin_analytics_questions
             SET attempts = MAX(0, attempts - 1),
                 correct = MAX(0, correct - ?),
                 wrong = MAX(0, wrong - ?),
                 unattempted = MAX(0, unattempted - ?),
                 total_time_seconds = MAX(0, total_time_seconds - ?)
             WHERE id = ?`
          )
          .bind(
            isCorrect ? 1 : 0,
            isWrong ? 1 : 0,
            isUnattempted ? 1 : 0,
            timeTaken,
            qKey
          )
      );
    }
  }

  // 3. Delete all attempt_answers for this exam
  batchStatements.push(
    db
      .prepare("DELETE FROM attempt_answers WHERE attempt_id IN (SELECT id FROM attempts WHERE user_id = ? AND exam_id = ?)")
      .bind(uid, examId)
  );

  // 4. Delete all attempts for this exam
  batchStatements.push(db.prepare("DELETE FROM attempts WHERE user_id = ? AND exam_id = ?").bind(uid, examId));

  // 5. Delete per-exam leaderboard row
  batchStatements.push(db.prepare("DELETE FROM leaderboard WHERE id = ?").bind(lbKey));

  // 6. Clean up overall_leaderboard if counted attempts existed
  if (countedAttempts && countedAttempts.length > 0) {
    let scoreToDeduct = 0;
    let questionsToDeduct = 0;
    let correctToDeduct = 0;
    const testsToDeduct = countedAttempts.length;

    for (const att of countedAttempts) {
      scoreToDeduct += Number(att.score || 0);
      questionsToDeduct += Number(att.total || 0);
      correctToDeduct += Number(att.correct || 0);
    }

    batchStatements.push(
      db
        .prepare(
          `UPDATE overall_leaderboard
           SET score = MAX(0.0, ROUND(score - ?, 2)),
               total_score = MAX(0.0, ROUND(total_score - ?, 2)),
               total = MAX(0, total - ?),
               tests_taken = MAX(0, tests_taken - ?),
               total_correct = MAX(0, total_correct - ?),
               accuracy = CASE WHEN MAX(0, total - ?) > 0
                               THEN ROUND((MAX(0, total_correct - ?) * 100.0) / MAX(0, total - ?))
                               ELSE 0 END,
               timestamp = ?
           WHERE user_id = ?`
        )
        .bind(
          scoreToDeduct,
          scoreToDeduct,
          questionsToDeduct,
          testsToDeduct,
          correctToDeduct,
          questionsToDeduct,
          correctToDeduct,
          questionsToDeduct,
          Date.now(),
          uid
        )
    );
  }

  await db.batch(batchStatements);

  return c.json({
    success: true,
    data: {
      examId,
      deletedAttemptsCount: countedAttempts?.length || 0,
      message: "Attempt data and lock reset successfully.",
    },
  });
});
