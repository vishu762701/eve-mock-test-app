import test from "node:test";
import assert from "node:assert/strict";
import { Hono } from "hono";
import { generatedTestRoutes } from "../src/routes/generatedTests.ts";
import { isolatedD1 } from "./support/sqliteD1.mjs";

const question = {
  questionText: "Which answer is correct?",
  optionA: "one", optionB: "two", optionC: "three", optionD: "four",
  correctAnswer: "B", explanation: "Explanation",
};

function fixture() {
  const { sqlite, d1 } = isolatedD1();
  sqlite.prepare("INSERT INTO exams (id, exam_name) VALUES (?, ?)").run("exam-1", "Exam");
  sqlite.prepare(
    "INSERT INTO generated_tests (id, exam_id, exam_name, title, generated_at, status, question_count, questions_json, available_from) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
  ).run("test-1", "exam-1", "Exam", "Generated", 1, "paused", 1, JSON.stringify([question]), 0);

  const editor = "generated-editor@example.test";
  const viewer = "generated-viewer@example.test";
  for (const [email, rights] of [
    [editor, ["manage_generated_tests"]],
    [viewer, ["view_analytics_logs"]],
  ]) {
    sqlite.prepare("INSERT INTO admins (email, created_at) VALUES (?, ?)").run(email, 1);
    sqlite.prepare("INSERT INTO admin_rights (email, rights, updated_at) VALUES (?, ?, ?)")
      .run(email, JSON.stringify(rights), 1);
  }

  async function request(path, payload, { email = editor, isAdmin = true, db = d1 } = {}) {
    const app = new Hono();
    app.use("*", async (c, next) => {
      c.set("user", { uid: email, email, isAdmin });
      await next();
    });
    app.route("/api/generated-tests", generatedTestRoutes);
    return app.request("http://localhost/api/generated-tests/" + path, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    }, { DB: db });
  }

  function value(field) {
    return sqlite.prepare("SELECT " + field + " FROM generated_tests WHERE id = ?").get("test-1")?.[field];
  }
  function audits(actionType) {
    return sqlite.prepare("SELECT COUNT(*) AS count FROM admin_audit_log WHERE action_type = ?")
      .get(actionType).count;
  }
  return { sqlite, d1, request, value, audits, editor, viewer };
}

test("generated-test schedule confirms one existing row and one atomic audit", async () => {
  const f = fixture();
  try {
    const response = await f.request("test-1/schedule", { availableFrom: 1893456000000 });
    assert.equal(response.status, 200);
    assert.deepEqual((await response.json()).data, { id: "test-1", availableFrom: 1893456000000 });
    assert.equal(f.value("available_from"), 1893456000000);
    assert.equal(f.audits("GENERATED_TEST_SCHEDULE_UPDATED"), 1);
  } finally { f.sqlite.close(); }
});

test("generated-test schedule rejects unknown ID and malformed times without audit", async () => {
  const f = fixture();
  try {
    for (const bad of [{}, null, { availableFrom: null }, { availableFrom: "" },
      { availableFrom: "0" }, { availableFrom: -1 }, { availableFrom: 1.5 },
      { availableFrom: Number.MAX_SAFE_INTEGER + 1 }]) {
      const response = await f.request("test-1/schedule", bad);
      assert.equal(response.status, 400);
    }
    const missing = await f.request("missing/schedule", { availableFrom: 1000 });
    assert.equal(missing.status, 404);
    assert.equal(f.value("available_from"), 0);
    assert.equal(f.audits("GENERATED_TEST_SCHEDULE_UPDATED"), 0);
  } finally { f.sqlite.close(); }
});

test("generated-test status requires explicit valid value, then confirms row and audit", async () => {
  const f = fixture();
  try {
    for (const bad of [{}, null, { status: null }, { status: "" },
      { status: 1 }, { status: "invalid" }]) {
      const response = await f.request("test-1/status", bad);
      assert.equal(response.status, 400);
    }
    const response = await f.request("test-1/status", { status: "live" });
    assert.equal(response.status, 200);
    assert.deepEqual(await response.json(), { success: true });
    assert.equal(f.value("status"), "live");
    assert.equal(f.audits("GENERATED_TEST_STATUS_UPDATED"), 1);
    const missing = await f.request("missing/status", { status: "paused" });
    assert.equal(missing.status, 404);
    assert.equal(f.audits("GENERATED_TEST_STATUS_UPDATED"), 1);
  } finally { f.sqlite.close(); }
});

test("generated-test mutations require fresh server permission, not client admin hints", async () => {
  const f = fixture();
  try {
    for (const path of ["test-1/schedule", "test-1/status"]) {
      const payload = path.endsWith("schedule") ? { availableFrom: 400 } : { status: "draft" };
      assert.equal((await f.request(path, payload, { email: f.viewer })).status, 403);
      assert.equal((await f.request(path, payload, { isAdmin: false })).status, 403);
    }
    assert.equal(f.value("available_from"), 0);
    assert.equal(f.value("status"), "paused");
    assert.equal(f.audits("GENERATED_TEST_SCHEDULE_UPDATED"), 0);
    assert.equal(f.audits("GENERATED_TEST_STATUS_UPDATED"), 0);
  } finally { f.sqlite.close(); }
});

test("deletion just before atomic update produces no phantom success or audit", async () => {
  for (const path of ["test-1/schedule", "test-1/status"]) {
    const f = fixture();
    try {
      const racingDb = {
        ...f.d1,
        async batch(statements) {
          f.sqlite.prepare("DELETE FROM generated_tests WHERE id = ?").run("test-1");
          return f.d1.batch(statements);
        },
      };
      const payload = path.endsWith("schedule") ? { availableFrom: 6000 } : { status: "draft" };
      const response = await f.request(path, payload, { db: racingDb });
      assert.equal(response.status, 409);
      assert.equal((await response.json()).success, false);
      assert.equal(f.audits(path.endsWith("schedule")
        ? "GENERATED_TEST_SCHEDULE_UPDATED" : "GENERATED_TEST_STATUS_UPDATED"), 0);
    } finally { f.sqlite.close(); }
  }
});
