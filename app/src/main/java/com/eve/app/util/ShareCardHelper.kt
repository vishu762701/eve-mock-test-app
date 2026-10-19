package com.eve.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object ShareCardHelper {

    fun generateAndShare(
        context: Context,
        examName: String,
        scoreText: String,
        percentageText: String,
        accuracyText: String,
        rankText: String,
        correctCount: Int,
        wrongCount: Int,
        skippedCount: Int
    ) {
        val width = 1080
        val height = 1350
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val darkConfiguration = android.content.res.Configuration(context.resources.configuration).apply {
            uiMode = uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK.inv() or
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        }
        val darkContext = context.createConfigurationContext(darkConfiguration)
        val bgDark = androidx.core.content.ContextCompat.getColor(darkContext, com.eve.app.R.color.bg_app)
        val surfaceDark = androidx.core.content.ContextCompat.getColor(darkContext, com.eve.app.R.color.surface)
        val labelSecDark = androidx.core.content.ContextCompat.getColor(darkContext, com.eve.app.R.color.text_secondary)
        val labelTerDark = androidx.core.content.ContextCompat.getColor(darkContext, com.eve.app.R.color.text_secondary)
        val accentContainerDark = androidx.core.content.ContextCompat.getColor(darkContext, com.eve.app.R.color.system_blue_container)
        val successDark = androidx.core.content.ContextCompat.getColor(darkContext, com.eve.app.R.color.system_green)
        val errorDark = androidx.core.content.ContextCompat.getColor(darkContext, com.eve.app.R.color.system_red)
        val systemBlue = androidx.core.content.ContextCompat.getColor(darkContext, com.eve.app.R.color.system_blue)

        val cardRadius = context.resources.getDimension(com.eve.app.R.dimen.design_radius_card) /
            context.resources.displayMetrics.density * (width / 360f)

        val boldTypeface = FontManager.typeface(context, bold = true)
        val regularTypeface = FontManager.typeface(context, bold = false)
        // Export on a fixed 360dp design canvas, independently of the device density.
        fun typeSize(token: Int): Float {
            val value = android.util.TypedValue()
            context.resources.getValue(token, value, true)
            return android.util.TypedValue.complexToFloat(value.data) * (width / 360f)
        }

        // 1. Dark Background
        val bgPaint = Paint().apply {
            color = bgDark
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. Card Container
        val cardPaint = Paint().apply {
            color = surfaceDark
            style = Paint.Style.FILL
        }

        val cardRect = RectF(60f, 60f, width - 60f, height - 60f)
        canvas.drawRoundRect(cardRect, cardRadius, cardRadius, cardPaint)

        // 3. App Title / Header
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = systemBlue
            textSize = typeSize(com.eve.app.R.dimen.design_type_heading)
            typeface = boldTypeface
            letterSpacing = 0.15f
        }
        canvas.drawText("EVE MOCK TEST", 120f, 150f, brandPaint)

        // 4. Test Name
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = androidx.core.content.ContextCompat.getColor(darkContext, com.eve.app.R.color.text_primary)
            textSize = typeSize(com.eve.app.R.dimen.design_type_title)
            typeface = boldTypeface
        }
        val truncatedName = if (examName.length > 36) examName.take(33) + "..." else examName
        canvas.drawText(truncatedName, 120f, 220f, titlePaint)

        // 5. Score Label & Value
        val scoreLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = labelSecDark
            textSize = typeSize(com.eve.app.R.dimen.design_type_secondary)
            typeface = regularTypeface
            letterSpacing = 0.08f
        }
        canvas.drawText("TOTAL SCORE", 120f, 330f, scoreLabelPaint)

        val scoreValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = androidx.core.content.ContextCompat.getColor(darkContext, com.eve.app.R.color.text_primary)
            textSize = typeSize(com.eve.app.R.dimen.design_type_title)
            typeface = boldTypeface
        }
        canvas.drawText(scoreText, 120f, 440f, scoreValuePaint)

        // Percentage & Accuracy Pills
        val pillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentContainerDark
            style = Paint.Style.FILL
        }
        val pillTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = systemBlue
            textSize = typeSize(com.eve.app.R.dimen.design_type_secondary)
            typeface = boldTypeface
        }
        val pill1Rect = RectF(120f, 475f, 340f, 530f)
        canvas.drawRoundRect(pill1Rect, EveShapes.capsule(pill1Rect.height()), EveShapes.capsule(pill1Rect.height()), pillBgPaint)
        canvas.drawText(percentageText, 140f, 513f, pillTextPaint)

        val pill2Rect = RectF(360f, 475f, 600f, 530f)
        canvas.drawRoundRect(pill2Rect, EveShapes.capsule(pill2Rect.height()), EveShapes.capsule(pill2Rect.height()), pillBgPaint)
        canvas.drawText("Acc: $accuracyText", 380f, 513f, pillTextPaint)

        // 6. Performance Breakdown Boxes (Correct, Wrong, Skipped)
        val boxWidth = (width - 240f - 40f) / 3f
        val boxY = 570f
        val boxH = 160f

        drawStatBox(canvas, 120f, boxY, boxWidth, boxH, "Correct", correctCount.toString(), successDark, boldTypeface, regularTypeface, cardRadius, bgDark, labelSecDark, typeSize(com.eve.app.R.dimen.design_type_heading), typeSize(com.eve.app.R.dimen.design_type_secondary))
        drawStatBox(canvas, 120f + boxWidth + 20f, boxY, boxWidth, boxH, "Wrong", wrongCount.toString(), errorDark, boldTypeface, regularTypeface, cardRadius, bgDark, labelSecDark, typeSize(com.eve.app.R.dimen.design_type_heading), typeSize(com.eve.app.R.dimen.design_type_secondary))
        drawStatBox(canvas, 120f + (boxWidth + 20f) * 2, boxY, boxWidth, boxH, "Skipped", skippedCount.toString(), labelSecDark, boldTypeface, regularTypeface, cardRadius, bgDark, labelSecDark, typeSize(com.eve.app.R.dimen.design_type_heading), typeSize(com.eve.app.R.dimen.design_type_secondary))

        // 7. Standing Card
        val standingRect = RectF(120f, 770f, width - 120f, 910f)
        val standingPaint = Paint().apply {
            color = bgDark
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(standingRect, cardRadius, cardRadius, standingPaint)

        val standLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = labelSecDark
            textSize = typeSize(com.eve.app.R.dimen.design_type_secondary)
            typeface = regularTypeface
            letterSpacing = 0.05f
        }
        canvas.drawText("PERFORMANCE STANDING", 160f, 820f, standLabelPaint)

        val standValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = systemBlue
            textSize = typeSize(com.eve.app.R.dimen.design_type_heading)
            typeface = boldTypeface
        }
        canvas.drawText(rankText, 160f, 880f, standValuePaint)

        // 8. Eve App Footer
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = labelTerDark
            textSize = typeSize(com.eve.app.R.dimen.design_type_secondary)
            typeface = regularTypeface
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Master competitive exams with real exam analytics • Eve Mock Test", width / 2f, 1220f, footerPaint)

        // 9. Save to cacheDir/shared/ and share
        try {
            val sharedDir = File(context.cacheDir, "shared").apply { mkdirs() }
            val file = File(sharedDir, "result_share_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "I scored $scoreText on $examName! Try it on Eve Mock Test App.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Test Result"))
        } catch (e: Exception) {
            android.util.Log.e("ShareCardHelper", "Failed to share card", e)
        }
    }

    private fun drawStatBox(
        canvas: Canvas,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        label: String,
        value: String,
        colorInt: Int,
        boldTypeface: Typeface,
        regularTypeface: Typeface,
        cardRadius: Float,
        bgDark: Int,
        labelSecDark: Int,
        headingSize: Float,
        secondarySize: Float
    ) {
        val rect = RectF(x, y, x + w, y + h)
        val fillPaint = Paint().apply {
            color = bgDark
            style = Paint.Style.FILL
        }

        canvas.drawRoundRect(rect, cardRadius, cardRadius, fillPaint)

        val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorInt
            textSize = headingSize
            typeface = boldTypeface
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(value, x + w / 2, y + 65f, valPaint)

        val lblPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = labelSecDark
            textSize = secondarySize
            typeface = regularTypeface
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(label, x + w / 2, y + 115f, lblPaint)
    }
}
