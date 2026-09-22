package com.eve.app.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/** Result screen ki answer key ke liye ek row. selected = "" matlab unattempted. */
@Parcelize
data class AnswerItem(
    val number: Int,
    val questionText: String,
    val selected: String,
    val selectedText: String,
    val correct: String,
    val correctText: String
) : Parcelable {
    val isAttempted: Boolean get() = selected.isNotEmpty()
    val isCorrect: Boolean get() = selected.isNotEmpty() && selected == correct
}
