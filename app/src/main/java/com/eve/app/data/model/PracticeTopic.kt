package com.eve.app.data.model

/** A selectable topic with the number of available questions for the practice set. */
data class PracticeTopic(
    val name: String,
    val questionCount: Int
)
