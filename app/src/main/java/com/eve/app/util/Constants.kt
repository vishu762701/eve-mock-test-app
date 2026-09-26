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

    // Cloudflare Worker API Base URL
    const val WORKER_BASE_URL = "https://eve-backend.anyqueairdrop.workers.dev/"

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
    const val EXTRA_TOPIC = "extra_topic"
    const val EXTRA_PRACTICE_MODE = "extra_practice_mode"
    const val EXTRA_PYQ_MODE = "extra_pyq_mode"
    const val EXTRA_PYQ_YEAR = "extra_pyq_year"
    const val EXTRA_PYQ_PAPER = "extra_pyq_paper"
    @Deprecated("Replaced by ResultDataHolder to prevent android.os.TransactionTooLargeException")
    const val EXTRA_ANSWERS = "extra_answers"

    // Test History (Phase 10)
    const val EXTRA_ATTEMPT_DATE = "extra_attempt_date"
    const val EXTRA_FROM_HISTORY = "extra_from_history"

    // Bookmarks
    const val EXTRA_INITIAL_QUESTION_ID = "extra_initial_question_id"
    const val EXTRA_FROM_BOOKMARK = "extra_from_bookmark"

    // Phase 14: About screen ke links. GitHub Pages par docs/ folder se serve hote hain
    // (repo Settings -> Pages -> Source: main branch, /docs folder). Agar tumhara
    // GitHub username ya repo naam alag hai to yeh 2 URLs update kar dena.
    const val PRIVACY_POLICY_URL = "https://vishu762701.github.io/eve-mock-test-app/privacy-policy.html"
    const val TERMS_URL = "https://vishu762701.github.io/eve-mock-test-app/terms.html"
    const val SUPPORT_EMAIL = "pronlike9@gmail.com"
}
