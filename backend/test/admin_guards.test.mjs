import test from "node:test";
import assert from "node:assert/strict";
import { DatabaseSync } from "node:sqlite";
import { readFileSync, readdirSync } from "node:fs";
import { Hono } from "hono";
import { adminRoutes } from "../src/routes/admin.ts";
import { authMiddleware } from "../src/middleware/authMiddleware.ts";
import { ALL_ADMIN_RIGHTS, HARDCODED_ADMIN_EMAILS } from "../src/auth.ts";

function grantRights(sqlite, email, rights) {
  sqlite.prepare("INSERT OR IGNORE INTO admins (email, created_at) VALUES (?, ?)").run(email, 1000);
  sqlite.prepare("INSERT INTO admin_rights (email, rights, updated_at) VALUES (?, ?, ?)")
    .run(email, JSON.stringify(rights), 1000);
}

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

function createAdminApp(d1, user) {
  const app = new Hono();
  app.use("*", async (c, next) => {
    c.env = { DB: d1, FIREBASE_PROJECT_ID: "test-project" };
    c.set("user", user);
    await next();
  });
  app.route("/api/admin", adminRoutes);
  return app;
}

test("Admin guards: POST /api/admin/admins email validation and idempotency", async () => {
  const { sqlite, d1 } = isolatedD1();
  const admin = { uid: "admin_1", email: "own.keni@gmail.com", isAdmin: true };
  const app = createAdminApp(d1, admin);

  // Reject invalid email
  const bad1 = await app.request("http://localhost/api/admin/admins", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email: "invalid-email" }),
  });
  assert.equal(bad1.status, 400);

  // Reject overlong email
  const bad2 = await app.request("http://localhost/api/admin/admins", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email: "a".repeat(250) + "@eve.app" }),
  });
  assert.equal(bad2.status, 400);

  // Accept valid email
  const ok1 = await app.request("http://localhost/api/admin/admins", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email: "NewAdmin@Eve.App " }),
  });
  assert.equal(ok1.status, 201);
  const json1 = await ok1.json();
  assert.equal(json1.data.email, "newadmin@eve.app");

  // Idempotent add
  const ok2 = await app.request("http://localhost/api/admin/admins", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email: "newadmin@eve.app" }),
  });
  assert.equal(ok2.status, 201);

  const count = sqlite.prepare("SELECT COUNT(*) as c FROM admins WHERE email = 'newadmin@eve.app'").get();
  assert.equal(count.c, 1);
});

test("Admin guards: DELETE /api/admin/admins/:email self, built-in, and last admin protection", async () => {
  const { sqlite, d1 } = isolatedD1();
  const adminUser = { uid: "admin_1", email: "dynadmin@eve.app", isAdmin: true };
  sqlite.prepare("INSERT INTO admins (email, created_at) VALUES ('dynadmin@eve.app', 1000)").run();
  sqlite.prepare("INSERT INTO admins (email, created_at) VALUES ('other@eve.app', 1000)").run();
  grantRights(sqlite, adminUser.email, [...ALL_ADMIN_RIGHTS]);

  const app = createAdminApp(d1, adminUser);

  // 1. Cannot remove yourself (409)
  const resSelf = await app.request("http://localhost/api/admin/admins/dynadmin@eve.app", { method: "DELETE" });
  assert.equal(resSelf.status, 409);
  const jsonSelf = await resSelf.json();
  assert.match(jsonSelf.error, /yourself/i);

  // 2. Cannot remove hardcoded built-in admin (409)
  const resBuiltin = await app.request("http://localhost/api/admin/admins/own.keni@gmail.com", { method: "DELETE" });
  assert.equal(resBuiltin.status, 409);
  const jsonBuiltin = await resBuiltin.json();
  assert.match(jsonBuiltin.error, /built-in/i);

  // 3. Unknown admin returns 404
  const resNotFound = await app.request("http://localhost/api/admin/admins/ghost@eve.app", { method: "DELETE" });
  assert.equal(resNotFound.status, 404);

  // 4. Can remove another dynamic admin when multiple exist
  const resOk = await app.request("http://localhost/api/admin/admins/other@eve.app", { method: "DELETE" });
  assert.equal(resOk.status, 200);
});

test("Admin guards: POST /api/admin/users/:id/status cannot disable self or any admin", async () => {
  const { sqlite, d1 } = isolatedD1();
  const actingAdmin = { uid: "admin_actor_uid", email: "acting@eve.app", isAdmin: true };
  grantRights(sqlite, actingAdmin.email, ["manage_users"]);

  // Insert acting admin, another admin, and a regular student
  sqlite.prepare("INSERT INTO users (id, email, display_name, created_at, last_active, disabled) VALUES (?, ?, ?, ?, ?, ?)").run(
    "admin_actor_uid", "acting@eve.app", "Acting Admin", 1000, 2000, 0
  );
  sqlite.prepare("INSERT INTO users (id, email, display_name, created_at, last_active, disabled) VALUES (?, ?, ?, ?, ?, ?)").run(
    "admin_peer_uid", "peer@eve.app", "Peer Admin", 1000, 2000, 0
  );
  sqlite.prepare("INSERT INTO admins (email, created_at) VALUES ('peer@eve.app', 1000)").run();
  sqlite.prepare("INSERT INTO users (id, email, display_name, created_at, last_active, disabled) VALUES (?, ?, ?, ?, ?, ?)").run(
    "student_uid", "student@eve.app", "Student User", 1000, 2000, 0
  );

  const app = createAdminApp(d1, actingAdmin);

  // 1. Unknown user returns 404
  const res404 = await app.request("http://localhost/api/admin/users/non_existent/status", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ disabled: true }),
  });
  assert.equal(res404.status, 404);

  // 2. Cannot disable acting admin (self) -> 409
  const resSelf = await app.request("http://localhost/api/admin/users/admin_actor_uid/status", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ disabled: true }),
  });
  assert.equal(resSelf.status, 409);
  const jsonSelf = await resSelf.json();
  assert.match(jsonSelf.error, /own/i);

  // 3. Cannot disable peer admin -> 409
  const resPeer = await app.request("http://localhost/api/admin/users/admin_peer_uid/status", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ disabled: true }),
  });
  assert.equal(resPeer.status, 409);
  const jsonPeer = await resPeer.json();
  assert.match(jsonPeer.error, /administrator/i);

  // 4. Can disable regular student
  const resStudentDisable = await app.request("http://localhost/api/admin/users/student_uid/status", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ disabled: true }),
  });
  assert.equal(resStudentDisable.status, 200);
  const rowDisabled = sqlite.prepare("SELECT disabled FROM users WHERE id = 'student_uid'").get();
  assert.equal(rowDisabled.disabled, 1);

  // 5. Can re-enable regular student
  const resStudentEnable = await app.request("http://localhost/api/admin/users/student_uid/status", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ disabled: false }),
  });
  assert.equal(resStudentEnable.status, 200);
  const rowEnabled = sqlite.prepare("SELECT disabled FROM users WHERE id = 'student_uid'").get();
  assert.equal(rowEnabled.disabled, 0);
});

test("Admin guards: Premium grant, extend, and revoke validation", async () => {
  const { sqlite, d1 } = isolatedD1();
  const admin = { uid: "admin_1", email: "admin@eve.app", isAdmin: true };
  grantRights(sqlite, admin.email, ["manage_premium"]);
  sqlite.prepare("INSERT INTO users (id, email, display_name, created_at, last_active, disabled) VALUES (?, ?, ?, ?, ?, ?)").run(
    "user_prem", "prem@eve.app", "Prem Student", 1000, 2000, 0
  );

  const app = createAdminApp(d1, admin);

  // Grant without lifetime confirmation fails (400)
  const resNoConf = await app.request("http://localhost/api/admin/premium/users/grant", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ userId: "user_prem", isLifetime: true }),
  });
  assert.equal(resNoConf.status, 400);
  assert.match((await resNoConf.json()).error, /confirmLifetime/i);

  // Grant with invalid durationDays fails (400)
  const resBadDays = await app.request("http://localhost/api/admin/premium/users/grant", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ userId: "user_prem", durationDays: -5 }),
  });
  assert.equal(resBadDays.status, 400);

  const resFloatDays = await app.request("http://localhost/api/admin/premium/users/grant", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ userId: "user_prem", durationDays: 30.5 }),
  });
  assert.equal(resFloatDays.status, 400);

  const resOverDays = await app.request("http://localhost/api/admin/premium/users/grant", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ userId: "user_prem", durationDays: 5000 }),
  });
  assert.equal(resOverDays.status, 400);

  // Valid grant succeeds
  const resGrantOk = await app.request("http://localhost/api/admin/premium/users/grant", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ userId: "user_prem", durationDays: 30, planName: "Pro Plan" }),
  });
  assert.equal(resGrantOk.status, 200);

  // Extend with invalid additionalDays fails (400)
  const resBadExtend = await app.request("http://localhost/api/admin/premium/users/extend", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ userId: "user_prem", additionalDays: 0 }),
  });
  assert.equal(resBadExtend.status, 400);

  // Valid extend succeeds
  const resExtendOk = await app.request("http://localhost/api/admin/premium/users/extend", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ userId: "user_prem", additionalDays: 15 }),
  });
  assert.equal(resExtendOk.status, 200);

  // Revoke unknown user fails (404)
  const resRevoke404 = await app.request("http://localhost/api/admin/premium/users/revoke", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ userId: "ghost_user" }),
  });
  assert.equal(resRevoke404.status, 404);

  // Valid revoke succeeds
  const resRevokeOk = await app.request("http://localhost/api/admin/premium/users/revoke", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ userId: "user_prem" }),
  });
  assert.equal(resRevokeOk.status, 200);
});

test("Admin guards: PUT /api/admin/config type and boundary validation", async () => {
  const { sqlite, d1 } = isolatedD1();
  const admin = { uid: "admin_1", email: "admin@eve.app", isAdmin: true };
  grantRights(sqlite, admin.email, ["manage_config_maintenance"]);
  const app = createAdminApp(d1, admin);

  // Reject negative minimum_supported_version_code
  const resNeg = await app.request("http://localhost/api/admin/config", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ minimum_supported_version_code: -1 }),
  });
  assert.equal(resNeg.status, 400);

  // Reject non-integer version code
  const resFloat = await app.request("http://localhost/api/admin/config", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ minimum_supported_version_code: 2.5 }),
  });
  assert.equal(resFloat.status, 400);

  // Reject non-boolean maintenance_mode
  const resBool = await app.request("http://localhost/api/admin/config", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ maintenance_mode: "yes" }),
  });
  assert.equal(resBool.status, 400);

  // Reject overlong maintenance_message
  const resMsg = await app.request("http://localhost/api/admin/config", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ maintenance_message: "x".repeat(1001) }),
  });
  assert.equal(resMsg.status, 400);

  // Reject invalid default_publish_mode
  const resMode = await app.request("http://localhost/api/admin/config", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ default_publish_mode: "invalid_mode" }),
  });
  assert.equal(resMode.status, 400);

  // Valid config update succeeds
  const resOk = await app.request("http://localhost/api/admin/config", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      minimum_supported_version_code: 5,
      maintenance_mode: true,
      maintenance_message: "Short scheduled downtime",
      default_publish_mode: "live",
    }),
  });
  assert.equal(resOk.status, 200);
  const jsonOk = await resOk.json();
  assert.equal(jsonOk.data.minimum_supported_version_code, 5);
  assert.equal(jsonOk.data.maintenance_mode, true);
  assert.equal(jsonOk.data.default_publish_mode, "live");
});

test("Admin grant and action log roll back together if audit schema is absent", async () => {
  const { sqlite, d1 } = isolatedD1();
  const admin = { uid: "admin_fixture", email: "audit-admin@example.test", isAdmin: true };
  grantRights(sqlite, admin.email, ["manage_premium"]);
  sqlite.prepare("INSERT INTO users (id, email, display_name, created_at, last_active) VALUES (?, ?, ?, ?, ?)")
    .run("premium-target", "premium-target@example.test", "Premium Target", 1, 1);
  sqlite.exec("DROP TABLE admin_audit_log");
  const response = await createAdminApp(d1, admin).request("http://localhost/api/admin/premium/users/grant", {
    method: "POST", headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ userId: "premium-target", durationDays: 30 }),
  });
  assert.equal(response.status, 500);
  assert.equal(sqlite.prepare("SELECT COUNT(*) AS n FROM premium_entitlements WHERE user_id = ?").get("premium-target").n, 0);
});
test("Admin premium grants persist exactly one matching server-owned audit entry", async () => {
  const { sqlite, d1 } = isolatedD1();
  const admin = { uid: "admin_fixture", email: "audit-admin@example.test", isAdmin: true };
  grantRights(sqlite, admin.email, ["manage_premium"]);
  sqlite.prepare("INSERT INTO users (id, email, display_name, created_at, last_active) VALUES (?, ?, ?, ?, ?)")
    .run("premium-target", "premium-target@example.test", "Premium Target", 1, 1);
  const response = await createAdminApp(d1, admin).request("http://localhost/api/admin/premium/users/grant", {
    method: "POST", headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ userId: "premium-target", durationDays: 30 }),
  });
  assert.equal(response.status, 200);
  assert.equal(sqlite.prepare("SELECT COUNT(*) AS n FROM premium_entitlements WHERE user_id = ?").get("premium-target").n, 1);
  assert.equal(sqlite.prepare("SELECT COUNT(*) AS n FROM admin_audit_log WHERE action_type = ? AND target_id = ?").get("PREMIUM_GRANTED", "premium-target").n, 1);
});

test("maintenance-only on/off writes preserve unrelated config and audit each write", async () => {
  const { sqlite, d1 } = isolatedD1();
  const admin = { uid: "maintenance-owner", email: "maintenance@eve.test", isAdmin: true };
  grantRights(sqlite, admin.email, ["manage_config_maintenance"]);
  const original = { maintenance_mode: false, minimum_supported_version_code: 47, maintenance_message: "Please retry later", default_publish_mode: "live", future_setting: { keep: true } };
  sqlite.prepare("INSERT INTO app_content (id,title,body,updated_at) VALUES ('app_config','Config',?,1)").run(JSON.stringify(original));
  const app = createAdminApp(d1, admin);
  for (const enabled of [true, false]) {
    const response = await app.request("/api/admin/config", {
      method: "PUT", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ maintenance_mode: enabled }),
    });
    assert.equal(response.status, 200);
    const body = await response.json();
    assert.deepEqual(body.data, { ...original, maintenance_mode: enabled });
    assert.deepEqual(JSON.parse(sqlite.prepare("SELECT body FROM app_content WHERE id='app_config'").get().body), body.data);
  }
  assert.equal(sqlite.prepare("SELECT count(*) as n FROM admin_audit_log WHERE action_type='MAINTENANCE_TOGGLED'").get().n, 2);
});

test("config refuses corrupt prior storage and malformed requests without overwriting", async () => {
  const { sqlite, d1 } = isolatedD1();
  const admin = { uid: "maintenance-owner", email: "maintenance@eve.test", isAdmin: true };
  grantRights(sqlite, admin.email, ["manage_config_maintenance"]);
  sqlite.prepare("INSERT INTO app_content (id,title,body,updated_at) VALUES ('app_config','Config','corrupt',1)").run();
  const app = createAdminApp(d1, admin);
  for (const [body, status] of [["{", 400], ["null", 400], ["[]", 400], ['{"maintenance_mode":true}', 503]]) {
    const response = await app.request("/api/admin/config", { method: "PUT", headers: { "Content-Type": "application/json" }, body });
    assert.equal(response.status, status);
    assert.equal(sqlite.prepare("SELECT body FROM app_content WHERE id='app_config'").get().body, 'corrupt');
  }
});

test("maintenance merge preserves an unrelated edit made after its initial read", async () => {
  const { sqlite, d1 } = isolatedD1();
  const admin = { uid: "maintenance-owner", email: "maintenance@eve.test", isAdmin: true };
  grantRights(sqlite, admin.email, ["manage_config_maintenance"]);
  const original = { minimum_supported_version_code: 1, maintenance_mode: false, maintenance_message: "old", default_publish_mode: "paused" };
  sqlite.prepare("INSERT INTO app_content (id,title,body,updated_at) VALUES ('app_config','Config',?,1)").run(JSON.stringify(original));
  const realBatch = d1.batch;
  d1.batch = async (statements) => {
    sqlite.prepare("UPDATE app_content SET body = json_set(body, '$.maintenance_message', 'concurrent message', '$.minimum_supported_version_code', 82) WHERE id='app_config'").run();
    return realBatch(statements);
  };
  const response = await createAdminApp(d1, admin).request("/api/admin/config", {
    method: "PUT", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ maintenance_mode: true }),
  });
  assert.equal(response.status, 200);
  assert.deepEqual((await response.json()).data, { ...original, maintenance_mode: true, maintenance_message: "concurrent message", minimum_supported_version_code: 82 });
});
