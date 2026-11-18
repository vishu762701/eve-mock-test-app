/*
 * Telegram: TMessagesProj/src/main/java/org/telegram/ui/Components/NumberPicker.java:50-250 @ 3de37c8cfb03
 * Telegram: TMessagesProj/src/main/java/org/telegram/ui/Components/EditTextBoldCursor.java:40-180 @ 3de37c8cfb03
 * Derived from Telegram Android NumberPicker / NumberTextView / ButtonBounce
 * DrKLO/Telegram@21f7d109da6ab3506c9e9577f98c6a38771ab335
 * Copyright Nikolai Kudashov, 2013-2018; GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.designsystem

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import com.eve.app.R
import com.eve.app.theme.TelegramThemeEngine

/**
 * 1:1 Telegram number stepper field.
 * Equipped with authentic Telegram ButtonBounce touch physics, hold-to-accelerate repeat,
 * and Telegram theme styling.
 */
open class TelegramNumberField @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    companion object {
        const val HOLD_DELAY = 400L
        const val HOLD_FASTEST = 40L
        const val DECAY_BASE = 0.86f
    }

    var min: Int = 1
        set(v) {
            field = v
            updateButtonStates()
        }

    var max: Int = 200
        set(v) {
            field = v
            updateButtonStates()
        }

    var step: Int = 1

    private var boundEditText: EditText? = null
    private var isUpdatingText = false

    private val btnMinus: ImageView
    private val btnPlus: ImageView

    private val bounceMinus: ButtonBounce
    private val bouncePlus: ButtonBounce

    private val handler = Handler(Looper.getMainLooper())
    private var holdRunnable: Runnable? = null
    private var currentInterval = HOLD_DELAY

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        clipChildren = false
        clipToPadding = false

        val density = resources.displayMetrics.density
        val btnSize = (36f * density + 0.5f).toInt()
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)

        btnMinus = ImageView(context).apply {
            setImageResource(R.drawable.msg_remove)
            setColorFilter(palette.accent)
            contentDescription = "Decrease count"
            isClickable = true
            isFocusable = true
            background = TelegramThemeSelectors.getRoundRectSelectorDrawable(
                btnSize / 2f,
                palette.actionSelector
            )
            layoutParams = LayoutParams(btnSize, btnSize).apply {
                gravity = Gravity.CENTER_VERTICAL
            }
        }

        btnPlus = ImageView(context).apply {
            setImageResource(R.drawable.msg_add)
            setColorFilter(palette.accent)
            contentDescription = "Increase count"
            isClickable = true
            isFocusable = true
            background = TelegramThemeSelectors.getRoundRectSelectorDrawable(
                btnSize / 2f,
                palette.actionSelector
            )
            layoutParams = LayoutParams(btnSize, btnSize).apply {
                gravity = Gravity.CENTER_VERTICAL
            }
        }

        bounceMinus = ButtonBounce(btnMinus)
        bouncePlus = ButtonBounce(btnPlus)

        setupTouchHandler(btnMinus, bounceMinus, isIncrement = false)
        setupTouchHandler(btnPlus, bouncePlus, isIncrement = true)

        addView(btnMinus)
        // Space between buttons when attached around an edit text
        val spacer = View(context).apply {
            layoutParams = LayoutParams(0, 1, 1f)
        }
        addView(spacer)
        addView(btnPlus)
    }

    private fun setupTouchHandler(button: View, bounce: ButtonBounce, isIncrement: Boolean) {
        button.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    bounce.setPressed(true)
                    triggerStep(isIncrement)
                    startHold(isIncrement)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    bounce.setPressed(false)
                    stopHold()
                    v.performClick()
                    true
                }
                else -> false
            }
        }
    }

    fun attachEditText(editText: EditText, minVal: Int = 1, maxVal: Int = 200, stepVal: Int = 1) {
        this.boundEditText = editText
        this.min = minVal
        this.max = maxVal
        this.step = stepVal

        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdatingText) return
                updateButtonStates()
            }
        })
        updateButtonStates()
    }

    private fun getCurrentValue(): Int {
        val text = boundEditText?.text?.toString() ?: return min
        return text.toIntOrNull() ?: min
    }

    fun setValue(value: Int) {
        val clamped = value.coerceIn(min, max)
        isUpdatingText = true
        boundEditText?.setText(clamped.toString())
        boundEditText?.setSelection(boundEditText?.text?.length ?: 0)
        isUpdatingText = false
        updateButtonStates()
    }

    private fun triggerStep(isIncrement: Boolean) {
        val current = getCurrentValue()
        val next = if (isIncrement) current + step else current - step
        setValue(next)
    }

    private fun startHold(isIncrement: Boolean) {
        stopHold()
        currentInterval = HOLD_DELAY
        val runnable = object : Runnable {
            override fun run() {
                triggerStep(isIncrement)
                currentInterval = (currentInterval * DECAY_BASE).toLong().coerceAtLeast(HOLD_FASTEST)
                handler.postDelayed(this, currentInterval)
            }
        }
        holdRunnable = runnable
        handler.postDelayed(runnable, currentInterval)
    }

    private fun stopHold() {
        holdRunnable?.let { handler.removeCallbacks(it) }
        holdRunnable = null
    }

    private fun updateButtonStates() {
        val current = getCurrentValue()
        btnMinus.isEnabled = current > min
        btnMinus.alpha = if (btnMinus.isEnabled) 1.0f else 0.4f
        btnPlus.isEnabled = current < max
        btnPlus.alpha = if (btnPlus.isEnabled) 1.0f else 0.4f
    }

    override fun onDetachedFromWindow() {
        stopHold()
        super.onDetachedFromWindow()
    }
}
