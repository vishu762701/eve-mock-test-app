import test from "node:test";
import assert from "node:assert/strict";
import { DatabaseSync } from "node:sqlite";
import { readFileSync, readdirSync } from "node:fs";
import { Hono } from "hono";
import { authRoutes, validateUsername, deriveDefaultUsername } from "../src/routes/auth.ts";
import { feedbackRoutes } from "../src/routes/feedback.ts";

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
  app.route("/api/feedback", feedbackRoutes);
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

const profileUser = { uid: "profile-owner", email: "actual@example.test", displayName: "Original Name" };
const originalProfile = { display_name: "Original Name", dob: "15/03/2004", category: "OBC", username: "original_user", phone: "+91 9876543210" };

function seedProfile(sqlite) {
  sqlite.prepare("INSERT INTO users (id, email, display_name, dob, category, username, phone, created_at, last_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")
    .run(profileUser.uid, profileUser.email, ...Object.values(originalProfile), 1, 1);
}
const profileRequest = (body, method = "PUT") => ({ method, headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) });

for (const [field, column, value] of [
  ["displayName", "display_name", "विशु कुमार"],
  ["username", "username", "new_username"],
  ["phone", "phone", "+91 1234567890"],
  ["dob", "dob", "20/04/2003"],
  ["category", "category", "General"],
]) {
  test(`Android single-field ${field} update preserves all other profile fields`, async () => {
    const { sqlite, d1 } = isolatedD1(); seedProfile(sqlite);
    const app = createTestApp(d1, profileUser);
    const response = await app.request("/api/users/profile", profileRequest({ [field]: value }));
    assert.equal(response.status, 200);
    const actual = sqlite.prepare("SELECT display_name, dob, category, username, phone FROM users WHERE id = ?").get(profileUser.uid);
    assert.deepEqual({ ...actual }, { ...originalProfile, [column]: value });
    const reload = await (await app.request("/api/users/profile")).json();
    assert.equal(reload.data[field], value);
  });
}

test("partial first profile save uses authenticated identity and preserves server-owned fields", async () => {
  const { sqlite, d1 } = isolatedD1();
  const app = createTestApp(d1, profileUser);
  const response = await app.request("/api/users/profile", profileRequest({ phone: "+91 1234567890", email: "forged@example.test", disabled: 1, id: "someone-else" }));
  assert.equal(response.status, 200);
  const row = sqlite.prepare("SELECT * FROM users WHERE id = ?").get(profileUser.uid);
  assert.equal(row.email, profileUser.email);
  assert.equal(row.display_name, profileUser.displayName);
  assert.equal(row.disabled, 0);
  assert.equal(sqlite.prepare("SELECT COUNT(*) AS n FROM users").get().n, 1);
});

test("profile rejects malformed fields and punctuation-only phone without changing stored data", async () => {
  const { sqlite, d1 } = isolatedD1(); seedProfile(sqlite);
  const app = createTestApp(d1, profileUser);
  for (const body of [null, [], {}, { displayName: {} }, { displayName: "x".repeat(101) },
    { displayName: "" }, { phone: "-------" }, { phone: "+1234567890123456" },
    { phone: 1234567 }, { username: null }, { dob: [] }, { category: "x".repeat(101) }]) {
    assert.equal((await app.request("/api/users/profile", profileRequest(body))).status, 400, JSON.stringify(body));
    const row = sqlite.prepare("SELECT display_name, dob, category, username, phone FROM users WHERE id = ?").get(profileUser.uid);
    assert.deepEqual({ ...row }, originalProfile);
  }
});

test("profile may explicitly clear phone and birthday without resetting other fields", async () => {
  const { sqlite, d1 } = isolatedD1(); seedProfile(sqlite);
  const app = createTestApp(d1, profileUser);
  assert.equal((await app.request("/api/users/profile", profileRequest({ phone: "", dob: "" }))).status, 200);
  const row = sqlite.prepare("SELECT display_name, dob, category, username, phone FROM users WHERE id = ?").get(profileUser.uid);
  assert.deepEqual({ ...row }, { ...originalProfile, phone: "", dob: "" });
});

test("profile sync stores token email even when request tries to impersonate another account", async () => {
  const { sqlite, d1 } = isolatedD1(); seedProfile(sqlite);
  const app = createTestApp(d1, profileUser);
  const response = await app.request("/api/users/sync", profileRequest({ email: "forged@example.test", displayName: "विशु" }, "POST"));
  assert.equal(response.status, 200);
  assert.equal(sqlite.prepare("SELECT email FROM users WHERE id = ?").get(profileUser.uid).email, profileUser.email);
});

test("concurrent independent profile edits both survive", async () => {
  const { sqlite, d1 } = isolatedD1(); seedProfile(sqlite);
  const app = createTestApp(d1, profileUser);
  const responses = await Promise.all([
    app.request("/api/users/profile", profileRequest({ displayName: "New Name" })),
    app.request("/api/users/profile", profileRequest({ phone: "+91 1234567890" })),
  ]);
  assert.deepEqual(responses.map(r => r.status), [200, 200]);
  const row = sqlite.prepare("SELECT display_name, dob, category, username, phone FROM users WHERE id = ?").get(profileUser.uid);
  assert.deepEqual({ ...row }, { ...originalProfile, display_name: "New Name", phone: "+91 1234567890" });
});

test("profile sync rejects malformed names before creating user data", async () => {
  const { sqlite, d1 } = isolatedD1();
  const app = createTestApp(d1, profileUser);
  for (const body of [null, [], { displayName: {} }, { displayName: "" }, { displayName: "x".repeat(101) }]) {
    assert.equal((await app.request("/api/users/sync", profileRequest(body, "POST"))).status, 400);
    assert.equal(sqlite.prepare("SELECT COUNT(*) AS n FROM users").get().n, 0);
  }
});

test("private feedback identity uses authenticated email rather than the request body", async () => {
  const { sqlite, d1 } = isolatedD1();
  const response = await createTestApp(d1, profileUser).request("/api/feedback/messages", profileRequest({ message: "Please review this", userEmail: "forged@example.test" }, "POST"));
  assert.equal(response.status, 201);
  const row = sqlite.prepare("SELECT user_id, user_email FROM feedback_messages").get();
  assert.equal(row.user_id, profileUser.uid);
  assert.equal(row.user_email, profileUser.email);
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

test("privacy guard: phone is never returned except on own profile", async () => {
  const { sqlite, d1 } = isolatedD1();
  const caller = { uid: "caller-user", email: "caller@example.com", displayName: "Caller" };
  const app = createTestApp(d1, caller);

  // Seed another user with sensitive phone
  sqlite.prepare("INSERT INTO users (id, email, display_name, username, phone, created_at, last_active) VALUES (?, ?, ?, ?, ?, 1, 1)")
    .run("target-user", "target@example.com", "Target", "target_user", "+91 99999 88888");

  // Own profile returns own phone (or empty if unassigned), never target's phone
  const ownProf = await app.request("/api/users/profile");
  const ownBody = await ownProf.json();
  assert.equal(ownBody.data.phone, "");
  assert.notEqual(ownBody.data.phone, "+91 99999 88888");

  // Checking username availability of target does NOT leak target's phone or PII
  const checkRes = await app.request("/api/users/username/check?u=target_user");
  const checkBody = await checkRes.json();
  assert.equal(checkBody.data.available, false);
  assert.equal(checkBody.data.code, "taken");
  assert.equal(checkBody.data.phone, undefined);
  assert.equal(checkBody.data.email, undefined);
});

test("race condition & unique constraint: handles concurrent username collision", async () => {
  const { sqlite, d1 } = isolatedD1();
  const user1 = { uid: "user-alpha", email: "alpha@example.com", displayName: "Alpha" };
  const user2 = { uid: "user-beta", email: "beta@example.com", displayName: "Beta" };
  const app2 = createTestApp(d1, user2);

  // User 1 claims "racing_star"
  sqlite.prepare("INSERT INTO users (id, email, display_name, username, created_at, last_active) VALUES (?, ?, ?, ?, 1, 1)")
    .run(user1.uid, user1.email, user1.displayName, "racing_star");

  // User 2 races with exact or case-insensitive duplicate "RACING_STAR"
  const raceRes = await app2.request("/api/users/profile", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      displayName: "Beta Racer",
      username: "RACING_STAR"
    })
  });
  assert.equal(raceRes.status, 409);
  const raceBody = await raceRes.json();
  assert.equal(raceBody.success, false);
  assert.equal(raceBody.error, "Sorry, this username is already taken.");
});

test("profile bio save and authenticated readback survive other partial edits", async () => {
  const { sqlite, d1 } = isolatedD1(); seedProfile(sqlite);
  const app = createTestApp(d1, profileUser);
  const bio = "Learning together.\nहिंदी में तैयारी • accessibility matters.";
  const saved = await app.request("/api/users/profile", profileRequest({ bio }));
  assert.equal(saved.status, 200);
  assert.equal(sqlite.prepare("SELECT bio FROM users WHERE id = ?").get(profileUser.uid).bio, bio);
  let readback = await (await app.request("/api/users/profile")).json();
  assert.equal(readback.data.bio, bio);
  assert.equal(readback.data.phone, originalProfile.phone);
  assert.equal((await app.request("/api/users/profile", profileRequest({ username: "other_user" }))).status, 200);
  readback = await (await app.request("/api/users/profile")).json();
  assert.equal(readback.data.bio, bio, "Independent profile updates cannot overwrite bio");
  assert.equal(readback.data.username, "other_user");

  const cleared = await app.request("/api/users/profile", profileRequest({ bio: "" }));
  assert.equal(cleared.status, 200);
  assert.equal((await (await app.request("/api/users/profile")).json()).data.bio, "");
});

test("profile bio rejects unbounded or non-text payloads without mutation", async () => {
  const { sqlite, d1 } = isolatedD1(); seedProfile(sqlite);
  const app = createTestApp(d1, profileUser);
  const before = sqlite.prepare("SELECT bio FROM users WHERE id = ?").get(profileUser.uid).bio;
  for (const value of ["x".repeat(161), null, 12, [], {}, true]) {
    const response = await app.request("/api/users/profile", profileRequest({ bio: value }));
    assert.equal(response.status, 400, String(value));
    assert.equal(sqlite.prepare("SELECT bio FROM users WHERE id = ?").get(profileUser.uid).bio, before);
  }
});

test("only authenticated profile owner can write or read their private bio", async () => {
  const { sqlite, d1 } = isolatedD1(); seedProfile(sqlite);
  const other = { uid: "other-user", email: "other@example.test", displayName: "Other" };
  const own = createTestApp(d1, profileUser);
  const second = createTestApp(d1, other);
  const saved = await own.request("/api/users/profile", profileRequest({ bio: "private profile", id: other.uid, email: other.email }));
  assert.equal(saved.status, 200);
  assert.equal(sqlite.prepare("SELECT bio FROM users WHERE id = ?").get(profileUser.uid).bio, "private profile");
  const otherResponse = await (await second.request("/api/users/profile")).json();
  assert.equal(otherResponse.data.bio, "", "Authenticated owner isolation is mandatory");
  assert.equal(sqlite.prepare("SELECT COUNT(*) AS n FROM users WHERE bio = ?").get("private profile").n, 1);
});

test("new profile has empty bio and migration is idempotent across persisted users", async () => {
  const { sqlite, d1 } = isolatedD1();
  const app = createTestApp(d1, profileUser);
  const initial = await (await app.request("/api/users/profile")).json();
  assert.equal(initial.data.bio, "");
  assert.equal(sqlite.prepare("PRAGMA table_info(users)").all().some(column => column.name === "bio"), true);
});

test("presence lease is bounded, authenticated and multi-device safe", async () => {
  const { sqlite, d1 } = isolatedD1();
  const userA = { uid: "presence-owner", email: "person@example.test", displayName: "Present Person" };
  const userB = { uid: "presence-other", email: "other@example.test", displayName: "Other" };
  const owner = createTestApp(d1, userA);
  const other = createTestApp(d1, userB);
  const session1 = "00000000-0000-4000-8000-000000000001";
  const session2 = "00000000-0000-4000-8000-000000000002";

  let data = await (await owner.request("/api/users/profile")).json();
  assert.equal(data.data.isOnline, false);
  assert.equal((await owner.request("/api/users/presence", profileRequest({ sessionId: session1 }, "POST"))).status, 200);
  assert.equal((await owner.request("/api/users/presence", profileRequest({ sessionId: session2 }, "POST"))).status, 200);
  data = await (await owner.request("/api/users/profile")).json();
  assert.equal(data.data.isOnline, true);
  assert.equal(sqlite.prepare("SELECT COUNT(*) AS n FROM user_presence_sessions WHERE user_id = ?").get(userA.uid).n, 2);

  // A different signed-in identity cannot revoke any session owned by userA.
  assert.equal((await other.request("/api/users/presence", profileRequest({ sessionId: session1 }, "DELETE"))).status, 200);
  data = await (await owner.request("/api/users/profile")).json();
  assert.equal(data.data.isOnline, true);
  assert.equal((await other.request("/api/users/profile").then(r => r.json())).data.isOnline, false);

  assert.equal((await owner.request("/api/users/presence", profileRequest({ sessionId: session1 }, "DELETE"))).status, 200);
  data = await (await owner.request("/api/users/profile")).json();
  assert.equal(data.data.isOnline, true, "Other device remains online");
  assert.equal((await owner.request("/api/users/presence", profileRequest({ sessionId: session2 }, "DELETE"))).status, 200);
  data = await (await owner.request("/api/users/profile")).json();
  assert.equal(data.data.isOnline, false);
});

test("expired or malformed presence never creates misleading online state", async () => {
  const { sqlite, d1 } = isolatedD1();
  const user = { uid: "presence-owner", email: "person@example.test", displayName: "Student" };
  const app = createTestApp(d1, user);
  const session = "00000000-0000-4000-8000-000000000001";
  for (const value of ["bad", 12, null, {}, [], "00000000-0000-4000-8000-000000000001' OR 1=1"]) {
    assert.equal((await app.request("/api/users/presence", profileRequest({ sessionId: value }, "POST"))).status, 400);
  }
  assert.equal(sqlite.prepare("SELECT COUNT(*) AS n FROM user_presence_sessions").get().n, 0);
  assert.equal((await app.request("/api/users/presence", profileRequest({ sessionId: session }, "POST"))).status, 200);
  sqlite.prepare("UPDATE user_presence_sessions SET expires_at = 1 WHERE user_id = ?").run(user.uid);
  assert.equal((await (await app.request("/api/users/profile")).json()).data.isOnline, false);
  assert.equal((await app.request("/api/users/presence", profileRequest({ sessionId: session }, "POST"))).status, 200);
  assert.equal((await (await app.request("/api/users/profile")).json()).data.isOnline, true);
  assert.equal(sqlite.prepare("SELECT expires_at FROM user_presence_sessions WHERE user_id = ?").get(user.uid).expires_at > Date.now(), true);
});

test("Username identity presentation preserves immutable UID and legacy installed-client contracts", async () => {
  const { sqlite, d1 } = isolatedD1(); seedProfile(sqlite);
  const app = createTestApp(d1, profileUser);
  const before = (await (await app.request('/api/users/profile')).json()).data;
  assert.equal((await app.request('/api/users/profile', profileRequest({ username: 'public_username' }))).status, 200);
  const after = (await (await app.request('/api/users/profile')).json()).data;
  assert.equal(after.username, 'public_username');
  assert.equal(after.id, before.id);
  assert.equal(after.eveId, before.eveId);
  for (const field of ['phone', 'dob', 'category', 'email', 'displayName']) assert.equal(after[field], before[field]);
  const other = createTestApp(d1, { uid: 'another-account', email: 'other@example.test', displayName: 'Other' });
  assert.equal((await other.request('/api/users/profile', profileRequest({ username: 'PUBLIC_USERNAME' }))).status, 409);
  assert.equal((await (await app.request('/api/users/profile')).json()).data.id, profileUser.uid);
});
