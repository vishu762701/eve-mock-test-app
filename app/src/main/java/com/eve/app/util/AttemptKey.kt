package com.eve.app.util

object AttemptKey {
    const val SEP = "__"

    fun forTest(sourceExamId: String, testId: String): String {
        return "$sourceExamId$SEP$testId"
    }

    fun sourceExamId(key: String): String {
        return if (key.contains(SEP)) key.substringBefore(SEP) else key
    }

    fun generatedTestId(key: String): String? {
        if (!key.contains(SEP)) return null
        val testId = key.substringAfter(SEP)
        return testId.ifBlank { null }
    }

    fun isTestKey(key: String): Boolean {
        return key.contains(SEP) && !generatedTestId(key).isNullOrBlank()
    }

    fun canonicalQuestionId(testId: String, index: Int): String {
        return "${testId.trim()}_$index"
    }

    fun parseGeneratedQuestionId(questionId: String): Pair<String, Int>? {
        val trimmed = questionId.trim()
        val lastUnderscore = trimmed.lastIndexOf('_')
        if (lastUnderscore <= 0) return null
        val testId = trimmed.substring(0, lastUnderscore)
        val idx = trimmed.substring(lastUnderscore + 1).toIntOrNull() ?: return null
        if (idx < 0) return null
        return Pair(testId, idx)
    }
}
