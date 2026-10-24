package com.eve.app.data.model

/**
 * Firestore path: users/{userId}/bookmarks/{questionId}
 * Preserves the complete Question structure along with exam title and question number for rich display.
 */
data class BookmarkedQuestion(
    val questionId: String = "",
    val examId: String = "",
    val examName: String = "",
    val questionNumber: Int = 0,
    val questionText: String = "",
    val questionTextHi: String = "",
    val optionA: String = "",
    val optionB: String = "",
    val optionC: String = "",
    val optionD: String = "",
    val optionAHi: String = "",
    val optionBHi: String = "",
    val optionCHi: String = "",
    val optionDHi: String = "",
    val correctAnswer: String = "",
    val explanation: String = "",
    val explanationHi: String = "",
    val topic: String = "",
    val isPyq: Boolean = false,
    val pyqYear: Int = 0,
    val pyqPaper: String = "",
    val bookmarkedAt: Long = System.currentTimeMillis()
) {
    fun optionText(letter: String): String = when (letter) {
        "A" -> optionA
        "B" -> optionB
        "C" -> optionC
        "D" -> optionD
        else -> ""
    }

    fun optionTextHi(letter: String): String = when (letter) {
        "A" -> optionAHi
        "B" -> optionBHi
        "C" -> optionCHi
        "D" -> optionDHi
        else -> ""
    }

    fun displayQuestionText(hindi: Boolean): String =
        if (hindi && questionTextHi.isNotBlank()) questionTextHi else questionText

    fun displayOptionText(letter: String, hindi: Boolean): String {
        val hi = optionTextHi(letter)
        return if (hindi && hi.isNotBlank()) hi else optionText(letter)
    }

    fun displayExplanation(hindi: Boolean): String =
        if (hindi && explanationHi.isNotBlank()) explanationHi else explanation

    fun toQuestion(): Question = Question(
        id = questionId,
        examId = examId,
        questionText = questionText,
        optionA = optionA,
        optionB = optionB,
        optionC = optionC,
        optionD = optionD,
        correctAnswer = correctAnswer,
        explanation = explanation,
        topic = topic,
        isPyq = isPyq,
        pyqYear = pyqYear,
        pyqPaper = pyqPaper,
        questionTextHi = questionTextHi,
        optionAHi = optionAHi,
        optionBHi = optionBHi,
        optionCHi = optionCHi,
        optionDHi = optionDHi,
        explanationHi = explanationHi
    )
}
