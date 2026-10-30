import test from "node:test";
import assert from "node:assert/strict";
import { DatabaseSync } from "node:sqlite";
import { readFileSync, readdirSync } from "node:fs";
import { Hono } from "hono";
import { adminRoutes } from "../src/routes/admin.ts";
import {
  getUtcYearMonth,
  getUtcMonthDetails,
  trackGeminiCall,
  getMonthlyApiUsage,
} from "../src/util/usageTracker.ts";
import {
  validateReformatInput,
  buildReformatPrompt,
  reformatExplanationWithAi,
} from "../src/ai/explanationReformat.ts";

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
    c.env = {
      DB: d1,
      GEMINI_API_KEY: "test-gemini-key",
      GEMINI_MODEL: "gemini-3.5-flash-lite",
    };
    if (user) {
      c.set("user", user);
    }
    await next();
  });
  app.route("/api/admin", adminRoutes);
  return app;
}

test("API Usage: UTC month boundary calculation and validation", () => {
  // Test specific dates
  const d1 = new Date(Date.UTC(2026, 9, 15, 12, 0, 0)); // Oct 15, 2026
  assert.equal(getUtcYearMonth(d1), "2026-10");

  const dMonthEdge = new Date(Date.UTC(2026, 9, 31, 23, 59, 59, 999));
  assert.equal(getUtcYearMonth(dMonthEdge), "2026-10");

  const dNextMonth = new Date(Date.UTC(2026, 10, 1, 0, 0, 0, 0));
  assert.equal(getUtcYearMonth(dNextMonth), "2026-11");

  // Test getUtcMonthDetails
  const octDetails = getUtcMonthDetails("2026-10");
  assert.equal(octDetails.displayName, "October 2026");
  assert.equal(octDetails.startMs, Date.UTC(2026, 9, 1, 0, 0, 0, 0));
  assert.equal(octDetails.endMs, Date.UTC(2026, 10, 1, 0, 0, 0, 0));

  // Invalid formats reject cleanly
  assert.throws(() => getUtcMonthDetails("2026-13"), /Invalid yearMonth format/);
  assert.throws(() => getUtcMonthDetails("invalid"), /Invalid yearMonth format/);
  assert.throws(() => getUtcMonthDetails("2026/10"), /Invalid yearMonth format/);
});

test("API Usage: trackGeminiCall atomic tracking in D1", async () => {
  const { d1, sqlite } = isolatedD1();
  const testTimeOct = Date.UTC(2026, 9, 10, 14, 0, 0);

  // First call: successful
  await trackGeminiCall(d1, true, testTimeOct);

  let row = sqlite.prepare("SELECT * FROM api_usage_monthly WHERE year_month = '2026-10'").get();
  assert.equal(row.gemini_calls, 1);
  assert.equal(row.gemini_successful_calls, 1);
  assert.equal(row.gemini_failed_calls, 0);
  assert.equal(row.first_recorded_at, testTimeOct);
  assert.equal(row.last_updated_at, testTimeOct);

  // Second call: failed, 1 hour later
  const testTimeOctLater = testTimeOct + 3600000;
  await trackGeminiCall(d1, false, testTimeOctLater);

  row = sqlite.prepare("SELECT * FROM api_usage_monthly WHERE year_month = '2026-10'").get();
  assert.equal(row.gemini_calls, 2);
  assert.equal(row.gemini_successful_calls, 1);
  assert.equal(row.gemini_failed_calls, 1);
  assert.equal(row.first_recorded_at, testTimeOct); // First recorded timestamp untouched
  assert.equal(row.last_updated_at, testTimeOctLater);

  // Call in next month (Nov 2026)
  const testTimeNov = Date.UTC(2026, 10, 5, 10, 0, 0);
  await trackGeminiCall(d1, true, testTimeNov);

  const novRow = sqlite.prepare("SELECT * FROM api_usage_monthly WHERE year_month = '2026-11'").get();
  assert.equal(novRow.gemini_calls, 1);
  assert.equal(novRow.gemini_successful_calls, 1);
  assert.equal(novRow.gemini_failed_calls, 0);
});

test("API Usage: getMonthlyApiUsage counts real attempts and audit documents", async () => {
  const { d1, sqlite } = isolatedD1();
  const octStart = Date.UTC(2026, 9, 1, 0, 0, 0, 0);
  const octMid = Date.UTC(2026, 9, 15, 12, 0, 0);
  const sepMid = Date.UTC(2026, 8, 15, 12, 0, 0); // Previous month

  // Seed attempts: 2 in Oct, 1 in Sep
  sqlite.prepare("INSERT INTO attempts (id, user_id, exam_id, exam_name, score, total, correct, wrong, unattempted, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
    .run("att-1", "user-1", "exam-1", "Math", 80, 100, 80, 20, 0, octMid);
  sqlite.prepare("INSERT INTO attempts (id, user_id, exam_id, exam_name, score, total, correct, wrong, unattempted, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
    .run("att-2", "user-2", "exam-1", "Math", 90, 100, 90, 10, 0, octMid + 1000);
  sqlite.prepare("INSERT INTO attempts (id, user_id, exam_id, exam_name, score, total, correct, wrong, unattempted, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
    .run("att-old", "user-1", "exam-1", "Math", 70, 100, 70, 30, 0, sepMid);

  // Seed admin_audit_log creation events: 2 in Oct, 1 update in Oct, 1 create in Sep
  sqlite.prepare("INSERT INTO admin_audit_log (id, timestamp, admin_email, action_type, description) VALUES (?, ?, ?, ?, ?)")
    .run("log-1", octMid, "admin@eve.local", "EXAM_CREATED", "Created exam");
  sqlite.prepare("INSERT INTO admin_audit_log (id, timestamp, admin_email, action_type, description) VALUES (?, ?, ?, ?, ?)")
    .run("log-2", octMid + 500, "admin@eve.local", "BROADCAST_SENT", "Sent notice");
  sqlite.prepare("INSERT INTO admin_audit_log (id, timestamp, admin_email, action_type, description) VALUES (?, ?, ?, ?, ?)")
    .run("log-3", octMid + 1000, "admin@eve.local", "CONFIG_UPDATED", "Updated config"); // NOT creation
  sqlite.prepare("INSERT INTO admin_audit_log (id, timestamp, admin_email, action_type, description) VALUES (?, ?, ?, ?, ?)")
    .run("log-old", sepMid, "admin@eve.local", "EXAM_CREATED", "Old exam");

  // Seed feedback_posts: 1 in Oct
  sqlite.prepare("INSERT INTO feedback_posts (id, title, message, author_id, author_email, timestamp) VALUES (?, ?, ?, ?, ?, ?)")
    .run("fb-1", "Help", "Issue with test", "u1", "u1@test.com", octMid + 200);

  // Track Gemini calls in Oct
  await trackGeminiCall(d1, true, octMid);
  await trackGeminiCall(d1, true, octMid + 100);
  await trackGeminiCall(d1, false, octMid + 200);

  const usage = await getMonthlyApiUsage(d1, "2026-10");
  assert.equal(usage.yearMonth, "2026-10");
  assert.equal(usage.monthDisplayName, "October 2026");
  assert.equal(usage.testSubmissions, 2); // att-1 and att-2
  assert.equal(usage.documentsCreated, 3); // log-1 (EXAM_CREATED) + log-2 (BROADCAST_SENT) + fb-1 (feedback_posts)
  assert.equal(usage.geminiCalls, 3);
  assert.equal(usage.geminiSuccessfulCalls, 2);
  assert.equal(usage.geminiFailedCalls, 1);
  assert.ok(usage.countedSince.startsWith("2026-10"));

  // Check query for historical month before tracking began
  const historical = await getMonthlyApiUsage(d1, "2024-01");
  assert.equal(historical.testSubmissions, 0);
  assert.equal(historical.geminiCalls, 0);
  assert.ok(historical.countedSince.includes("Not tracked"));
});

test("Route GET /api/admin/api-usage: guards and response validation", async () => {
  const { d1 } = isolatedD1();
  const adminUser = { uid: "admin1", email: "own.keni@gmail.com", email_verified: true, role: "admin", isAdmin: true };
  const studentUser = { uid: "student1", email: "student@test.com", email_verified: true, role: "student", isAdmin: false };

  const appAdmin = createAdminApp(d1, adminUser);
  const appStudent = createAdminApp(d1, studentUser);
  const appAnon = createAdminApp(d1, null);

  // 1. Anon rejected (403)
  const resAnon = await appAnon.request("/api/admin/api-usage");
  assert.equal(resAnon.status, 403);

  // 2. Student rejected (403)
  const resStudent = await appStudent.request("/api/admin/api-usage");
  assert.equal(resStudent.status, 403);

  // 3. Admin: invalid month param format returns 400
  const resBad = await appAdmin.request("/api/admin/api-usage?month=202610");
  assert.equal(resBad.status, 400);

  // 4. Admin: valid call returns 200 and schema
  const resOk = await appAdmin.request("/api/admin/api-usage?month=2026-10");
  assert.equal(resOk.status, 200);
  const json = await resOk.json();
  assert.equal(json.success, true);
  assert.equal(json.data.yearMonth, "2026-10");
  assert.equal(typeof json.data.geminiCalls, "number");
  assert.equal(typeof json.data.documentsCreated, "number");
  assert.equal(typeof json.data.testSubmissions, "number");
});

test("Route POST /api/admin/questions/reformat-explanation: validation and formatting", async () => {
  const { d1 } = isolatedD1();
  const adminUser = { uid: "admin1", email: "own.keni@gmail.com", email_verified: true, role: "admin", isAdmin: true };
  const appAdmin = createAdminApp(d1, adminUser);

  // 1. Empty body returns 400
  const resEmpty = await appAdmin.request("/api/admin/questions/reformat-explanation", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({}),
  });
  assert.equal(resEmpty.status, 400);

  // 2. Overlong questionText returns 400
  const resOverlong = await appAdmin.request("/api/admin/questions/reformat-explanation", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      questionText: "a".repeat(1001),
      explanation: "valid",
    }),
  });
  assert.equal(resOverlong.status, 400);

  // 3. Prompt builder format verification
  const prompt = buildReformatPrompt({
    questionText: "What is capital of India?",
    optionA: "Mumbai",
    optionB: "New Delhi",
    optionC: "Kolkata",
    optionD: "Chennai",
    correctAnswer: "B",
    explanation: "New Delhi became capital in 1911.",
  });
  assert.ok(prompt.includes("Key Points"));
  assert.ok(prompt.includes("Correct Answer: Option B"));
  assert.ok(prompt.includes("New Delhi became capital in 1911."));

  // 4. Successful mock execution with reformatExplanationWithAi
  const mockFetchSuccess = async () => {
    return new Response(
      JSON.stringify({
        candidates: [
          {
            content: {
              parts: [
                {
                  text: "**The correct answer is Option B.**\n\nKey Points:\n• **Capital:** New Delhi is the capital of India.",
                },
              ],
            },
          },
        ],
      }),
      { status: 200, headers: { "Content-Type": "application/json" } }
    );
  };

  const env = {
    DB: d1,
    GEMINI_API_KEY: "mock-key",
  };
  const reformatted = await reformatExplanationWithAi(
    env,
    {
      questionText: "What is capital of India?",
      optionA: "Mumbai",
      optionB: "New Delhi",
      optionC: "Kolkata",
      optionD: "Chennai",
      correctAnswer: "B",
      explanation: "New Delhi",
    },
    mockFetchSuccess
  );

  assert.ok(reformatted.includes("**The correct answer is Option B.**"));
  assert.ok(reformatted.includes("Key Points:"));

  // Verify that successful reformat recorded a Gemini call in api_usage_monthly
  const ym = getUtcYearMonth();
  const usageRow = d1.prepare("SELECT * FROM api_usage_monthly WHERE year_month = ?").bind(ym).first();
  const resolvedUsage = await usageRow;
  assert.equal(resolvedUsage.gemini_calls, 1);
  assert.equal(resolvedUsage.gemini_successful_calls, 1);
});
