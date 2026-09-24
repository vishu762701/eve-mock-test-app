package com.eve.app.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.airbnb.lottie.LottieAnimationView
import com.eve.app.R
import com.eve.app.databinding.ViewErrorStateBinding

class ErrorStateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    enum class DisplayMode {
        FULL,
        COMPACT
    }

    enum class ErrorType(val defaultTitle: String, val defaultMessage: String) {
        NO_INTERNET(
            "No Internet Connection",
            "Please check your network connection and try again."
        ),
        SERVER_ERROR(
            "Server Error",
            "Something went wrong on our end. Please try again later."
        ),
        EMPTY_DATA(
            "Nothing Found",
            "There are no items to display at this moment."
        ),
        GENERIC(
            "Something Went Wrong",
            "An unexpected error occurred. Please try again."
        )
    }

    val binding: ViewErrorStateBinding =
        ViewErrorStateBinding.inflate(LayoutInflater.from(context), this, true)

    var displayMode: DisplayMode = DisplayMode.FULL
        set(value) {
            field = value
            applyDisplayMode()
        }

    init {
        applyDisplayMode()
    }

    private fun dpToPx(dp: Int): Int =
        (dp * context.resources.displayMetrics.density).toInt()

    private fun applyDisplayMode() {
        val density = context.resources.displayMetrics.density
        when (displayMode) {
            DisplayMode.FULL -> {
                binding.layoutErrorRoot.setPadding(
                    (24 * density).toInt(),
                    (24 * density).toInt(),
                    (24 * density).toInt(),
                    (24 * density).toInt()
                )
                binding.lottieView.layoutParams.width = (180 * density).toInt()
                binding.lottieView.layoutParams.height = (180 * density).toInt()
                binding.lottieView.requestLayout()
                binding.tvErrorTitle.visibility = View.VISIBLE
            }
            DisplayMode.COMPACT -> {
                binding.layoutErrorRoot.setPadding(
                    (12 * density).toInt(),
                    (12 * density).toInt(),
                    (12 * density).toInt(),
                    (12 * density).toInt()
                )
                binding.lottieView.layoutParams.width = (72 * density).toInt()
                binding.lottieView.layoutParams.height = (72 * density).toInt()
                binding.lottieView.requestLayout()
                binding.tvErrorTitle.visibility = View.GONE
            }
        }
    }

    fun show(
        type: ErrorType = ErrorType.GENERIC,
        customMessage: String? = null,
        customTitle: String? = null,
        onRetry: (() -> Unit)? = null
    ) {
        visibility = View.VISIBLE
        binding.tvErrorTitle.text = customTitle ?: type.defaultTitle
        binding.tvErrorMessage.text = customMessage ?: type.defaultMessage

        if (onRetry != null) {
            binding.btnRetry.visibility = View.VISIBLE
            binding.btnRetry.setOnClickListener { onRetry() }
        } else {
            binding.btnRetry.visibility = View.GONE
            binding.btnRetry.setOnClickListener(null)
        }

        if (!binding.lottieView.isAnimating) {
            binding.lottieView.playAnimation()
        }
    }

    fun hide() {
        if (binding.lottieView.isAnimating) {
            binding.lottieView.pauseAnimation()
        }
        visibility = View.GONE
    }
}
