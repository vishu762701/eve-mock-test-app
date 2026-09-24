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

    fun show(title: String = "Nothing here yet", message: String? = null) {
        visibility = View.VISIBLE
        binding.tvEmptyTitle.text = title
        if (!message.isNullOrBlank()) {
            binding.tvEmptyMessage.text = message
            binding.tvEmptyMessage.visibility = View.VISIBLE
        } else {
            binding.tvEmptyMessage.visibility = View.GONE
        }
        if (!binding.lottieEmpty.isAnimating) {
            binding.lottieEmpty.playAnimation()
        }
    }

    fun hide() {
        if (binding.lottieEmpty.isAnimating) {
            binding.lottieEmpty.pauseAnimation()
        }
        visibility = View.GONE
    }
}
