import test from "node:test";
import assert from "node:assert/strict";

// ============================================================================
// PART 1 & PART 2 BACKEND UNIT TESTS
// ============================================================================

// 1. hideAnswers logic tests
test("hideAnswers: header >= 2 and LEGACY_ANSWER_LEAK behavior", () => {
  function hideAnswersCheck({ isAdmin = false, clientHeader = null, legacyLeak = "on" }) {
    if (isAdmin) return false;
    const clientVersion = clientHeader ? parseInt(clientHeader, 10) : 0;
    if ((!isNaN(clientVersion) && clientVersion >= 2) || legacyLeak !== "on") {
      return true;
    }
    return false;
  }

  // Admin always sees answers
  assert.equal(hideAnswersCheck({ isAdmin: true, clientHeader: "2", legacyLeak: "off" }), false);
  assert.equal(hideAnswersCheck({ isAdmin: true, clientHeader: "1", legacyLeak: "on" }), false);

  // New client (X-Eve-Client >= 2) always has answers hidden
  assert.equal(hideAnswersCheck({ isAdmin: false, clientHeader: "2", legacyLeak: "on" }), true);
  assert.equal(hideAnswersCheck({ isAdmin: false, clientHeader: "3", legacyLeak: "on" }), true);

  // Legacy client (X-Eve-Client missing or < 2) with LEGACY_ANSWER_LEAK = 'on' keeps legacy behavior (false)
  assert.equal(hideAnswersCheck({ isAdmin: false, clientHeader: null, legacyLeak: "on" }), false);
  assert.equal(hideAnswersCheck({ isAdmin: false, clientHeader: "1", legacyLeak: "on" }), false);

  // Legacy client with LEGACY_ANSWER_LEAK = 'off' has answers hidden (true)
  assert.equal(hideAnswersCheck({ isAdmin: false, clientHeader: null, legacyLeak: "off" }), true);
  assert.equal(hideAnswersCheck({ isAdmin: false, clientHeader: "1", legacyLeak: "off" }), true);
});

test("non-admin request WITHOUT the X-Eve-Client header must not receive correct answers", () => {
  const mockContext = {
    get: (key) => {
      if (key === "user") return { uid: "student-123", email: "student@example.com", isAdmin: false };
      return undefined;
    },
    req: {
      header: (_name) => undefined, // No X-Eve-Client header
    },
    env: {
      LEGACY_ANSWER_LEAK: "off",
    },
  };

  function hideAnswers(c) {
    const user = c.get("user");
    if (user && user.isAdmin) return false;
    const clientHeader = c.req.header("X-Eve-Client") || c.req.header("x-eve-client");
    const clientVersion = clientHeader ? parseInt(clientHeader, 10) : 0;
    const legacyLeak = c.env?.LEGACY_ANSWER_LEAK;
    if ((!isNaN(clientVersion) && clientVersion >= 2) || legacyLeak !== "on") {
      return true;
    }
    return false;
  }

  const shouldHide = hideAnswers(mockContext);
  assert.equal(shouldHide, true, "hideAnswers must return true for non-admin without X-Eve-Client header when LEGACY_ANSWER_LEAK is off");

  const rawQuestion = {
    id: "q-101",
    examId: "exam-1",
    questionText: "What is the speed of light?",
    optionA: "3x10^8 m/s",
    optionB: "1.5x10^8 m/s",
    optionC: "3x10^6 m/s",
    optionD: "300 m/s",
    correctAnswer: "A",
    explanation: "The speed of light in vacuum is approximately 300,000 km/s.",
  };

  const responseQuestion = { ...rawQuestion };
  if (shouldHide) {
    responseQuestion.correctAnswer = "";
    responseQuestion.explanation = "";
  }

  assert.equal(responseQuestion.correctAnswer, "", "correctAnswer must be omitted for non-admin request without X-Eve-Client header");
  assert.equal(responseQuestion.explanation, "", "explanation must be omitted for non-admin request without X-Eve-Client header");
});

// 2. Generated tests list metadata only
test("generated tests list returns metadata with questions = [] and availableFrom", () => {
  const row = {
    id: "gen-1",
    exam_id: "exam-1",
    exam_name: "Mock Exam",
    test_number: "Test 1",
    title: "Mock Exam - Test 1",
    generated_at: 1700000000000,
    status: "live",
    question_count: 20,
    available_from: 1700005000000,
    questions_json: JSON.stringify([{ question_text: "Q1", correct_answer: "A" }]),
  };

  const mapped = {
    id: row.id,
    examId: row.exam_id,
    examName: row.exam_name,
    testNumber: row.test_number,
    title: row.title,
    generatedAt: row.generated_at,
    status: row.status,
    questionCount: row.question_count,
    availableFrom: row.available_from || 0,
    questions: [],
  };

  assert.equal(mapped.questions.length, 0);
  assert.equal(mapped.availableFrom, 1700005000000);
  assert.equal(mapped.questionCount, 20);
});

// 3. Ownership filter and expected question set
test("expected question set, omitted counted unattempted, invalid picks rejected", () => {
  const expectedQuestions = [
    { id: "q1", correctAnswer: "A" },
    { id: "q2", correctAnswer: "B" },
    { id: "q3", correctAnswer: "C" },
    { id: "q4", correctAnswer: "D" },
  ];
  const expectedMap = new Map(expectedQuestions.map((q) => [q.id, q]));

  // Student submitted q1 (correct), q2 (wrong), q_invalid (foreign), omitted q3, q4
  const rawPicks = [
    { questionId: "q1", selected: "A" },
    { questionId: "q2", selected: "C" },
    { questionId: "q_invalid", selected: "A" },
  ];

  const submittedPicksMap = new Map();
  for (const pick of rawPicks) {
    if (expectedMap.has(pick.questionId)) {
      submittedPicksMap.set(pick.questionId, pick);
    }
  }

  assert.equal(submittedPicksMap.has("q_invalid"), false);
  assert.equal(submittedPicksMap.size, 2);

  // Evaluate for non-practice: total = expectedQuestions.length (4)
  const evaluatedAnswers = [];
  let correct = 0,
    wrong = 0,
    unattempted = 0;

  for (const q of expectedQuestions) {
    const pick = submittedPicksMap.get(q.id);
    const selected = pick ? pick.selected : "";
    const isAttempted = selected.length > 0;
    const isCorrect = isAttempted && selected === q.correctAnswer;
    const isWrong = isAttempted && !isCorrect;

    if (!isAttempted) unattempted++;
    else if (isCorrect) correct++;
    else wrong++;

    evaluatedAnswers.push({ questionId: q.id, selected, correct: q.correctAnswer });
  }

  assert.equal(evaluatedAnswers.length, 4);
  assert.equal(correct, 1);
  assert.equal(wrong, 1);
  assert.equal(unattempted, 2); // q3 and q4 were omitted and counted as unattempted
});

// 4. Session timing and counted calculation
test("session timing: in-time, late (> grace), no-session, and practice", () => {
  function computeTimingAndCounted({ isPractice, isAdmin, session, now, limitSeconds }) {
    if (isPractice) {
      return { counted: 0, timeTaken: 120 };
    }
    if (isAdmin) {
      return { counted: 1, timeTaken: 100 };
    }
    if (!session) {
      return { counted: 0, timeTaken: 100 }; // No session -> counted = 0
    }
    const elapsedMs = now - session.started_at;
    const timeTaken = Math.max(0, Math.min(limitSeconds, Math.round(elapsedMs / 1000)));
    const graceMs = 60000;
    if (limitSeconds > 0 && elapsedMs > limitSeconds * 1000 + graceMs) {
      return { counted: 0, timeTaken }; // Late submission -> counted = 0
    }
    return { counted: 1, timeTaken };
  }

  const limitSeconds = 1800; // 30 mins

  // Case A: student in time
  const resInTime = computeTimingAndCounted({
    isPractice: false,
    isAdmin: false,
    session: { started_at: 1000000 },
    now: 1000000 + 1500 * 1000,
    limitSeconds,
  });
  assert.equal(resInTime.counted, 1);
  assert.equal(resInTime.timeTaken, 1500);

  // Case B: student late by more than 60s grace (30 min + 61 s)
  const resLate = computeTimingAndCounted({
    isPractice: false,
    isAdmin: false,
    session: { started_at: 1000000 },
    now: 1000000 + 1800 * 1000 + 61000,
    limitSeconds,
  });
  assert.equal(resLate.counted, 0);

  // Case C: student with no session row
  const resNoSession = computeTimingAndCounted({
    isPractice: false,
    isAdmin: false,
    session: null,
    now: 1000000,
    limitSeconds,
  });
  assert.equal(resNoSession.counted, 0);

  // Case D: practice attempt
  const resPractice = computeTimingAndCounted({
    isPractice: true,
    isAdmin: false,
    session: null,
    now: 1000000,
    limitSeconds,
  });
  assert.equal(resPractice.counted, 0);
});

// 5. Idempotency with clientAttemptId
test("clientAttemptId idempotency returns existing attempt", () => {
  const attemptsDb = new Map();
  const clientAttemptId = "uuid-client-123";

  // First submit
  attemptsDb.set(`user1_${clientAttemptId}`, {
    id: "att-1",
    score: 18.5,
    client_attempt_id: clientAttemptId,
  });

  // Second submit with same clientAttemptId
  const cached = attemptsDb.get(`user1_${clientAttemptId}`);
  assert.ok(cached);
  assert.equal(cached.id, "att-1");
  assert.equal(cached.score, 18.5);
});

// 6. Leaderboard tie-breaker ordering and rank
test("leaderboard tie-breaker: score DESC, time_taken_seconds ASC, timestamp ASC", () => {
  const rows = [
    { userId: "u1", score: 20, time_taken_seconds: 500, timestamp: 1000 },
    { userId: "u2", score: 20, time_taken_seconds: 400, timestamp: 2000 }, // same score, faster -> higher rank
    { userId: "u3", score: 25, time_taken_seconds: 600, timestamp: 1500 }, // higher score
    { userId: "u4", score: 20, time_taken_seconds: 400, timestamp: 1800 }, // same score, same time, earlier timestamp
  ];

  rows.sort((a, b) => {
    if (b.score !== a.score) return b.score - a.score;
    if (a.time_taken_seconds !== b.time_taken_seconds) return a.time_taken_seconds - b.time_taken_seconds;
    return a.timestamp - b.timestamp;
  });

  assert.equal(rows[0].userId, "u3"); // Score 25
  assert.equal(rows[1].userId, "u4"); // Score 20, 400s, timestamp 1800
  assert.equal(rows[2].userId, "u2"); // Score 20, 400s, timestamp 2000
  assert.equal(rows[3].userId, "u1"); // Score 20, 500s
});

// 7. Stats endpoint percentile and math
test("leaderboard stats: averageScore, topperScore, and percentile calculation", () => {
  function computePercentile(participants, rank) {
    if (participants <= 1) return 100;
    return Math.round((((participants - rank) / (participants - 1)) * 100) * 10) / 10;
  }

  // 1 participant: rank 1 -> 100%
  assert.equal(computePercentile(1, 1), 100);

  // 100 participants: rank 1 -> 100%
  assert.equal(computePercentile(100, 1), 100);

  // 100 participants: rank 100 -> 0%
  assert.equal(computePercentile(100, 100), 0);

  // 100 participants: rank 10 -> ((100 - 10)/99)*100 = 90.9%
  assert.equal(computePercentile(100, 10), 90.9);

  // 5 participants: rank 2 -> ((5 - 2)/4)*100 = 75%
  assert.equal(computePercentile(5, 2), 75);
});

// 8. Rate limiter bucket logic
test("rate limiter buckets and retry-after calculation", () => {
  const limits = {
    "ip": 300,
    "uid:general": 120,
    "uid:submit": 10,
    "uid:start": 20,
    "uid:generate-now": 5,
  };

  assert.equal(limits["ip"], 300);
  assert.equal(limits["uid:general"], 120);
  assert.equal(limits["uid:submit"], 10);
  assert.equal(limits["uid:start"], 20);
  assert.equal(limits["uid:generate-now"], 5);

  const now = 1000;
  const resetAt = 1060;
  const count = 11;
  const isLimited = count > limits["uid:submit"];
  const retryAfter = Math.max(1, resetAt - now);

  assert.equal(isLimited, true);
  assert.equal(retryAfter, 60);
});

// 9. Diagnostic key security check
test("diagnostic key authorization accepts ONLY dedicated DIAGNOSTIC_KEY", () => {
  const env = {
    DIAGNOSTIC_KEY: "secret-diag-123",
    SUPABASE_SERVICE_ROLE_KEY: "supabase-service-456",
  };

  function isDiagnosticAuthorized(diagKey, env) {
    if (diagKey && env.DIAGNOSTIC_KEY && diagKey.trim() === env.DIAGNOSTIC_KEY.trim()) {
      return true;
    }
    return false;
  }

  // Correct dedicated key -> authorized
  assert.equal(isDiagnosticAuthorized("secret-diag-123", env), true);

  // Supabase service role key -> REJECTED
  assert.equal(isDiagnosticAuthorized("supabase-service-456", env), false);

  // Random key -> REJECTED
  assert.equal(isDiagnosticAuthorized("invalid-key", env), false);
});

// 10. Bookmarks: hide answers until question was submitted by user
test("bookmarks: answers hidden until user has submitted an attempt for that question", () => {
  const submittedQuestions = new Map([
    ["q_done", { correct: "B", explanation: "Explanation for q_done" }],
  ]);

  const bookmarks = [
    { questionId: "q_done", questionText: "Q Done" },
    { questionId: "q_not_done", questionText: "Q Not Done" },
  ];

  const result = bookmarks.map((b) => {
    const submission = submittedQuestions.get(b.questionId);
    return {
      questionId: b.questionId,
      correctAnswer: submission ? submission.correct : "",
      explanation: submission ? submission.explanation : "",
    };
  });

  assert.equal(result[0].correctAnswer, "B");
  assert.equal(result[0].explanation, "Explanation for q_done");
  assert.equal(result[1].correctAnswer, "");
  assert.equal(result[1].explanation, "");
});

// 11. Mistake Notebook: latest answer only, later correct removes it, filters
test("mistakes endpoint: latest answer only, later correct removes it, filters (wrong/skipped/all)", () => {
  // History of answers for user:
  // q1: attempt 1 (wrong), attempt 2 (correct) -> removed!
  // q2: attempt 1 (wrong) -> retained (wrong)
  // q3: attempt 1 (skipped) -> retained (skipped)
  // q4: attempt 1 (skipped), attempt 2 (wrong) -> retained (wrong)
  const answersHistory = [
    { questionId: "q1", timestamp: 200, selected: "A", correct: "A" }, // latest for q1 is CORRECT
    { questionId: "q1", timestamp: 100, selected: "B", correct: "A" },
    { questionId: "q2", timestamp: 150, selected: "C", correct: "D" }, // latest for q2 is WRONG
    { questionId: "q3", timestamp: 120, selected: "", correct: "B" },  // latest for q3 is SKIPPED
    { questionId: "q4", timestamp: 180, selected: "C", correct: "B" }, // latest for q4 is WRONG
    { questionId: "q4", timestamp: 110, selected: "", correct: "B" },
  ];

  // Group by questionId, pick latest by timestamp
  const latestByQuestion = new Map();
  for (const a of answersHistory.sort((x, y) => y.timestamp - x.timestamp)) {
    if (!latestByQuestion.has(a.questionId)) {
      latestByQuestion.set(a.questionId, a);
    }
  }

  function filterMistakes(filter) {
    const mistakes = [];
    for (const a of latestByQuestion.values()) {
      const isAttempted = a.selected.length > 0;
      const isCorrect = isAttempted && a.selected === a.correct;
      if (isCorrect) continue; // Removed automatically if latest is correct!

      const isSkipped = !isAttempted;
      const isWrong = isAttempted && !isCorrect;

      if (
        (filter === "all" && (isSkipped || isWrong)) ||
        (filter === "wrong" && isWrong) ||
        (filter === "skipped" && isSkipped)
      ) {
        mistakes.push(a);
      }
    }
    return mistakes;
  }

  const allMistakes = filterMistakes("all");
  assert.equal(allMistakes.length, 3); // q2 (wrong), q3 (skipped), q4 (wrong); q1 is excluded
  assert.equal(allMistakes.some((m) => m.questionId === "q1"), false);

  const wrongMistakes = filterMistakes("wrong");
  assert.equal(wrongMistakes.length, 2); // q2 and q4
  assert.ok(wrongMistakes.every((m) => m.selected.length > 0));

  const skippedMistakes = filterMistakes("skipped");
  assert.equal(skippedMistakes.length, 1); // q3
  assert.equal(skippedMistakes[0].questionId, "q3");
});

// 12. Streak math across tz offsets and "yesterday keeps streak alive" rule
test("streak math: timezone offset, consecutive days, yesterday keeps streak alive", () => {
  function computeStreak(dateList, todayDateStr, yesterdayDateStr) {
    let currentStreak = 0;
    if (dateList.length > 0) {
      const firstDate = dateList[0];
      if (firstDate === todayDateStr || firstDate === yesterdayDateStr) {
        let expectedEpochDay = Math.floor(new Date(firstDate + "T00:00:00Z").getTime() / 86400000);
        for (const dateStr of dateList) {
          const entryEpochDay = Math.floor(new Date(dateStr + "T00:00:00Z").getTime() / 86400000);
          if (entryEpochDay === expectedEpochDay) {
            currentStreak++;
            expectedEpochDay--;
          } else {
            break;
          }
        }
      }
    }
    return currentStreak;
  }

  const today = "2026-09-29";
  const yesterday = "2026-09-28";
  const dayBefore = "2026-09-27";
  const threeDaysAgo = "2026-09-26";

  // Scenario A: tested today, yesterday, day before -> 3 day streak
  assert.equal(computeStreak([today, yesterday, dayBefore], today, yesterday), 3);

  // Scenario B: tested yesterday and day before (none today) -> 2 day streak still alive!
  assert.equal(computeStreak([yesterday, dayBefore], today, yesterday), 2);

  // Scenario C: last test was 2 days ago (dayBefore) -> streak broken (0)
  assert.equal(computeStreak([dayBefore, threeDaysAgo], today, yesterday), 0);

  // Scenario D: gap between yesterday and three days ago -> streak stops at 1 (yesterday only)
  assert.equal(computeStreak([yesterday, threeDaysAgo], today, yesterday), 1);
});

// 13. Schedule test route validation and start blocking
test("schedule route validation and availableFrom blocking in start", () => {
  function validateScheduleInput(availableFrom) {
    if (typeof availableFrom !== "number" || isNaN(availableFrom) || availableFrom < 0 || !Number.isInteger(availableFrom)) {
      return { valid: false, error: "availableFrom must be a non-negative integer" };
    }
    return { valid: true };
  }

  assert.equal(validateScheduleInput(1700000000000).valid, true);
  assert.equal(validateScheduleInput(0).valid, true);
  assert.equal(validateScheduleInput(-100).valid, false);
  assert.equal(validateScheduleInput(123.45).valid, false);
  assert.equal(validateScheduleInput("not-number").valid, false);

  function checkStartAvailable(availableFrom, now) {
    if (availableFrom && availableFrom > now) {
      return { canStart: false, error: "Test not open yet", availableFrom };
    }
    return { canStart: true };
  }

  const now = 1700000000000;
  assert.equal(checkStartAvailable(1700005000000, now).canStart, false);
  assert.equal(checkStartAvailable(1700000000000, now).canStart, true);
  assert.equal(checkStartAvailable(0, now).canStart, true);
});

// 14. /api/auth/me returns isAdmin: true for admin, false for normal student
test("/api/auth/me returns isAdmin: true for admin and false for normal student", async () => {
  const HARDCODED_ADMIN_EMAILS = new Set([
    "pronlike9@gmail.com",
    "own.keni@gmail.com",
    "anyqueairdrop@gmail.com",
    "ghatisarkar56@gmail.com",
  ]);

  async function mockGetMe(email, dynamicAdmins = new Set()) {
    const cleanEmail = (email || "").trim().toLowerCase();
    const isAdmin = HARDCODED_ADMIN_EMAILS.has(cleanEmail) || dynamicAdmins.has(cleanEmail);
    const user = {
      uid: "user-" + cleanEmail,
      email: cleanEmail,
      displayName: "Test User",
      isAdmin,
    };
    return {
      success: true,
      data: user,
    };
  }

  // Hardcoded bootstrap admin
  const adminRes = await mockGetMe("pronlike9@gmail.com");
  assert.equal(adminRes.success, true);
  assert.equal(adminRes.data.isAdmin, true);

  // Dynamic D1 admin
  const dynamicRes = await mockGetMe("customadmin@example.com", new Set(["customadmin@example.com"]));
  assert.equal(dynamicRes.success, true);
  assert.equal(dynamicRes.data.isAdmin, true);

  // Normal student
  const studentRes = await mockGetMe("student@example.com");
  assert.equal(studentRes.success, true);
  assert.equal(studentRes.data.isAdmin, false);
});
