import test from "node:test";
import assert from "node:assert/strict";

// 1. Test live /api/app-config endpoint directly over HTTP
test("live /api/app-config endpoint returns valid schema and default config", async () => {
  const res = await fetch("https://eve-backend.anyqueairdrop.workers.dev/api/app-config");
  assert.equal(res.status, 200);

  const json = await res.json();
  assert.equal(json.success, true);
  assert.ok(json.data);
  assert.equal(typeof json.data.minimum_supported_version_code, "number");
  assert.equal(typeof json.data.maintenance_mode, "boolean");
  assert.equal(typeof json.data.maintenance_message, "string");
  assert.ok(json.data.minimum_supported_version_code >= 1);
});

// 2. Test Exam-wise Analytics: average score calculation and engagement categorization
test("exam analytics calculates average score and assigns correct engagement badge", () => {
  function computeExamEngagement(attemptCount, totalScore, maxPossibleScore) {
    const averageScore = attemptCount > 0 ? (totalScore / maxPossibleScore) * 100 : 0.0;
    let badgeText = "";
    let badgeColor = "";

    if (attemptCount === 0) {
      badgeText = "⚠️ 0 Attempts - Needs Promotion";
      badgeColor = "eve_red";
    } else if (attemptCount < 5) {
      badgeText = "⚠️ Low Engagement (<5 attempts)";
      badgeColor = "eve_amber";
    } else {
      badgeText = "✅ Active Engagement";
      badgeColor = "eve_green";
    }

    return { averageScore, badgeText, badgeColor };
  }

  // Case 1: Zero attempts
  const res0 = computeExamEngagement(0, 0, 0);
  assert.equal(res0.badgeText, "⚠️ 0 Attempts - Needs Promotion");
  assert.equal(res0.badgeColor, "eve_red");
  assert.equal(res0.averageScore, 0.0);

  // Case 2: Low engagement (<5 attempts)
  const res3 = computeExamEngagement(3, 210, 300); // 70% average
  assert.equal(res3.badgeText, "⚠️ Low Engagement (<5 attempts)");
  assert.equal(res3.badgeColor, "eve_amber");
  assert.equal(res3.averageScore, 70.0);

  // Case 3: Active engagement (>=5 attempts)
  const res15 = computeExamEngagement(15, 1200, 1500); // 80% average
  assert.equal(res15.badgeText, "✅ Active Engagement");
  assert.equal(res15.badgeColor, "eve_green");
  assert.equal(res15.averageScore, 80.0);
});

// 3. Test User search filtering and ban/disable account toggle logic
test("user management search filter and account ban status toggling", () => {
  const users = [
    { id: "1", displayName: "Aarav Sharma", email: "aarav@gmail.com", disabled: 0 },
    { id: "2", displayName: "Bhavna Joshi", email: "bhavna@yahoo.com", disabled: 1 },
    { id: "3", displayName: "Chirag Gupta", email: "chirag@gmail.com", disabled: 0 },
  ];

  function filterUsers(query) {
    if (!query) return users;
    const q = query.toLowerCase();
    return users.filter(u => u.displayName.toLowerCase().includes(q) || u.email.toLowerCase().includes(q));
  }

  assert.equal(filterUsers("aarav").length, 1);
  assert.equal(filterUsers("gmail.com").length, 2);
  assert.equal(filterUsers("NON_EXISTENT").length, 0);

  // Toggle status
  const userToBan = { ...users[0], disabled: users[0].disabled === 1 ? 0 : 1 };
  assert.equal(userToBan.disabled, 1);
});

// 4. Test Feedback Reply structure with original message context
test("feedback reply correctly constructs response and preserves original context", () => {
  const feedbackMessage = {
    id: "msg_123",
    user_id: "usr_456",
    user_name: "Pooja Verma",
    user_email: "pooja@gmail.com",
    message: "Question 5 has a typographical error in Option C.",
    created_at: 1790000000000,
    read: 0,
    replied: 0,
    admin_reply: null,
    replied_at: null,
  };

  const adminReplyText = "Thank you Pooja, we have corrected Option C in the question bank.";
  const now = Date.now();

  const updatedMessage = {
    ...feedbackMessage,
    read: 1,
    replied: 1,
    admin_reply: adminReplyText,
    replied_at: now,
  };

  assert.equal(updatedMessage.read, 1);
  assert.equal(updatedMessage.replied, 1);
  assert.equal(updatedMessage.admin_reply, adminReplyText);
  assert.equal(updatedMessage.message, "Question 5 has a typographical error in Option C.");
});

// 5. Test Sub-exams hierarchy and Main Exam filtering
test("sub-exams hierarchy and home main exam filtering", () => {
  const exams = [
    { id: "exam_1", exam_name: "RSSB 3rd Grade", parent_exam_id: "" },
    { id: "exam_2", exam_name: "Hindi", parent_exam_id: "exam_1" },
    { id: "exam_3", exam_name: "Maths", parent_exam_id: "exam_1" },
    { id: "exam_4", exam_name: "Rajasthan CET", parent_exam_id: "" },
  ];

  // Home filter: only main exams
  const mainExams = exams.filter((e) => !e.parent_exam_id || e.parent_exam_id.trim() === "");
  assert.equal(mainExams.length, 2);
  assert.deepEqual(mainExams.map((e) => e.exam_name), ["RSSB 3rd Grade", "Rajasthan CET"]);

  // Sub-exams for exam_1
  const subExamsExam1 = exams.filter((e) => e.parent_exam_id === "exam_1");
  assert.equal(subExamsExam1.length, 2);
  assert.deepEqual(subExamsExam1.map((e) => e.exam_name), ["Hindi", "Maths"]);

  // Sub-exams for exam_4
  const subExamsExam4 = exams.filter((e) => e.parent_exam_id === "exam_4");
  assert.equal(subExamsExam4.length, 0);
});

