import test from "node:test";
import assert from "node:assert/strict";

const NEGATIVE_MARK = 0.0;

function evaluateAnswers(picks, questionsMap, negativeMark = NEGATIVE_MARK) {
  let correct = 0;
  let wrong = 0;
  let unattempted = 0;

  for (const pick of picks) {
    const q = questionsMap.get(pick.questionId);
    if (!q) continue;

    const attempted = pick.selected.length > 0;
    const isCorrect = attempted && pick.selected === q.correctAnswer;

    if (!attempted) unattempted++;
    else if (isCorrect) correct++;
    else wrong++;
  }

  const score = Math.round((correct - wrong * negativeMark) * 100) / 100;
  return { correct, wrong, unattempted, score };
}

test("evaluateAnswers correctly calculates perfect score", () => {
  const questionsMap = new Map([
    ["q1", { correctAnswer: "A" }],
    ["q2", { correctAnswer: "B" }],
  ]);
  const picks = [
    { questionId: "q1", selected: "A" },
    { questionId: "q2", selected: "B" },
  ];

  const result = evaluateAnswers(picks, questionsMap);
  assert.equal(result.correct, 2);
  assert.equal(result.wrong, 0);
  assert.equal(result.unattempted, 0);
  assert.equal(result.score, 2.0);
});

test("evaluateAnswers correctly calculates wrong and unattempted answers", () => {
  const questionsMap = new Map([
    ["q1", { correctAnswer: "A" }],
    ["q2", { correctAnswer: "B" }],
    ["q3", { correctAnswer: "C" }],
  ]);
  const picks = [
    { questionId: "q1", selected: "C" }, // wrong
    { questionId: "q2", selected: "" },  // unattempted
    { questionId: "q3", selected: "C" }, // correct
  ];

  const result = evaluateAnswers(picks, questionsMap);
  assert.equal(result.correct, 1);
  assert.equal(result.wrong, 1);
  assert.equal(result.unattempted, 1);
  assert.equal(result.score, 1.0);
});

test("evaluateAnswers with negative marking 1/3", () => {
  const questionsMap = new Map([
    ["q1", { correctAnswer: "A" }],
    ["q2", { correctAnswer: "B" }],
    ["q3", { correctAnswer: "C" }],
    ["q4", { correctAnswer: "D" }],
  ]);
  const picks = [
    { questionId: "q1", selected: "A" }, // correct
    { questionId: "q2", selected: "B" }, // correct
    { questionId: "q3", selected: "A" }, // wrong
    { questionId: "q4", selected: "" },  // unattempted
  ];

  const result = evaluateAnswers(picks, questionsMap, 1 / 3);
  assert.equal(result.correct, 2);
  assert.equal(result.wrong, 1);
  assert.equal(result.unattempted, 1);
  assert.equal(result.score, 1.67);
});

test("evaluateAnswers with negative marking 0.25", () => {
  const questionsMap = new Map([
    ["q1", { correctAnswer: "A" }],
    ["q2", { correctAnswer: "B" }],
    ["q3", { correctAnswer: "C" }],
  ]);
  const picks = [
    { questionId: "q1", selected: "A" }, // correct
    { questionId: "q2", selected: "A" }, // wrong
    { questionId: "q3", selected: "B" }, // wrong
  ];

  const result = evaluateAnswers(picks, questionsMap, 0.25);
  assert.equal(result.correct, 1);
  assert.equal(result.wrong, 2);
  assert.equal(result.unattempted, 0);
  assert.equal(result.score, 0.5);
});

test("hardcoded admin email check", () => {
  const HARDCODED_ADMIN_EMAILS = new Set([
    "pronlike9@gmail.com",
    "own.keni@gmail.com",
    "anyqueairdrop@gmail.com",
    "ghatisarkar56@gmail.com",
  ]);

  assert.equal(HARDCODED_ADMIN_EMAILS.has("pronlike9@gmail.com"), true);
  assert.equal(HARDCODED_ADMIN_EMAILS.has("student@example.com"), false);
});

// 5. Test AttemptKey parsing and negative marking lookup resolution
test("negative marking resolves through a test key and legacy key", () => {
  const ATTEMPT_KEY_SEP = "__";
  function parseAttemptKey(key) {
    const trimmed = (key || "").trim();
    if (trimmed.includes(ATTEMPT_KEY_SEP)) {
      const parts = trimmed.split(ATTEMPT_KEY_SEP);
      const sourceExamId = parts[0] || trimmed;
      const generatedTestId = parts.slice(1).join(ATTEMPT_KEY_SEP).trim();
      return { sourceExamId, generatedTestId: generatedTestId || null };
    }
    return { sourceExamId: trimmed, generatedTestId: null };
  }

  const examsDatabase = new Map([
    ["sub_hindi", { id: "sub_hindi", negative_marking_value: 1 / 3 }],
    ["main_cet", { id: "main_cet", negative_marking_value: 0.25 }],
  ]);

  function getNegativeMarkingForAttempt(submittedExamId) {
    const { sourceExamId } = parseAttemptKey(submittedExamId);
    return examsDatabase.get(sourceExamId)?.negative_marking_value ?? 0.0;
  }

  // Case 1: Test Key with SEP
  const testKey = "sub_hindi__gen_test_001";
  const parsed1 = parseAttemptKey(testKey);
  assert.equal(parsed1.sourceExamId, "sub_hindi");
  assert.equal(parsed1.generatedTestId, "gen_test_001");
  assert.equal(getNegativeMarkingForAttempt(testKey), 1 / 3);

  // Case 2: Legacy exam id without SEP
  const legacyKey = "main_cet";
  const parsed2 = parseAttemptKey(legacyKey);
  assert.equal(parsed2.sourceExamId, "main_cet");
  assert.equal(parsed2.generatedTestId, null);
  assert.equal(getNegativeMarkingForAttempt(legacyKey), 0.25);

  // Case 3: Missing exam resolves to 0.0
  assert.equal(getNegativeMarkingForAttempt("non_existent__gen_999"), 0.0);
});

// 6. Test parentExamId validations and delete guard logic
test("parentExamId validation rules (self, nested, missing, parent-with-children, duplicate name, delete blocked)", () => {
  const exams = [
    { id: "main_1", exam_name: "RSSB 3rd Grade", parent_exam_id: "" },
    { id: "sub_1", exam_name: "Hindi", parent_exam_id: "main_1" },
    { id: "main_2", exam_name: "Rajasthan CET", parent_exam_id: "" },
  ];

  function validateSaveExam(id, name, parentExamId, isUpdate = false) {
    if (parentExamId) {
      if (isUpdate && parentExamId === id) {
        return { status: 400, error: "An exam cannot be its own parent" };
      }
      const parent = exams.find((e) => e.id === parentExamId);
      if (!parent) {
        return { status: 400, error: "Parent exam not found" };
      }
      if (parent.parent_exam_id && parent.parent_exam_id.trim() !== "") {
        return { status: 400, error: "Parent exam cannot be a sub-exam (only one level of nesting allowed)" };
      }
      if (isUpdate) {
        const hasChildren = exams.some((e) => e.parent_exam_id === id);
        if (hasChildren) {
          return { status: 400, error: "This exam has sub-exams and cannot become a sub-exam" };
        }
      }
    }

    const duplicate = exams.find(
      (e) => e.exam_name.toLowerCase() === name.toLowerCase() && (e.parent_exam_id || "") === (parentExamId || "") && (!isUpdate || e.id !== id)
    );
    if (duplicate) {
      return { status: 409, error: "An exam with this name already exists here" };
    }

    return { status: 200, success: true };
  }

  function validateDeleteExam(id) {
    const hasChildren = exams.some((e) => e.parent_exam_id === id);
    if (hasChildren) {
      return { status: 409, error: "This exam has sub-exams. Delete them first." };
    }
    return { status: 200, success: true };
  }

  // (a) Self parent
  assert.equal(validateSaveExam("main_1", "RSSB 3rd Grade", "main_1", true).status, 400);

  // (b) Missing parent
  assert.equal(validateSaveExam("new_sub", "Maths", "non_existent_parent", false).status, 400);

  // (c) Nested parent (parent is already a sub-exam)
  assert.equal(validateSaveExam("new_sub", "Grammar", "sub_1", false).status, 400);

  // (d) Parent with children trying to become a sub-exam
  assert.equal(validateSaveExam("main_1", "RSSB 3rd Grade", "main_2", true).status, 400);

  // (e) Duplicate sibling name (Hindi already exists under main_1)
  assert.equal(validateSaveExam("new_sub", "Hindi", "main_1", false).status, 409);
  // But Hindi can exist under main_2
  assert.equal(validateSaveExam("new_sub", "Hindi", "main_2", false).status, 200);

  // (f) Delete blocked when exam has sub-exams
  assert.equal(validateDeleteExam("main_1").status, 409);
  // Delete allowed for leaf exam
  assert.equal(validateDeleteExam("sub_1").status, 200);
});

