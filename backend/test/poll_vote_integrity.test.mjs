import test from "node:test";
import assert from "node:assert/strict";
import { Hono } from "hono";
import { pollRoutes } from "../src/routes/polls.ts";
import { isolatedD1 } from "./support/sqliteD1.mjs";

function fixture() {
  const { sqlite, d1 } = isolatedD1();
  const app = new Hono();
  app.use("*", async (c, next) => {
    c.env = { DB: d1 };
    c.set("user", { uid: "student-A", email: "student@example.test", isAdmin: false });
    await next();
  });
  app.route("/api/polls", pollRoutes);
  const addPoll = (id, options = ["First", "Second"], active = 1, endsAt = 0) =>
    sqlite.prepare(
      "INSERT INTO polls (id,question,options_json,created_at,ends_at,active,created_by) VALUES (?,?,?,?,?,?,?)"
    ).run(id, "Fixture", typeof options === "string" ? options : JSON.stringify(options), 100, endsAt, active, "admin@example.test");
  const vote = (pollId, payload) => app.request(`/api/polls/${pollId}/vote`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  return { sqlite, d1, app, addPoll, vote };
}

test("student can vote only for a real integer option and cannot coerce null to zero", async () => {
  const f = fixture();
  try {
    f.addPoll("poll");
    const invalid = [null, {}, { optionIndex: null }, { optionIndex: "" },
      { optionIndex: "0" }, { optionIndex: true }, { optionIndex: false },
      { optionIndex: 0.5 }, { optionIndex: -1 }, { optionIndex: 2 },
      { optionIndex: 99 }, { optionIndex: 1e100 }];
    for (const body of invalid) {
      const res = await f.vote("poll", body);
      assert.equal(res.status, 400, `Unexpected status for ${JSON.stringify(body)}`);
    }
    assert.equal(f.sqlite.prepare("SELECT COUNT(*) AS n FROM poll_votes").get().n, 0);
    assert.equal((await f.vote("poll", { optionIndex: 1 })).status, 200);
    const stored = f.sqlite.prepare("SELECT poll_id, user_id, option_index FROM poll_votes").get();
    assert.equal(stored.poll_id, "poll");
    assert.equal(stored.user_id, "student-A");
    assert.equal(stored.option_index, 1);
    assert.equal((await f.vote("poll", { optionIndex: 0 })).status, 409);
    assert.equal(f.sqlite.prepare("SELECT COUNT(*) AS n FROM poll_votes").get().n, 1);
  } finally {
    f.sqlite.close();
  }
});

test("simultaneous vote attempts remain single-write with primary-key conflict reporting", async () => {
  const f = fixture();
  try {
    f.addPoll("poll");
    const statuses = await Promise.all(
      [f.vote("poll", { optionIndex: 0 }), f.vote("poll", { optionIndex: 1 })]
    );
    assert.deepEqual(statuses.map((r) => r.status).sort(), [200, 409]);
    assert.equal(f.sqlite.prepare("SELECT COUNT(*) AS n FROM poll_votes").get().n, 1);
  } finally {
    f.sqlite.close();
  }
});

test("inactive, expired and malformed polls never accept votes", async () => {
  const f = fixture();
  try {
    f.addPoll("inactive", ["A", "B"], 0);
    f.addPoll("expired", ["A", "B"], 1, Date.now() - 1000);
    f.addPoll("invalid-json", "[", 1);
    f.addPoll("empty", [], 1);
    f.addPoll("bad-options", ["", "B"], 1);
    assert.equal((await f.vote("missing", { optionIndex: 0 })).status, 404);
    for (const id of ["inactive", "expired"]) {
      assert.equal((await f.vote(id, { optionIndex: 0 })).status, 400);
    }
    for (const id of ["invalid-json", "empty", "bad-options"]) {
      assert.equal((await f.vote(id, { optionIndex: 0 })).status, 409);
    }
    assert.equal(f.sqlite.prepare("SELECT COUNT(*) AS n FROM poll_votes").get().n, 0);
  } finally {
    f.sqlite.close();
  }
});

test("existing out-of-range legacy vote stays on disk but never creates an invalid result option", async () => {
  const f = fixture();
  try {
    f.addPoll("poll");
    f.sqlite.prepare(
      "INSERT INTO poll_votes (poll_id,user_id,option_index,voted_at) VALUES (?,?,?,?)"
    ).run("poll", "legacy-user", 99, 100);
    const response = await f.app.request("/api/polls");
    assert.equal(response.status, 200);
    const body = await response.json();
    const row = body.data.find((p) => p.id === "poll");
    assert.deepEqual(row.voteCounts, { "0": 0, "1": 0 });
    assert.equal(f.sqlite.prepare("SELECT COUNT(*) AS n FROM poll_votes").get().n, 1);
  } finally {
    f.sqlite.close();
  }
});

