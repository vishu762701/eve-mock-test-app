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
const { initializeApp } = require("firebase-admin/app");
const { getMessaging } = require("firebase-admin/messaging");
const { getFirestore } = require("firebase-admin/firestore");
const { getAuth } = require("firebase-admin/auth");

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
});
