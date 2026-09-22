package com.eve.app.ui.home

import com.eve.app.data.model.Exam

/** RecyclerView row: ya to ek category header, ya ek exam card */
sealed class HomeListItem {
    data class Header(val title: String) : HomeListItem()
    data class ExamRow(val exam: Exam) : HomeListItem()
}

/** Home screen ka poora render-ready data: chips ke liye categories + list ke liye rows */
data class HomeUiData(
    val categories: List<String>, // "All" + har distinct category, alphabetically
    val selectedCategory: String,
    val items: List<HomeListItem>
)
