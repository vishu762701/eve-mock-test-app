import test from "node:test";
import assert from "node:assert/strict";
import { Hono } from "hono";
import { questionRoutes } from "../src/routes/questions.ts";

const QUESTION = {
  id: "question-1",
  exam_id: "exam-1",
  question_text: "What is 1 + 1?",
  option_a: "1",
  option_b: "2",
  option_c: "3",
  option_d: "4",
  correct_answer: "B",
  explanation: "Two.",
  explanation_hi: "Do.",
};

function questionApp(isAdmin) {
  const app = new Hono();
  app.use("*", async (c, next) => {
    // The legacy setting is intentionally hostile. It must have no effect.
    c.env = {
      LEGACY_ANSWER_LEAK: "on",
      DB: {
        prepare: () => ({
          bind: () => ({
            all: async () => ({ results: [QUESTION] }),
          }),
        }),
      },
    };
    c.set("user", { uid: "test-user", email: "student@example.invalid", isAdmin });
    await next();
  });
  app.route("/api/questions", questionRoutes);
  return app;
}

test("GET /api/questions/exam hides answers from legacy non-admin clients", async () => {
  const app = questionApp(false);
  const res = await app.request("http://localhost/api/questions/exam/exam-1", {
    headers: { "X-Eve-Client": "1" },
  });
  assert.equal(res.status, 200);
  const payload = await res.json();
  assert.equal(payload.success, true);
  assert.equal(payload.data.length, 1);
  assert.equal(payload.data[0].correctAnswer, "");
  assert.equal(payload.data[0].explanation, "");
  assert.equal(payload.data[0].explanationHi, "");
});

test("GET /api/questions/exam includes answer details for server-verified admins", async () => {
  const app = questionApp(true);
  const res = await app.request("http://localhost/api/questions/exam/exam-1");
  assert.equal(res.status, 200);
  const payload = await res.json();
  assert.equal(payload.data[0].correctAnswer, "B");
  assert.equal(payload.data[0].explanation, "Two.");
  assert.equal(payload.data[0].explanationHi, "Do.");
});
