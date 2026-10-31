import test from "node:test";
import assert from "node:assert/strict";
import { Hono } from "hono";
import { broadcastRoutes } from "../src/routes/broadcasts.ts";
import { HARDCODED_ADMIN_EMAILS } from "../src/auth.ts";
import { isolatedD1 } from "./support/sqliteD1.mjs";

const ownerEmail = [...HARDCODED_ADMIN_EMAILS][0];
// This fixture's owner is a D1 member; isAdmin alone no longer authorizes writes.
function withOwnerMembership(db) {
  return { ...db, prepare(sql) {
    if (sql.includes("FROM admins")) return { bind: () => ({ first: async () => ({ email: ownerEmail }) }) };
    return db.prepare(sql);
  } };
}


const stored = {
  id: "broadcast-1", title: "Exam schedule", message: "New test published",
  sent_at: 1700000000000, sent_by: "private-admin@example.test",
  type: "general", target_category: "All Users"
};

function makeApp(isAdmin) {
  const app = new Hono();
  app.use("*", async (c, next) => {
    c.set("user", { uid: isAdmin ? "admin" : "student", email: isAdmin ? ownerEmail : "user@example.test", isAdmin });
    await next();
  });
  app.route("/api/broadcasts", broadcastRoutes);
  const env = { DB: withOwnerMembership({ prepare: () => ({ bind: () => ({ all: async () => ({ results: [stored] }) }) }) }) };
  return { app, env };
}

for (const isAdmin of [false, true]) {
  test(`broadcast GET ${isAdmin ? "admin can see" : "student cannot see"} sender identity`, async () => {
    const { app, env } = makeApp(isAdmin);
    const res = await app.fetch(new Request("https://example.test/api/broadcasts"), env, {});
    assert.equal(res.status, 200);
    const body = await res.json();
    assert.equal(body.data.length, 1);
    assert.equal(body.data[0].title, stored.title);
    assert.equal(body.data[0].message, stored.message);
    if (isAdmin) assert.equal(body.data[0].sentBy, stored.sent_by);
    else assert.equal(Object.hasOwn(body.data[0], "sentBy"), false);
  });
}

// Resource validation must be enforced even for authenticated admins;
// these isolated tests never touch D1 or send a real notification.
function requestAsAdmin(method, endpoint, body, onPrepare = () => { throw new Error("Unexpected database access"); }) {
  const app = new Hono();
  app.use("*", async (c, next) => {
    c.set("user", { uid: "admin-fixture", email: ownerEmail, isAdmin: true });
    await next();
  });
  app.route("/api/broadcasts", broadcastRoutes);
  const init = { method };
  if (body !== undefined) {
    init.headers = { "Content-Type": "application/json" };
    init.body = JSON.stringify(body);
  }
  return app.fetch(new Request("https://example.test" + endpoint, init),
    { DB: withOwnerMembership({ prepare: onPrepare }) }, {});
}

test("broadcast list rejects malformed limits before reading D1", async () => {
  for (const q of ["0", "-3", "abc", "3.5", "1e2", "99999999999"]) {
    const res = await requestAsAdmin("GET", "/api/broadcasts?limit=" + encodeURIComponent(q));
    assert.equal(res.status, 400, q);
  }
});

test("broadcast limit remains bounded to 100 for positive large values", async () => {
  let bound;
  const res = await requestAsAdmin("GET", "/api/broadcasts?limit=500", undefined, () => ({
    bind: value => { bound = value; return { all: async () => ({ results: [] }) }; }
  }));
  assert.equal(res.status, 200);
  assert.equal(bound, 100);
});

test("broadcast creation rejects empty, overlong and non-string data before D1 writes", async () => {
  const valid = { title: "Title", message: "Message", targetCategory: "All Users" };
  for (const bad of [
    { ...valid, title: "" },
    { ...valid, title: "x".repeat(121) },
    { ...valid, message: "" },
    { ...valid, message: "x".repeat(4001) },
    { ...valid, message: { fake: true } },
    { ...valid, targetCategory: "x".repeat(101) },
    { ...valid, targetCategory: {} },
    { ...valid, type: [] }
  ]) {
    const res = await requestAsAdmin("POST", "/api/broadcasts", bad);
    assert.equal(res.status, 400, JSON.stringify(bad).slice(0,80));
  }
});

test("broadcast bulk deletion rejects invalid batches without D1 calls", async () => {
  for (const ids of [[], ["x", "x"], [""], [null], [4], ["x".repeat(129)], Array.from({ length: 101 }, (_, i) => String(i))]) {
    const res = await requestAsAdmin("POST", "/api/broadcasts/bulk-delete", { ids });
    assert.equal(res.status, 400);
  }
});


// Audience targeting is NOT implemented in D1. This explicit fail-closed policy
// prevents a restricted message from reaching students in a different category.
test("students cannot see pre-existing category-targeted broadcasts", async () => {
  const app = new Hono();
  app.use("*", async (c, next) => { c.set("user", { uid: "student", isAdmin: false }); await next(); });
  app.route("/api/broadcasts", broadcastRoutes);
  const env = { DB: { prepare: () => ({ bind: () => ({ all: async () => ({ results: [
    stored, { ...stored, id: "targeted-old", target_category: "Other Exam", title: "Private category post" }
  ] }) }) }) } };
  const res = await app.fetch(new Request("https://example.test/api/broadcasts"), env, {});
  assert.equal(res.status, 200);
  const json = await res.json();
  assert.deepEqual(json.data.map(x => x.id), ["broadcast-1"]);
});

test("targeted broadcasts fail closed before creating any D1 row", async () => {
  const res = await requestAsAdmin("POST", "/api/broadcasts", {
    title: "Limited group", message: "Restricted", targetCategory: "Exam A"
  });
  assert.equal(res.status, 422);
  const body = await res.json();
  assert.match(body.error, /targeted delivery is unavailable/i);
});

function adminDeletionFixture(changesById = {}) {
  const { sqlite, d1 } = isolatedD1();
  const insert = sqlite.prepare(
    "INSERT INTO notifications (id, title, message, sent_at, sent_by, type, target_category) VALUES (?, ?, ?, ?, ?, ?, ?)"
  );
  for (const [id, changes] of Object.entries(changesById)) {
    if (changes > 0) insert.run(id, "Test", "Broadcast", 1, ownerEmail, "general", "All Users");
  }

  const app = new Hono();
  app.use("*", async (c, next) => {
    c.set("user", { uid: "admin", email: ownerEmail, isAdmin: true });
    await next();
  });
  app.route("/api/broadcasts", broadcastRoutes);
  app.onError(() => new Response("Database operation failed", { status: 500 }));
  return { app, env: { DB: d1 }, sqlite };
}

test("delete ID reports 404 for stale records, never fake success", async () => {
  const { app, env, sqlite } = adminDeletionFixture({ known: 1 });
  try {
    const missing = await app.fetch(new Request("https://example.test/api/broadcasts/missing", { method: "DELETE" }), env, {});
    assert.equal(missing.status, 404);
    assert.equal((await missing.json()).success, false);
    const found = await app.fetch(new Request("https://example.test/api/broadcasts/known", { method: "DELETE" }), env, {});
    assert.equal(found.status, 200);
    assert.equal((await found.json()).success, true);
    assert.equal(sqlite.prepare("SELECT COUNT(*) AS n FROM admin_audit_log WHERE action_type = 'BROADCAST_DELETED'").get().n, 1);
  } finally { sqlite.close(); }
});

test("bulk delete returns real changed row count and rejects all-stale requests", async () => {
  const { app, env, sqlite } = adminDeletionFixture({ yes: 1 });
  try {
    const send = ids => app.fetch(new Request("https://example.test/api/broadcasts/bulk-delete", {
      method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ ids })
    }), env, {});
    const res = await send(["yes", "missing"]);
    assert.equal(res.status, 200);
    const json = await res.json();
    assert.equal(json.success, true);
    assert.equal(json.data.count, 1);
    assert.equal(json.data.requested, 2);
    const stale = await send(["missing"]);
    assert.equal(stale.status, 404);
    assert.equal((await stale.json()).success, false);
    assert.equal(sqlite.prepare("SELECT COUNT(*) AS n FROM admin_audit_log WHERE action_type = 'BROADCAST_BULK_DELETED'").get().n, 1);
  } finally { sqlite.close(); }
});

test("audit table unavailable rolls back broadcast deletion", async () => {
  const { app, env, sqlite } = adminDeletionFixture({ keep: 1 });
  try {
    sqlite.exec("DROP TABLE admin_audit_log");
    const res = await app.fetch(new Request("https://example.test/api/broadcasts/keep", { method: "DELETE" }), env, {});
    assert.equal(res.status, 500);
    assert.equal(sqlite.prepare("SELECT COUNT(*) AS n FROM notifications WHERE id = 'keep'").get().n, 1);
  } finally { sqlite.close(); }
});

test("limited admins cannot read targeted broadcasts or sender identity without send_broadcasts", async () => {
  const { sqlite, d1 } = isolatedD1();
  try {
    const email = "limited-broadcast-admin@example.test";
    sqlite.prepare("INSERT INTO admins (email, created_at) VALUES (?, ?)").run(email, 1);
    sqlite.prepare("INSERT INTO admin_rights (email, rights, updated_at) VALUES (?, ?, ?)")
      .run(email, JSON.stringify(["view_analytics_logs"]), 1);
    const insert = sqlite.prepare("INSERT INTO notifications (id, title, message, sent_at, sent_by, target_category) VALUES (?, ?, ?, ?, ?, ?)");
    insert.run("all", "All", "For students", 1, ownerEmail, "All Users");
    insert.run("restricted", "Other audience", "Private", 2, ownerEmail, "Other");
    const app = new Hono();
    app.use("*", async (c, next) => { c.set("user", { uid: email, email, isAdmin: true }); await next(); });
    app.route("/api/broadcasts", broadcastRoutes);
    const res = await app.request("http://localhost/api/broadcasts", {}, { DB: d1 });
    assert.equal(res.status, 200);
    const list = (await res.json()).data;
    assert.deepEqual(list.map(x => x.id), ["all"]);
    assert.equal(Object.hasOwn(list[0], "sentBy"), false);
  } finally { sqlite.close(); }
});
