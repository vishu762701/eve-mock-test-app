import test from "node:test";
import assert from "node:assert/strict";
import { DatabaseSync } from "node:sqlite";
import { readFileSync, readdirSync } from "node:fs";
import { Hono } from "hono";
import { authRoutes } from "../src/routes/auth.ts";

function isolatedD1() {
  const sqlite = new DatabaseSync(":memory:");
  sqlite.exec("PRAGMA foreign_keys = ON");
  for (const name of readdirSync(new URL("../migrations/", import.meta.url)).filter(x => x.endsWith(".sql")).sort()) {
    sqlite.exec(readFileSync(new URL("../migrations/" + name, import.meta.url), "utf8"));
  }
  return {
    sqlite,
    d1: {
      prepare(query) {
        return { bind(...values) {
          return { async run() { sqlite.prepare(query).run(...values); } };
        } };
      },
      async batch(statements) {
        sqlite.exec("BEGIN");
        try {
          for (const statement of statements) await statement.run();
          sqlite.exec("COMMIT");
        } catch (error) {
          sqlite.exec("ROLLBACK");
          throw error;
        }
      }
    }
  };
}

test("account deletion removes live owner state but preserves other users and financial records", async () => {
  const { sqlite, d1 } = isolatedD1();
  const people = ["deleted-user", "other-user"];
  for (const uid of people) {
    sqlite.prepare("INSERT INTO users (id,email,display_name,created_at,last_active) VALUES (?,?,?,1,1)")
      .run(uid, uid + "@example.test", uid);
    sqlite.prepare("INSERT INTO attempt_sessions (id,user_id,exam_key,started_at,time_limit_seconds) VALUES (?,?,'exam',1,60)")
      .run("session-" + uid, uid);
    sqlite.prepare("INSERT INTO admin_analytics_exam_users (id,exam_id,user_id,created_at) VALUES (?,'exam',?,1)")
      .run("exam_" + uid, uid);
    sqlite.prepare("INSERT INTO premium_entitlements (user_id,activated_at,created_at,updated_at) VALUES (?,1,1,1)")
      .run(uid);
    sqlite.prepare("INSERT INTO premium_orders (id,user_id,plan_name,amount,payment_method,created_at,expires_at) VALUES (?,?,'Premium',99,'test',1,2)")
      .run("order-" + uid, uid);
    sqlite.prepare("INSERT INTO rate_limits (key,count,reset_at) VALUES (?,1,3)")
      .run("uid:" + uid);
  }
  const app = new Hono();
  app.use("*", async (c,next) => {
    c.set("user", { uid: "deleted-user", email: "deleted-user@example.test", isAdmin: false });
    await next();
  });
  app.route("/api/users", authRoutes);
  const response = await app.fetch(new Request("https://example.test/api/users/account", { method: "DELETE" }), { DB: d1 }, {});
  assert.equal(response.status, 200);
  assert.equal((await response.json()).success, true);
  for (const table of ["users", "attempt_sessions", "admin_analytics_exam_users", "premium_entitlements"]) {
    const column = table === "users" ? "id" : "user_id";
    assert.equal(sqlite.prepare("SELECT COUNT(*) AS n FROM " + table + " WHERE " + column + " = ?").get("deleted-user").n, 0, table);
    assert.equal(sqlite.prepare("SELECT COUNT(*) AS n FROM " + table + " WHERE " + column + " = ?").get("other-user").n, 1, table);
  }
  assert.equal(sqlite.prepare("SELECT COUNT(*) AS n FROM rate_limits WHERE key=?").get("uid:deleted-user").n, 0);
  assert.equal(sqlite.prepare("SELECT COUNT(*) AS n FROM rate_limits WHERE key=?").get("uid:other-user").n, 1);
  assert.equal(sqlite.prepare("SELECT COUNT(*) AS n FROM premium_orders WHERE user_id=?").get("deleted-user").n, 1);
  sqlite.close();
});
