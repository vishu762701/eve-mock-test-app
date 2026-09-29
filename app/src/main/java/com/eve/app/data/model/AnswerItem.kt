package com.eve.app.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/** Result screen ki answer key ke liye ek row. selected = "" matlab unattempted. */
@Parcelize
data class AnswerItem(
    val questionId: String = "",
    val number: Int,
    val questionText: String,
    val selected: String,
    val selectedText: String,
    val correct: String,
    val correctText: String,
    val explanation: String = "",
    val isBookmarked: Boolean = false,
    /** Question ka topic tag (Phase 17) — snapshot at submit-time, taaki baad me question
     * edit/delete ho jaaye tab bhi purane attempts ki Performance stats sahi rahein. */
    val topic: String = "",
    // Hindi translations (Phase 11) — raw values, blank matlab translate nahi hua
    val questionTextHi: String = "",
    val selectedTextHi: String = "",
    val correctTextHi: String = "",
    val explanationHi: String = "",
    val timeTakenSeconds: Long = 0L,
    val optionA: String = "",
    val optionB: String = "",
    val optionC: String = "",
    val optionD: String = "",
    val optionAHi: String = "",
    val optionBHi: String = "",
    val optionCHi: String = "",
    val optionDHi: String = ""
) : Parcelable {
    val isAttempted: Boolean get() = !selected.isNullOrEmpty()
    val isCorrect: Boolean get() = !selected.isNullOrEmpty() && selected.equals(correct, ignoreCase = true)
    val isUnattempted: Boolean get() = selected.isNullOrEmpty()
    val isWrong: Boolean get() = !selected.isNullOrEmpty() && !selected.equals(correct, ignoreCase = true)

    fun optionText(letter: String): String = when (letter) {
        "A" -> optionA ?: ""
        "B" -> optionB ?: ""
        "C" -> optionC ?: ""
        "D" -> optionD ?: ""
        else -> ""
    }

    fun optionTextHi(letter: String): String = when (letter) {
        "A" -> optionAHi ?: ""
        "B" -> optionBHi ?: ""
        "C" -> optionCHi ?: ""
        "D" -> optionDHi ?: ""
        else -> ""
    }

    fun displayOptionText(letter: String, hindi: Boolean): String {
        val hi = optionTextHi(letter)
        val en = optionText(letter)
        return if (hindi && !hi.isNullOrBlank()) hi else (en ?: "")
    }

    fun displayQuestionText(hindi: Boolean): String =
        if (hindi && !questionTextHi.isNullOrBlank()) questionTextHi else (questionText ?: "")

    fun displaySelectedText(hindi: Boolean): String =
        if (hindi && !selectedTextHi.isNullOrBlank()) selectedTextHi else (selectedText ?: "")

    fun displayCorrectText(hindi: Boolean): String =
        if (hindi && !correctTextHi.isNullOrBlank()) correctTextHi else (correctText ?: "")

    fun displayExplanation(hindi: Boolean): String =
        if (hindi && !explanationHi.isNullOrBlank()) explanationHi else (explanation ?: "")
}
