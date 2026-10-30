import test from "node:test";
import assert from "node:assert/strict";
import { DatabaseSync } from "node:sqlite";
import { readFileSync, readdirSync } from "node:fs";
import { Hono } from "hono";
import { authRoutes, validateUsername, deriveDefaultUsername } from "../src/routes/auth.ts";

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
              }
            };
          }
        };
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

function createTestApp(d1, user) {
  const app = new Hono();
  app.use("*", async (c, next) => {
    c.env = { DB: d1 };
    c.set("user", user);
    await next();
  });
  app.route("/api/users", authRoutes);
  return app;
}

test("validateUsername enforces Telegram-exact rules", () => {
  // Valid
  assert.equal(validateUsername("ev_wk96r9").valid, true);
  assert.equal(validateUsername("student_1").valid, true);
  assert.equal(validateUsername("alice").valid, true);
  assert.equal(validateUsername("a".repeat(32)).valid, true);

  // Short
  const shortRes = validateUsername("user");
  assert.equal(shortRes.valid, false);
  assert.equal(shortRes.error, "A username must have at least 5 characters.");

  // Long
  const longRes = validateUsername("a".repeat(33));
  assert.equal(longRes.valid, false);
  assert.equal(longRes.error, "The username must not exceed 32 characters.");

  // Starts with number
  const numRes = validateUsername("123user");
  assert.equal(numRes.valid, false);
  assert.equal(numRes.error, "Sorry, a username can't start with a number.");

  // Underscore at start or end
  const startRes = validateUsername("_student");
  assert.equal(startRes.valid, false);
  assert.equal(startRes.error, "Sorry, this username is invalid.");

  const endRes = validateUsername("student_");
  assert.equal(endRes.valid, false);
  assert.equal(endRes.error, "Sorry, this username is invalid.");

  // Invalid characters
  assert.equal(validateUsername("user-name").valid, false);
  assert.equal(validateUsername("user.name").valid, false);
  assert.equal(validateUsername("user@eve").valid, false);
  assert.equal(validateUsername("user name").valid, false);
});

test("deriveDefaultUsername converts Eve ID to lowercase underscore format", () => {
  assert.equal(deriveDefaultUsername("EV-WK96R9"), "ev_wk96r9");
  assert.equal(deriveDefaultUsername("EV-123456"), "ev_123456");
  assert.equal(deriveDefaultUsername("EV-ABC123"), "ev_abc123");
});

test("GET /api/users/profile lazily assigns default username derived from Eve ID", async () => {
  const { d1 } = isolatedD1();
  const user = { uid: "user-1", email: "student1@example.com", displayName: "Student One" };
  const app = createTestApp(d1, user);

  const res = await app.request("/api/users/profile");
  assert.equal(res.status, 200);
  const body = await res.json();
  assert.equal(body.success, true);
  assert.ok(body.data.eveId);
  assert.ok(body.data.username);
  assert.equal(body.data.username.startsWith("ev_"), true);
  assert.equal(body.data.phone, "");
});

test("GET /api/users/username/check handles available, taken, current, and invalid", async () => {
  const { sqlite, d1 } = isolatedD1();
  const user1 = { uid: "user-1", email: "user1@example.com", displayName: "User 1" };
  const app1 = createTestApp(d1, user1);

  // Seed user2 with username "taken_user"
  sqlite.prepare("INSERT INTO users (id, email, display_name, username, created_at, last_active) VALUES (?, ?, ?, ?, 1, 1)")
    .run("user-2", "user2@example.com", "User 2", "taken_user");

  // Invalid
  const invRes = await app1.request("/api/users/username/check?u=abc");
  const invBody = await invRes.json();
  assert.equal(invBody.data.available, false);
  assert.equal(invBody.data.code, "invalid");

  // Taken (case-insensitive)
  const takenRes = await app1.request("/api/users/username/check?u=TAKEN_USER");
  const takenBody = await takenRes.json();
  assert.equal(takenBody.data.available, false);
  assert.equal(takenBody.data.code, "taken");

  // Available
  const availRes = await app1.request("/api/users/username/check?u=awesome_user");
  const availBody = await availRes.json();
  assert.equal(availBody.data.available, true);
  assert.equal(availBody.data.code, "available");

  // Current user's own username
  sqlite.prepare("INSERT INTO users (id, email, display_name, username, created_at, last_active) VALUES (?, ?, ?, ?, 1, 1)")
    .run("user-1", "user1@example.com", "User 1", "my_own_name");
  const currentRes = await app1.request("/api/users/username/check?u=@my_own_name");
  const currentBody = await currentRes.json();
  assert.equal(currentBody.data.available, true);
  assert.equal(currentBody.data.code, "current");
});

test("PUT /api/users/profile updates username and phone with validation and uniqueness", async () => {
  const { sqlite, d1 } = isolatedD1();
  const user = { uid: "user-1", email: "user1@example.com", displayName: "User 1" };
  const app = createTestApp(d1, user);

  // Insert user 2
  sqlite.prepare("INSERT INTO users (id, email, display_name, username, created_at, last_active) VALUES (?, ?, ?, ?, 1, 1)")
    .run("user-2", "user2@example.com", "User 2", "existing_nick");

  // Conflict on existing username
  const conflictRes = await app.request("/api/users/profile", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      displayName: "User One",
      username: "existing_nick"
    })
  });
  assert.equal(conflictRes.status, 409);

  // Invalid username
  const badUserRes = await app.request("/api/users/profile", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      displayName: "User One",
      username: "123bad"
    })
  });
  assert.equal(badUserRes.status, 400);

  // Invalid phone
  const badPhoneRes = await app.request("/api/users/profile", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      displayName: "User One",
      phone: "invalid-phone"
    })
  });
  assert.equal(badPhoneRes.status, 400);

  // Valid update
  const successRes = await app.request("/api/users/profile", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      displayName: "Updated Name",
      username: "unique_nick_99",
      phone: "+91 98765 43210",
      dob: "15/03/2004",
      category: "OBC"
    })
  });
  assert.equal(successRes.status, 200);

  // Verify get profile returns updated info
  const profRes = await app.request("/api/users/profile");
  const profBody = await profRes.json();
  assert.equal(profBody.data.displayName, "Updated Name");
  assert.equal(profBody.data.username, "unique_nick_99");
  assert.equal(profBody.data.phone, "+91 98765 43210");
  assert.equal(profBody.data.dob, "15/03/2004");
  assert.equal(profBody.data.category, "OBC");
});
