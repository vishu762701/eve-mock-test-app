package com.eve.app.util

object Constants {
    // Yeh emails hamesha admin rahenge (Firestore down ho tab bhi kaam karega)
    val ADMIN_EMAILS = setOf(
        "pronlike9@gmail.com",
        "own.keni@gmail.com",
        "anyqueairdrop@gmail.com",
        "ghatisarkar56@gmail.com"
    )

    // Per wrong answer negative marking (0.0 = koi negative marking nahi)
    const val NEGATIVE_MARK = 0.0

    // Admin form ke Category spinner ke liye preset list. "Other" chuno to custom naam type kar sakte ho.
    val CATEGORIES = listOf(
        "SSC", "UPSC", "Banking", "Railway", "State PSC", "Police", "Defence", "Teaching", "Other"
    )
    const val CATEGORY_OTHER = "Other"
    const val CATEGORY_ALL = "All"

    const val EXTRA_EXAM_ID = "extra_exam_id"
    const val EXTRA_EXAM_NAME = "extra_exam_name"
    const val EXTRA_EXAM_CATEGORY = "extra_exam_category"
    const val EXTRA_TIME_LIMIT = "extra_time_limit"
    const val EXTRA_ANSWERS = "extra_answers"

    // Test History (Phase 10)
    const val EXTRA_ATTEMPT_DATE = "extra_attempt_date"
    const val EXTRA_FROM_HISTORY = "extra_from_history"
}
