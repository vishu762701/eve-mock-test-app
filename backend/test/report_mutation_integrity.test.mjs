import test from "node:test";
import assert from "node:assert/strict";
import { Hono } from "hono";
import { adminReportRoutes } from "../src/routes/reports.ts";
import { isolatedD1 } from "./support/sqliteD1.mjs";

function fixture() {
  const { sqlite, d1 } = isolatedD1();
  const moderator = "report-moderator@example.test";
  const viewer = "report-viewer@example.test";
  for (const [email, rights] of [
    [moderator, ["moderate_feedback"]],
    [viewer, ["view_analytics_logs"]],
  ]) {
    sqlite.prepare("INSERT INTO admins (email, created_at) VALUES (?, ?)").run(email, 1);
    sqlite.prepare("INSERT INTO admin_rights (email, rights, updated_at) VALUES (?, ?, ?)")
      .run(email, JSON.stringify(rights), 1);
  }
  function seed(id, status = "pending", type = "content") {
    sqlite.prepare(
      `INSERT INTO question_reports
       (id, question_id, exam_id, exam_name, question_text, reason, comment,
        student_id, student_email, timestamp, status, report_type)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
    ).run(id, "q-" + id, "exam", "Exam", "Example question", "Other", "",
      "student", "student@example.test", 1000, status, type);
  }
  seed("report-a");
  seed("report-b", "pending", "technical");
  seed("report-old", "dismissed");

  function app(db = d1, email = moderator, isAdmin = true) {
    const server = new Hono();
    server.use("*", async (c, next) => {
      c.set("user", { uid: email, email, isAdmin });
      await next();
    });
    server.onError(() => new Response(JSON.stringify({ success: false, error: "Transaction failed" }),
      { status: 500, headers: { "Content-Type": "application/json" } }));
    server.route("/api/admin/reports", adminReportRoutes);
    return async (path, method, body) => server.request("http://localhost/api/admin/reports" + path, {
      method,
      headers: { "Content-Type": "application/json" },
      ...(body === undefined ? {} : { body: JSON.stringify(body) }),
    }, { DB: db });
  }
  function status(id) {
    return sqlite.prepare("SELECT status FROM question_reports WHERE id = ?").get(id)?.status;
  }
  function audit(action) {
    return sqlite.prepare("SELECT * FROM admin_audit_log WHERE action_type = ?").all(action);
  }
  return { sqlite, d1, app, seed, status, audit, moderator, viewer };
}

test("single report dismissal confirms exactly one pending row and one audited write", async () => {
  const f = fixture();
  try {
    const send = f.app();
    assert.equal((await send("/report-a/dismiss", "PUT")).status, 200);
    assert.equal(f.status("report-a"), "dismissed");
    assert.equal(f.audit("REPORT_DISMISSED").length, 1);
    assert.equal(f.audit("REPORT_DISMISSED")[0].target_id, "report-a");
    for (const id of ["report-a", "report-old", "missing"]) {
      const response = await send("/" + id + "/dismiss", "PUT");
      assert.equal(response.status, 404);
      assert.equal((await response.json()).success, false);
    }
    assert.equal(f.audit("REPORT_DISMISSED").length, 1);
  } finally { f.sqlite.close(); }
});

test("batch dismissal is all-or-nothing and counts only real pending IDs", async () => {
  const f = fixture();
  try {
    const send = f.app();
    for (const body of [null, {}, { ids: [] }, { ids: "report-a" },
      { ids: ["report-a", "report-a"] }, { ids: ["report-a", ""] },
      { ids: ["report-a", 1] }, { ids: [" report-a"] },
      { ids: Array.from({ length: 1001 }, (_, i) => "id-" + i) }]) {
      assert.equal((await send("/dismiss-batch", "POST", body)).status, 400);
    }
    for (const ids of [["report-a", "missing"], ["report-a", "report-old"]]) {
      const response = await send("/dismiss-batch", "POST", { ids });
      assert.equal(response.status, 409);
      assert.equal(f.status("report-a"), "pending");
      assert.equal(f.audit("REPORT_BATCH_DISMISSED").length, 0);
    }
    const response = await send("/dismiss-batch", "POST", { ids: ["report-a", "report-b"] });
    assert.equal(response.status, 200);
    assert.deepEqual(await response.json(), { success: true });
    assert.equal(f.status("report-a"), "dismissed");
    assert.equal(f.status("report-b"), "dismissed");
    assert.equal(f.audit("REPORT_BATCH_DISMISSED").length, 1);
    assert.deepEqual(JSON.parse(f.audit("REPORT_BATCH_DISMISSED")[0].metadata), { count: 2 });
    assert.equal((await send("/dismiss-batch", "POST", { ids: ["report-a", "report-b"] })).status, 409);
    assert.equal(f.audit("REPORT_BATCH_DISMISSED").length, 1);
  } finally { f.sqlite.close(); }
});

test("single delete requires existing ID and authenticated audit", async () => {
  const f = fixture();
  try {
    const send = f.app();
    assert.equal((await send("/missing", "DELETE")).status, 404);
    assert.equal((await send("/report-old", "DELETE")).status, 200);
    assert.equal(f.status("report-old"), undefined);
    assert.equal((await send("/report-old", "DELETE")).status, 404);
    assert.equal(f.audit("REPORT_DELETED").length, 1);
    assert.equal(f.audit("REPORT_DELETED")[0].target_id, "report-old");
  } finally { f.sqlite.close(); }
});

test("report mutations enforce real Worker moderator right for every route", async () => {
  const f = fixture();
  try {
    for (const [path, method, body] of [
      ["/report-a/dismiss", "PUT", undefined],
      ["/dismiss-batch", "POST", { ids: ["report-a"] }],
      ["/report-a", "DELETE", undefined],
    ]) {
      for (const send of [f.app(f.d1, f.viewer), f.app(f.d1, f.moderator, false)]) {
        const result = await send(path, method, body);
        assert.equal(result.status, 403);
      }
    }
    assert.equal(f.status("report-a"), "pending");
    assert.equal(f.audit("REPORT_DELETED").length, 0);
    assert.equal(f.audit("REPORT_DISMISSED").length, 0);
    assert.equal(f.audit("REPORT_BATCH_DISMISSED").length, 0);
  } finally { f.sqlite.close(); }
});

test("large moderator batches stay within Cloudflare D1's 100-parameter limit", async () => {
  const f = fixture();
  try {
    const ids = Array.from({ length: 125 }, (_, i) => "large-" + i);
    ids.forEach((id) => f.seed(id));
    // Local node:sqlite allows more bindings than deployed D1. Enforce the real
    // platform's query limit in this transport boundary, not via SQLite defaults.
    const limitedD1 = {
      ...f.d1,
      prepare(sql) {
        const stmt = f.d1.prepare(sql);
        return {
          ...stmt,
          bind(...args) {
            assert.ok(args.length <= 100, "D1 rejected more than 100 parameters for one statement");
            return stmt.bind(...args);
          },
        };
      },
    };
    const response = await f.app(limitedD1)("/dismiss-batch", "POST", { ids });
    assert.equal(response.status, 200);
    assert.equal((await response.json()).success, true);
    assert.ok(ids.every((id) => f.status(id) === "dismissed"));
    assert.equal(f.audit("REPORT_BATCH_DISMISSED").length, 1);
    assert.deepEqual(JSON.parse(f.audit("REPORT_BATCH_DISMISSED")[0].metadata), { count: 125 });
  } finally { f.sqlite.close(); }
});

test("a competing update/delete between preflight and batch cannot forge success or audit", async () => {
  for (const [path, method, body, racingChange, expected] of [
    ["/report-a/dismiss", "PUT", undefined,
      (sql) => sql.prepare("UPDATE question_reports SET status = 'dismissed' WHERE id = 'report-a'").run(),
      "REPORT_DISMISSED"],
    ["/dismiss-batch", "POST", { ids: ["report-a", "report-b"] },
      (sql) => sql.prepare("DELETE FROM question_reports WHERE id = 'report-b'").run(),
      "REPORT_BATCH_DISMISSED"],
    ["/report-a", "DELETE", undefined,
      (sql) => sql.prepare("DELETE FROM question_reports WHERE id = 'report-a'").run(),
      "REPORT_DELETED"],
  ]) {
    const f = fixture();
    try {
      const racingD1 = {
        ...f.d1,
        async batch(statements) {
          racingChange(f.sqlite);
          return f.d1.batch(statements);
        },
      };
      const res = await f.app(racingD1)(path, method, body);
      assert.equal(res.status, 409);
      assert.equal((await res.json()).success, false);
      assert.equal(f.audit(expected).length, 0);
      if (method === "POST") assert.equal(f.status("report-a"), "pending");
    } finally { f.sqlite.close(); }
  }
});

test("an audit insertion failure rolls the report mutation back atomically", async () => {
  for (const [path, method, body] of [
    ["/report-a/dismiss", "PUT", undefined],
    ["/dismiss-batch", "POST", { ids: ["report-a", "report-b"] }],
    ["/report-a", "DELETE", undefined],
  ]) {
    const f = fixture();
    try {
      f.sqlite.exec(`CREATE TRIGGER reject_report_audit BEFORE INSERT ON admin_audit_log
        BEGIN SELECT RAISE(ABORT, 'audit temporarily unavailable'); END`);
      const res = await f.app()(path, method, body);
      assert.equal(res.status, 500);
      assert.equal(f.status("report-a"), "pending");
      assert.equal(f.status("report-b"), "pending");
      assert.equal(f.audit("REPORT_DISMISSED").length, 0);
      assert.equal(f.audit("REPORT_BATCH_DISMISSED").length, 0);
      assert.equal(f.audit("REPORT_DELETED").length, 0);
    } finally { f.sqlite.close(); }
  }
});
