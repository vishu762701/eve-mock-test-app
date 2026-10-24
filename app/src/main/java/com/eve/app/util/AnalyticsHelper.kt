package com.eve.app.util

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Phase 15: Firebase Analytics events — kaunsa exam sabse zyada attempt ho raha, kahan users
 * drop-off ho rahe (test start karke submit nahi karte), yeh sab Firebase Console (Analytics
 * -> Events) me is data se dikhega. Screen views khud-ba-khud track hoti hain (Analytics SDK
 * default), yahan sirf app-specific events add kiye hain.
 */
object AnalyticsHelper {

    private fun analytics(context: Context) = FirebaseAnalytics.getInstance(context)

    fun logLogin(context: Context) {
        analytics(context).logEvent(FirebaseAnalytics.Event.LOGIN, Bundle().apply {
            putString(FirebaseAnalytics.Param.METHOD, "google")
        })
    }

    fun logExamStart(context: Context, examId: String, examName: String, category: String) {
        analytics(context).logEvent("exam_start", Bundle().apply {
            putString("exam_id", examId)
            putString("exam_name", examName)
            putString("exam_category", category)
        })
    }

    /**
     * Test submit hone par score summary bhejta hai — isse Console me pata chalta hai kaunsa
     * exam sabse zyada attempt ho raha hai aur average score kya hai. `logExamStart` ke bina
     * `exam_submit` aane ka matlab beech me hi user chhod gaya (drop-off).
     */
    fun logExamSubmit(
        context: Context,
        examId: String,
        examName: String,
        category: String,
        correct: Int,
        wrong: Int,
        unattempted: Int,
        total: Int
    ) {
        analytics(context).logEvent("exam_submit", Bundle().apply {
            putString("exam_id", examId)
            putString("exam_name", examName)
            putString("exam_category", category)
            putInt("correct", correct)
            putInt("wrong", wrong)
            putInt("unattempted", unattempted)
            putInt("total_questions", total)
        })
    }
}
