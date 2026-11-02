package com.eve.app.ui.admin

import com.eve.app.data.model.Question

/** EVE question-bank identity adapter; never guess a replacement for a known ID. */
internal fun resolveQuestionEditorTarget(
    questions: List<Question>, examId: String?, questionId: String?, questionText: String?,
    legacyId: String? = null
): Question? {
    if (examId.isNullOrBlank()) return null
    val scoped = questions.filter { it.examId == examId && !it.id.isNullOrBlank() }
    if (!questionId.isNullOrBlank()) return scoped.singleOrNull { it.id == questionId }
    if (!legacyId.isNullOrBlank()) {
        val legacyMatches = scoped.filter { it.id == legacyId }
        if (legacyMatches.isNotEmpty()) return legacyMatches.singleOrNull()
    }
    if (questionText.isNullOrBlank()) return null
    return scoped.singleOrNull { it.questionText == questionText }
}
