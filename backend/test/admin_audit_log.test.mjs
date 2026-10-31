import test from "node:test";
import assert from "node:assert/strict";
import { DatabaseSync } from "node:sqlite";
import { readFileSync, readdirSync } from "node:fs";
import { Hono } from "hono";
import { adminAuditLogRoutes, adminReportRoutes } from "../src/routes/reports.ts";
import { adminRoutes } from "../src/routes/admin.ts";
import { prepareAdminAuditLog, recordAdminAction, runBatch, sanitizeMetadata } from "../src/util/auditLog.ts";

function isolatedD1() {
  const sqlite = new DatabaseSync(":memory:");
  sqlite.exec("PRAGMA foreign_keys = ON");
  const migrationsDir = new URL("../migrations/", import.meta.url);
  const files = readdirSync(migrationsDir).filter((x) => x.endsWith(".sql")).sort();
  for (const name of files) {
    sqlite.exec(readFileSync(new URL("../migrations/" + name, import.meta.url), "utf8"));
  }
  // These route fixtures are actual delegated audit viewers, not an isAdmin bypass.
  for (const email of ["legit@eve.app", "admin@eve.app", "admin1@eve.app"]) {
    sqlite.prepare("INSERT INTO admins (email, created_at) VALUES (?, ?)").run(email, 1000);
    sqlite.prepare("INSERT INTO admin_rights (email, rights, updated_at) VALUES (?, ?, ?)")
      .run(email, JSON.stringify(["view_analytics_logs"]), 1000);
  }
  return {
    sqlite,
    d1: {
      prepare(query) {
        return {
          bind(...values) {
            return {
              async run() {
                const info = sqlite.prepare(query).run(...values);
                return { meta: { changes: info.changes } };
              },
              async first() {
                return sqlite.prepare(query).get(...values) || null;
              },
              async all() {
                return { results: sqlite.prepare(query).all(...values) };
              },
            };
          },
        };
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
  app.route("/api/admin/reports", adminReportRoutes);
  app.route("/api/admin", adminRoutes);
  return app;
}

test("sanitizeMetadata strips sensitive keys and limits size", () => {
  const unsafe = {
    userToken: "secret-token-xyz",
    password_hash: "argon2id$...",
    authCredential: "supersecret",
    correctAnswer: "Option B",
    username: "john_doe",
    user_phone: "+919876543210",
    safeKey: "legitimate_info",
    numberVal: 42,
    boolVal: true,
    longString: "a".repeat(300),
  };

  const sanitizedStr = sanitizeMetadata(unsafe);
  assert.ok(sanitizedStr);
  const parsed = JSON.parse(sanitizedStr);

  assert.equal(parsed.safeKey, "legitimate_info");
  assert.equal(parsed.numberVal, 42);
  assert.equal(parsed.boolVal, true);
  assert.equal(parsed.longString.length, 203); // 200 + "..."
  assert.equal(parsed.userToken, undefined);
  assert.equal(parsed.password_hash, undefined);
  assert.equal(parsed.authCredential, undefined);
  assert.equal(parsed.correctAnswer, undefined);
  assert.equal(parsed.username, undefined);
  assert.equal(parsed.user_phone, undefined);
});

test("prepareAdminAuditLog and recordAdminAction insert valid audit records with actor_uid", async () => {
  const { sqlite, d1 } = isolatedD1();
  const testAdmin = {
    uid: "admin_uid_123",
    email: "superadmin@eve.app",
    isAdmin: true,
  };

  const id = await recordAdminAction(d1, testAdmin, {
    actionType: "TEST_ACTION",
    description: "Testing server-side audit helper",
    targetType: "test_entity",
    targetId: "entity_999",
    metadata: { key: "value" },
  });

  const row = sqlite.prepare("SELECT * FROM admin_audit_log WHERE id = ?").get(id);
  assert.ok(row);
  assert.equal(row.action_type, "TEST_ACTION");
  assert.equal(row.description, "Testing server-side audit helper");
  assert.equal(row.admin_email, "superadmin@eve.app");
  assert.equal(row.actor_uid, "admin_uid_123");
  assert.equal(row.target_type, "test_entity");
  assert.equal(row.target_id, "entity_999");
  assert.equal(row.source, "server");
  assert.equal(JSON.parse(row.metadata).key, "value");
});

test("POST /api/admin/audit-log creates client audit record and ignores forged email/source", async () => {
  const { sqlite, d1 } = isolatedD1();
  const legitimateAdmin = {
    uid: "legit_admin_uid",
    email: "legit@eve.app",
    isAdmin: true,
  };

  const app = createTestApp(d1, legitimateAdmin);

  // Attempt to forge adminEmail and set source to server in body
  const res = await app.request("http://localhost/api/admin/audit-log", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      actionType: "EXAM_DATA_EXPORTED",
      description: "Exported backup of 5 exams",
      adminEmail: "victim@eve.app", // FORGED
      source: "server", // FORGED
    }),
  });

  assert.equal(res.status, 201);
  const json = await res.json();
  assert.equal(json.success, true);
  assert.ok(json.data.id);

  const row = sqlite.prepare("SELECT * FROM admin_audit_log WHERE id = ?").get(json.data.id);
  assert.ok(row);
  assert.equal(row.action_type, "EXAM_DATA_EXPORTED");
  assert.equal(row.description, "Exported backup of 5 exams");
  // Identity MUST come from verified token, not forged body
  assert.equal(row.admin_email, "legit@eve.app");
  assert.equal(row.actor_uid, "legit_admin_uid");
  assert.equal(row.source, "client");
});

test("POST /api/admin/audit-log validates actionType format", async () => {
  const { d1 } = isolatedD1();
  const legitimateAdmin = {
    uid: "admin_uid",
    email: "admin@eve.app",
    isAdmin: true,
  };

  const app = createTestApp(d1, legitimateAdmin);

  // Lowercase actionType
  const resBad = await app.request("http://localhost/api/admin/audit-log", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      actionType: "invalid_action_name",
      description: "Should fail validation",
    }),
  });
  assert.equal(resBad.status, 400);

  // Missing actionType
  const resMissing = await app.request("http://localhost/api/admin/audit-log", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      description: "Missing action type",
    }),
  });
  assert.equal(resMissing.status, 400);
});

test("GET /api/admin/audit-log returns records with actorUid and metadata", async () => {
  const { sqlite, d1 } = isolatedD1();
  const admin = {
    uid: "admin_1",
    email: "admin1@eve.app",
    isAdmin: true,
  };

  const now = Date.now();
  sqlite.prepare(
    `INSERT INTO admin_audit_log (id, action_type, description, admin_email, timestamp, actor_uid, target_type, target_id, metadata, source)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
  ).run("log_1", "ADMIN_ADDED", "Added new admin test@eve.app", "admin1@eve.app", now, "admin_1", "admin", "test@eve.app", JSON.stringify({ email: "test@eve.app" }), "server");

  const app = createTestApp(d1, admin);
  const res = await app.request("http://localhost/api/admin/audit-log?range=all");
  assert.equal(res.status, 200);
  const json = await res.json();
  assert.equal(json.success, true);
  assert.equal(json.data.length, 1);
  assert.equal(json.data[0].id, "log_1");
  assert.equal(json.data[0].actionType, "ADMIN_ADDED");
  assert.equal(json.data[0].adminEmail, "admin1@eve.app");
  assert.equal(json.data[0].actorUid, "admin_1");
  assert.equal(json.data[0].targetType, "admin");
  assert.equal(json.data[0].targetId, "test@eve.app");
  assert.equal(json.data[0].source, "server");
  assert.equal(json.data[0].metadata.email, "test@eve.app");
});

test("Batch atomicity: audit log is rolled back if mutation fails", async () => {
  const { sqlite, d1 } = isolatedD1();
  const admin = {
    uid: "admin_atomic",
    email: "atomic@eve.app",
    isAdmin: true,
  };

  const { stmt: auditStmt } = prepareAdminAuditLog(d1, admin, {
    actionType: "FAILED_MUTATION_TEST",
    description: "Should not persist if transaction fails",
  });

  const failingStmt = d1.prepare("INSERT INTO non_existent_table (x) VALUES (?)").bind(1);

  await assert.rejects(async () => {
    await d1.batch([failingStmt, auditStmt]);
  });

  const count = sqlite.prepare("SELECT COUNT(*) as c FROM admin_audit_log WHERE action_type = 'FAILED_MUTATION_TEST'").get();
  assert.equal(count.c, 0);
});

test("missing audit migration prevents admin mutation and rolls back its D1 batch", async () => {
  const { sqlite, d1 } = isolatedD1();
  sqlite.exec("CREATE TABLE audit_guard_fixture (id TEXT PRIMARY KEY); DROP TABLE admin_audit_log");

  const mutation = d1.prepare("INSERT INTO audit_guard_fixture (id) VALUES (?)").bind("should-rollback");
  const audit = prepareAdminAuditLog(d1, { uid: "admin_1", email: "admin@eve.app", isAdmin: true }, {
    actionType: "TEST_MUTATION",
    description: "This should not be committed without an audit record",
  }).stmt;

  await assert.rejects(() => runBatch(d1, [mutation, audit]), /no such table: admin_audit_log/i);
  assert.equal(sqlite.prepare("SELECT COUNT(*) AS count FROM audit_guard_fixture").get().count, 0);
});

test("single admin audit writes reject a missing audit table", async () => {
  const { sqlite, d1 } = isolatedD1();
  sqlite.exec("DROP TABLE admin_audit_log");
  await assert.rejects(() => recordAdminAction(d1, {
    uid: "admin_1", email: "admin@eve.app", isAdmin: true,
  }, { actionType: "TEST_MUTATION", description: "Fail closed" }), /no such table: admin_audit_log/i);
});

test("audited mutations require an atomic D1 batch", async () => {
  await assert.rejects(() => runBatch({}, []), /Atomic D1 batch is required/i);
});

test("oversized sanitized audit metadata is valid bounded JSON", () => {
  const payload = Object.fromEntries(Array.from({ length: 40 }, (_, i) => [`safe_${i}`, "x".repeat(180)]));
  const result = sanitizeMetadata(payload);
  assert.ok(result);
  assert.ok(result.length <= 2048);
  const parsed = JSON.parse(result);
  assert.ok(Object.keys(parsed).length > 0);
  assert.ok(Object.keys(parsed).length < 40);
});
