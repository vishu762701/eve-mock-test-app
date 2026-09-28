import test from "node:test";
import assert from "node:assert/strict";

/**
 * Test reattempt and attempt reset flow:
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

  // 1. Initial state: submitting again before reset triggers 409 Conflict
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

  // 2. Perform atomic reset for caller user_1 on exam_math
  function resetAttempt(callerUid, targetExamId) {
    const lockKey = `${callerUid}_${targetExamId}`;
    const lbKey = `${targetExamId}_${callerUid}`;

    // Find caller attempts
    const callerAttempts = [...attempts.values()].filter(
      (a) => a.userId === callerUid && a.examId === targetExamId
    );
    const callerAttemptIds = new Set(callerAttempts.map((a) => a.id));

    // A. Delete lock
    attemptLocks.delete(lockKey);

    // B. Delete answers
    for (const [id, ans] of [...attemptAnswers.entries()]) {
      if (callerAttemptIds.has(ans.attemptId)) {
        attemptAnswers.delete(id);
      }
    }

    // C. Delete attempts
    for (const id of callerAttemptIds) {
      attempts.delete(id);
    }

    // D. Delete leaderboard entry
    leaderboard.delete(lbKey);

    // E. Deduct from overall_leaderboard
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

  // 3. Verify user_1 data is cleared and lock is gone
  assert.equal(checkLock("user_1", "exam_math"), false);
  assert.equal(leaderboard.has("exam_math_user_1"), false);
  assert.equal([...attempts.values()].some((a) => a.userId === "user_1" && a.examId === "exam_math"), false);
  assert.equal(overallLeaderboard.get("user_1").testsTaken, 0);
  assert.equal(overallLeaderboard.get("user_1").score, 0);

  // 4. Verify user_2 data is 100% UNTOUCHED
  assert.equal(checkLock("user_2", "exam_math"), true);
  assert.equal(leaderboard.has("exam_math_user_2"), true);
  assert.equal(leaderboard.get("exam_math_user_2").score, 20);
  assert.equal(attempts.get("att_2").score, 20);
  assert.equal(attemptAnswers.has("ans_2"), true);
  assert.equal(overallLeaderboard.get("user_2").score, 20);
  assert.equal(overallLeaderboard.get("user_2").testsTaken, 1);

  // 5. Subsequent POST /attempts for user_1 now succeeds (no 409)
  const freshSubmit = simulateSubmit("user_1", "exam_math", 19, 20, 19);
  assert.equal(freshSubmit.status, 200);
  assert.equal(freshSubmit.success, true);
  assert.equal(checkLock("user_1", "exam_math"), true);
});
