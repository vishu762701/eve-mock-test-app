import test from "node:test";
import assert from "node:assert/strict";
import { DatabaseSync } from "node:sqlite";
import { readFileSync, readdirSync } from "node:fs";
import { Hono } from "hono";
import { adminAuditLogRoutes } from "../src/routes/reports.ts";

function isolatedD1() {
  const sqlite = new DatabaseSync(":memory:");
  sqlite.exec("PRAGMA foreign_keys = ON");
  const migrationsDir = new URL("../migrations/", import.meta.url);
  const files = readdirSync(migrationsDir).filter((x) => x.endsWith(".sql")).sort();
  for (const name of files) {
    sqlite.exec(readFileSync(new URL("../migrations/" + name, import.meta.url), "utf8"));
  }
  return {
    sqlite,
    d1: {
      prepare(query) {
        function makeStmt(boundValues = []) {
          return {
            bind(...values) {
              return makeStmt(values);
            },
            async run() {
              const info = sqlite.prepare(query).run(...boundValues);
              return { meta: { changes: info.changes } };
            },
            async first() {
              return sqlite.prepare(query).get(...boundValues) || null;
            },
            async all() {
              return { results: sqlite.prepare(query).all(...boundValues) };
            },
          };
        }
        return makeStmt();
      },
      async batch(statements) {
        sqlite.exec("BEGIN");
        try {
          const results = [];
          for (const statement of statements) {
            results.push(await statement.run());
          }
          sqlite.exec("COMMIT");
          return results;
        } catch (error) {
          sqlite.exec("ROLLBACK");
          throw error;
        }
      },
    },
  };
}

function createTestApp(d1, user) {
  const app = new Hono();
  app.use("*", async (c, next) => {
    c.env = { DB: d1 };
    c.set("user", user);
    await next();
  });
  app.route("/api/admin/audit-log", adminAuditLogRoutes);
  return app;
}

test("Recent Actions: cursor pagination with limit and before cursor", async () => {
  const { sqlite, d1 } = isolatedD1();
  const admin = { uid: "owner_1", email: "pronlike9@gmail.com", isAdmin: true };

  // Insert 5 audit log entries with staggered timestamps
  const baseTs = 1700000000000;
  for (let i = 1; i <= 5; i++) {
    sqlite.prepare(
      `INSERT INTO admin_audit_log (id, action_type, description, admin_email, timestamp, actor_uid, target_type, target_id, source)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`
    ).run(`log_${i}`, "EXAM_CREATED", `Created Exam ${i}`, "pronlike9@gmail.com", baseTs + i * 1000, "owner_1", "exam", `exam_${i}`, "server");
  }

  const app = createTestApp(d1, admin);

  // 1. Fetch first page with limit=2
  const res1 = await app.request("http://localhost/api/admin/audit-log?limit=2");
  assert.equal(res1.status, 200);
  const json1 = await res1.json();
  assert.equal(json1.success, true);
  assert.equal(json1.data.length, 2);
  assert.equal(json1.hasMore, true);
  assert.equal(json1.data[0].id, "log_5");
  assert.equal(json1.data[1].id, "log_4");
  assert.equal(json1.nextCursor, baseTs + 4000);

  // 2. Fetch second page using before=nextCursor
  const res2 = await app.request(`http://localhost/api/admin/audit-log?limit=2&before=${json1.nextCursor}`);
  assert.equal(res2.status, 200);
  const json2 = await res2.json();
  assert.equal(json2.success, true);
  assert.equal(json2.data.length, 2);
  assert.equal(json2.hasMore, true);
  assert.equal(json2.data[0].id, "log_3");
  assert.equal(json2.data[1].id, "log_2");
  assert.equal(json2.nextCursor, baseTs + 2000);

  // 3. Fetch final page
  const res3 = await app.request(`http://localhost/api/admin/audit-log?limit=2&before=${json2.nextCursor}`);
  assert.equal(res3.status, 200);
  const json3 = await res3.json();
  assert.equal(json3.success, true);
  assert.equal(json3.data.length, 1);
  assert.equal(json3.hasMore, false);
  assert.equal(json3.data[0].id, "log_1");
  assert.equal(json3.nextCursor, null);
});

test("Recent Actions: filter by admin and action type", async () => {
  const { sqlite, d1 } = isolatedD1();
  const admin = { uid: "owner_1", email: "pronlike9@gmail.com", isAdmin: true };

  sqlite.prepare(
    `INSERT INTO admin_audit_log (id, action_type, description, admin_email, timestamp)
     VALUES (?, ?, ?, ?, ?)`
  ).run("a1", "EXAM_CREATED", "Exam created by Admin A", "admin_a@eve.app", 1000);
  sqlite.prepare(
    `INSERT INTO admin_audit_log (id, action_type, description, admin_email, timestamp)
     VALUES (?, ?, ?, ?, ?)`
  ).run("a2", "BROADCAST_SENT", "Broadcast by Admin A", "admin_a@eve.app", 2000);
  sqlite.prepare(
    `INSERT INTO admin_audit_log (id, action_type, description, admin_email, timestamp)
     VALUES (?, ?, ?, ?, ?)`
  ).run("b1", "EXAM_CREATED", "Exam created by Admin B", "admin_b@eve.app", 3000);

  const app = createTestApp(d1, admin);

  // Filter by admin_a only
  const resAdmin = await app.request("http://localhost/api/admin/audit-log?admin=admin_a@eve.app");
  const jsonAdmin = await resAdmin.json();
  assert.equal(jsonAdmin.success, true);
  assert.equal(jsonAdmin.data.length, 2);
  assert.ok(jsonAdmin.data.every((r) => r.adminEmail === "admin_a@eve.app"));

  // Filter by action type BROADCAST_SENT
  const resType = await app.request("http://localhost/api/admin/audit-log?type=BROADCAST_SENT");
  const jsonType = await resType.json();
  assert.equal(jsonType.success, true);
  assert.equal(jsonType.data.length, 1);
  assert.equal(jsonType.data[0].id, "a2");

  // Filter by both admin and type
  const resBoth = await app.request("http://localhost/api/admin/audit-log?admin=admin_a@eve.app&type=EXAM_CREATED");
  const jsonBoth = await resBoth.json();
  assert.equal(jsonBoth.success, true);
  assert.equal(jsonBoth.data.length, 1);
  assert.equal(jsonBoth.data[0].id, "a1");
});

test("Recent Actions: search query q matches description and targetId", async () => {
  const { sqlite, d1 } = isolatedD1();
  const admin = { uid: "owner_1", email: "pronlike9@gmail.com", isAdmin: true };

  sqlite.prepare(
    `INSERT INTO admin_audit_log (id, action_type, description, admin_email, timestamp, target_id)
     VALUES (?, ?, ?, ?, ?, ?)`
  ).run("s1", "EXAM_CREATED", "Created exam: SSC CGL Tier 1", "admin@eve.app", 1000, "exam_ssc_cgl");
  sqlite.prepare(
    `INSERT INTO admin_audit_log (id, action_type, description, admin_email, timestamp, target_id)
     VALUES (?, ?, ?, ?, ?, ?)`
  ).run("s2", "EXAM_CREATED", "Created exam: UPSC Prelims", "admin@eve.app", 2000, "exam_upsc_prelims");

  const app = createTestApp(d1, admin);

  const resSearch = await app.request("http://localhost/api/admin/audit-log?q=CGL");
  const jsonSearch = await resSearch.json();
  assert.equal(jsonSearch.success, true);
  assert.equal(jsonSearch.data.length, 1);
  assert.equal(jsonSearch.data[0].id, "s1");

  const resTargetSearch = await app.request("http://localhost/api/admin/audit-log?q=upsc_prelims");
  const jsonTargetSearch = await resTargetSearch.json();
  assert.equal(jsonTargetSearch.success, true);
  assert.equal(jsonTargetSearch.data.length, 1);
  assert.equal(jsonTargetSearch.data[0].id, "s2");
});

test("Recent Actions: metadata redaction excludes forbidden keys and secrets", async () => {
  const { sqlite, d1 } = isolatedD1();
  const admin = { uid: "owner_1", email: "pronlike9@gmail.com", isAdmin: true };

  const unsafeMetadata = JSON.stringify({
    safeName: "Exam 1",
    apiKey: "secret_12345",
    userPassword: "hash",
    token: "bearer_xyz",
    validScore: 100,
  });

  sqlite.prepare(
    `INSERT INTO admin_audit_log (id, action_type, description, admin_email, timestamp, metadata)
     VALUES (?, ?, ?, ?, ?, ?)`
  ).run("redact_1", "CONFIG_UPDATED", "Updated system config", "admin@eve.app", 1000, unsafeMetadata);

  const app = createTestApp(d1, admin);
  const res = await app.request("http://localhost/api/admin/audit-log");
  const json = await res.json();
  assert.equal(json.success, true);
  assert.equal(json.data.length, 1);
  const meta = json.data[0].metadata;
  assert.equal(meta.safeName, "Exam 1");
  assert.equal(meta.validScore, 100);
  assert.equal(meta.apiKey, undefined);
  assert.equal(meta.userPassword, undefined);
  assert.equal(meta.token, undefined);
});

test("Recent Actions: GET /api/admin/audit-log/types returns distinct sorted action types", async () => {
  const { sqlite, d1 } = isolatedD1();
  const admin = { uid: "owner_1", email: "pronlike9@gmail.com", isAdmin: true };

  sqlite.prepare(
    `INSERT INTO admin_audit_log (id, action_type, description, admin_email, timestamp)
     VALUES (?, ?, ?, ?, ?)`
  ).run("t1", "QUESTION_CREATED", "Q1", "admin@eve.app", 1000);
  sqlite.prepare(
    `INSERT INTO admin_audit_log (id, action_type, description, admin_email, timestamp)
     VALUES (?, ?, ?, ?, ?)`
  ).run("t2", "EXAM_CREATED", "E1", "admin@eve.app", 2000);
  sqlite.prepare(
    `INSERT INTO admin_audit_log (id, action_type, description, admin_email, timestamp)
     VALUES (?, ?, ?, ?, ?)`
  ).run("t3", "EXAM_CREATED", "E2", "admin@eve.app", 3000);

  const app = createTestApp(d1, admin);
  const res = await app.request("http://localhost/api/admin/audit-log/types");
  assert.equal(res.status, 200);
  const json = await res.json();
  assert.equal(json.success, true);
  assert.deepEqual(json.data, ["EXAM_CREATED", "QUESTION_CREATED"]);
});

test("Recent Actions: authorization requires view_analytics_logs right", async () => {
  const { sqlite, d1 } = isolatedD1();
  const restrictedAdmin = { uid: "helper_1", email: "helper@eve.app", isAdmin: false };

  // Helper has an entry in admins and admin_rights, but only with manage_content (no view_analytics_logs)
  sqlite.prepare("INSERT INTO admins (email, created_at) VALUES (?, ?)").run("helper@eve.app", Date.now());
  sqlite.prepare("INSERT INTO admin_rights (email, rights, custom_title, updated_at) VALUES (?, ?, ?, ?)").run("helper@eve.app", JSON.stringify(["manage_content"]), "Assistant", Date.now());

  const app = createTestApp(d1, restrictedAdmin);
  const res = await app.request("http://localhost/api/admin/audit-log");
  assert.equal(res.status, 403);
});
