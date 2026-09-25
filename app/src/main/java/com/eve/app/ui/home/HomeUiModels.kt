package com.eve.app.ui.home

import com.eve.app.data.model.Exam
import com.eve.app.data.model.FeedbackPost

/** RecyclerView row: category header, exam card, ya feedback post card */
sealed class HomeListItem {
    data class Header(val title: String) : HomeListItem()
    data class ExamRow(
        val exam: Exam,
        val attempted: Boolean = false,
        val isPinned: Boolean = false
    ) : HomeListItem()
    data class FeedbackPostRow(val post: FeedbackPost) : HomeListItem()
}

/** Home screen ka poora render-ready data. */
data class HomeUiData(
    val categories: List<String>,
    val selectedCategory: String,
    val items: List<HomeListItem>
)
