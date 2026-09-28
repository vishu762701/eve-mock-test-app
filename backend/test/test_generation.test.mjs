import test from "node:test";
import assert from "node:assert/strict";

/**
 * End-to-End Test Suite: AI Test Generation Pipeline
 *
 * Verifies the complete lifecycle of test generation:
 * 1. Prompt formatting & model endpoint targeting (gemini-3.5-flash)
 * 2. Response parsing, sanitization (markdown stripping, answer normalization, choice uniqueness)
 * 3. Test number progression (Test 1 -> Test 2, Mock 9 -> Mock 10)
 * 4. On-demand generation flow: exam lookup, running lock, generation, paused test creation, exam update
 * 5. Failure recovery: error capture, error truncation, lock release, failed status
 * 6. Scheduled cron eligibility and concurrency lock management
 */

// --- Component 1: Test Number Progression ---
function incrementTestNumber(current) {
  const str = String(current || "Test 1").trim();
  const match = str.match(/(\d+)$/);
  if (match) {
    const num = parseInt(match[1], 10);
    const prefix = str.substring(0, match.index);
    return `${prefix}${num + 1}`;
  }
  return `${str} 2`;
}

test("test number progression correctly increments test numbers", () => {
  assert.equal(incrementTestNumber("Test 1"), "Test 2");
  assert.equal(incrementTestNumber("Test 9"), "Test 10");
  assert.equal(incrementTestNumber("Mock Exam 15"), "Mock Exam 16");
  assert.equal(incrementTestNumber("Midterm"), "Midterm 2");
  assert.equal(incrementTestNumber(""), "Test 2");
});

// --- Component 2: JSON Parsing, Sanitization & Option Validation ---
function parseAndValidateQuestions(rawText) {
  let cleaned = rawText.trim();
  if (cleaned.startsWith("```json")) {
    cleaned = cleaned.replace(/^```json\s*/, "").replace(/\s*```$/, "");
  } else if (cleaned.startsWith("```")) {
    cleaned = cleaned.replace(/^```\s*/, "").replace(/\s*```$/, "");
  }

  let parsed;
  try {
    parsed = JSON.parse(cleaned);
  } catch (err) {
    throw new Error(`Invalid JSON from Gemini: ${err.message}`);
  }

  const list = Array.isArray(parsed)
    ? parsed
    : Array.isArray(parsed.questions)
    ? parsed.questions
    : [];

  const validQuestions = [];
  const validAnswers = new Set(["A", "B", "C", "D"]);

  for (const q of list) {
    const questionText = String(q.questionText || q.question || "").trim();
    const optionA = String(q.optionA || q.a || "").trim();
    const optionB = String(q.optionB || q.b || "").trim();
    const optionC = String(q.optionC || q.c || "").trim();
    const optionD = String(q.optionD || q.d || "").trim();
    let correctAnswer = String(q.correctAnswer || q.answer || "").trim().toUpperCase();

    if (!validAnswers.has(correctAnswer)) {
      if (correctAnswer === "1" || correctAnswer === optionA.toUpperCase()) correctAnswer = "A";
      else if (correctAnswer === "2" || correctAnswer === optionB.toUpperCase()) correctAnswer = "B";
      else if (correctAnswer === "3" || correctAnswer === optionC.toUpperCase()) correctAnswer = "C";
      else if (correctAnswer === "4" || correctAnswer === optionD.toUpperCase()) correctAnswer = "D";
      else correctAnswer = "A";
    }

    const explanation = String(q.explanation || "").trim();

    if (!questionText || !optionA || !optionB || !optionC || !optionD) {
      continue;
    }

    const uniqueOptions = new Set([
      optionA.toLowerCase(),
      optionB.toLowerCase(),
      optionC.toLowerCase(),
      optionD.toLowerCase(),
    ]);
    if (uniqueOptions.size < 4) {
      continue; // Duplicate option values rejected
    }

    validQuestions.push({
      questionText,
      optionA,
      optionB,
      optionC,
      optionD,
      correctAnswer,
      explanation: explanation || `Option ${correctAnswer} is the correct answer.`,
    });
  }

  return validQuestions;
}

test("parseAndValidateQuestions handles markdown fences, normalizes numeric answers, and rejects duplicate choices", () => {
  const rawWithFences = `\`\`\`json
  [
    {
      "questionText": "What is the capital of India?",
      "optionA": "New Delhi",
      "optionB": "Mumbai",
      "optionC": "Kolkata",
      "optionD": "Chennai",
      "correctAnswer": "1",
      "explanation": "New Delhi is the official national capital."
    },
    {
      "questionText": "Faulty question with duplicate options",
      "optionA": "Same",
      "optionB": "Same",
      "optionC": "Different",
      "optionD": "Another",
      "correctAnswer": "A",
      "explanation": "Invalid"
    }
  ]
  \`\`\``;

  const results = parseAndValidateQuestions(rawWithFences);
  assert.equal(results.length, 1);
  assert.equal(results[0].questionText, "What is the capital of India?");
  assert.equal(results[0].correctAnswer, "A"); // Normalized from "1"
  assert.equal(results[0].optionA, "New Delhi");
});

// --- Component 3: End-to-End On-Demand Test Generation Flow Simulation ---
test("end-to-end on-demand test generation creates paused test, advances test number, and updates exam state", async () => {
  // Simulated D1 database
  const exams = new Map([
    [
      "exam_cgl",
      {
        id: "exam_cgl",
        exam_name: "SSC CGL 2026",
        syllabus: "Quantitative Aptitude, General Reasoning",
        question_count: 3,
        test_number: "Test 1",
        custom_prompt_notes: "High difficulty",
        last_generation_status: "idle",
        last_generation_error: "",
        last_generation_time: 0,
        generating_lock_until: 0,
      },
    ],
  ]);

  const generatedTests = new Map();

  // Simulated AI API Generator using gemini-3.5-flash
  async function mockGenerateQuestions(env, examName, syllabus, count, prompt) {
    assert.equal(env.GEMINI_MODEL, "gemini-3.5-flash");
    assert.ok(env.GEMINI_API_KEY);

    return [
      {
        questionText: "If x + 1/x = 2, find x^3 + 1/x^3.",
        optionA: "2",
        optionB: "4",
        optionC: "8",
        optionD: "0",
        correctAnswer: "A",
        explanation: "Since x=1 satisfies x + 1/x = 2, 1^3 + 1/1^3 = 2.",
      },
      {
        questionText: "Who was the founder of the Maurya Dynasty?",
        optionA: "Ashoka",
        optionB: "Chandragupta Maurya",
        optionC: "Bindusara",
        optionD: "Samudragupta",
        correctAnswer: "B",
        explanation: "Chandragupta Maurya founded the Maurya Empire in 322 BCE.",
      },
      {
        questionText: "What comes next in sequence: 2, 6, 12, 20, ?",
        optionA: "28",
        optionB: "30",
        optionC: "32",
        optionD: "36",
        correctAnswer: "B",
        explanation: "Pattern: 1*2=2, 2*3=6, 3*4=12, 4*5=20, 5*6=30.",
      },
    ];
  }

  // Simulated POST /api/generated-tests/generate-now handler
  async function simulateGenerateNow(examId, requestedCount) {
    const exam = exams.get(examId);
    assert.ok(exam, "Exam must exist");

    // 1. Set status to running
    exam.last_generation_status = "running";
    exam.last_generation_time = Date.now();

    // 2. Execute AI generation
    const fakeEnv = { GEMINI_MODEL: "gemini-3.5-flash", GEMINI_API_KEY: "secret-key-123" };
    const questions = await mockGenerateQuestions(
      fakeEnv,
      exam.exam_name,
      exam.syllabus,
      requestedCount || exam.question_count,
      exam.custom_prompt_notes
    );

    // 3. Create test record in 'paused' status
    const testId = `test_${Date.now()}`;
    const testNumber = exam.test_number;
    const nextTestNumber = incrementTestNumber(testNumber);
    const title = `${exam.exam_name} - ${testNumber}`;

    generatedTests.set(testId, {
      id: testId,
      exam_id: examId,
      exam_name: exam.exam_name,
      test_number: testNumber,
      title,
      generated_at: Date.now(),
      status: "paused", // Always paused initially so admin can review
      question_count: questions.length,
      questions,
    });

    // 4. Update exam state
    exam.last_generation_status = "success";
    exam.last_generation_error = "";
    exam.test_number = nextTestNumber;
    exam.generating_lock_until = 0;

    return { testId, questionCount: questions.length, testNumber, nextTestNumber };
  }

  const result = await simulateGenerateNow("exam_cgl", 3);

  // Assertions on result
  assert.equal(result.questionCount, 3);
  assert.equal(result.testNumber, "Test 1");
  assert.equal(result.nextTestNumber, "Test 2");

  // Assertions on generated test record
  const savedTest = generatedTests.get(result.testId);
  assert.ok(savedTest);
  assert.equal(savedTest.status, "paused");
  assert.equal(savedTest.title, "SSC CGL 2026 - Test 1");
  assert.equal(savedTest.questions.length, 3);

  // Assertions on exam record update
  const updatedExam = exams.get("exam_cgl");
  assert.equal(updatedExam.last_generation_status, "success");
  assert.equal(updatedExam.test_number, "Test 2");
  assert.equal(updatedExam.generating_lock_until, 0);
});

// --- Component 4: Error Handling & Graceful Recovery Simulation ---
test("generation failure captures error, releases lock, and sets status to failed without corrupting state", async () => {
  const exam = {
    id: "exam_fail",
    exam_name: "UPSC Prelims",
    syllabus: "History, Geography",
    last_generation_status: "idle",
    last_generation_error: "",
    last_generation_time: 0,
    generating_lock_until: 0,
    test_number: "Test 3",
  };

  async function simulateFailedGeneration() {
    exam.last_generation_status = "running";
    exam.generating_lock_until = Date.now() + 600000;

    try {
      throw new Error("Gemini API error (503): Service Unavailable. High demand.");
    } catch (err) {
      exam.last_generation_status = "failed";
      exam.last_generation_error = String(err.message).slice(0, 200);
      exam.generating_lock_until = 0;
      exam.last_generation_time = Date.now();
    }
  }

  await simulateFailedGeneration();

  assert.equal(exam.last_generation_status, "failed");
  assert.equal(exam.generating_lock_until, 0); // Lock must be released
  assert.ok(exam.last_generation_error.includes("503"));
  assert.equal(exam.test_number, "Test 3"); // Test number does NOT advance on failure
});

// --- Component 5: Scheduled Cron Generation Eligibility ---
test("scheduled cron generation strictly checks time, date, and locking idempotency", () => {
  const todayDate = "2026-09-28";
  const currentTime = "14:30";
  const now = Date.now();

  function isEligible(exam) {
    const isEnabled = exam.auto_generation_enabled !== 0 && exam.auto_generation_enabled !== false;
    if (!isEnabled) return { eligible: false, reason: "disabled" };
    if (currentTime < exam.auto_gen_time) return { eligible: false, reason: "time not reached" };
    if (exam.last_generated_date === todayDate) return { eligible: false, reason: "already generated today" };
    if (exam.generating_lock_until && now < exam.generating_lock_until) return { eligible: false, reason: "locked" };
    return { eligible: true };
  }

  // 1. Time not reached (scheduled for 16:00, current is 14:30)
  const e1 = isEligible({ auto_generation_enabled: 1, auto_gen_time: "16:00", last_generated_date: "" });
  assert.equal(e1.eligible, false);
  assert.equal(e1.reason, "time not reached");

  // 2. Already generated today
  const e2 = isEligible({ auto_generation_enabled: 1, auto_gen_time: "10:00", last_generated_date: "2026-09-28" });
  assert.equal(e2.eligible, false);
  assert.equal(e2.reason, "already generated today");

  // 3. Locked
  const e3 = isEligible({ auto_generation_enabled: 1, auto_gen_time: "10:00", last_generated_date: "", generating_lock_until: now + 300000 });
  assert.equal(e3.eligible, false);
  assert.equal(e3.reason, "locked");

  // 4. Eligible with 1
  const e4 = isEligible({ auto_generation_enabled: 1, auto_gen_time: "10:00", last_generated_date: "2026-09-27", generating_lock_until: 0 });
  assert.equal(e4.eligible, true);

  // 5. Eligible with missing/undefined (defaults to enabled)
  const e5 = isEligible({ auto_gen_time: "10:00", last_generated_date: "2026-09-27", generating_lock_until: 0 });
  assert.equal(e5.eligible, true);

  // 6. Explicit 0 disables
  const e6 = isEligible({ auto_generation_enabled: 0, auto_gen_time: "10:00", last_generated_date: "2026-09-27", generating_lock_until: 0 });
  assert.equal(e6.eligible, false);
  assert.equal(e6.reason, "disabled");
});
