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

    const val EXTRA_EXAM_ID = "extra_exam_id"
    const val EXTRA_EXAM_NAME = "extra_exam_name"
    const val EXTRA_TIME_LIMIT = "extra_time_limit"
    const val EXTRA_ANSWERS = "extra_answers"
}
