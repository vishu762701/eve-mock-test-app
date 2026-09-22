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
    val correctAnswer: String = ""
) {
    fun optionText(letter: String): String = when (letter) {
        "A" -> optionA
        "B" -> optionB
        "C" -> optionC
        "D" -> optionD
        else -> ""
    }
}
