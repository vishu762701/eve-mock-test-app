import test from "node:test";
import assert from "node:assert/strict";
import { Hono } from "hono";
import { generatedTestRoutes } from "../src/routes/generatedTests.ts";
import { isolatedD1 } from "./support/sqliteD1.mjs";

const question = {
  questionText: "Sample question",
  optionA: "one",
  optionB: "two",
  optionC: "three",
  optionD: "four",
  correctAnswer: "B",
  explanation: "Only admins with test rights may see this before submission.",
};

function fixture() {
  const { sqlite, d1 } = isolatedD1();
  sqlite.prepare("INSERT INTO exams (id, exam_name) VALUES (?, ?)").run("exam-1", "Exam");
  const add = sqlite.prepare(
    "INSERT INTO generated_tests (id, exam_id, exam_name, title, generated_at, status, question_count, questions_json, available_from) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
  );
  add.run("draft-1", "exam-1", "Exam", "Draft", 1, "draft", 1, JSON.stringify([question]), 0);
  add.run("published-1", "exam-1", "Exam", "Published", 2, "published", 1, JSON.stringify([question]), 0);
  add.run("scheduled-1", "exam-1", "Exam", "Scheduled", 3, "published", 1, JSON.stringify([question]), 9999999999999);

  const limitedEmail = "limited-generated-admin@example.test";
  const authorizedEmail = "generated-editor@example.test";
  for (const [email, rights] of [
    [limitedEmail, ["view_analytics_logs"]],
    [authorizedEmail, ["manage_generated_tests"]],
  ]) {
    sqlite.prepare("INSERT INTO admins (email, created_at) VALUES (?, ?)").run(email, 1);
    sqlite.prepare("INSERT INTO admin_rights (email, rights, updated_at) VALUES (?, ?, ?)")
      .run(email, JSON.stringify(rights), 1);
  }

  const client = (email, isAdmin = true) => {
    const app = new Hono();
    app.use("*", async (c, next) => {
      c.set("user", { uid: email, email, isAdmin });
      await next();
    });
    app.route("/api/generated-tests", generatedTestRoutes);
    return (path) => app.request("http://localhost/api/generated-tests" + path, {}, { DB: d1 });
  };
  return { sqlite, client, limitedEmail, authorizedEmail };
}

test("an admin without manage_generated_tests cannot list or read unpublished generated tests", async () => {
  const { sqlite, client, limitedEmail } = fixture();
  try {
    const request = client(limitedEmail);
    const list = await request("/");
    assert.equal(list.status, 200);
    assert.deepEqual((await list.json()).data.map((entry) => entry.id), ["scheduled-1", "published-1"]);
    const draft = await request("/draft-1");
    assert.equal(draft.status, 403);
    const scheduled = await request("/scheduled-1");
    assert.equal(scheduled.status, 403);
  } finally { sqlite.close(); }
});

test("a limited admin cannot read generated answer keys and explanations from published tests", async () => {
  const { sqlite, client, limitedEmail } = fixture();
  try {
    const response = await client(limitedEmail)("/published-1");
    assert.equal(response.status, 200);
    const data = (await response.json()).data;
    assert.equal(data.questions.length, 1);
    assert.equal(data.questions[0].correctAnswer, "");
    assert.equal(data.questions[0].explanation, "");
  } finally { sqlite.close(); }
});

test("an authorized generated-test admin sees draft tests and their complete questions", async () => {
  const { sqlite, client, authorizedEmail } = fixture();
  try {
    const request = client(authorizedEmail);
    const list = await request("/");
    assert.equal(list.status, 200);
    assert.equal((await list.json()).data.length, 3);
    const detail = await request("/draft-1");
    assert.equal(detail.status, 200);
    const data = (await detail.json()).data;
    assert.equal(data.questions[0].correctAnswer, "B");
    assert.equal(data.questions[0].explanation, question.explanation);
  } finally { sqlite.close(); }
});
