import test from "node:test";
import assert from "node:assert/strict";
import { DatabaseSync } from "node:sqlite";
import { readFileSync, readdirSync } from "node:fs";
import { Hono } from "hono";
import { adminRoutes } from "../src/routes/admin.ts";
import { examRoutes } from "../src/routes/exams.ts";
import { ALL_ADMIN_RIGHTS, HARDCODED_ADMIN_EMAILS, isUserAdmin } from "../src/auth.ts";

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
    c.env = { DB: d1, FIREBASE_PROJECT_ID: "test-project" };
    c.set("user", user);
    await next();
  });
  app.route("/api/admin", adminRoutes);
  app.route("/api/exams", examRoutes);
  return app;
}

test("Admin rights: GET /api/admin/me returns owner vs dynamic admin permissions", async () => {
  const { sqlite, d1 } = isolatedD1();
  const ownerUser = { uid: "owner_1", email: "pronlike9@gmail.com", isAdmin: true };
  const ownerApp = createTestApp(d1, ownerUser);

  const resOwner = await ownerApp.request("http://localhost/api/admin/me");
  assert.equal(resOwner.status, 200);
  const dataOwner = await resOwner.json();
  assert.equal(dataOwner.success, true);
  assert.equal(dataOwner.data.isOwner, true);
  assert.equal(dataOwner.data.customTitle, "Owner");
  assert.equal(dataOwner.data.rights.length, ALL_ADMIN_RIGHTS.length);

  // Setup dynamic admin with custom rights
  const dynamicEmail = "helper@eve.app";
  sqlite.prepare("INSERT INTO admins (email, created_at) VALUES (?, ?)").run(dynamicEmail, 1000);
  sqlite.prepare("INSERT INTO admin_rights (email, rights, custom_title, updated_at) VALUES (?, ?, ?, ?)").run(
    dynamicEmail,
    JSON.stringify(["manage_exams", "manage_questions"]),
    "Content Lead",
    1000
  );

  const helperUser = { uid: "helper_1", email: dynamicEmail, isAdmin: true };
  const helperApp = createTestApp(d1, helperUser);

  const resHelper = await helperApp.request("http://localhost/api/admin/me");
  assert.equal(resHelper.status, 200);
  const dataHelper = await resHelper.json();
  assert.equal(dataHelper.success, true);
  assert.equal(dataHelper.data.isOwner, false);
  assert.equal(dataHelper.data.customTitle, "Content Lead");
  assert.deepEqual(dataHelper.data.rights, ["manage_exams", "manage_questions"]);
});

test("Admin rights: requirePermission enforcement allows permitted action and denies unpermitted action", async () => {
  const { sqlite, d1 } = isolatedD1();
  const dynamicEmail = "helper@eve.app";
  sqlite.prepare("INSERT INTO admins (email, created_at) VALUES (?, ?)").run(dynamicEmail, 1000);
  // helper holds only manage_exams
  sqlite.prepare("INSERT INTO admin_rights (email, rights, custom_title, updated_at) VALUES (?, ?, ?, ?)").run(
    dynamicEmail,
    JSON.stringify(["manage_exams"]),
    "Exam Manager",
    1000
  );

  const helperUser = { uid: "helper_1", email: dynamicEmail, isAdmin: true };
  const app = createTestApp(d1, helperUser);

  // POST /api/exams requires manage_exams -> should succeed (validation error 400 is fine, but not 403)
  const resExam = await app.request("http://localhost/api/exams", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({}),
  });
  // Empty payload should fail with 400 bad request, proving permission passed
  assert.equal(resExam.status, 400);

  // PUT /api/admin/config requires manage_config_maintenance -> must return 403 Forbidden
  const resConfig = await app.request("http://localhost/api/admin/config", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ maintenanceMode: true }),
  });
  assert.equal(resConfig.status, 403);
  const jsonConfig = await resConfig.json();
  assert.match(jsonConfig.error, /Missing required permission: manage_config_maintenance/i);
});

test("Admin rights: Owner permissions cannot be modified (built-in owner immutability)", async () => {
  const { d1 } = isolatedD1();
  const ownerUser = { uid: "owner_1", email: "pronlike9@gmail.com", isAdmin: true };
  const app = createTestApp(d1, ownerUser);

  const res = await app.request("http://localhost/api/admin/admins/pronlike9@gmail.com/rights", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ rights: ["manage_exams"] }),
  });
  assert.equal(res.status, 403);
  const json = await res.json();
  assert.match(json.error, /Built-in owner permissions and role cannot be modified/i);
});

test("Admin rights: Self-escalation and granting unheld rights are blocked", async () => {
  const { sqlite, d1 } = isolatedD1();
  const adminEmail = "subadmin@eve.app";
  sqlite.prepare("INSERT INTO admins (email, created_at) VALUES (?, ?)").run(adminEmail, 1000);
  sqlite.prepare("INSERT INTO admin_rights (email, rights, custom_title, updated_at) VALUES (?, ?, ?, ?)").run(
    adminEmail,
    JSON.stringify(["manage_admins_rights", "manage_exams"]),
    "Admin",
    1000
  );

  const adminUser = { uid: "subadmin_1", email: adminEmail, isAdmin: true };
  const app = createTestApp(d1, adminUser);

  // 1. Self-escalation attempt: subadmin tries to add manage_users to self
  const resSelf = await app.request(`http://localhost/api/admin/admins/${adminEmail}/rights`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ rights: ["manage_admins_rights", "manage_exams", "manage_users"] }),
  });
  assert.equal(resSelf.status, 403);
  const jsonSelf = await resSelf.json();
  assert.match(jsonSelf.error, /Self-escalation is not allowed/i);

  // 2. Granting unheld rights to another admin
  const peerEmail = "peer@eve.app";
  sqlite.prepare("INSERT INTO admins (email, created_at) VALUES (?, ?)").run(peerEmail, 1000);
  sqlite.prepare("INSERT INTO admin_rights (email, rights, custom_title, updated_at) VALUES (?, ?, ?, ?)").run(
    peerEmail,
    JSON.stringify(["manage_exams"]),
    "Admin",
    1000
  );

  const resPeer = await app.request(`http://localhost/api/admin/admins/${peerEmail}/rights`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ rights: ["manage_exams", "manage_users"] }),
  });
  assert.equal(resPeer.status, 403);
  const jsonPeer = await resPeer.json();
  assert.match(jsonPeer.error, /Cannot grant permission 'manage_users' which you do not possess/i);
});

test("Admin rights: Successful rights update generates ADMIN_RIGHTS_CHANGED audit log", async () => {
  const { sqlite, d1 } = isolatedD1();
  const ownerUser = { uid: "owner_1", email: "pronlike9@gmail.com", isAdmin: true };
  const app = createTestApp(d1, ownerUser);

  const targetEmail = "delegate@eve.app";
  sqlite.prepare("INSERT INTO admins (email, created_at) VALUES (?, ?)").run(targetEmail, 1000);

  const res = await app.request(`http://localhost/api/admin/admins/${targetEmail}/rights`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      rights: ["manage_exams", "manage_questions", "moderate_feedback"],
      customTitle: "Senior Moderator",
    }),
  });
  assert.equal(res.status, 200);
  const json = await res.json();
  assert.equal(json.success, true);
  assert.equal(json.data.customTitle, "Senior Moderator");
  assert.deepEqual(json.data.rights, ["manage_exams", "manage_questions", "moderate_feedback"]);

  // Verify in D1
  const updatedRights = sqlite.prepare("SELECT rights, custom_title FROM admin_rights WHERE email = ?").get(targetEmail);
  assert.equal(updatedRights.custom_title, "Senior Moderator");
  assert.deepEqual(JSON.parse(updatedRights.rights), ["manage_exams", "manage_questions", "moderate_feedback"]);

  // Verify audit log record
  const auditRow = sqlite.prepare("SELECT action_type, target_type, target_id FROM admin_audit_log WHERE target_id = ?").get(targetEmail);
  assert.ok(auditRow);
  assert.equal(auditRow.action_type, "ADMIN_RIGHTS_CHANGED");
  assert.equal(auditRow.target_type, "admin");
});

function seedAdmin(sqlite, email, rights) {
  sqlite.prepare("INSERT INTO admins (email, created_at) VALUES (?, ?)").run(email, 1000);
  if (rights !== undefined) {
    sqlite.prepare("INSERT INTO admin_rights (email, rights, updated_at) VALUES (?, ?, ?)")
      .run(email, JSON.stringify(rights), 1000);
  }
  return { uid: email, email, isAdmin: true };
}

function jsonRequest(method, body) {
  return { method, headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) };
}

test("Admin rights: permission lookup failure cannot reach a protected write", async () => {
  const { sqlite, d1 } = isolatedD1();
  const helper = seedAdmin(sqlite, "helper@eve.app", ["manage_exams"]);
  let writes = 0;
  const unavailable = {
    ...d1,
    prepare(sql) {
      if (sql.includes("admin_rights")) throw new Error("permission store unavailable");
      if (/INSERT|UPDATE|DELETE/.test(sql)) writes++;
      return d1.prepare(sql);
    },
  };
  const res = await createTestApp(unavailable, helper).request("/api/admin/config", jsonRequest("PUT", { maintenanceMode: true }));
  assert.equal(res.status, 403);
  assert.equal(writes, 0);
});

test("Admin rights: missing explicit rights never becomes all rights", async () => {
  const { sqlite, d1 } = isolatedD1();
  const helper = seedAdmin(sqlite, "unconfigured@eve.app");
  const app = createTestApp(d1, helper);
  const res = await app.request("/api/admin/config", jsonRequest("PUT", { maintenanceMode: true }));
  assert.equal(res.status, 403);
  const me = await (await app.request("/api/admin/me")).json();
  assert.deepEqual(me.data.rights, []);
  const ownerApp = createTestApp(d1, { uid: "owner", email: [...HARDCODED_ADMIN_EMAILS][0], isAdmin: true });
  const list = await (await ownerApp.request("/api/admin/admins")).json();
  assert.deepEqual(list.data.find(x => x.email === helper.email).rights, []);
});

test("Admin rights: stale rights without active admin membership cannot authorize writes", async () => {
  const { sqlite, d1 } = isolatedD1();
  const helper = seedAdmin(sqlite, "removed@eve.app", ["manage_exams"]);
  sqlite.prepare("DELETE FROM admins WHERE email = ?").run(helper.email);
  const app = createTestApp(d1, helper);
  const res = await app.request("/api/exams", jsonRequest("POST", {}));
  assert.equal(res.status, 403);
  assert.equal((await app.request("/api/admin/me")).status, 403);
});

for (const [label, bodyRights, expected] of [
  ["omitted", undefined, ["manage_admins_rights", "manage_exams"]],
  ["empty", [], []],
  ["selected", ["manage_exams"], ["manage_exams"]],
]) {
  test(`Admin rights: ${label} creation rights cannot exceed the delegating admin`, async () => {
    const { sqlite, d1 } = isolatedD1();
    const helper = seedAdmin(sqlite, "delegator@eve.app", ["manage_admins_rights", "manage_exams"]);
    const app = createTestApp(d1, helper);
    const body = { email: "new@eve.app", ...(bodyRights === undefined ? {} : { rights: bodyRights }) };
    const res = await app.request("/api/admin/admins", jsonRequest("POST", body));
    assert.equal(res.status, 201);
    assert.deepEqual((await res.json()).data.rights, expected);
    const row = sqlite.prepare("SELECT rights FROM admin_rights WHERE email = ?").get(body.email);
    assert.deepEqual(JSON.parse(row.rights), expected);
    const childApp = createTestApp(d1, { uid: "new", email: body.email, isAdmin: true });
    assert.equal((await childApp.request("/api/admin/config", jsonRequest("PUT", { maintenanceMode: true }))).status, 403);
  });
}

test("Admin rights: owner default creation remains compatible with existing Android clients", async () => {
  const { d1 } = isolatedD1();
  const app = createTestApp(d1, { uid: "owner", email: [...HARDCODED_ADMIN_EMAILS][0], isAdmin: true });
  const res = await app.request("/api/admin/admins", jsonRequest("POST", { email: "new@eve.app" }));
  assert.equal(res.status, 201);
  assert.deepEqual((await res.json()).data.rights, [...ALL_ADMIN_RIGHTS]);
});

test("Admin rights: malformed creation rights never grant implicit privileges", async () => {
  const { sqlite, d1 } = isolatedD1();
  const helper = seedAdmin(sqlite, "delegator@eve.app", ["manage_admins_rights"]);
  const app = createTestApp(d1, helper);
  for (const rights of [null, "manage_exams", {}, [1], ["unknown_permission"]]) {
    const res = await app.request("/api/admin/admins", jsonRequest("POST", { email: "new@eve.app", rights }));
    assert.equal(res.status, 400, JSON.stringify(rights));
    assert.equal(sqlite.prepare("SELECT COUNT(*) AS n FROM admins WHERE email = 'new@eve.app'").get().n, 0);
  }
});

test("Admin rights: lesser admin cannot remove a more privileged admin", async () => {
  const { sqlite, d1 } = isolatedD1();
  const helper = seedAdmin(sqlite, "helper@eve.app", ["manage_admins_rights"]);
  seedAdmin(sqlite, "senior@eve.app", ["manage_admins_rights", "manage_users"]);
  const res = await createTestApp(d1, helper).request("/api/admin/admins/senior@eve.app", { method: "DELETE" });
  assert.equal(res.status, 403);
  assert.ok(sqlite.prepare("SELECT email FROM admins WHERE email = 'senior@eve.app'").get());
  assert.ok(sqlite.prepare("SELECT email FROM admin_rights WHERE email = 'senior@eve.app'").get());
  assert.equal(sqlite.prepare("SELECT COUNT(*) AS n FROM admin_audit_log WHERE action_type = 'ADMIN_REMOVED'").get().n, 0);
});

test("Admin rights: verified admin may remove a lesser privileged admin", async () => {
  const { sqlite, d1 } = isolatedD1();
  const helper = seedAdmin(sqlite, "helper@eve.app", ["manage_admins_rights", "manage_exams"]);
  seedAdmin(sqlite, "junior@eve.app", ["manage_exams"]);
  const res = await createTestApp(d1, helper).request("/api/admin/admins/junior@eve.app", { method: "DELETE" });
  assert.equal(res.status, 200);
  assert.equal(sqlite.prepare("SELECT email FROM admins WHERE email = 'junior@eve.app'").get(), undefined);
  assert.equal(sqlite.prepare("SELECT email FROM admin_rights WHERE email = 'junior@eve.app'").get(), undefined);
});


test("built-in owner authority requires active D1 membership", async () => {
  const { sqlite, d1 } = isolatedD1();
  const email = [...HARDCODED_ADMIN_EMAILS][0];
  const app = createTestApp(d1, { uid: "owner", email, isAdmin: true });
  assert.equal(await isUserAdmin(d1, email), true);
  sqlite.prepare("DELETE FROM admins WHERE email = ?").run(email);
  assert.equal(await isUserAdmin(d1, email), false);
  assert.equal((await app.request("/api/admin/me")).status, 403);
  assert.equal((await app.request("/api/exams", jsonRequest("POST", {}))).status, 403);
  assert.ok(sqlite.prepare("SELECT email FROM admin_rights WHERE email = ?").get(email));
});

test("owner membership migration is idempotent and excludes forged owner titles", async () => {
  const { sqlite } = isolatedD1();
  sqlite.prepare("INSERT INTO admin_rights (email, rights, custom_title, updated_at) VALUES (?, ?, ?, ?)")
    .run("forged@example.test", "[]", "Owner", 1);
  const migration = readFileSync(new URL("../migrations/0027_owner_admin_membership.sql", import.meta.url), "utf8");
  const before = sqlite.prepare("SELECT COUNT(*) AS n FROM admins").get().n;
  sqlite.exec(migration); sqlite.exec(migration);
  assert.equal(sqlite.prepare("SELECT COUNT(*) AS n FROM admins").get().n, before);
  assert.equal(sqlite.prepare("SELECT email FROM admins WHERE email = 'forged@example.test'").get(), undefined);
});
