/**
 * OPTIONAL (Phase 12 bonus): jab bhi "exams" collection me naya document add hota hai
 * (yaani Admin Dashboard se koi naya exam upload hota hai), yeh function automatically
 * saare devices ko "new_exams" topic par ek push notification bhej deta hai.
 *
 * Client se seedha doosre devices ko push bhejna possible nahi hai (koi bhi app apni FCM
 * server-side key expose nahi kar sakti) — isliye ek chhota sa server-side function chahiye.
 * Yeh "backend API" nahi hai (koi custom server/route nahi) — Firebase ka hi built-in
 * Cloud Functions feature hai, poora app ka baaki architecture (no backend, sab Firestore
 * se direct) bilkul waisa hi rehta hai.
 *
 * Deploy karne ke liye Firebase project ko Blaze (pay-as-you-go) plan par hona zaroori hai —
 * free Spark plan par Cloud Functions deploy nahi hoti. Bina isko deploy kiye baaki poora
 * Phase 12 (daily reminder, notification permission, FCM receive karna) already kaam karta
 * hai — sirf "naya exam add hote hi automatic alert" ke liye yeh extra step chahiye.
 *
 * Setup (ek baar):
 *   1. `npm install -g firebase-tools` (agar pehle se nahi hai)
 *   2. Repo ke root me: `firebase login` phir `firebase init functions` (existing project select
 *      karo, is `functions` folder ko overwrite mat karo — already yahan hai)
 *   3. `cd functions && npm install`
 *   4. `firebase deploy --only functions`
 *
 * Test karne ke liye (bina deploy kiye bhi): Firebase Console → Cloud Messaging → "New
 * campaign" → topic "new_exams" choose karke seedha ek test notification bhej sakte ho.
 */

const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { defineSecret } = require("firebase-functions/params");
const { initializeApp } = require("firebase-admin/app");
const { getMessaging } = require("firebase-admin/messaging");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { getAuth } = require("firebase-admin/auth");
const crypto = require("crypto");

const geminiKey1 = defineSecret("GEMINI_API_KEY_1");
const geminiKey2 = defineSecret("GEMINI_API_KEY_2");
const geminiKey3 = defineSecret("GEMINI_API_KEY_3");
const geminiKey4 = defineSecret("GEMINI_API_KEY_4");
const geminiSecrets = [geminiKey1, geminiKey2, geminiKey3, geminiKey4];

initializeApp();

exports.notifyNewExam = onDocumentCreated("exams/{examId}", async (event) => {
  const exam = event.data?.data();
  if (!exam) return;

  const examName = exam.examName || "Naya Exam";
  const category = exam.category || "";

  await getMessaging().send({
    topic: "new_exams",
    notification: {
      title: "Naya Exam Available!",
      body: category
        ? `${examName} (${category}) abhi add hua hai — abhi try karo!`
        : `${examName} abhi add hua hai — abhi try karo!`,
    },
    android: {
      priority: "high",
      notification: { channelId: "new_exam_channel" },
    },
  });
});

/**
 * Phase 16 (Leaderboard/Rank): jab bhi koi "attempts" document create hota hai (yaani
 * koi student test submit karta hai), yeh function `leaderboard` collection me uska
 * BEST score wala entry upsert kar deta hai (doc ID = "{examId}_{userId}", isliye ek
 * user ka ek exam me sirf ek hi entry rehti hai — dobara attempt dene par sirf tabhi
 * update hota hai jab naya score purane se better ho).
 *
 * Yeh Cloud Function ke through kyun: client apni khud ki attempt (Firestore rules) ke
 * alawa kisi aur ki nahi padh sakta, aur `leaderboard` collection me client se seedha
 * likhna bhi disallowed hai (firestore.rules) — taaki koi apna fake score khud se na
 * likh de. Sirf yeh server-side function (Admin SDK, rules bypass) likh sakta hai.
 *
 * Deploy karne ke steps README.md aur upar wale `notifyNewExam` function ke comment me
 * hain (same `firebase deploy --only functions`, Blaze plan chahiye).
 */
exports.updateLeaderboard = onDocumentCreated("attempts/{attemptId}", async (event) => {
  const attempt = event.data?.data();
  if (!attempt) return;

  const { userId, examId, examName, category, score, total } = attempt;
  if (!userId || !examId) return;

  const db = getFirestore();
  const entryRef = db.collection("leaderboard").doc(`${examId}_${userId}`);

  // displayName purane attempts (is Phase se pehle ke) me nahi hoga — tab Auth se fallback
  let displayName = attempt.displayName;
  if (!displayName) {
    try {
      const userRecord = await getAuth().getUser(userId);
      displayName = userRecord.displayName || "Student";
    } catch (e) {
      displayName = "Student";
    }
  }

  await db.runTransaction(async (tx) => {
    const existing = await tx.get(entryRef);
    if (existing.exists && (existing.data().score || 0) >= (score || 0)) {
      // Purana attempt already behtar ya barabar hai — overwrite mat karo
      return;
    }
    tx.set(entryRef, {
      userId,
      examId,
      examName: examName || "",
      category: category || "",
      displayName,
      score: score || 0,
      total: total || 0,
      timestamp: attempt.timestamp || Date.now(),
    });
  });

  // Overall Leaderboard aggregation
  try {
    const overallRef = db.collection("overall_leaderboard").doc(userId);
    await db.runTransaction(async (tx) => {
      const overallSnap = await tx.get(overallRef);
      const prev = overallSnap.exists ? overallSnap.data() : {
        totalScore: 0,
        testsTaken: 0,
        totalQuestions: 0,
        totalCorrect: 0
      };
      const testsTaken = (prev.testsTaken || 0) + 1;
      const totalScore = Math.round(((prev.totalScore || 0) + (score || 0)) * 100) / 100;
      const totalQuestions = (prev.totalQuestions || 0) + (total || 0);
      const correctCount = attempt.answers ? attempt.answers.filter(a => a.isCorrect).length : (score > 0 ? Math.round(score) : 0);
      const totalCorrect = (prev.totalCorrect || 0) + correctCount;
      const accuracy = totalQuestions > 0 ? Math.round((totalCorrect / totalQuestions) * 100) : 0;

      tx.set(overallRef, {
        userId,
        displayName: displayName || prev.displayName || "Student",
        score: totalScore,
        totalScore,
        total: totalQuestions,
        testsTaken,
        totalCorrect,
        accuracy,
        timestamp: Date.now()
      }, { merge: true });
    });
  } catch (e) {
    console.error("Failed to update overall leaderboard:", e);
  }
});


/**
 * Phase 22 — Admin analytics.
 *
 * attempts are private to their owner, so the Admin app cannot query the whole attempts
 * collection directly. This trusted server-side trigger turns each submitted attempt into
 * small aggregate documents that admins can read:
 *   admin_analytics_exams/{examId}
 *   admin_analytics_questions/{stableQuestionKey}
 *
 * No answer data is exposed to students through these collections.
 */
function stableQuestionKey(examId, answer) {
  const explicitId = String(answer.questionId || "").trim();
  if (explicitId) return `${examId}_${explicitId}`.replace(/[^A-Za-z0-9_-]/g, "_").slice(0, 120);
  const legacy = `${examId}|${answer.number || 0}|${answer.questionText || ""}`;
  return `${examId}_legacy_${crypto.createHash("sha256").update(legacy).digest("hex").slice(0, 24)}`;
}

exports.updateAdminAnalytics = onDocumentCreated("attempts/{attemptId}", async (event) => {
  const attempt = event.data?.data();
  if (!attempt) return;

  const db = getFirestore();
  const examId = String(attempt.examId || "").trim();
  const userId = String(attempt.userId || "").trim();
  if (!examId || !userId) return;

  const examRef = db.collection("admin_analytics_exams").doc(examId);
  const userMarkerRef = db.collection("admin_analytics_exam_users").doc(`${examId}_${userId}`);

  // Count a submission every time, but count a student only once per exam.
  await db.runTransaction(async (tx) => {
    const marker = await tx.get(userMarkerRef);
    const examData = {
      examId,
      examName: String(attempt.examName || ""),
      category: String(attempt.category || ""),
      attemptCount: FieldValue.increment(1),
      lastAttemptAt: Number(attempt.timestamp || Date.now())
    };
    if (!marker.exists) {
      examData.uniqueUsers = FieldValue.increment(1);
      tx.set(userMarkerRef, { createdAt: Date.now() });
    }
    tx.set(examRef, examData, { merge: true });
  });

  const answers = Array.isArray(attempt.answers) ? attempt.answers : [];
  const writes = [];
  for (const answer of answers) {
    const selected = String(answer.selected || "");
    const correct = String(answer.correct || "");
    const attempted = selected.length > 0;
    const isCorrect = attempted && selected === correct;
    const qRef = db.collection("admin_analytics_questions").doc(stableQuestionKey(examId, answer));

    const data = {
      examId,
      examName: String(attempt.examName || ""),
      questionId: String(answer.questionId || ""),
      questionNumber: Number(answer.number || 0),
      questionText: String(answer.questionText || ""),
      topic: String(answer.topic || ""),
      attempts: FieldValue.increment(attempted ? 1 : 0),
      correct: FieldValue.increment(isCorrect ? 1 : 0),
      wrong: FieldValue.increment(attempted && !isCorrect ? 1 : 0),
      unattempted: FieldValue.increment(attempted ? 0 : 1)
    };
    writes.push({ ref: qRef, data });
  }

  // Batched writes are limited to 500 operations. A normal mock test is far below this,
  // but chunking keeps the trigger safe for unusually large exams.
  for (let i = 0; i < writes.length; i += 400) {
    const batch = db.batch();
    writes.slice(i, i + 400).forEach(({ ref, data }) => batch.set(ref, data, { merge: true }));
    await batch.commit();
  }
});

/**
 * Phase 23 — Trusted attempt submission (security fix).
 *
 * Before this phase, the Android client computed its own score and wrote the whole
 * "attempts" document directly to Firestore (rules only checked that userId == auth uid,
 * never that the score was real). A modified client could submit any score/answers it
 * wanted, which fed straight into the Leaderboard (updateLeaderboard above) and the
 * Phase 22 admin analytics (updateAdminAnalytics above).
 *
 * Now the client sends ONLY which question it saw and which option it picked
 * ({questionId, number, selected}) through this callable function. Everything that
 * matters for trust — the correct answer, the score, correct/wrong/unattempted counts —
 * is computed here from the real "questions"/"daily_questions" documents (Admin SDK,
 * ignores client input for that part entirely). "attempts" documents are only ever
 * written from here now (see firestore.rules: attempts create is `false`), so a modified
 * client can no longer forge a result.
 *
 * NEGATIVE_MARK below must be kept in sync with Constants.NEGATIVE_MARK in the Android app.
 */
const NEGATIVE_MARK = 0.0;
const VALID_OPTIONS = ["A", "B", "C", "D"];

function questionOptionText(q, letter) {
  return String(q[`option${letter}`] || "");
}
function questionOptionTextHi(q, letter) {
  return String(q[`option${letter}Hi`] || "");
}

exports.submitAttempt = onCall(async (request) => {
  const uid = request.auth?.uid;
  if (!uid) {
    throw new HttpsError("unauthenticated", "Login required.");
  }

  const data = request.data || {};
  const examId = String(data.examId || "").trim();
  const db = getFirestore();

  // A student may submit a particular exam only once. Admins retain preview/retry access.
  const adminEmails = new Set([
    "pronlike9@gmail.com",
    "own.keni@gmail.com",
    "anyqueairdrop@gmail.com",
    "ghatisarkar56@gmail.com"
  ]);
  const callerEmail = String(request.auth?.token?.email || "").toLowerCase();
  const dynamicAdmin = await db.collection("admins").doc(callerEmail).get();
  const isAdminCaller = adminEmails.has(callerEmail) || dynamicAdmin.exists;
  const examName = String(data.examName || "Test").trim();
  const category = String(data.category || "").trim();
  const rawAnswers = Array.isArray(data.answers) ? data.answers : [];

  if (!examId || rawAnswers.length === 0 || rawAnswers.length > 300) {
    throw new HttpsError("invalid-argument", "Invalid attempt payload.");
  }

  // Trust nothing from rawAnswers except which question + which option was picked.
  const seen = new Set();
  const picks = [];
  for (const a of rawAnswers) {
    const questionId = String(a?.questionId || "").trim();
    if (!questionId || seen.has(questionId)) continue;
    seen.add(questionId);
    const selectedRaw = String(a?.selected || "").trim().toUpperCase();
    picks.push({
      questionId,
      number: Number(a?.number || 0),
      selected: VALID_OPTIONS.includes(selectedRaw) ? selectedRaw : "",
      isBookmarked: a?.isBookmarked === true
    });
  }
  if (picks.length === 0) {
    throw new HttpsError("invalid-argument", "No valid answers in payload.");
  }

  const isDaily = examId === "daily-gk";
  const isPractice = Boolean(data.topic || data.pyqYear);
  const todayDateStr = new Date().toISOString().slice(0, 10);
  const lockKey = isPractice ? null : (isDaily ? `${uid}_daily-gk_${todayDateStr}` : `${uid}_${examId}`);
  const lockRef = lockKey ? db.collection("attempt_locks").doc(lockKey) : null;
  if (!isAdminCaller && lockRef) {
    // Legacy compatibility: convert an old attempts document into the new deterministic lock.
    const existingLock = await lockRef.get();
    if (existingLock.exists) {
      throw new HttpsError("already-exists", "You have already completed this test.");
    }
    const existing = await db.collection("attempts")
      .where("userId", "==", uid)
      .where("examId", "==", examId)
      .limit(1)
      .get();
    if (!existing.empty) {
      await lockRef.set({ userId: uid, examId, timestamp: Date.now(), source: "legacy_migration" });
      throw new HttpsError("already-exists", "You have already completed this test.");
    }

    // Close the race where two devices submit the same test simultaneously.
    await db.runTransaction(async (tx) => {
      const lock = await tx.get(lockRef);
      if (lock.exists) {
        throw new HttpsError("already-exists", "You have already completed this test.");
      }
      tx.create(lockRef, { userId: uid, examId, timestamp: Date.now(), source: "submit" });
    });
  }

  const collectionName = isDaily ? "daily_questions" : "questions";
  const refs = picks.map((p) => db.collection(collectionName).doc(p.questionId));
  const snaps = await db.getAll(...refs);

  const answersData = [];
  let correct = 0, wrong = 0, unattempted = 0;
  const generatedTestsCache = new Map();

  for (let i = 0; i < picks.length; i++) {
    const snap = snaps[i];
    const pick = picks[i];
    let q = null;

    if (snap && snap.exists) {
      q = snap.data();
      if (!isDaily && String(q.examId || "") !== examId) continue;
    } else if (!isDaily && pick.questionId.includes("_")) {
      const lastUnderscore = pick.questionId.lastIndexOf("_");
      const testId = pick.questionId.substring(0, lastUnderscore);
      const qIndex = parseInt(pick.questionId.substring(lastUnderscore + 1), 10);
      if (!isNaN(qIndex)) {
        if (!generatedTestsCache.has(testId)) {
          const gDoc = await db.collection("generated_tests").doc(testId).get();
          generatedTestsCache.set(testId, gDoc.exists ? gDoc.data() : null);
        }
        const gData = generatedTestsCache.get(testId);
        if (gData && String(gData.examId || "") === examId && Array.isArray(gData.questions)) {
          q = gData.questions[qIndex] || null;
        }
      }
    }

    if (!q) continue;

    const correctAnswer = String(q.correctAnswer || "");
    const attempted = pick.selected.length > 0;
    const isCorrect = attempted && pick.selected === correctAnswer;

    if (!attempted) unattempted++;
    else if (isCorrect) correct++;
    else wrong++;

    answersData.push({
      questionId: pick.questionId,
      number: pick.number,
      questionText: String(q.questionText || ""),
      selected: pick.selected,
      selectedText: pick.selected ? questionOptionText(q, pick.selected) : "",
      correct: correctAnswer,
      correctText: questionOptionText(q, correctAnswer),
      explanation: String(q.explanation || ""),
      isBookmarked: pick.isBookmarked,
      topic: String(q.topic || ""),
      questionTextHi: String(q.questionTextHi || ""),
      selectedTextHi: pick.selected ? questionOptionTextHi(q, pick.selected) : "",
      correctTextHi: questionOptionTextHi(q, correctAnswer),
      explanationHi: String(q.explanationHi || "")
    });
  }

  if (answersData.length === 0) {
    throw new HttpsError("invalid-argument", "None of the submitted questions could be verified.");
  }

  const total = answersData.length;
  const score = correct - wrong * NEGATIVE_MARK;

  let displayName = String(data.displayName || "").trim();
  if (!displayName) {
    try {
      const userRecord = await getAuth().getUser(uid);
      displayName = userRecord.displayName || "Student";
    } catch (e) {
      displayName = "Student";
    }
  }

  const attemptRef = db.collection("attempts").doc();
  await attemptRef.set({
    userId: uid,
    displayName,
    examId,
    examName,
    category,
    score,
    total,
    correct,
    wrong,
    unattempted,
    timestamp: Date.now(),
    answers: answersData
  });

  return { attemptId: attemptRef.id, score, total, correct, wrong, unattempted };
});

/**
 * ============================================================================
 * PART D: Admin Push Notification Broadcast
 * ============================================================================
 * When an admin creates a document in the `notifications` collection,
 * this function automatically broadcasts a high-priority push notification
 * to the `all_users` topic.
 */
exports.broadcastNotification = onDocumentCreated("notifications/{notificationId}", async (event) => {
  const notif = event.data?.data();
  if (!notif) return;

  const title = String(notif.title || "Eve Notification").trim();
  const body = String(notif.message || notif.body || "").trim();
  if (!title && !body) return;

  try {
    await getMessaging().send({
      topic: "all_users",
      notification: {
        title,
        body,
      },
      data: {
        title,
        message: body,
        body,
        type: String(notif.type || "general"),
        notificationId: String(event.params.notificationId || ""),
        sentAt: String(Date.now()),
      },
      android: {
        priority: "high",
        notification: {
          channelId: "new_exam_channel",
        },
      },
    });
    console.log(`Successfully broadcast notification "${title}" to topic all_users.`);
  } catch (err) {
    console.error("Failed to broadcast notification to all_users:", err);
  }
});

/**
 * ============================================================================
 * PART E: Automated AI Question-Generation System
 * ============================================================================
 */

const HARDCODED_ADMIN_EMAILS = new Set([
  "pronlike9@gmail.com",
  "own.keni@gmail.com",
  "anyqueairdrop@gmail.com",
  "ghatisarkar56@gmail.com"
]);

async function verifyIsAdmin(db, email) {
  const callerEmail = String(email || "").toLowerCase().trim();
  if (!callerEmail) return false;
  if (HARDCODED_ADMIN_EMAILS.has(callerEmail)) return true;
  const dynamicAdmin = await db.collection("admins").doc(callerEmail).get();
  return dynamicAdmin.exists;
}

function getGeminiApiKeys() {
  const keys = [
    geminiKey1.value() || process.env.GEMINI_API_KEY_1,
    geminiKey2.value() || process.env.GEMINI_API_KEY_2,
    geminiKey3.value() || process.env.GEMINI_API_KEY_3,
    geminiKey4.value() || process.env.GEMINI_API_KEY_4,
  ].filter(Boolean);

  if (keys.length === 0) {
    const fallback = process.env.GEMINI_API_KEYS || process.env.GEMINI_API_KEY || "";
    return fallback.split(",").map((k) => k.trim()).filter(Boolean);
  }
  return keys;
}

async function getLastUsedKeyIndex(db, poolSize) {
  if (poolSize <= 0) return 0;
  try {
    const snap = await db.collection("system_config").doc("apiRotation").get();
    if (snap.exists) {
      const idx = snap.data().lastUsedKeyIndex;
      if (typeof idx === "number" && idx >= 0) {
        return (idx + 1) % poolSize;
      }
    }
  } catch (err) {
    console.warn("Could not read system_config/apiRotation:", err);
  }
  return 0;
}

async function persistLastUsedKeyIndex(db, index) {
  try {
    await db.collection("system_config").doc("apiRotation").set(
      {
        lastUsedKeyIndex: index,
        updatedAt: Date.now(),
      },
      { merge: true }
    );
  } catch (err) {
    console.warn("Could not persist system_config/apiRotation:", err);
  }
}

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

async function callGeminiWithRotation(db, promptText, startingIndex = 0) {
  const keys = getGeminiApiKeys();
  if (keys.length === 0) {
    throw new Error("No Gemini API keys configured. Set GEMINI_API_KEY_1 through GEMINI_API_KEY_4 in Firebase secrets.");
  }

  let lastError = null;
  for (let offset = 0; offset < keys.length; offset++) {
    const keyIndex = (startingIndex + offset) % keys.length;
    const currentKey = keys[keyIndex];

    try {
      const endpoint = `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${encodeURIComponent(currentKey)}`;
      const body = {
        contents: [
          {
            parts: [{ text: promptText }],
          },
        ],
        generationConfig: {
          temperature: 0.7,
          responseMimeType: "application/json",
        },
      };

      const response = await fetch(endpoint, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });

      if (!response.ok) {
        const errText = await response.text();
        console.warn(`Gemini key #${keyIndex + 1} failed: HTTP ${response.status} - ${errText}`);

        // Rate-limit or quota error detection
        const isQuotaOrRateLimit =
          response.status === 429 ||
          response.status === 403 ||
          response.status === 503 ||
          errText.toLowerCase().includes("quota") ||
          errText.toLowerCase().includes("resource_exhausted") ||
          errText.toLowerCase().includes("rate limit");

        if (isQuotaOrRateLimit) {
          console.log(`Rate-limit / quota hit on key #${keyIndex + 1}. Retrying with next key in pool...`);
          lastError = new Error(`Key #${keyIndex + 1} quota/rate-limited: ${errText}`);
          continue;
        }

        throw new Error(`Gemini API error (HTTP ${response.status}): ${errText}`);
      }

      const data = await response.json();
      const text = data.candidates?.[0]?.content?.parts?.[0]?.text;
      if (!text) {
        throw new Error("Empty candidate received from Gemini API.");
      }

      // Persist successfully used key index in Firestore
      await persistLastUsedKeyIndex(db, keyIndex);
      return { text, usedKeyIndex: keyIndex };
    } catch (err) {
      console.warn(`Attempt with Gemini key #${keyIndex + 1} failed:`, err.message);
      lastError = err;
    }
  }

  throw new Error(`All ${keys.length} keys exhausted: ${lastError?.message || "Quota exceeded"}`);
}

function parseGeminiQuestions(rawJson) {
  let cleaned = rawJson.trim();
  if (cleaned.startsWith("```json")) {
    cleaned = cleaned.replace(/^```json\s*/, "").replace(/\s*```$/, "");
  } else if (cleaned.startsWith("```")) {
    cleaned = cleaned.replace(/^```\s*/, "").replace(/\s*```$/, "");
  }

  const parsed = JSON.parse(cleaned);
  const list = Array.isArray(parsed) ? parsed : (Array.isArray(parsed.questions) ? parsed.questions : []);

  const validQuestions = [];
  const validAnswers = new Set(["A", "B", "C", "D"]);

  for (const q of list) {
    const questionText = String(q.questionText || q.question || "").trim();
    const optionA = String(q.optionA || q.a || "").trim();
    const optionB = String(q.optionB || q.b || "").trim();
    const optionC = String(q.optionC || q.c || "").trim();
    const optionD = String(q.optionD || q.d || "").trim();
    let correctAnswer = String(q.correctAnswer || q.answer || "").trim().toUpperCase();

    if (!validAnswers.has(correctAnswer)) {
      if (correctAnswer === "1" || correctAnswer === optionA.toUpperCase()) correctAnswer = "A";
      else if (correctAnswer === "2" || correctAnswer === optionB.toUpperCase()) correctAnswer = "B";
      else if (correctAnswer === "3" || correctAnswer === optionC.toUpperCase()) correctAnswer = "C";
      else if (correctAnswer === "4" || correctAnswer === optionD.toUpperCase()) correctAnswer = "D";
      else correctAnswer = "A";
    }

    const explanation = String(q.explanation || "").trim();

    if (questionText && optionA && optionB && optionC && optionD) {
      validQuestions.push({
        questionText,
        optionA,
        optionB,
        optionC,
        optionD,
        correctAnswer,
        explanation,
      });
    }
  }

  return validQuestions;
}

function buildPrompt(examName, syllabusText, questionCount, customPromptNotes) {
  return `You are an expert question-setter for ${examName}, a well-known Indian government/entrance exam.
Generate exactly ${questionCount} unique multiple-choice questions strictly based on this syllabus:
${syllabusText || "General competitive exam topics including General Awareness, Reasoning, Quantitative Aptitude, and English Comprehension."}
Additional instructions from the exam admin: ${customPromptNotes || "None"}
Rules you must follow:
1. Each question must match real exam-level difficulty and be factually accurate.
2. Before deciding the correct answer, mentally solve the question step-by-step yourself first — never guess. Then match your solved answer to the correct option.
3. For every question, also write a clear, detailed explanation (3-5 sentences) of why the correct answer is correct, written the way a teacher would explain it to a student.
4. Do not repeat concepts/questions that are commonly duplicated — vary sub-topics, numbers, and phrasing across all ${questionCount} questions.
5. Output ONLY a valid JSON array, nothing else — no markdown, no explanation outside the JSON, no text before or after.
Exact output format:
[
{
"questionText": "...",
"optionA": "...",
"optionB": "...",
"optionC": "...",
"optionD": "...",
"correctAnswer": "A",
"explanation": "..."
}
]`;
}

/**
 * Scheduled Nightly Test Generation (runs daily at midnight).
 * Automatically generates a fresh mock test for every exam with autoGenerationEnabled == true.
 */
exports.generateNightlyTests = onSchedule(
  {
    schedule: "every day 00:00",
    secrets: geminiSecrets,
  },
  async (event) => {
    const db = getFirestore();
    const examsSnap = await db.collection("exams").where("autoGenerationEnabled", "==", true).get();
    if (examsSnap.empty) {
      console.log("No exams with autoGenerationEnabled == true found.");
      return;
    }

    console.log(`Found ${examsSnap.size} exams configured for automatic generation.`);

    const poolSize = getGeminiApiKeys().length || 4;
    let nextKeyIndex = await getLastUsedKeyIndex(db, poolSize);

    for (const doc of examsSnap.docs) {
      const exam = doc.data();
      const examId = doc.id;
      const examName = exam.examName || "Mock Test";
      const syllabus = exam.syllabus || "";
      const questionCount = Math.max(5, Math.min(50, Number(exam.questionCount || 10)));
      const customPromptNotes = exam.customPromptNotes || "";

      try {
        console.log(`Generating nightly test for ${examName} (${examId})...`);

        // Duplicate suppression: gather questions from up to 5 recent tests
        const recentSnap = await db.collection("generated_tests")
          .where("examId", "==", examId)
          .limit(10)
          .get();

        const seenSet = new Set();
        recentSnap.docs
          .sort((a, b) => (b.data().generatedAt || 0) - (a.data().generatedAt || 0))
          .slice(0, 5)
          .forEach((d) => {
            const qs = d.data().questions || [];
            qs.forEach((q) => {
              if (q.questionText) {
                seenSet.add(q.questionText.trim().toLowerCase());
              }
            });
          });

        const prompt = buildPrompt(examName, syllabus, questionCount, customPromptNotes);
        const { text: rawJson, usedKeyIndex } = await callGeminiWithRotation(db, prompt, nextKeyIndex);
        nextKeyIndex = (usedKeyIndex + 1) % poolSize;

        const parsedQuestions = parseGeminiQuestions(rawJson);

        const uniqueQuestions = [];
        const batchSeen = new Set();
        for (const q of parsedQuestions) {
          const norm = q.questionText.trim().toLowerCase();
          if (!seenSet.has(norm) && !batchSeen.has(norm)) {
            batchSeen.add(norm);
            uniqueQuestions.push(q);
          }
        }

        if (uniqueQuestions.length === 0) {
          console.warn(`No unique questions generated for ${examName}.`);
          continue;
        }

        // Rule 6: Write the validated question set as a new document in generated_tests with status "paused"
        const generatedTestRef = db.collection("generated_tests").doc();
        await generatedTestRef.set({
          examId,
          examName,
          generatedAt: Date.now(),
          status: "paused",
          questionCount: uniqueQuestions.length,
          questions: uniqueQuestions,
        });

        console.log(
          `Successfully published nightly test ${generatedTestRef.id} with ${uniqueQuestions.length} questions for ${examName} (status: paused).`
        );
      } catch (err) {
        console.error(`Nightly test generation failed for ${examName} (${examId}):`, err.message);

        // If all keys exhausted, log to generation_logs and skip this exam
        if (err.message && err.message.includes("exhausted")) {
          try {
            await db.collection("generation_logs").add({
              examId,
              examName,
              timestamp: Date.now(),
              status: "failed",
              error: "all keys exhausted",
              details: err.message,
            });
          } catch (logErr) {
            console.error("Failed to write to generation_logs:", logErr);
          }
        }
      }

      // Add a short delay (a few seconds) between each exam's API call to respect per-minute (RPM) limits
      await sleep(3000);
    }
  }
);

/**
 * Callable function to manually generate a test via AI on demand from Admin Dashboard.
 */
exports.triggerAiTestGeneration = onCall(
  {
    secrets: geminiSecrets,
  },
  async (request) => {
    const uid = request.auth?.uid;
    if (!uid) {
      throw new HttpsError("unauthenticated", "Login required.");
    }

    const db = getFirestore();
    const callerEmail = String(request.auth?.token?.email || "");
    const isAdmin = await verifyIsAdmin(db, callerEmail);
    if (!isAdmin) {
      throw new HttpsError("permission-denied", "Admin privileges required.");
    }

    const data = request.data || {};
    const examId = String(data.examId || "").trim();
    if (!examId) {
      throw new HttpsError("invalid-argument", "examId is required.");
    }

    const examDoc = await db.collection("exams").doc(examId).get();
    if (!examDoc.exists) {
      throw new HttpsError("not-found", "Exam not found.");
    }

    const exam = examDoc.data();
    const examName = exam.examName || "Mock Test";
    const syllabus = String(data.syllabus || exam.syllabus || "");
    const customPromptNotes = String(data.customPromptNotes || exam.customPromptNotes || "");
    const targetCount = Math.max(3, Math.min(50, Number(data.questionCount || exam.questionCount || 10)));
    const status = String(data.status || "paused");

    // Duplicate suppression
    const recentSnap = await db.collection("generated_tests")
      .where("examId", "==", examId)
      .limit(10)
      .get();

    const seenSet = new Set();
    recentSnap.docs
      .sort((a, b) => (b.data().generatedAt || 0) - (a.data().generatedAt || 0))
      .slice(0, 5)
      .forEach((d) => {
        const qs = d.data().questions || [];
        qs.forEach((q) => {
          if (q.questionText) {
            seenSet.add(q.questionText.trim().toLowerCase());
          }
        });
      });

    const poolSize = getGeminiApiKeys().length || 4;
    const startIndex = await getLastUsedKeyIndex(db, poolSize);
    const prompt = buildPrompt(examName, syllabus, targetCount, customPromptNotes);

    try {
      const { text: rawJson } = await callGeminiWithRotation(db, prompt, startIndex);
      const parsedQuestions = parseGeminiQuestions(rawJson);

      const uniqueQuestions = [];
      const batchSeen = new Set();
      for (const q of parsedQuestions) {
        const norm = q.questionText.trim().toLowerCase();
        if (!seenSet.has(norm) && !batchSeen.has(norm)) {
          batchSeen.add(norm);
          uniqueQuestions.push(q);
        }
      }

      if (uniqueQuestions.length === 0) {
        throw new HttpsError("internal", "AI failed to generate unique questions.");
      }

      const generatedTestRef = db.collection("generated_tests").doc();
      await generatedTestRef.set({
        examId,
        examName,
        generatedAt: Date.now(),
        status,
        questionCount: uniqueQuestions.length,
        questions: uniqueQuestions,
      });

      return {
        success: true,
        testId: generatedTestRef.id,
        questionCount: uniqueQuestions.length,
        status,
      };
    } catch (err) {
      if (err instanceof HttpsError) throw err;

      // Log failure to generation_logs if all keys exhausted
      if (err.message && err.message.includes("exhausted")) {
        await db.collection("generation_logs").add({
          examId,
          examName,
          timestamp: Date.now(),
          status: "failed",
          error: "all keys exhausted",
          details: err.message,
        });
      }
      throw new HttpsError("resource-exhausted", err.message);
    }
  }
);
