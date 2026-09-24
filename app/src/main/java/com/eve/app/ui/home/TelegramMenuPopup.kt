package com.eve.app.ui.home

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.eve.app.R
import com.eve.app.databinding.PopupTelegramMenuBinding
import com.eve.app.util.ThemeManager

class TelegramMenuPopup(
    private val context: Context,
    private val onThemeToggle: () -> Unit,
    private val onHistory: () -> Unit,
    private val onPerformance: () -> Unit,
    private val onTopic: () -> Unit,
    private val onPyq: () -> Unit,
    private val onSyllabus: () -> Unit,
    private val onLeaderboard: () -> Unit,
    private val onLogout: () -> Unit
) : PopupWindow(context) {

    private val binding: PopupTelegramMenuBinding =
        PopupTelegramMenuBinding.inflate(LayoutInflater.from(context))
    private var isDismissing = false

    init {
        contentView = binding.root
        width = ViewGroup.LayoutParams.WRAP_CONTENT
        height = ViewGroup.LayoutParams.WRAP_CONTENT
        isFocusable = true
        isOutsideTouchable = true
        elevation = 20f
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        setupThemeCard()
        setupListeners()
    }

    private fun setupThemeCard() {
        val isDark = ThemeManager.isDarkMode(context)
        if (isDark) {
            binding.ivThemeIcon.setImageResource(R.drawable.ic_sun)
            binding.tvThemeTitle.text = "Day Mode"
        } else {
            binding.ivThemeIcon.setImageResource(R.drawable.ic_moon)
            binding.tvThemeTitle.text = "Dark Mode"
        }
    }

    private fun setupListeners() {
        binding.cardTheme.setOnClickListener {
            binding.ivThemeIcon.animate()
                .rotationBy(360f)
                .scaleX(0.75f)
                .scaleY(0.75f)
                .setDuration(180)
                .withEndAction {
                    binding.ivThemeIcon.scaleX = 1f
                    binding.ivThemeIcon.scaleY = 1f
                    dismissWithAction { onThemeToggle() }
                }
                .start()
        }
        binding.menuRowHistory.setOnClickListener {
            dismissWithAction { onHistory() }
        }
        binding.menuRowPerformance.setOnClickListener {
            dismissWithAction { onPerformance() }
        }
        binding.menuRowTopic.setOnClickListener {
            dismissWithAction { onTopic() }
        }
        binding.menuRowPyq.setOnClickListener {
            dismissWithAction { onPyq() }
        }
        binding.menuRowSyllabus.setOnClickListener {
            dismissWithAction { onSyllabus() }
        }
        binding.menuRowLeaderboard.setOnClickListener {
            dismissWithAction { onLeaderboard() }
        }
        binding.menuRowLogout.setOnClickListener {
            dismissWithAction { onLogout() }
        }
    }

    fun show(anchorView: View) {
        if (isShowing) return
        isDismissing = false
        setupThemeCard()

        // Anchor below the 3-dot icon, aligned to right edge
        showAsDropDown(anchorView, -binding.root.paddingStart, 8)

        binding.root.post {
            binding.root.pivotX = binding.root.width.toFloat()
            binding.root.pivotY = 0f
            binding.root.scaleX = 0.85f
            binding.root.scaleY = 0.85f
            binding.root.alpha = 0f

            binding.root.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setDuration(200)
                .setInterpolator(FastOutSlowInInterpolator())
                .start()
        }
    }

    private fun dismissWithAction(action: () -> Unit) {
        if (isDismissing) return
        isDismissing = true
        binding.root.pivotX = binding.root.width.toFloat()
        binding.root.pivotY = 0f
        binding.root.animate()
            .scaleX(0.85f)
            .scaleY(0.85f)
            .alpha(0f)
            .setDuration(150)
            .setInterpolator(FastOutSlowInInterpolator())
            .withEndAction {
                isDismissing = false
                super.dismiss()
                action()
            }
            .start()
    }

    override fun dismiss() {
        if (isDismissing) return
        isDismissing = true
        binding.root.pivotX = binding.root.width.toFloat()
        binding.root.pivotY = 0f
        binding.root.animate()
            .scaleX(0.85f)
            .scaleY(0.85f)
            .alpha(0f)
            .setDuration(150)
            .setInterpolator(FastOutSlowInInterpolator())
            .withEndAction {
                isDismissing = false
                super.dismiss()
            }
            .start()
    }
}
