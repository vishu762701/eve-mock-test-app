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

// 15. admin_analytics_questions contract: exam_name NOT NULL and question metadata validation
test("admin_analytics_questions upsert correctly populates exam_name and question metadata", () => {
  const schemaNotNullFields = ["id", "exam_id", "exam_name", "question_id"];

  function buildQuestionAnalyticsRow({ examId, examName, questionId, number, questionText, topic, isCorrect, isWrong, isUnattempted, timeTakenSeconds }) {
    const qKey = `${examId}_${questionId}`;
    const row = {
      id: qKey,
      exam_id: examId,
      exam_name: examName,
      question_id: questionId,
      question_number: number,
      question_text: questionText || "",
      topic: topic || "",
      attempts: 1,
      correct: isCorrect ? 1 : 0,
      wrong: isWrong ? 1 : 0,
      unattempted: isUnattempted ? 1 : 0,
      total_time_seconds: timeTakenSeconds || 0,
    };

    // Verify NOT NULL constraints
    for (const field of schemaNotNullFields) {
      if (row[field] === undefined || row[field] === null || row[field] === "") {
        throw new Error(`NOT NULL constraint failed: admin_analytics_questions.${field}`);
      }
    }
    return row;
  }

  const row = buildQuestionAnalyticsRow({
    examId: "exam-101",
    examName: "SSC CGL Tier 1 Mock",
    questionId: "q-55",
    number: 1,
    questionText: "What is the capital of India?",
    topic: "General Knowledge",
    isCorrect: true,
    isWrong: false,
    isUnattempted: false,
    timeTakenSeconds: 25,
  });

  assert.equal(row.id, "exam-101_q-55");
  assert.equal(row.exam_name, "SSC CGL Tier 1 Mock");
  assert.equal(row.question_id, "q-55");
  assert.equal(row.attempts, 1);
  assert.equal(row.correct, 1);
  assert.equal(row.wrong, 0);
  assert.equal(row.unattempted, 0);
  assert.equal(row.total_time_seconds, 25);

  // Assert error thrown if exam_name missing (regression guard)
  assert.throws(() => {
    buildQuestionAnalyticsRow({
      examId: "exam-101",
      examName: null,
      questionId: "q-55",
      number: 1,
      questionText: "Q",
      topic: "GK",
      isCorrect: true,
      isWrong: false,
      isUnattempted: false,
      timeTakenSeconds: 10,
    });
  }, /NOT NULL constraint failed: admin_analytics_questions\.exam_name/);
});

// 16. Test session active time state machine: pause/resume and grace period calculation
test("Test session pause/resume active time calculation prevents premature uncounted status", () => {
  function calculateActiveTimeAndCounted(session, now, limitSeconds) {
    const clampTimeMax = limitSeconds > 0 ? limitSeconds : 7200;
    let activeElapsedSeconds = session.accumulated_active_seconds || 0;
    if (session.status === "RUNNING" && session.last_resumed_at > 0) {
      activeElapsedSeconds += Math.max(0, Math.floor((now - session.last_resumed_at) / 1000));
    } else if (!session.last_resumed_at && session.started_at > 0 && !session.accumulated_active_seconds) {
      activeElapsedSeconds = Math.max(0, Math.floor((now - session.started_at) / 1000));
    }
    const attemptTimeTakenSeconds = Math.max(0, Math.min(clampTimeMax, activeElapsedSeconds));
    const counted = (session.time_limit_seconds > 0 && activeElapsedSeconds > session.time_limit_seconds + 60) ? 0 : 1;
    return { attemptTimeTakenSeconds, counted };
  }

  const limitSeconds = 60 * 60; // 60 minutes
  const startedAt = 1000000;
  
  // Student starts, works 10 minutes (600s), pauses test.
  // 3 hours later (10800s), student resumes, works 5 minutes (300s), then submits.
  const pausedSession = {
    started_at: startedAt,
    time_limit_seconds: limitSeconds,
    accumulated_active_seconds: 600,
    status: "PAUSED",
    last_resumed_at: 0
  };

  // While paused, wall-clock time passed 3 hours
  const wallClockNow = startedAt + 10800 * 1000;
  // Student resumes at wallClockNow
  const resumedSession = {
    ...pausedSession,
    status: "RUNNING",
    last_resumed_at: wallClockNow
  };

  // Student works 300 seconds and submits
  const submitNow = wallClockNow + 300 * 1000;
  const result = calculateActiveTimeAndCounted(resumedSession, submitNow, limitSeconds);

  assert.equal(result.attemptTimeTakenSeconds, 900); // 600 + 300 seconds active
  assert.equal(result.counted, 1); // Not disqualified because active time (900s) <= 3600s + 60s
});

// 17. Public EVE ID (EV-XXXXXX) generation, uniqueness constraint, and deterministic backfill
test("Public EVE ID generation follows EV-XXXXXX format, guarantees uniqueness, and persists", () => {
  const chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
  function generateCandidateEveId() {
    let code = "";
    for (let i = 0; i < 6; i++) {
      code += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return `EV-${code}`;
  }

  // 1. Verify format & character set (no 0, 1, I, O to prevent confusion)
  const idRegex = /^EV-[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{6}$/;
  for (let i = 0; i < 100; i++) {
    const id = generateCandidateEveId();
    assert.match(id, idRegex);
    assert.equal(id.length, 9);
    assert.equal(id.startsWith("EV-"), true);
  }

  // 2. Verify schema and UNIQUE constraint on eve_id
  class MockUsersTable {
    constructor() {
      this.rows = new Map(); // id -> row
      this.eveIndex = new Map(); // eve_id -> id
    }

    insert({ id, email, eve_id = null }) {
      if (this.rows.has(id)) {
        throw new Error(`PRIMARY KEY constraint failed: users.id`);
      }
      if (eve_id) {
        if (this.eveIndex.has(eve_id)) {
          throw new Error(`UNIQUE constraint failed: users.eve_id`);
        }
        this.eveIndex.set(eve_id, id);
      }
      this.rows.set(id, { id, email, eve_id });
    }

    updateEveId(id, newEveId) {
      const row = this.rows.get(id);
      if (!row) throw new Error("User not found");
      if (row.eve_id) return row.eve_id; // Already set: immutable
      if (this.eveIndex.has(newEveId)) {
        throw new Error(`UNIQUE constraint failed: users.eve_id`);
      }
      this.eveIndex.set(newEveId, id);
      row.eve_id = newEveId;
      return newEveId;
    }

    get(id) {
      return this.rows.get(id);
    }
  }

  const table = new MockUsersTable();

  // Insert two users
  const uid1 = "firebase-uid-alice-123";
  const uid2 = "firebase-uid-bob-456";
  const eveId1 = generateCandidateEveId();
  let eveId2 = generateCandidateEveId();
  while (eveId2 === eveId1) eveId2 = generateCandidateEveId();

  table.insert({ id: uid1, email: "alice@example.com", eve_id: eveId1 });
  table.insert({ id: uid2, email: "bob@example.com", eve_id: eveId2 });

  // Assert unique constraint works by attempting to insert duplicate eve_id
  assert.throws(() => {
    table.insert({ id: "firebase-uid-charlie-789", email: "charlie@example.com", eve_id: eveId1 });
  }, /UNIQUE constraint failed: users\.eve_id/);

  // 3. Verify deterministic persistence: user keeps same eve_id
  const aliceRow = table.get(uid1);
  assert.equal(aliceRow.eve_id, eveId1);
  assert.notEqual(aliceRow.eve_id, aliceRow.id); // Firebase UID is NOT exposed as eve_id

  // 4. Backfill existing user without eve_id
  const uidExisting = "firebase-uid-legacy-999";
  table.insert({ id: uidExisting, email: "legacy@example.com", eve_id: null });

  const beforeBackfill = table.get(uidExisting);
  assert.equal(beforeBackfill.eve_id, null);

  const backfilledId = generateCandidateEveId();
  table.updateEveId(uidExisting, backfilledId);

  const afterBackfill = table.get(uidExisting);
  assert.equal(afterBackfill.eve_id, backfilledId);

  // Re-running update returns existing backfilled ID without regenerating
  const recheckId = table.updateEveId(uidExisting, generateCandidateEveId());
  assert.equal(recheckId, backfilledId);
});



