import test from "node:test";
import assert from "node:assert/strict";
import { Hono } from "hono";
import { examRoutes } from "../src/routes/exams.ts";
import { isolatedD1 } from "./support/sqliteD1.mjs";

function fixture() {
  const { sqlite, d1 } = isolatedD1();
  const editor = "unpublish-editor@example.test";
  const viewer = "unpublish-viewer@example.test";
  for (const [email, rights] of [
    [editor, ["manage_exams"]],
    [viewer, ["view_analytics_logs"]],
  ]) {
    sqlite.prepare("INSERT INTO admins (email, created_at) VALUES (?, ?)").run(email, 1);
    sqlite.prepare("INSERT INTO admin_rights (email, rights, updated_at) VALUES (?, ?, ?)")
      .run(email, JSON.stringify(rights), 1);
  }
  sqlite.prepare("INSERT INTO exams (id, exam_name, auto_generation_enabled) VALUES (?, ?, ?)")
    .run("exam-1", "Unpublish fixture", 1);
  sqlite.prepare("INSERT INTO exams (id, exam_name, auto_generation_enabled) VALUES (?, ?, ?)")
    .run("exam-2", "Other exam", 1);
  const insert = sqlite.prepare(
    "INSERT INTO generated_tests (id, exam_id, exam_name, title, generated_at, status) VALUES (?, ?, ?, ?, ?, ?)");
  insert.run("test-1", "exam-1", "Unpublish fixture", "First test", 1, "live");
  insert.run("test-2", "exam-1", "Unpublish fixture", "Second test", 2, "published");
  insert.run("other-test", "exam-2", "Other exam", "Other", 3, "live");

  function request(id, { db = d1, email = editor, isAdmin = true } = {}) {
    const app = new Hono();
    app.use("*", async (c, next) => {
      c.set("user", { uid: email, email, isAdmin });
      await next();
    });
    app.onError(() => new Response(JSON.stringify({ success: false, error: "Transaction failure" }),
      { status: 500, headers: { "Content-Type": "application/json" } }));
    app.route("/api/exams", examRoutes);
    return app.request("http://localhost/api/exams/" + encodeURIComponent(id) + "/unpublish-tests",
      { method: "POST" }, { DB: db });
  }

  function value(id, col) {
    return sqlite.prepare("SELECT " + col + " FROM exams WHERE id = ?").get(id)?.[col];
  }
  function testStatus(id) {
    return sqlite.prepare("SELECT status FROM generated_tests WHERE id = ?").get(id)?.status;
  }
  function audits() {
    return sqlite.prepare("SELECT * FROM admin_audit_log WHERE action_type = 'EXAM_TESTS_UNPUBLISHED'").all();
  }
  return { sqlite, d1, request, value, testStatus, audits, editor, viewer };
}

test("unpublish preserves other exams and confirms exam update plus audit in one batch", async () => {
  const f = fixture();
  try {
    const result = await f.request("exam-1");
    assert.equal(result.status, 200);
    assert.equal((await result.json()).success, true);
    assert.equal(f.value("exam-1", "auto_generation_enabled"), 0);
    assert.equal(f.testStatus("test-1"), "paused");
    assert.equal(f.testStatus("test-2"), "paused");
    assert.equal(f.value("exam-2", "auto_generation_enabled"), 1);
    assert.equal(f.testStatus("other-test"), "live");
    assert.equal(f.audits().length, 1);
    assert.equal(f.audits()[0].target_id, "exam-1");
  } finally { f.sqlite.close(); }
});

test("unpublish nonexistent exam fails without creating an audit entry", async () => {
  const f = fixture();
  try {
    const result = await f.request("missing");
    assert.equal(result.status, 404);
    assert.equal((await result.json()).success, false);
    assert.equal(f.testStatus("test-1"), "live");
    assert.equal(f.value("exam-1", "auto_generation_enabled"), 1);
    assert.equal(f.audits().length, 0);
  } finally { f.sqlite.close(); }
});

test("unpublish enforces fresh Worker manage_exams permission", async () => {
  const f = fixture();
  try {
    for (const options of [{ email: f.viewer }, { isAdmin: false }]) {
      const response = await f.request("exam-1", options);
      assert.equal(response.status, 403);
    }
    assert.equal(f.testStatus("test-1"), "live");
    assert.equal(f.value("exam-1", "auto_generation_enabled"), 1);
    assert.equal(f.audits().length, 0);
  } finally { f.sqlite.close(); }
});

test("unpublish cannot acknowledge a deleted exam between preflight and D1 transaction", async () => {
  const f = fixture();
  try {
    const racing = {
      ...f.d1,
      async batch(statements) {
        f.sqlite.prepare("DELETE FROM exams WHERE id = ?").run("exam-1");
        return f.d1.batch(statements);
      },
    };
    const response = await f.request("exam-1", { db: racing });
    assert.equal(response.status, 409);
    assert.equal((await response.json()).success, false);
    assert.equal(f.value("exam-1", "auto_generation_enabled"), undefined);
    assert.equal(f.testStatus("test-1"), undefined);
    assert.equal(f.audits().length, 0);
    assert.equal(f.testStatus("other-test"), "live");
  } finally { f.sqlite.close(); }
});

test("unpublish rolls back paused tests and exam setting when audit insert fails", async () => {
  const f = fixture();
  try {
    f.sqlite.exec(`CREATE TRIGGER reject_unpublish_audit BEFORE INSERT ON admin_audit_log
      WHEN NEW.action_type = 'EXAM_TESTS_UNPUBLISHED'
      BEGIN SELECT RAISE(ABORT, 'audit unavailable'); END`);
    const response = await f.request("exam-1");
    assert.equal(response.status, 500);
    assert.equal(f.testStatus("test-1"), "live");
    assert.equal(f.testStatus("test-2"), "published");
    assert.equal(f.value("exam-1", "auto_generation_enabled"), 1);
    assert.equal(f.audits().length, 0);
  } finally { f.sqlite.close(); }
});

test("unpublish refuses an unaudited nontransactional Worker adapter", async () => {
  const f = fixture();
  try {
    const withoutBatch = { ...f.d1, batch: undefined };
    const response = await f.request("exam-1", { db: withoutBatch });
    assert.equal(response.status, 500);
    assert.equal(f.testStatus("test-1"), "live");
    assert.equal(f.value("exam-1", "auto_generation_enabled"), 1);
    assert.equal(f.audits().length, 0);
  } finally { f.sqlite.close(); }
});
