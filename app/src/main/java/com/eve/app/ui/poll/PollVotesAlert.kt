/*
 * Derived from Telegram Android PollVotesAlert.java, Copyright Nikolai Kudashov.
 * GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.poll

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import com.eve.app.data.model.Poll
import com.eve.app.databinding.ItemPollOptionResultBinding
import com.eve.app.designsystem.TelegramBottomSheet
import com.eve.app.theme.TelegramThemeEngine

object PollVotesAlert {

    fun show(context: Context, poll: Poll): TelegramBottomSheet {
        val sheet = TelegramBottomSheet(context)
        val density = context.resources.displayMetrics.density
        fun dp(v: Float) = (v * density).toInt()

        val palette = TelegramThemeEngine.resolveCurrentPalette(context)

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(21f), dp(16f), dp(21f), dp(24f))
        }

        // Title / Question
        val tvQuestion = TextView(context).apply {
            text = poll.question
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(palette.textPrimary)
        }
        root.addView(tvQuestion)

        // Subtitle / Type & Total votes
        val typeStr = if (poll.quizMode) "Quiz" else "Anonymous Poll"
        val tvSub = TextView(context).apply {
            text = "$typeStr • ${poll.totalVotes} votes"
            textSize = 13f
            setTextColor(palette.textSecondary)
            setPadding(0, dp(4f), 0, dp(16f))
        }
        root.addView(tvSub)

        // Options
        val maxVotes = poll.options.indices.maxOfOrNull { poll.getVotesForOption(it) } ?: 0
        val inflater = LayoutInflater.from(context)

        for (index in poll.options.indices) {
            val optionText = poll.options[index]
            val votes = poll.getVotesForOption(index)
            val percent = poll.getPercentageForOption(index)
            val isLeading = votes == maxVotes && votes > 0
            val isCorrect = poll.quizMode && index == poll.correctOptionIndex

            val rowBinding = ItemPollOptionResultBinding.inflate(inflater, root, false)
            rowBinding.tvOptionTitle.text = optionText
            rowBinding.tvOptionPercent.text = "$percent%"
            rowBinding.tvOptionVotes.text = "$votes votes"

            if (isCorrect) {
                rowBinding.ivOptionCheck.visibility = View.VISIBLE
                rowBinding.ivOptionCheck.setChecked(true, false)
                rowBinding.ivOptionCheck.setColors(palette.textSecondary, 0xFF34C759.toInt(), android.graphics.Color.WHITE)
                rowBinding.tvOptionTitle.setTextColor(0xFF34C759.toInt())
                rowBinding.tvOptionPercent.setTextColor(0xFF34C759.toInt())
                rowBinding.pbOption.progressColor = 0xFF34C759.toInt()
                rowBinding.pbOption.backColor = ColorUtils.setAlphaComponent(0xFF34C759.toInt(), 40)
            } else if (isLeading) {
                rowBinding.tvOptionTitle.setTextColor(palette.accent)
                rowBinding.tvOptionPercent.setTextColor(palette.accent)
                rowBinding.pbOption.progressColor = palette.accent
                rowBinding.pbOption.backColor = ColorUtils.setAlphaComponent(palette.accent, 40)
            } else {
                rowBinding.tvOptionTitle.setTextColor(palette.textPrimary)
                rowBinding.tvOptionPercent.setTextColor(palette.textSecondary)
                rowBinding.pbOption.progressColor = palette.textSecondary
                rowBinding.pbOption.backColor = ColorUtils.setAlphaComponent(palette.textSecondary, 30)
            }

            rowBinding.pbOption.progress = percent
            root.addView(rowBinding.root)
        }

        // Quiz Explanation
        if (poll.quizMode && poll.explanation.isNotBlank()) {
            val tvExplTitle = TextView(context).apply {
                text = "EXPLANATION"
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(palette.accent)
                setPadding(0, dp(16f), 0, dp(4f))
            }
            root.addView(tvExplTitle)

            val tvExpl = TextView(context).apply {
                text = poll.explanation
                textSize = 14f
                setTextColor(palette.textPrimary)
            }
            root.addView(tvExpl)
        }

        val scroll = ScrollView(context).apply {
            addView(root)
        }
        sheet.setCustomView(scroll)
        sheet.show()
        return sheet
    }
}
