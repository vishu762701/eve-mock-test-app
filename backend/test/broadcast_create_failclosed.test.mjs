import test from "node:test";
import assert from "node:assert/strict";
import { Hono } from "hono";
import { broadcastRoutes } from "../src/routes/broadcasts.ts";
import { isolatedD1 } from "./support/sqliteD1.mjs";

const editorEmail = "broadcast-operator@example.test";
const deniedEmail = "broadcast-viewer@example.test";
const valid = {
  title: "Exam schedule",
  message: "A new mock test is available",
  type: "general",
  targetCategory: "All Users",
};

function fixture() {
  const { sqlite, d1 } = isolatedD1();
  for (const [email, rights] of [
    [editorEmail, ["send_broadcasts"]],
    [deniedEmail, ["view_analytics_logs"]],
  ]) {
    sqlite.prepare("INSERT INTO admins (email, created_at) VALUES (?, ?)").run(email, 1);
    sqlite.prepare("INSERT INTO admin_rights (email, rights, updated_at) VALUES (?, ?, ?)")
      .run(email, JSON.stringify(rights), 1);
  }
  async function post(db = d1, payload = valid, email = editorEmail) {
    const app = new Hono();
    app.use("*", async (c, next) => {
      c.set("user", { uid: email, email, isAdmin: true });
      await next();
    });
    app.route("/api/broadcasts", broadcastRoutes);
    const response = await app.request("http://localhost/api/broadcasts", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    }, { DB: db });
    return response;
  }
  const count = table => sqlite.prepare("SELECT COUNT(*) AS count FROM " + table).get().count;
  const auditCount = () => sqlite.prepare("SELECT COUNT(*) AS count FROM admin_audit_log WHERE action_type = 'BROADCAST_SENT'").get().count;
  return { sqlite, d1, post, count, auditCount };
}

test("broadcast creation commits exactly one notification and audit before 201", async () => {
  const f = fixture();
  try {
    const response = await f.post();
    assert.equal(response.status, 201);
    const body = await response.json();
    assert.equal(body.success, true);
    assert.equal(body.data.title, valid.title);
    assert.equal(body.data.message, valid.message);
    assert.equal(body.data.targetCategory, "All Users");
    assert.equal(typeof body.data.id, "string");
    assert.ok(body.data.id.length > 0);
    assert.equal(f.count("notifications"), 1);
    assert.equal(f.auditCount(), 1);
  } finally { f.sqlite.close(); }
});

test("a failed transaction is not retried with DDL or a legacy write", async () => {
  const f = fixture();
  try {
    let attempts = 0;
    let ddl = 0;
    const db = {
      ...f.d1,
      prepare(sql) {
        if (/^\s*ALTER TABLE/i.test(sql)) ddl++;
        return f.d1.prepare(sql);
      },
      async batch() { attempts++; throw new Error("simulated transient D1 failure"); },
    };
    const response = await f.post(db);
    assert.equal(response.status, 503);
    assert.equal((await response.json()).success, false);
    assert.equal(attempts, 1, "No unsafe attempt with a second INSERT");
    assert.equal(ddl, 0, "A live HTTP mutation must not run schema migrations");
    assert.equal(f.count("notifications"), 0);
    assert.equal(f.auditCount(), 0);
  } finally { f.sqlite.close(); }
});

test("legacy missing-column conditions cannot trigger runtime migration or fallback", async () => {
  const f = fixture();
  try {
    let ddl = 0, fallback = 0;
    const db = {
      ...f.d1,
      prepare(sql) {
        if (/^\s*ALTER TABLE/i.test(sql)) ddl++;
        if (/INSERT INTO notifications/i.test(sql) && !/target_category/i.test(sql)) fallback++;
        if (/INSERT INTO notifications/i.test(sql) && /target_category/i.test(sql)) {
          throw new Error("no such column: target_category");
        }
        return f.d1.prepare(sql);
      },
    };
    const response = await f.post(db);
    assert.equal(response.status, 503);
    assert.equal((await response.json()).success, false);
    assert.equal(ddl, 0);
    assert.equal(fallback, 0);
    assert.equal(f.count("notifications"), 0);
    assert.equal(f.auditCount(), 0);
  } finally { f.sqlite.close(); }
});

test("missing audit migration rolls back the new broadcast, never a partial success", async () => {
  const f = fixture();
  try {
    f.sqlite.exec("DROP TABLE admin_audit_log");
    const response = await f.post();
    assert.equal(response.status, 503);
    assert.equal((await response.json()).success, false);
    assert.equal(f.count("notifications"), 0);
  } finally { f.sqlite.close(); }
});

test("failed audit metadata or no changed-row evidence never produces created notice", async () => {
  const f = fixture();
  try {
    const db = { ...f.d1, async batch() {
      return [{ meta: { changes: 0 } }, { meta: { changes: 1 } }];
    }};
    const response = await f.post(db);
    assert.equal(response.status, 503);
    assert.equal((await response.json()).success, false);
    assert.equal(f.count("notifications"), 0);
  } finally { f.sqlite.close(); }
});

test("a limited admin cannot publish a broadcast through client-side role hints", async () => {
  const f = fixture();
  try {
    const response = await f.post(f.d1, valid, deniedEmail);
    assert.equal(response.status, 403);
    assert.equal(f.count("notifications"), 0);
    assert.equal(f.auditCount(), 0);
  } finally { f.sqlite.close(); }
});
