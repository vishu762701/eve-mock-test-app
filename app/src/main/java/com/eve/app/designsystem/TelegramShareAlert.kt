/*
 * Derived from Telegram Android ShareAlert.java:1-850
 * Copyright Nikolai Kudashov, 2013-2023. GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.designsystem

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.FileProvider
import com.eve.app.R
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.util.AppBulletin
import com.eve.app.util.VibrationHelper
import java.io.File
import java.io.FileOutputStream

/**
 * Authentic Telegram Share Sheet (ShareAlert).
 * Upstream: ShareAlert.java
 */
class TelegramShareAlert(
    context: Context,
    private val title: String,
    private val subtitle: String?,
    private val shareText: String,
    private val shareUrl: String?,
    private val previewBitmap: Bitmap?
) : TelegramBottomSheet(context) {

    private fun dp(value: Float): Int = (value * context.resources.displayMetrics.density + 0.5f).toInt()

    init {
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16f), dp(8f), dp(16f), dp(20f))
        }

        // 1. Preview Card
        val previewCard = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12f), dp(12f), dp(12f), dp(12f))
            background = GradientDrawable().apply {
                cornerRadius = dp(12f).toFloat()
                setColor(palette.surfaceInset)
                setStroke(dp(1f), palette.hairline)
            }
        }

        if (previewBitmap != null) {
            val ivPreview = ImageView(context).apply {
                setImageBitmap(previewBitmap)
                scaleType = ImageView.ScaleType.CENTER_CROP
                background = GradientDrawable().apply {
                    cornerRadius = dp(8f).toFloat()
                    setColor(palette.hairline)
                }
                clipToOutline = true
            }
            previewCard.addView(ivPreview, LinearLayout.LayoutParams(dp(48f), dp(48f)))
        }

        val textContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(if (previewBitmap != null) dp(12f) else 0, 0, 0, 0)
        }

        val tvTitle = TextView(context).apply {
            text = title
            textSize = 15f
            setTextColor(palette.textPrimary)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            maxLines = 1
        }
        textContainer.addView(tvTitle)

        if (!subtitle.isNullOrBlank()) {
            val tvSub = TextView(context).apply {
                text = subtitle
                textSize = 13f
                setTextColor(palette.textSecondary)
                maxLines = 2
                setPadding(0, dp(2f), 0, 0)
            }
            textContainer.addView(tvSub)
        }

        previewCard.addView(textContainer, LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        ))
        root.addView(previewCard, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(16f)
        })

        // 2. Destinations Row
        val destinationsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(4f), 0, dp(16f))
        }

        // Destination: Copy Link
        val btnCopy = createDestinationButton(
            iconRes = R.drawable.ic_copy,
            label = "Copy Link",
            bgColor = palette.accent,
            iconTint = 0xFFFFFFFF.toInt()
        ) {
            val target = shareUrl ?: shareText
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("EVE Share", target))
            VibrationHelper.vibrateLightHaptic(context)
            AppBulletin.showSuccess(context, "Link copied to clipboard")
            dismiss()
        }
        destinationsRow.addView(btnCopy, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        // Destination: Telegram
        val btnTelegram = createDestinationButton(
            iconRes = R.drawable.ic_send_plane,
            label = "Telegram",
            bgColor = 0xFF229AF0.toInt(),
            iconTint = 0xFFFFFFFF.toInt()
        ) {
            shareToSpecificPackage("org.telegram.messenger")
        }
        destinationsRow.addView(btnTelegram, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        // Destination: WhatsApp
        val btnWhatsApp = createDestinationButton(
            iconRes = R.drawable.ic_send_plane,
            label = "WhatsApp",
            bgColor = 0xFF25D366.toInt(),
            iconTint = 0xFFFFFFFF.toInt()
        ) {
            shareToSpecificPackage("com.whatsapp")
        }
        destinationsRow.addView(btnWhatsApp, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        // Destination: More / System Chooser
        val btnMore = createDestinationButton(
            iconRes = R.drawable.ic_share,
            label = "More",
            bgColor = palette.surfaceInset,
            iconTint = palette.textPrimary
        ) {
            shareToSystemChooser()
        }
        destinationsRow.addView(btnMore, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        root.addView(destinationsRow)

        // 3. Optional Comment Input
        val commentInput = EditText(context).apply {
            hint = "Add a comment..."
            textSize = 14f
            setTextColor(palette.textPrimary)
            setHintTextColor(palette.textSecondary)
            setPadding(dp(14f), dp(10f), dp(14f), dp(10f))
            background = GradientDrawable().apply {
                cornerRadius = dp(10f).toFloat()
                setColor(palette.surfaceInset)
            }
            maxLines = 3
        }
        root.addView(commentInput, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(12f)
        })

        // 4. Send / Share Button
        val btnShare = AppCompatButton(context).apply {
            text = "Share"
            textSize = 15f
            setTextColor(0xFFFFFFFF.toInt())
            background = GradientDrawable().apply {
                cornerRadius = dp(10f).toFloat()
                setColor(palette.accent)
            }
            isAllCaps = false
            setOnClickListener {
                val comment = commentInput.text.toString().trim()
                val fullText = if (comment.isNotEmpty()) "$comment\n\n$shareText" else shareText
                shareToSystemChooser(fullText)
            }
        }
        root.addView(btnShare, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(48f)
        ))

        setCustomView(root)
    }

    private fun createDestinationButton(
        iconRes: Int,
        label: String,
        bgColor: Int,
        iconTint: Int,
        onClick: () -> Unit
    ): LinearLayout {
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }

        val circle = FrameLayout(context).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(bgColor)
            }
            val iv = ImageView(context).apply {
                setImageResource(iconRes)
                setColorFilter(iconTint)
            }
            addView(iv, FrameLayout.LayoutParams(dp(24f), dp(24f), Gravity.CENTER))
        }
        container.addView(circle, LinearLayout.LayoutParams(dp(52f), dp(52f)))

        val tv = TextView(context).apply {
            text = label
            textSize = 12f
            setTextColor(palette.textPrimary)
            gravity = Gravity.CENTER
            setPadding(0, dp(6f), 0, 0)
            maxLines = 1
        }
        container.addView(tv, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        return container
    }

    private fun shareToSpecificPackage(pkg: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
                if (!shareUrl.isNullOrBlank()) {
                    putExtra(Intent.EXTRA_SUBJECT, title)
                }
                setPackage(pkg)
            }
            context.startActivity(intent)
            dismiss()
        } catch (_: Exception) {
            shareToSystemChooser()
        }
    }

    private fun shareToSystemChooser(overrideText: String? = null) {
        val targetText = overrideText ?: shareText
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, targetText)
            putExtra(Intent.EXTRA_SUBJECT, title)
        }
        context.startActivity(Intent.createChooser(intent, "Share with..."))
        dismiss()
    }

    companion object {
        fun show(
            context: Context,
            title: String,
            subtitle: String? = null,
            shareText: String,
            shareUrl: String? = null,
            previewBitmap: Bitmap? = null
        ) {
            val sheet = TelegramShareAlert(context, title, subtitle, shareText, shareUrl, previewBitmap)
            sheet.show()
        }
    }
}
