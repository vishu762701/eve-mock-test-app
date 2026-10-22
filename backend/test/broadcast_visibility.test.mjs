import test from "node:test";
import assert from "node:assert/strict";
import { Hono } from "hono";
import { broadcastRoutes } from "../src/routes/broadcasts.ts";

const stored = {
  id: "broadcast-1", title: "Exam schedule", message: "New test published",
  sent_at: 1700000000000, sent_by: "private-admin@example.test",
  type: "general", target_category: "All Users"
};

function makeApp(isAdmin) {
  const app = new Hono();
  app.use("*", async (c, next) => {
    c.set("user", { uid: isAdmin ? "admin" : "student", email: "user@example.test", isAdmin });
    await next();
  });
  app.route("/api/broadcasts", broadcastRoutes);
  const env = { DB: { prepare: () => ({ bind: () => ({ all: async () => ({ results: [stored] }) }) }) } };
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
    c.set("user", { uid: "admin-fixture", email: "admin@example.test", isAdmin: true });
    await next();
  });
  app.route("/api/broadcasts", broadcastRoutes);
  const init = { method };
  if (body !== undefined) {
    init.headers = { "Content-Type": "application/json" };
    init.body = JSON.stringify(body);
  }
  return app.fetch(new Request("https://example.test" + endpoint, init),
    { DB: { prepare: onPrepare } }, {});
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

