import test from "node:test";
import assert from "node:assert/strict";
import { Hono } from "hono";
import { questionRoutes } from "../src/routes/questions.ts";
import { HARDCODED_ADMIN_EMAILS } from "../src/auth.ts";
import { isolatedD1 } from "./support/sqliteD1.mjs";

function questionApp(email, isAdmin) {
  const { sqlite, d1 } = isolatedD1();
  sqlite.prepare("INSERT INTO exams (id, exam_name) VALUES (?, ?)").run("exam-1", "Example exam");
  sqlite.prepare(`INSERT INTO questions
    (id, exam_id, question_text, option_a, option_b, option_c, option_d, correct_answer, explanation, explanation_hi)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`)
    .run("question-1", "exam-1", "What is 1 + 1?", "1", "2", "3", "4", "B", "Two.", "Do.");

  const app = new Hono();
  app.use("*", async (c, next) => {
    c.env = { DB: d1, LEGACY_ANSWER_LEAK: "on" };
    c.set("user", { uid: email, email, isAdmin });
    await next();
  });
  app.route("/api/questions", questionRoutes);
  const request = () => app.request("http://localhost/api/questions/exam/exam-1", {
    headers: { "X-Eve-Client": "1" },
  });
  return { sqlite, request };
}

test("GET /api/questions/exam hides answers from legacy non-admin clients", async () => {
  const { sqlite, request } = questionApp("student@example.invalid", false);
  try {
    const res = await request();
    assert.equal(res.status, 200);
    const payload = await res.json();
    assert.equal(payload.success, true);
    assert.equal(payload.data.length, 1);
    assert.equal(payload.data[0].correctAnswer, "");
    assert.equal(payload.data[0].explanation, "");
    assert.equal(payload.data[0].explanationHi, "");
  } finally { sqlite.close(); }
});

test("GET /api/questions/exam includes answers only for a D1-authorized question manager", async () => {
  const { sqlite, request } = questionApp([...HARDCODED_ADMIN_EMAILS][0], true);
  try {
    const res = await request();
    assert.equal(res.status, 200);
    const payload = await res.json();
    assert.equal(payload.data[0].correctAnswer, "B");
    assert.equal(payload.data[0].explanation, "Two.");
    assert.equal(payload.data[0].explanationHi, "Do.");
  } finally { sqlite.close(); }
});

test("an authenticated admin without manage_questions sees no answer keys", async () => {
  const email = "restricted-questions-admin@example.test";
  const { sqlite, request } = questionApp(email, true);
  try {
    sqlite.prepare("INSERT INTO admins (email, created_at) VALUES (?, ?)").run(email, 1);
    sqlite.prepare("INSERT INTO admin_rights (email, rights, updated_at) VALUES (?, ?, ?)")
      .run(email, JSON.stringify(["view_analytics_logs"]), 1);
    const response = await request();
    assert.equal(response.status, 200);
    const { data } = await response.json();
    assert.equal(data[0].correctAnswer, "");
    assert.equal(data[0].explanation, "");
    assert.equal(data[0].explanationHi, "");
  } finally { sqlite.close(); }
});

test("forged admin claim without D1 membership cannot reveal questions", async () => {
  const { sqlite, request } = questionApp("not-an-admin@example.test", true);
  try {
    const response = await request();
    assert.equal(response.status, 200);
    const { data } = await response.json();
    assert.equal(data[0].correctAnswer, "");
  } finally { sqlite.close(); }
});
