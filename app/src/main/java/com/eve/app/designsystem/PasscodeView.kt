/*
 * Derived from Telegram Android PasscodeView.java:464-960,1822-1857
 * Copyright Nikolai Kudashov, 2013-2023. GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.designsystem

import android.animation.ValueAnimator
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.eve.app.R
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.util.PasscodeManager
import com.eve.app.util.TelegramShake
import org.telegram.ui.Components.CubicBezierInterpolator

/**
 * Exact Telegram Passcode Lock Keypad View.
 * Upstream: PasscodeView.java:800-959
 */
class PasscodeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Int = (v * density + 0.5f).toInt()

    private val contentContainer = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
    }

    val titleTextView = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        includeFontPadding = false
    }

    val subtitleTextView = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        gravity = Gravity.CENTER
        includeFontPadding = false
    }

    private val dotsContainer = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
    }

    private val dotViews = mutableListOf<View>()
    private val enteredPin = StringBuilder()
    private val maxPinLength = 4

    var onPasscodeEntered: ((String) -> Boolean)? = null
    var onSuccess: (() -> Unit)? = null

    private val lettersMap = mapOf(
        1 to "",
        2 to "ABC",
        3 to "DEF",
        4 to "GHI",
        5 to "JKL",
        6 to "MNO",
        7 to "PQRS",
        8 to "TUV",
        9 to "WXYZ",
        0 to "+"
    )

    init {
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        setBackgroundColor(palette.windowBackground)

        addView(
            contentContainer,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        )

        titleTextView.setTextColor(palette.textPrimary)
        titleTextView.text = "Enter Passcode"
        contentContainer.addView(titleTextView)

        subtitleTextView.setTextColor(palette.textSecondary)
        subtitleTextView.text = "Enter your 4-digit passcode"
        val subLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(8f)
            bottomMargin = dp(24f)
        }
        contentContainer.addView(subtitleTextView, subLp)

        // PIN dots
        setupPinDots(palette)
        contentContainer.addView(dotsContainer)

        // 3x4 Number Keypad
        setupKeypad(palette)
    }

    private fun setupPinDots(palette: TelegramThemeEngine.ResolvedPalette) {
        dotsContainer.removeAllViews()
        dotViews.clear()

        val dotSize = dp(14f)
        val dotGap = dp(16f)

        for (i in 0 until maxPinLength) {
            val dot = View(context).apply {
                val shape = GradientDrawable().apply {
                    this.shape = GradientDrawable.OVAL
                    setSize(dotSize, dotSize)
                    setStroke(dp(2f), palette.hairline)
                    setColor(Color.TRANSPARENT)
                }
                background = shape
            }
            val lp = LinearLayout.LayoutParams(dotSize, dotSize).apply {
                if (i > 0) marginStart = dotGap
            }
            dotsContainer.addView(dot, lp)
            dotViews.add(dot)
        }

        val dotsLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(36f)
        }
        dotsContainer.layoutParams = dotsLp
    }

    private fun updatePinDots() {
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        val count = enteredPin.length
        val dotSize = dp(14f)

        for (i in 0 until maxPinLength) {
            val dot = dotViews.getOrNull(i) ?: continue
            val isFilled = i < count
            val shape = GradientDrawable().apply {
                this.shape = GradientDrawable.OVAL
                setSize(dotSize, dotSize)
                if (isFilled) {
                    setColor(palette.accent)
                    setStroke(0, Color.TRANSPARENT)
                } else {
                    setColor(Color.TRANSPARENT)
                    setStroke(dp(2f), palette.hairline)
                }
            }
            dot.background = shape
        }
    }

    private fun setupKeypad(palette: TelegramThemeEngine.ResolvedPalette) {
        val keypadContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val rows = listOf(
            listOf(1, 2, 3),
            listOf(4, 5, 6),
            listOf(7, 8, 9),
            listOf(-1, 0, -2) // -1: Fingerprint, -2: Backspace
        )

        for (row in rows) {
            val rowLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            for (key in row) {
                val button = createKeypadButton(key, palette)
                rowLayout.addView(button)
            }
            val rowLp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(14f)
            }
            keypadContainer.addView(rowLayout, rowLp)
        }

        contentContainer.addView(keypadContainer)
    }

    private fun createKeypadButton(
        key: Int,
        palette: TelegramThemeEngine.ResolvedPalette
    ): View {
        val btnSize = dp(70f)
        val marginH = dp(14f)

        val frame = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(btnSize, btnSize).apply {
                marginStart = marginH
                marginEnd = marginH
            }
            background = TelegramThemeSelectors.getCircleSelectorDrawable(palette.actionSelector)
            isClickable = true
            isFocusable = true
        }

        when (key) {
            in 0..9 -> {
                val container = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                }
                val tvNumber = TextView(context).apply {
                    text = key.toString()
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f)
                    setTextColor(palette.textPrimary)
                    includeFontPadding = false
                    gravity = Gravity.CENTER
                }
                container.addView(tvNumber)

                val letters = lettersMap[key].orEmpty()
                if (letters.isNotEmpty()) {
                    val tvLetters = TextView(context).apply {
                        text = letters
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
                        setTextColor(palette.textSecondary)
                        typeface = Typeface.DEFAULT_BOLD
                        includeFontPadding = false
                        gravity = Gravity.CENTER
                    }
                    container.addView(tvLetters)
                }

                frame.addView(container, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER))
                frame.contentDescription = key.toString()
                frame.setOnClickListener {
                    onDigitPressed(key)
                }
            }
            -1 -> {
                // Fingerprint button
                val iv = ImageView(context).apply {
                    setImageResource(R.drawable.ic_fingerprint)
                    setColorFilter(palette.textPrimary)
                }
                frame.addView(iv, LayoutParams(dp(28f), dp(28f), Gravity.CENTER))
                frame.contentDescription = "Fingerprint Unlock"
                frame.setOnClickListener {
                    promptBiometrics()
                }
                if (!PasscodeManager.isFingerprintEnabled(context)) {
                    frame.visibility = View.INVISIBLE
                }
            }
            -2 -> {
                // Backspace button
                val iv = ImageView(context).apply {
                    setImageResource(R.drawable.ic_close)
                    setColorFilter(palette.textPrimary)
                }
                frame.addView(iv, LayoutParams(dp(22f), dp(22f), Gravity.CENTER))
                frame.contentDescription = "Backspace"
                frame.setOnClickListener {
                    onBackspacePressed()
                }
                frame.setOnLongClickListener {
                    clearPin()
                    true
                }
            }
        }

        // 0.92 scale on touch press
        installTouchScale(frame)

        return frame
    }

    private fun installTouchScale(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> animateScale(v, 0.92f)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> animateScale(v, 1.0f)
            }
            false
        }
    }

    private fun animateScale(view: View, target: Float) {
        ValueAnimator.ofFloat(view.scaleX, target).apply {
            duration = 100L
            interpolator = CubicBezierInterpolator.EASE_OUT_QUINT
            addUpdateListener {
                val s = it.animatedValue as Float
                view.scaleX = s
                view.scaleY = s
            }
            start()
        }
    }

    private fun onDigitPressed(digit: Int) {
        if (enteredPin.length >= maxPinLength) return
        enteredPin.append(digit)
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        updatePinDots()

        if (enteredPin.length == maxPinLength) {
            val pin = enteredPin.toString()
            val isValid = onPasscodeEntered?.invoke(pin) ?: PasscodeManager.checkPasscode(context, pin)
            if (isValid) {
                PasscodeManager.unlock(context)
                onSuccess?.invoke()
            } else {
                showError()
            }
        }
    }

    private fun onBackspacePressed() {
        if (enteredPin.isNotEmpty()) {
            enteredPin.deleteCharAt(enteredPin.length - 1)
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            updatePinDots()
        }
    }

    fun clearPin() {
        enteredPin.clear()
        updatePinDots()
    }

    fun showError() {
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        TelegramShake.shakeViewSpring(dotsContainer)
        postDelayed({
            clearPin()
        }, 350L)
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun promptBiometrics() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val executor = ContextCompat.getMainExecutor(context)
            val cancelSignal = CancellationSignal()
            try {
                val prompt = BiometricPrompt.Builder(context)
                    .setTitle("Unlock Eve")
                    .setSubtitle("Confirm your biometric to unlock")
                    .setNegativeButton("Cancel", executor, DialogInterface.OnClickListener { _, _ ->
                        cancelSignal.cancel()
                    })
                    .build()

                prompt.authenticate(
                    cancelSignal,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                            PasscodeManager.unlock(context)
                            onSuccess?.invoke()
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                            // Biometric cancelled or not recognized
                        }
                    }
                )
            } catch (_: Exception) {
                // Biometrics not enrolled or unavailable
            }
        }
    }
}
