package com.eve.app.ui.test

/** R09: conceal future question navigation only; all exam questions stay loaded.
 * No answer state, timer, backend permissions or correctness is altered. */
object ProgressiveQuestionStrip {
    fun restoreFurthest(savedIndex: Int, currentIndex: Int, visited: Set<Int>): Int =
        maxOf(0, savedIndex, currentIndex, visited.maxOrNull() ?: 0)

    fun visibleCount(totalQuestions: Int, furthestRevealedIndex: Int): Int =
        if (totalQuestions <= 0) 0
        else furthestRevealedIndex.coerceIn(0, totalQuestions - 1) + 1
}
