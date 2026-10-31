package com.eve.app.ui.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.ui.common.EveBaseActivity
import com.google.android.material.button.MaterialButton

/** The established EVE QR destination. No account lookup, sign-in, presence or private data. */
class UsernameCardActivity : EveBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val username = ProfileQrPayload.parse(intent?.dataString.orEmpty())
        if (username == null) { finish(); return }
        val palette = TelegramThemeEngine.resolveCurrentPalette(this)
        val dp = resources.displayMetrics.density
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            setPadding((24 * dp).toInt(), (24 * dp).toInt(), (24 * dp).toInt(), (24 * dp).toInt())
        }
        fun text(value: String) = TextView(this).apply {
            text = value; setTextColor(palette.textPrimary); gravity = Gravity.CENTER
            setTextAppearance(com.eve.app.R.style.TextAppearance_Eve_Token_Body)
            setPadding(0, (12 * dp).toInt(), 0, (12 * dp).toInt())
        }
        content.addView(text("Username\n@$username"))
        val matrix = ProfileQrPayload.matrix(username)
        val pixels = IntArray(matrix.width * matrix.height) { index ->
            if (matrix[index % matrix.width, index / matrix.width]) Color.BLACK else Color.WHITE
        }
        val bitmap = Bitmap.createBitmap(pixels, matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
        content.addView(ImageView(this).apply {
            setImageBitmap(bitmap); contentDescription = "QR code for username @$username"
            adjustViewBounds = true; setBackgroundColor(Color.WHITE)
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        content.addView(text("Usernames can change. This shared card does not verify account ownership."))
        content.addView(MaterialButton(this).apply {
            text = "Copy username"; minHeight = (48 * dp).toInt()
            setOnClickListener {
                (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Username", "@$username"))
                com.eve.app.util.AppBulletin.showSuccess(this@UsernameCardActivity, "Username copied")
            }
        })
        content.addView(MaterialButton(this).apply { text = "Close"; minHeight = (48 * dp).toInt(); setOnClickListener { finish() } })
        setContentView(ScrollView(this).apply { isFillViewport = true; addView(content) })
    }
}
