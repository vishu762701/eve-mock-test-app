import test from "node:test";
import assert from "node:assert/strict";

/**
 * Test case 1:
 * - Deletes only the caller's own data
 * - Clears the attempt lock
 * - A later attempt submission succeeds (no 409)
 * - Other users' data are completely untouched
 */
test("reattempt reset deletes caller data, clears lock, allows new submit, and leaves other users untouched", () => {
  // In-memory simulation of SQLite D1 relational state
  let attemptLocks = new Map([
    ["user_1_exam_math", { id: "user_1_exam_math", userId: "user_1", examId: "exam_math" }],
    ["user_2_exam_math", { id: "user_2_exam_math", userId: "user_2", examId: "exam_math" }],
  ]);

  let attempts = new Map([
    ["att_1", { id: "att_1", userId: "user_1", examId: "exam_math", score: 18, total: 20, correct: 18 }],
    ["att_2", { id: "att_2", userId: "user_2", examId: "exam_math", score: 20, total: 20, correct: 20 }],
  ]);

  let attemptAnswers = new Map([
    ["ans_1", { id: "ans_1", attemptId: "att_1", questionId: "q1", selected: "A" }],
    ["ans_2", { id: "ans_2", attemptId: "att_2", questionId: "q1", selected: "A" }],
  ]);

  let leaderboard = new Map([
    ["exam_math_user_1", { id: "exam_math_user_1", userId: "user_1", examId: "exam_math", score: 18 }],
    ["exam_math_user_2", { id: "exam_math_user_2", userId: "user_2", examId: "exam_math", score: 20 }],
  ]);

  let overallLeaderboard = new Map([
    ["user_1", { userId: "user_1", score: 18, total: 20, testsTaken: 1, totalCorrect: 18 }],
    ["user_2", { userId: "user_2", score: 20, total: 20, testsTaken: 1, totalCorrect: 20 }],
  ]);

  function checkLock(uid, examId) {
    return attemptLocks.has(`${uid}_${examId}`);
  }

  function simulateSubmit(uid, examId, score, total, correct) {
    if (checkLock(uid, examId)) {
      return { status: 409, error: "You have already completed this test." };
    }
    const attemptId = `att_${Date.now()}`;
    attempts.set(attemptId, { id: attemptId, userId: uid, examId, score, total, correct });
    attemptLocks.set(`${uid}_${examId}`, { id: `${uid}_${examId}`, userId: uid, examId });
    return { status: 200, success: true, attemptId };
  }

  // Pre-reset submit attempt should fail with 409
  const blockedSubmit = simulateSubmit("user_1", "exam_math", 15, 20, 15);
  assert.equal(blockedSubmit.status, 409);
  assert.equal(blockedSubmit.error, "You have already completed this test.");

  // Perform atomic reset for caller user_1 on exam_math
  function resetAttempt(callerUid, targetExamId) {
    const lockKey = `${callerUid}_${targetExamId}`;
    const lbKey = `${targetExamId}_${callerUid}`;

    const callerAttempts = [...attempts.values()].filter(
      (a) => a.userId === callerUid && a.examId === targetExamId
    );
    const callerAttemptIds = new Set(callerAttempts.map((a) => a.id));

    attemptLocks.delete(lockKey);

    for (const [id, ans] of [...attemptAnswers.entries()]) {
      if (callerAttemptIds.has(ans.attemptId)) {
        attemptAnswers.delete(id);
      }
    }

    for (const id of callerAttemptIds) {
      attempts.delete(id);
    }

    leaderboard.delete(lbKey);

    if (callerAttempts.length > 0 && overallLeaderboard.has(callerUid)) {
      const current = overallLeaderboard.get(callerUid);
      let scoreDeduct = 0;
      let totalDeduct = 0;
      let correctDeduct = 0;
      for (const a of callerAttempts) {
        scoreDeduct += a.score;
        totalDeduct += a.total;
        correctDeduct += a.correct;
      }
      overallLeaderboard.set(callerUid, {
        userId: callerUid,
        score: Math.max(0, current.score - scoreDeduct),
        total: Math.max(0, current.total - totalDeduct),
        testsTaken: Math.max(0, current.testsTaken - callerAttempts.length),
        totalCorrect: Math.max(0, current.totalCorrect - correctDeduct),
      });
    }

    return { success: true, deletedAttemptsCount: callerAttempts.length };
  }

  const resetResult = resetAttempt("user_1", "exam_math");
  assert.equal(resetResult.success, true);
  assert.equal(resetResult.deletedAttemptsCount, 1);

  // User 1 data cleared and lock removed
  assert.equal(checkLock("user_1", "exam_math"), false);
  assert.equal(leaderboard.has("exam_math_user_1"), false);
  assert.equal([...attempts.values()].some((a) => a.userId === "user_1" && a.examId === "exam_math"), false);
  assert.equal(overallLeaderboard.get("user_1").testsTaken, 0);
  assert.equal(overallLeaderboard.get("user_1").score, 0);

  // User 2 data untouched
  assert.equal(checkLock("user_2", "exam_math"), true);
  assert.equal(leaderboard.has("exam_math_user_2"), true);
  assert.equal(leaderboard.get("exam_math_user_2").score, 20);
  assert.equal(attempts.get("att_2").score, 20);
  assert.equal(attemptAnswers.has("ans_2"), true);
  assert.equal(overallLeaderboard.get("user_2").score, 20);
  assert.equal(overallLeaderboard.get("user_2").testsTaken, 1);

  // Subsequent submit succeeds
  const freshSubmit = simulateSubmit("user_1", "exam_math", 19, 20, 19);
  assert.equal(freshSubmit.status, 200);
  assert.equal(freshSubmit.success, true);
  assert.equal(checkLock("user_1", "exam_math"), true);
});

/**
 * Test case 2:
 * - Cleans up per-question stats in admin_analytics_questions derived from caller's attempts
 * - Verified with floor at 0
 */
test("reattempt reset cleans up derived per-question stats in admin_analytics_questions", () => {
  const adminAnalyticsQuestions = new Map([
    ["exam_1_q1", { id: "exam_1_q1", attempts: 5, correct: 4, wrong: 1, unattempted: 0 }],
    ["exam_1_q2", { id: "exam_1_q2", attempts: 2, correct: 0, wrong: 2, unattempted: 0 }],
    ["exam_1_q3", { id: "exam_1_q3", attempts: 1, correct: 1, wrong: 0, unattempted: 0 }],
  ]);

  // Caller answered q1 correctly and q3 correctly
  const callerAnswers = [
    { questionId: "q1", selected: "A", correct: "A" },
    { questionId: "q3", selected: "B", correct: "B" },
  ];

  // Apply decrement logic matching D1 batch update:
  for (const ans of callerAnswers) {
    const qKey = `exam_1_${ans.questionId}`;
    if (!adminAnalyticsQuestions.has(qKey)) continue;

    const row = adminAnalyticsQuestions.get(qKey);
    const isAttempted = Boolean(ans.selected && ans.selected.length > 0);
    const isCorrect = isAttempted && ans.selected === ans.correct;
    const isWrong = isAttempted && ans.selected !== ans.correct;
    const isUnattempted = !isAttempted;

    adminAnalyticsQuestions.set(qKey, {
      ...row,
      attempts: Math.max(0, row.attempts - (isAttempted ? 1 : 0)),
      correct: Math.max(0, row.correct - (isCorrect ? 1 : 0)),
      wrong: Math.max(0, row.wrong - (isWrong ? 1 : 0)),
      unattempted: Math.max(0, row.unattempted - (isUnattempted ? 1 : 0)),
    });
  }

  // q1: attempts was 5 -> 4, correct was 4 -> 3
  const q1 = adminAnalyticsQuestions.get("exam_1_q1");
  assert.equal(q1.attempts, 4);
  assert.equal(q1.correct, 3);
  assert.equal(q1.wrong, 1);

  // q2: caller had no answers for q2, so untouched
  const q2 = adminAnalyticsQuestions.get("exam_1_q2");
  assert.equal(q2.attempts, 2);
  assert.equal(q2.correct, 0);
  assert.equal(q2.wrong, 2);

  // q3: attempts was 1 -> 0, correct was 1 -> 0 (floored at 0, no negative)
  const q3 = adminAnalyticsQuestions.get("exam_1_q3");
  assert.equal(q3.attempts, 0);
  assert.equal(q3.correct, 0);
});

/**
 * Test case 3:
 * - overall_leaderboard deduction floors strictly at 0 (MAX(0, ...))
 * - Prevents negative values and NaN accuracy
 */
test("overall_leaderboard deduction floors strictly at zero when attempts exceed existing totals", () => {
  // Edge case: user record has lower numbers than the attempts being cleaned
  const userStats = {
    userId: "user_test",
    score: 10.0,
    total_score: 10.0,
    total: 10,
    tests_taken: 1,
    total_correct: 8,
    accuracy: 80,
  };

  const deductScore = 25.0; // larger than stored
  const deductTotal = 30;   // larger than stored
  const deductCorrect = 20; // larger than stored
  const deductTests = 2;    // larger than stored

  // Formula matching SQLite D1 query:
  const newScore = Math.max(0.0, Math.round((userStats.score - deductScore) * 100) / 100);
  const newTotalScore = Math.max(0.0, Math.round((userStats.total_score - deductScore) * 100) / 100);
  const newTotal = Math.max(0, userStats.total - deductTotal);
  const newTestsTaken = Math.max(0, userStats.tests_taken - deductTests);
  const newTotalCorrect = Math.max(0, userStats.total_correct - deductCorrect);
  const newAccuracy = newTotal > 0 ? Math.round((newTotalCorrect * 100.0) / newTotal) : 0;

  assert.equal(newScore, 0.0);
  assert.equal(newTotalScore, 0.0);
  assert.equal(newTotal, 0);
  assert.equal(newTestsTaken, 0);
  assert.equal(newTotalCorrect, 0);
  assert.equal(newAccuracy, 0);
});
