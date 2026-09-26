import test from "node:test";
import assert from "node:assert/strict";

const NEGATIVE_MARK = 0.0;

function evaluateAnswers(picks, questionsMap) {
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

  const score = correct - wrong * NEGATIVE_MARK;
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
