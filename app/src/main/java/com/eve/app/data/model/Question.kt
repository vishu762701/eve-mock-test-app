package com.eve.app.data.model

/** Firestore collection: questions. correctAnswer = "A" | "B" | "C" | "D" */
data class Question(
    val id: String = "",
    val examId: String = "",
    val questionText: String = "",
    val optionA: String = "",
    val optionB: String = "",
    val optionC: String = "",
    val optionD: String = "",
    val correctAnswer: String = "",
    /** Optional — "kyun sahi hai" wali detail. Blank ho to Result screen me section hide rehta hai. */
    val explanation: String = "",
    /** Optional — subject/topic tag (jaise "Percentage", "Polity"). Blank ho to Performance
     * screen (Phase 17) is question ko "General" ke bucket me count karta hai. */
    val topic: String = "",
    // Hindi translations (Phase 11) — sab optional. Admin ne translate na kiya ho to
    // display*() functions apne aap English par fallback ho jaate hain.
    val questionTextHi: String = "",
    val optionAHi: String = "",
    val optionBHi: String = "",
    val optionCHi: String = "",
    val optionDHi: String = "",
    val explanationHi: String = ""
) {
    fun optionText(letter: String): String = when (letter) {
        "A" -> optionA
        "B" -> optionB
        "C" -> optionC
        "D" -> optionD
        else -> ""
    }

    /** Raw Hindi option text — blank matlab abhi translate nahi hua. */
    fun optionTextHi(letter: String): String = when (letter) {
        "A" -> optionAHi
        "B" -> optionBHi
        "C" -> optionCHi
        "D" -> optionDHi
        else -> ""
    }

    /** hindi=true aur translation available ho tabhi Hindi dikhega, warna English hi dikhega. */
    fun displayQuestionText(hindi: Boolean): String =
        if (hindi && questionTextHi.isNotBlank()) questionTextHi else questionText

    fun displayOptionText(letter: String, hindi: Boolean): String {
        val hi = optionTextHi(letter)
        return if (hindi && hi.isNotBlank()) hi else optionText(letter)
    }

    fun displayExplanation(hindi: Boolean): String =
        if (hindi && explanationHi.isNotBlank()) explanationHi else explanation
}
