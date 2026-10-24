package com.eve.app.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.eve.app.databinding.ViewEmptyStateBinding

class EmptyStateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    val binding: ViewEmptyStateBinding =
        ViewEmptyStateBinding.inflate(LayoutInflater.from(context), this, true)

    enum class DisplayMode {
        FULL,
        COMPACT
    }

    var displayMode: DisplayMode = DisplayMode.FULL
        set(value) {
            field = value
            applyDisplayMode()
        }

    init {
        applyDisplayMode()
    }

    private fun applyDisplayMode() {
        val density = context.resources.displayMetrics.density
        val padding = resources.getDimensionPixelSize(com.eve.app.R.dimen.design_card_padding)
        when (displayMode) {
            DisplayMode.FULL -> {
                binding.layoutEmptyRoot.setPadding(padding, padding, padding, padding)
                binding.lottieEmpty.layoutParams.width = (140 * density).toInt()
                binding.lottieEmpty.layoutParams.height = (140 * density).toInt()
                binding.lottieEmpty.requestLayout()
                com.eve.app.designsystem.DesignType.apply(binding.tvEmptyTitle, com.eve.app.designsystem.DesignType.Role.HEADING)
            }
            DisplayMode.COMPACT -> {
                binding.layoutEmptyRoot.setPadding(padding, padding, padding, padding)
                binding.lottieEmpty.layoutParams.width = (76 * density).toInt()
                binding.lottieEmpty.layoutParams.height = (76 * density).toInt()
                binding.lottieEmpty.requestLayout()
                com.eve.app.designsystem.DesignType.apply(binding.tvEmptyTitle, com.eve.app.designsystem.DesignType.Role.SECONDARY)
            }
        }
    }

    private var hasPlayedOnce = false

    fun show(title: String = "No files found", message: String? = null) {
        visibility = View.VISIBLE
        binding.tvEmptyTitle.text = title
        if (!message.isNullOrBlank()) {
            binding.tvEmptyMessage.text = message
            binding.tvEmptyMessage.visibility = View.VISIBLE
        } else {
            binding.tvEmptyMessage.visibility = View.GONE
        }
        hasPlayedOnce = com.eve.app.util.EmptyStateAnimationHelper.showEmptyState(binding.lottieEmpty, hasPlayedOnce)
    }

    fun resetPlayOnNextShow() {
        hasPlayedOnce = false
    }

    fun resetPlayback() {
        hasPlayedOnce = false
    }

    fun hide() {
        com.eve.app.util.EmptyStateAnimationHelper.stopEmptyState(binding.lottieEmpty)
        visibility = View.GONE
    }

    override fun onSaveInstanceState(): android.os.Parcelable {
        val superState = super.onSaveInstanceState()
        val bundle = android.os.Bundle()
        bundle.putParcelable("super_state", superState)
        bundle.putInt("saved_visibility", visibility)
        bundle.putBoolean("has_played_once", hasPlayedOnce)
        return bundle
    }

    override fun onRestoreInstanceState(state: android.os.Parcelable?) {
        var superState = state
        if (state is android.os.Bundle) {
            val savedVis = state.getInt("saved_visibility", View.GONE)
            hasPlayedOnce = state.getBoolean("has_played_once", false)
            visibility = savedVis
            if (savedVis == View.VISIBLE) {
                com.eve.app.util.EmptyStateAnimationHelper.showEmptyState(binding.lottieEmpty, hasPlayedOnce)
            }
            superState = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                state.getParcelable("super_state", android.os.Parcelable::class.java)
            } else {
                @Suppress("DEPRECATION")
                state.getParcelable("super_state")
            }
        }
        super.onRestoreInstanceState(superState)
    }
}
