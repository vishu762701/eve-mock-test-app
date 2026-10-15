package com.eve.app.ui

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.test.platform.app.InstrumentationRegistry
import com.eve.app.R
import com.eve.app.ui.common.EveGlassToolbar
import org.junit.Assert.*
import org.junit.Test

/** Production chrome fixtures. Canvas evidence does not certify compositor blur. */
class Ios26ChromeTest {
    @Test fun chromeAndModalFixturesInBothAppearances() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            for (dark in listOf(false, true)) {
                val config = Configuration(instrumentation.targetContext.resources.configuration).apply {
                    uiMode = uiMode and Configuration.UI_MODE_NIGHT_MASK.inv() or
                        if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                }
                val context = ContextThemeWrapper(instrumentation.targetContext.createConfigurationContext(config), R.style.Theme_Eve)
                val density = context.resources.displayMetrics.density
                fun dp(n: Int) = (n * density + .5f).toInt()
                val cases = listOf("nav-bar" to R.layout.activity_settings,
                    "tab-bar" to R.layout.activity_result, "menu" to R.layout.popup_telegram_menu,
                    "sheet" to R.layout.bottom_sheet_target_exams, "dialog" to R.layout.dialog_report_question)
                for ((name, layout) in cases) {
                    val whole = LayoutInflater.from(context).inflate(layout, null)
                    fun chrome(view: View): EveGlassToolbar? {
                        if (view is EveGlassToolbar) return view
                        if (view is ViewGroup) for (i in 0 until view.childCount) chrome(view.getChildAt(i))?.let { return it }
                        return null
                    }
                    val root = when (name) {
                        "nav-bar" -> chrome(whole)!!
                        "tab-bar" -> whole.findViewById<View>(R.id.tabLayoutResult).parent as View
                        else -> whole
                    }
                    (root.parent as? ViewGroup)?.removeView(root)
                    root.measure(View.MeasureSpec.makeMeasureSpec(dp(if (name == "menu") 250 else 360), View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(dp(780), View.MeasureSpec.AT_MOST))
                    root.layout(0, 0, root.measuredWidth, root.measuredHeight)
                    assertTrue(root.width > 0 && root.height > 0)
                    assertEquals(26f * density, context.resources.getDimension(R.dimen.eve_radius_card), .01f)
                    assertEquals(34f * density, context.resources.getDimension(R.dimen.eve_radius_bottom_sheet), .01f)
                    val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(context.getColor(R.color.eve_secondary_system_background))
                    root.draw(canvas)
                    val values = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "ios26-$name-${if (dark) "dark" else "light"}.png")
                        put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
                        put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/eve-approved-ui")
                    }
                    val resolver = instrumentation.targetContext.contentResolver
                    val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
                    resolver.openOutputStream(uri)!!.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    bitmap.recycle()
                }
            }
        }
    }
}
