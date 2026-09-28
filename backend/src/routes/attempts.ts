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
  const row = await db.prepare("SELECT timestamp FROM attempt_locks WHERE id = ?").bind(lockKey).first<{ timestamp: number }>();

  return c.json({
    success: true,
    data: {
      hasLock: Boolean(row),
      timestamp: row ? Number(row.timestamp) : null,
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
    .prepare("SELECT id, score, total, correct FROM attempts WHERE user_id = ? AND exam_id = ? AND timestamp = ?")
    .bind(uid, examId, lockTimestamp)
    .first<{ id: string; score: number; total: number; correct: number }>();

  // If no exact match, use the nearest timestamp within 60 s
  if (!targetAttempt && lockTimestamp > 0) {
    targetAttempt = await db
      .prepare(
        "SELECT id, score, total, correct FROM attempts WHERE user_id = ? AND exam_id = ? AND timestamp >= ? AND timestamp <= ? ORDER BY ABS(timestamp - ?) ASC LIMIT 1"
      )
      .bind(uid, examId, lockTimestamp - 60000, lockTimestamp + 60000, lockTimestamp)
      .first<{ id: string; score: number; total: number; correct: number }>();
  }

  // If still none, delete only the lock and return clearedAttempts: 0
  if (!targetAttempt) {
    await db.prepare("DELETE FROM attempt_locks WHERE id = ?").bind(lockKey).run();
    return c.json({ success: true, data: { cleared: true, clearedAttempts: 0 } });
  }

  const batchStatements: D1PreparedStatement[] = [];

  // A. Delete attempt_answers for this attempt
  batchStatements.push(
    db.prepare("DELETE FROM attempt_answers WHERE attempt_id = ?").bind(targetAttempt.id)
  );

  // B. Delete the attempts row
  batchStatements.push(
    db.prepare("DELETE FROM attempts WHERE id = ?").bind(targetAttempt.id)
  );

  // C. Recompute leaderboard row ${examId}_${uid} from remaining attempts (best score), or delete row
  const { results: remainingAttempts } = await db
    .prepare("SELECT score, total, timestamp, display_name, category, exam_name FROM attempts WHERE user_id = ? AND exam_id = ? AND id != ? ORDER BY score DESC, timestamp DESC")
    .bind(uid, examId, targetAttempt.id)
    .all<{ score: number; total: number; timestamp: number; display_name: string; category: string; exam_name: string }>();

  if (remainingAttempts && remainingAttempts.length > 0) {
    const best = remainingAttempts[0];
    batchStatements.push(
      db
        .prepare(
          `INSERT INTO leaderboard (id, user_id, exam_id, exam_name, category, display_name, score, total, timestamp)
           VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
           ON CONFLICT(id) DO UPDATE SET
             score = excluded.score,
             total = excluded.total,
             display_name = excluded.display_name,
             timestamp = excluded.timestamp`
        )
        .bind(lbKey, uid, examId, best.exam_name, best.category, best.display_name, best.score, best.total, best.timestamp)
    );
  } else {
    batchStatements.push(
      db.prepare("DELETE FROM leaderboard WHERE id = ?").bind(lbKey)
    );
  }

  // D. Adjust overall_leaderboard: subtract score, total, correct; decrement tests_taken; recompute accuracy; delete if tests_taken reaches 0
  const overallRow = await db
    .prepare("SELECT score, total_score, total, tests_taken, total_correct FROM overall_leaderboard WHERE user_id = ?")
    .bind(uid)
    .first<{ score: number; total_score: number; total: number; tests_taken: number; total_correct: number }>();

  if (overallRow) {
    const newTestsTaken = Math.max(0, overallRow.tests_taken - 1);
    if (newTestsTaken === 0) {
      batchStatements.push(
        db.prepare("DELETE FROM overall_leaderboard WHERE user_id = ?").bind(uid)
      );
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

  // E. Delete the attempt_locks row
  batchStatements.push(
    db.prepare("DELETE FROM attempt_locks WHERE id = ?").bind(lockKey)
  );

  // Execute in ONE atomic batch
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

  // Fetch existing attempts for this user and exam to adjust overall_leaderboard
  const { results: existingAttempts } = await db
    .prepare("SELECT id, score, total, correct FROM attempts WHERE user_id = ? AND exam_id = ?")
    .bind(uid, examId)
    .all<{ id: string; score: number; total: number; correct: number }>();

  // Fetch evaluated answers for these attempts to clean up derived per-question stats in admin_analytics_questions
  const { results: existingAnswers } = await db
    .prepare(`
      SELECT question_id, selected, correct
      FROM attempt_answers
      WHERE attempt_id IN (SELECT id FROM attempts WHERE user_id = ? AND exam_id = ?)
    `)
    .bind(uid, examId)
    .all<{ question_id: string; selected: string; correct: string }>();

  const batchStatements: D1PreparedStatement[] = [];

  // 1. Delete lock
  batchStatements.push(
    db.prepare("DELETE FROM attempt_locks WHERE id = ?").bind(lockKey)
  );

  // 2. Decrement derived per-question metrics in admin_analytics_questions
  if (existingAnswers && existingAnswers.length > 0) {
    for (const ans of existingAnswers) {
      const qKey = `${examId}_${ans.question_id}`;
      const isAttempted = Boolean(ans.selected && ans.selected.length > 0);
      const isCorrect = isAttempted && ans.selected === ans.correct;
      const isWrong = isAttempted && ans.selected !== ans.correct;
      const isUnattempted = !isAttempted;

      batchStatements.push(
        db.prepare(`
          UPDATE admin_analytics_questions
          SET attempts = MAX(0, attempts - ?),
              correct = MAX(0, correct - ?),
              wrong = MAX(0, wrong - ?),
              unattempted = MAX(0, unattempted - ?)
          WHERE id = ?
        `).bind(
          isAttempted ? 1 : 0,
          isCorrect ? 1 : 0,
          isWrong ? 1 : 0,
          isUnattempted ? 1 : 0,
          qKey
        )
      );
    }
  }

  // 3. Delete evaluated answers
  batchStatements.push(
    db.prepare(
      "DELETE FROM attempt_answers WHERE attempt_id IN (SELECT id FROM attempts WHERE user_id = ? AND exam_id = ?)"
    ).bind(uid, examId)
  );

  // 4. Delete attempts
  batchStatements.push(
    db.prepare("DELETE FROM attempts WHERE user_id = ? AND exam_id = ?").bind(uid, examId)
  );

  // 5. Delete per-exam leaderboard row
  batchStatements.push(
    db.prepare("DELETE FROM leaderboard WHERE id = ?").bind(lbKey)
  );

  // 6. Clean up overall_leaderboard if attempts existed (floored at 0)
  if (existingAttempts && existingAttempts.length > 0) {
    let scoreToDeduct = 0;
    let questionsToDeduct = 0;
    let correctToDeduct = 0;
    const testsToDeduct = existingAttempts.length;

    for (const att of existingAttempts) {
      scoreToDeduct += Number(att.score || 0);
      questionsToDeduct += Number(att.total || 0);
      correctToDeduct += Number(att.correct || 0);
    }

    batchStatements.push(
      db.prepare(`
        UPDATE overall_leaderboard
        SET score = MAX(0.0, ROUND(score - ?, 2)),
            total_score = MAX(0.0, ROUND(total_score - ?, 2)),
            total = MAX(0, total - ?),
            tests_taken = MAX(0, tests_taken - ?),
            total_correct = MAX(0, total_correct - ?),
            accuracy = CASE WHEN MAX(0, total - ?) > 0
                            THEN ROUND((MAX(0, total_correct - ?) * 100.0) / MAX(0, total - ?))
                            ELSE 0 END,
            timestamp = ?
        WHERE user_id = ?
      `).bind(
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

  // Execute in ONE atomic batch
  await db.batch(batchStatements);

  return c.json({
    success: true,
    data: {
      examId,
      deletedAttemptsCount: existingAttempts?.length || 0,
      message: "Attempt data and lock reset successfully."
    }
  });
});

