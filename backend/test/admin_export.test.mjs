import test from "node:test";
import assert from "node:assert/strict";
import { DatabaseSync } from "node:sqlite";
import { readFileSync, readdirSync } from "node:fs";
import { Hono } from "hono";
import { adminRoutes } from "../src/routes/admin.ts";

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
      FIREBASE_PROJECT_ID: "test-project",
    };
    if (user) {
      c.set("user", user);
    }
    await next();
  });
  app.route("/api/admin", adminRoutes);
  return app;
}

test("Admin Export: Unauthorized and non-admin requests are rejected", async () => {
  const { d1 } = isolatedD1();
  const student = { uid: "s1", email: "student@test.com", isAdmin: false };
  const admin = { uid: "a1", email: "own.keni@gmail.com", isAdmin: true };

  const appAnon = createAdminApp(d1, null);
  const appStudent = createAdminApp(d1, student);
  const appAdmin = createAdminApp(d1, admin);

  const resAnon = await appAnon.request("/api/admin/export/exams");
  assert.equal(resAnon.status, 403);

  const resStudent = await appStudent.request("/api/admin/export/exams");
  assert.equal(resStudent.status, 403);

  const resAdmin = await appAdmin.request("/api/admin/export/exams");
  assert.equal(resAdmin.status, 200);
});

test("Admin Export: Validates offset and limit boundaries", async () => {
  const { d1 } = isolatedD1();
  const admin = { uid: "a1", email: "own.keni@gmail.com", isAdmin: true };
  const app = createAdminApp(d1, admin);

  const resBadOffset = await app.request("/api/admin/export/exams?offset=-1");
  assert.equal(resBadOffset.status, 400);

  const resAlphaOffset = await app.request("/api/admin/export/exams?offset=abc");
  assert.equal(resAlphaOffset.status, 400);

  const resZeroLimit = await app.request("/api/admin/export/exams?limit=0");
  assert.equal(resZeroLimit.status, 400);

  const resBadLimit = await app.request("/api/admin/export/exams?limit=invalid");
  assert.equal(resBadLimit.status, 400);
});

test("Admin Export: Output schema matches contract and contains ZERO student PII", async () => {
  const { d1, sqlite } = isolatedD1();
  const admin = { uid: "a1", email: "own.keni@gmail.com", isAdmin: true };
  const app = createAdminApp(d1, admin);

  // Seed sensitive student data that must NEVER appear in export
  sqlite.prepare("INSERT INTO users (id, email, display_name, phone, created_at, last_active) VALUES (?, ?, ?, ?, ?, ?)")
    .run("student_secret_uid", "private.student@example.com", "Private Student", "+919999999999", 1000, 2000);

  sqlite.prepare("INSERT INTO attempts (id, user_id, exam_id, exam_name, score, total, correct, wrong, unattempted, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
    .run("att_secret_id", "student_secret_uid", "exam_1", "Test Exam", 95, 100, 95, 5, 0, 3000);

  // Seed an exam and questions
  sqlite.prepare("INSERT INTO exams (id, exam_name, category, time_limit_minutes, auto_generation_enabled, auto_gen_time, generation_prompt, syllabus_file_name, syllabus_url) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")
    .run("exam_1", "General Knowledge 2026", "General Studies", 45, 1, "08:00", "Current affairs prompt", "gk_syllabus.pdf", "https://storage.eve.app/gk.pdf");

  sqlite.prepare("INSERT INTO questions (id, exam_id, question_text, option_a, option_b, option_c, option_d, correct_answer, explanation, topic) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
    .run("q_1", "exam_1", "What is the boiling point of water at 1 atm?", "90°C", "100°C", "110°C", "120°C", "B", "Water boils at 100 degrees Celsius at sea level.", "Physics");

  sqlite.prepare("INSERT INTO questions (id, exam_id, question_text, option_a, option_b, option_c, option_d, correct_answer, explanation, topic) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
    .run("q_2", "exam_1", "What is the speed of light?", "3x10^8 m/s", "3x10^6 m/s", "3x10^5 m/s", "3x10^7 m/s", "A", "Speed of light in vacuum is approx 300,000 km/s.", "Physics");

  const res = await app.request("/api/admin/export/exams");
  assert.equal(res.status, 200);

  const json = await res.json();
  assert.equal(json.success, true);
  assert.equal(json.data.exams.length, 1);

  const exportedExam = json.data.exams[0];
  assert.equal(exportedExam.id, "exam_1");
  assert.equal(exportedExam.examName, "General Knowledge 2026");
  assert.equal(exportedExam.category, "General Studies");
  assert.equal(exportedExam.timeLimitMinutes, 45);
  assert.equal(exportedExam.questionCount, 2);
  assert.equal(exportedExam.autoGenerationEnabled, true);
  assert.equal(exportedExam.autoGenTime, "08:00");
  assert.equal(exportedExam.generationPrompt, "Current affairs prompt");
  assert.equal(exportedExam.syllabusFileName, "gk_syllabus.pdf");
  assert.equal(exportedExam.syllabusUrl, "https://storage.eve.app/gk.pdf");
  assert.equal(exportedExam.questions.length, 2);

  const q1 = exportedExam.questions[0];
  assert.equal(q1.id, "q_1");
  assert.equal(q1.examId, "exam_1");
  assert.equal(q1.questionText, "What is the boiling point of water at 1 atm?");
  assert.equal(q1.optionA, "90°C");
  assert.equal(q1.optionB, "100°C");
  assert.equal(q1.optionC, "110°C");
  assert.equal(q1.optionD, "120°C");
  assert.equal(q1.correctAnswer, "B");
  assert.equal(q1.explanation, "Water boils at 100 degrees Celsius at sea level.");
  assert.equal(q1.topic, "Physics");

  // Stringified inspection: guarantee NO student PII leak
  const rawExportString = JSON.stringify(json);
  assert.ok(!rawExportString.includes("private.student@example.com"), "Student email leaked!");
  assert.ok(!rawExportString.includes("student_secret_uid"), "Student UID leaked!");
  assert.ok(!rawExportString.includes("+919999999999"), "Student phone leaked!");
  assert.ok(!rawExportString.includes("att_secret_id"), "Student attempt ID leaked!");

  // Verify server-side audit log entry
  const auditRow = sqlite.prepare("SELECT * FROM admin_audit_log WHERE action_type = 'EXAM_EXPORTED'").get();
  assert.ok(auditRow, "Audit log row must exist");
  assert.equal(auditRow.actor_uid, "a1");
  assert.equal(auditRow.target_type, "system");
  assert.equal(auditRow.target_id, "export_exams");
  const metadata = JSON.parse(auditRow.metadata);
  assert.equal(metadata.examCount, 1);
  assert.equal(metadata.questionCount, 2);
  assert.equal(metadata.totalExams, 1);
});

test("Admin Export: Handles 200+ exams and verifies bounded chunking", async () => {
  const { d1, sqlite } = isolatedD1();
  const admin = { uid: "a1", email: "own.keni@gmail.com", isAdmin: true };
  const app = createAdminApp(d1, admin);

  // Seed 205 exams with 2 questions each (total 410 questions)
  sqlite.exec("BEGIN");
  const examStmt = sqlite.prepare("INSERT INTO exams (id, exam_name, category, time_limit_minutes) VALUES (?, ?, ?, ?)");
  const questionStmt = sqlite.prepare("INSERT INTO questions (id, exam_id, question_text, option_a, option_b, option_c, option_d, correct_answer, explanation) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");

  for (let i = 1; i <= 205; i++) {
    const examId = `exam_${String(i).padStart(3, "0")}`;
    examStmt.run(examId, `Exam Number ${i}`, "Mock Exam", 30);
    questionStmt.run(`q_${examId}_1`, examId, `Question 1 for ${examId}`, "A", "B", "C", "D", "A", "Exp 1");
    questionStmt.run(`q_${examId}_2`, examId, `Question 2 for ${examId}`, "A", "B", "C", "D", "B", "Exp 2");
  }
  sqlite.exec("COMMIT");

  // Chunk 1: offset=0, limit=50
  const res1 = await app.request("/api/admin/export/exams?offset=0&limit=50");
  assert.equal(res1.status, 200);
  const json1 = await res1.json();
  assert.equal(json1.data.exams.length, 50);
  assert.equal(json1.data.pagination.offset, 0);
  assert.equal(json1.data.pagination.limit, 50);
  assert.equal(json1.data.pagination.totalExams, 205);
  assert.equal(json1.data.pagination.totalQuestions, 410);
  assert.equal(json1.data.pagination.hasMore, true);
  assert.equal(json1.data.exams[0].id, "exam_001");
  assert.equal(json1.data.exams[49].id, "exam_050");

  // Chunk 4: offset=150, limit=50
  const res4 = await app.request("/api/admin/export/exams?offset=150&limit=50");
  assert.equal(res4.status, 200);
  const json4 = await res4.json();
  assert.equal(json4.data.exams.length, 50);
  assert.equal(json4.data.pagination.hasMore, true);
  assert.equal(json4.data.exams[0].id, "exam_151");
  assert.equal(json4.data.exams[49].id, "exam_200");

  // Final Chunk 5: offset=200, limit=50
  const res5 = await app.request("/api/admin/export/exams?offset=200&limit=50");
  assert.equal(res5.status, 200);
  const json5 = await res5.json();
  assert.equal(json5.data.exams.length, 5); // 205 - 200 = 5 remaining exams
  assert.equal(json5.data.pagination.hasMore, false);
  assert.equal(json5.data.exams[0].id, "exam_201");
  assert.equal(json5.data.exams[4].id, "exam_205");

  // Verify limit clamping: requesting limit=200 is clamped to 100
  const resClamped = await app.request("/api/admin/export/exams?offset=0&limit=200");
  assert.equal(resClamped.status, 200);
  const jsonClamped = await resClamped.json();
  assert.equal(jsonClamped.data.exams.length, 100);
  assert.equal(jsonClamped.data.pagination.limit, 100);
});
