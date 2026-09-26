// ============================================================================
// Attempt Submission, Results History, and Anti-Cheat Locking
// ============================================================================

import { Hono } from "hono";
import { AttemptAnswerRow, AttemptRow, AuthUser, Env, QuestionRow } from "../types";

export const attemptRoutes = new Hono<{ Bindings: Env; Variables: { user: AuthUser } }>();

const NEGATIVE_MARK = 0.0;
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
  const rawAnswers: any[] = Array.isArray(body.answers) ? body.answers : [];

  if (!examId || rawAnswers.length === 0 || rawAnswers.length > 300) {
    return c.json({ success: false, error: "Invalid attempt payload" }, 400);
  }

  // Filter & sanitize student picks
  const seen = new Set<string>();
  const picks: Array<{ questionId: string; number: number; selected: string; isBookmarked: boolean }> = [];

  for (const a of rawAnswers) {
    const questionId = String(a?.questionId || "").trim();
    if (!questionId || seen.has(questionId)) continue;
    seen.add(questionId);

    const selectedRaw = String(a?.selected || "").trim().toUpperCase();
    picks.push({
      questionId,
      number: Number(a?.number || 0),
      selected: VALID_OPTIONS.has(selectedRaw) ? selectedRaw : "",
      isBookmarked: Boolean(a?.isBookmarked),
    });
  }

  if (picks.length === 0) {
    return c.json({ success: false, error: "No valid answers in payload" }, 400);
  }

  const isPractice = Boolean(body.topic || body.pyqYear);
  const lockKey = `${uid}_${examId}`;

  // Enforce anti-cheat single attempt lock for regular mock tests
  if (!isPractice && !user.isAdmin) {
    const existingLock = await db
      .prepare("SELECT 1 FROM attempt_locks WHERE id = ?")
      .bind(lockKey)
      .first();

    if (existingLock) {
      return c.json({ success: false, error: "You have already completed this test." }, 409);
    }
  }

  // Fetch true questions from questions table
  const placeholders = picks.map(() => "?").join(",");
  const qIds = picks.map((p) => p.questionId);
  const { results: dbQuestions } = await db
    .prepare(`SELECT * FROM questions WHERE id IN (${placeholders})`)
    .bind(...qIds)
    .all<QuestionRow>();

  const questionsMap = new Map<string, QuestionRow>();
  for (const q of dbQuestions || []) {
    questionsMap.set(q.id, q);
  }

  // Fallback for AI generated tests if questionId matches testId_index
  const generatedTestsCache = new Map<string, any>();

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
  }> = [];

  let correct = 0;
  let wrong = 0;
  let unattempted = 0;

  for (const pick of picks) {
    let q: any = questionsMap.get(pick.questionId);

    if (!q && pick.questionId.includes("_")) {
      const lastUnderscore = pick.questionId.lastIndexOf("_");
      const testId = pick.questionId.substring(0, lastUnderscore);
      const qIndex = parseInt(pick.questionId.substring(lastUnderscore + 1), 10);

      if (!isNaN(qIndex)) {
        if (!generatedTestsCache.has(testId)) {
          const gRow = await db
            .prepare("SELECT questions_json FROM generated_tests WHERE id = ?")
            .bind(testId)
            .first<{ questions_json: string }>();
          try {
            generatedTestsCache.set(testId, gRow ? JSON.parse(gRow.questions_json) : null);
          } catch (_e) {
            generatedTestsCache.set(testId, null);
          }
        }
        const gQuestions = generatedTestsCache.get(testId);
        if (Array.isArray(gQuestions) && gQuestions[qIndex]) {
          q = gQuestions[qIndex];
        }
      }
    }

    if (!q) continue;

    const correctAnswer = String(q.correct_answer || q.correctAnswer || "A").toUpperCase();
    const attempted = pick.selected.length > 0;
    const isCorrect = attempted && pick.selected === correctAnswer;

    if (!attempted) unattempted++;
    else if (isCorrect) correct++;
    else wrong++;

    answersData.push({
      questionId: pick.questionId,
      number: pick.number,
      questionText: String(q.question_text || q.questionText || ""),
      selected: pick.selected,
      selectedText: pick.selected ? questionOptionText(q, pick.selected) : "",
      correct: correctAnswer,
      correctText: questionOptionText(q, correctAnswer),
      explanation: String(q.explanation || ""),
      isBookmarked: pick.isBookmarked,
      topic: String(q.topic || ""),
      questionTextHi: String(q.question_text_hi || q.questionTextHi || ""),
      selectedTextHi: pick.selected ? questionOptionTextHi(q, pick.selected) : "",
      correctTextHi: questionOptionTextHi(q, correctAnswer),
      explanationHi: String(q.explanation_hi || q.explanationHi || ""),
    });
  }

  if (answersData.length === 0) {
    return c.json({ success: false, error: "None of the submitted questions could be verified." }, 400);
  }

  const total = answersData.length;
  const score = Math.round((correct - wrong * NEGATIVE_MARK) * 100) / 100;
  const attemptId = crypto.randomUUID();
  const now = Date.now();

  const batchStatements: D1PreparedStatement[] = [];

  // 1. Lock test for student
  if (!isPractice) {
    batchStatements.push(
      db
        .prepare(
          "INSERT INTO attempt_locks (id, user_id, exam_id, timestamp, source) VALUES (?, ?, ?, ?, ?) ON CONFLICT(id) DO NOTHING"
        )
        .bind(lockKey, uid, examId, now, "submit")
    );
  }

  // 2. Insert attempt
  batchStatements.push(
    db
      .prepare(
        `INSERT INTO attempts (id, user_id, display_name, exam_id, exam_name, category, score, total, correct, wrong, unattempted, timestamp)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
      )
      .bind(attemptId, uid, displayName, examId, examName, category, score, total, correct, wrong, unattempted, now)
  );

  // 3. Insert evaluated answer sheet
  for (const a of answersData) {
    const answerId = crypto.randomUUID();
    batchStatements.push(
      db
        .prepare(
          `INSERT INTO attempt_answers (
            id, attempt_id, question_id, question_number, question_text, selected, selected_text,
            correct, correct_text, explanation, is_bookmarked, topic,
            question_text_hi, selected_text_hi, correct_text_hi, explanation_hi
          ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
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
          a.explanationHi
        )
    );
  }

  // 4. Update Leaderboard (best score upsert per exam)
  const lbKey = `${examId}_${uid}`;
  batchStatements.push(
    db
      .prepare(
        `INSERT INTO leaderboard (id, user_id, exam_id, exam_name, category, display_name, score, total, timestamp)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
         ON CONFLICT(id) DO UPDATE SET
           score = CASE WHEN excluded.score > leaderboard.score THEN excluded.score ELSE leaderboard.score END,
           total = CASE WHEN excluded.score > leaderboard.score THEN excluded.total ELSE leaderboard.total END,
           display_name = excluded.display_name,
           timestamp = excluded.timestamp`
      )
      .bind(lbKey, uid, examId, examName, category, displayName, score, total, now)
  );

  // 5. Update Overall Leaderboard
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

  // 6. Update Admin Analytics: Exam
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

  // Execute D1 batch in chunks
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

  // Load evaluated answers for each attempt
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
      questionId: ans.question_id,
      number: ans.question_number,
      questionText: ans.question_text,
      selected: ans.selected,
      selectedText: ans.selected_text,
      correct: ans.correct,
      correctText: ans.correct_text,
      explanation: ans.explanation,
      isBookmarked: Boolean(ans.is_bookmarked),
      topic: ans.topic,
      questionTextHi: ans.question_text_hi,
      selectedTextHi: ans.selected_text_hi,
      correctTextHi: ans.correct_text_hi,
      explanationHi: ans.explanation_hi,
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
    answers: answersByAttempt.get(a.id) || [],
  }));

  return c.json({ success: true, data: list });
});

// GET /api/attempts/locks - Get all completed exam IDs for user
attemptRoutes.get("/locks", async (c) => {
  const user = c.get("user");
  const db = c.env.DB;

  const { results } = await db
    .prepare("SELECT exam_id FROM attempt_locks WHERE user_id = ?")
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

  const lockKey = `${user.uid}_${examId}`;
  const row = await db.prepare("SELECT 1 FROM attempt_locks WHERE id = ?").bind(lockKey).first();

  return c.json({ success: true, data: { hasLock: Boolean(row) } });
});
