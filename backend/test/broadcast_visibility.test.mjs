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
