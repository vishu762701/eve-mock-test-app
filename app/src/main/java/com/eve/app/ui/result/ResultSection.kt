package com.eve.app.ui.result

/** The approved segmented-tab order, shared by selection and restoration. */
enum class ResultSection {
    OVERVIEW, REVIEW, LEADERBOARD;

    companion object {
        fun fromTab(position: Int): ResultSection = values().getOrElse(position) { OVERVIEW }
    }
}
