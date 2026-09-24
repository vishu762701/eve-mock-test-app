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
        when (displayMode) {
            DisplayMode.FULL -> {
                binding.layoutEmptyRoot.setPadding(
                    (24 * density).toInt(),
                    (24 * density).toInt(),
                    (24 * density).toInt(),
                    (24 * density).toInt()
                )
                binding.lottieEmpty.layoutParams.width = (140 * density).toInt()
                binding.lottieEmpty.layoutParams.height = (140 * density).toInt()
                binding.lottieEmpty.requestLayout()
                binding.tvEmptyTitle.textSize = 17f
            }
            DisplayMode.COMPACT -> {
                binding.layoutEmptyRoot.setPadding(
                    (12 * density).toInt(),
                    (12 * density).toInt(),
                    (12 * density).toInt(),
                    (12 * density).toInt()
                )
                binding.lottieEmpty.layoutParams.width = (76 * density).toInt()
                binding.lottieEmpty.layoutParams.height = (76 * density).toInt()
                binding.lottieEmpty.requestLayout()
                binding.tvEmptyTitle.textSize = 14f
            }
        }
    }

    private var hasPlayedOnce: Boolean = false

    fun show(title: String = "Nothing here yet", message: String? = null) {
        visibility = View.VISIBLE
        binding.tvEmptyTitle.text = title
        if (!message.isNullOrBlank()) {
            binding.tvEmptyMessage.text = message
            binding.tvEmptyMessage.visibility = View.VISIBLE
        } else {
            binding.tvEmptyMessage.visibility = View.GONE
        }
        hasPlayedOnce = com.eve.app.util.EmptyStateAnimationHelper.showEmptyState(
            binding.lottieEmpty,
            hasPlayedOnce
        )
    }

    fun hide() {
        if (binding.lottieEmpty.isAnimating) {
            binding.lottieEmpty.pauseAnimation()
        }
        visibility = View.GONE
        hasPlayedOnce = false
    }

    override fun onSaveInstanceState(): android.os.Parcelable {
        val superState = super.onSaveInstanceState()
        val bundle = android.os.Bundle()
        bundle.putParcelable("super_state", superState)
        bundle.putBoolean("has_played_once", hasPlayedOnce)
        return bundle
    }

    override fun onRestoreInstanceState(state: android.os.Parcelable?) {
        var superState = state
        if (state is android.os.Bundle) {
            hasPlayedOnce = state.getBoolean("has_played_once", false)
            superState = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                state.getParcelable("super_state", android.os.Parcelable::class.java)
            } else {
                @Suppress("DEPRECATION")
                state.getParcelable("super_state")
            }
        }
        super.onRestoreInstanceState(superState)
        if (hasPlayedOnce && visibility == View.VISIBLE) {
            binding.lottieEmpty.progress = 1.0f
        }
    }
}
