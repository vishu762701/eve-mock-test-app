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
