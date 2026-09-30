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

        val boldTypeface = try {
            androidx.core.content.res.ResourcesCompat.getFont(context, com.eve.app.R.font.source_serif_4_bold) ?: Typeface.DEFAULT_BOLD
        } catch (_: Throwable) {
            Typeface.DEFAULT_BOLD
        }
        val regularTypeface = try {
            androidx.core.content.res.ResourcesCompat.getFont(context, com.eve.app.R.font.source_serif_4_regular) ?: Typeface.DEFAULT
        } catch (_: Throwable) {
            Typeface.DEFAULT
        }

        // 1. Dark Background
        val bgPaint = Paint().apply {
            color = Color.parseColor("#0F172A") // Deep slate
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. Card Container
        val cardPaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            style = Paint.Style.FILL
        }
        val cardStrokePaint = Paint().apply {
            color = Color.parseColor("#334155")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val cardRect = RectF(60f, 60f, width - 60f, height - 60f)
        canvas.drawRoundRect(cardRect, 36f, 36f, cardPaint)
        canvas.drawRoundRect(cardRect, 36f, 36f, cardStrokePaint)

        // 3. App Title / Header
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8") // Sky blue
            textSize = 34f
            typeface = boldTypeface
            letterSpacing = 0.15f
        }
        canvas.drawText("EVE MOCK TEST", 120f, 150f, brandPaint)

        // 4. Test Name
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 50f
            typeface = boldTypeface
        }
        val truncatedName = if (examName.length > 36) examName.take(33) + "..." else examName
        canvas.drawText(truncatedName, 120f, 220f, titlePaint)

        // Subtle divider
        val divPaint = Paint().apply {
            color = Color.parseColor("#334155")
            strokeWidth = 2f
        }
        canvas.drawLine(120f, 260f, width - 120f, 260f, divPaint)

        // 5. Score Label & Value
        val scoreLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 30f
            typeface = regularTypeface
            letterSpacing = 0.08f
        }
        canvas.drawText("TOTAL SCORE", 120f, 330f, scoreLabelPaint)

        val scoreValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 100f
            typeface = boldTypeface
        }
        canvas.drawText(scoreText, 120f, 440f, scoreValuePaint)

        // Percentage & Accuracy Pills
        val pillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0369A1") // Deep ocean
            style = Paint.Style.FILL
        }
        val pillTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E0F2FE")
            textSize = 28f
            typeface = boldTypeface
        }
        val pill1Rect = RectF(120f, 475f, 340f, 530f)
        canvas.drawRoundRect(pill1Rect, 20f, 20f, pillBgPaint)
        canvas.drawText(percentageText, 140f, 513f, pillTextPaint)

        val pill2Rect = RectF(360f, 475f, 600f, 530f)
        canvas.drawRoundRect(pill2Rect, 20f, 20f, pillBgPaint)
        canvas.drawText("Acc: $accuracyText", 380f, 513f, pillTextPaint)

        // 6. Performance Breakdown Boxes (Correct, Wrong, Skipped)
        val boxWidth = (width - 240f - 40f) / 3f
        val boxY = 570f
        val boxH = 160f

        drawStatBox(canvas, 120f, boxY, boxWidth, boxH, "Correct", correctCount.toString(), "#10B981", boldTypeface, regularTypeface)
        drawStatBox(canvas, 120f + boxWidth + 20f, boxY, boxWidth, boxH, "Wrong", wrongCount.toString(), "#EF4444", boldTypeface, regularTypeface)
        drawStatBox(canvas, 120f + (boxWidth + 20f) * 2, boxY, boxWidth, boxH, "Skipped", skippedCount.toString(), "#94A3B8", boldTypeface, regularTypeface)

        // 7. Standing Card
        val standingRect = RectF(120f, 770f, width - 120f, 910f)
        val standingPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(standingRect, 24f, 24f, standingPaint)
        canvas.drawRoundRect(standingRect, 24f, 24f, cardStrokePaint)

        val standLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 26f
            typeface = regularTypeface
            letterSpacing = 0.05f
        }
        canvas.drawText("PERFORMANCE STANDING", 160f, 820f, standLabelPaint)

        val standValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            textSize = 42f
            typeface = boldTypeface
        }
        canvas.drawText(rankText, 160f, 880f, standValuePaint)

        // 8. Eve App Footer
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 24f
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
        colorHex: String,
        boldTypeface: Typeface,
        regularTypeface: Typeface
    ) {
        val rect = RectF(x, y, x + w, y + h)
        val fillPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            style = Paint.Style.FILL
        }
        val borderPaint = Paint().apply {
            color = Color.parseColor("#334155")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(rect, 20f, 20f, fillPaint)
        canvas.drawRoundRect(rect, 20f, 20f, borderPaint)

        val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(colorHex)
            textSize = 46f
            typeface = boldTypeface
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(value, x + w / 2, y + 65f, valPaint)

        val lblPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 22f
            typeface = regularTypeface
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(label, x + w / 2, y + 115f, lblPaint)
    }
}
