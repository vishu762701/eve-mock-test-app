package com.eve.app.ui

import android.content.ContentValues
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.provider.MediaStore
import android.view.ContextThemeWrapper
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieCompositionFactory
import com.eve.app.R
import com.eve.app.ui.home.HomeAppearance
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class HomeLicensedBellNativeTest {
    @Test fun independentBellHasMatchingEndpointsVisibleMotionAndDayNightInk() {
        val inst = InstrumentationRegistry.getInstrumentation()
        inst.runOnMainSync {
            for (dark in listOf(false, true)) {
                val base = inst.targetContext
                val config = Configuration(base.resources.configuration).apply {
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                        if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                }
                val context = ContextThemeWrapper(base.createConfigurationContext(config), R.style.Theme_Eve)
                val comp = LottieCompositionFactory.fromRawResSync(context, R.raw.home_notification_bell).value!!
                assertEquals(2333f, comp.duration, 2f)
                val bell = LottieAnimationView(context)
                bell.setComposition(comp)
                HomeAppearance.tintNotificationBell(bell)
                val size = 192
                bell.measure(View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY))
                bell.layout(0, 0, size, size)
                fun frame(progress: Float): Bitmap {
                    bell.progress = progress
                    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bell.draw(Canvas(it)) }
                }
                val idle = frame(0f)
                val first = frame(.10f)
                val later = frame(.35f)
                val end = frame(1f)
                val fallback = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                val drawable = AppCompatResources.getDrawable(context, R.drawable.ic_home_notification)!!.mutate()
                drawable.setTint(if (dark) Color.WHITE else Color.BLACK)
                drawable.setBounds(48, 48, 144, 144)
                drawable.draw(Canvas(fallback))
                var union = 0; var mismatch = 0; var motion = 0; var endpoint = 0; var ink = 0; var luminance = 0L
                for (y in 0 until size) for (x in 0 until size) {
                    val a = idle.getPixel(x, y); val b = fallback.getPixel(x, y)
                    val am = Color.alpha(a) > 100; val bm = Color.alpha(b) > 100
                    if (am || bm) union++
                    if (am != bm) mismatch++
                    if (first.getPixel(x, y) != later.getPixel(x, y)) motion++
                    if (a != end.getPixel(x, y)) endpoint++
                    if (Color.alpha(a) > 200) { ink++; luminance += Color.red(a) + Color.green(a) + Color.blue(a) }
                }
                assertTrue("Idle and animated bell must share the same silhouette", union > 100 && mismatch < union * .12)
                assertTrue("Real ringing frames must differ", motion > 30)
                assertEquals("The end frame must restore the independent idle artwork", 0, endpoint)
                assertTrue("Visible bell ink required", ink > 100)
                val mean = luminance.toFloat() / (3 * ink)
                assertTrue("Bell ink must contrast in both themes", if (dark) mean > 180 else mean < 80)
                bell.cancelAnimation()
                bell.progress = 0f
                val reset = frame(0f)
                assertTrue("Interruption restores the same artwork", idle.sameAs(reset))
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "home-licensed-bell-${if (dark) "dark" else "light"}.png")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/eve-approved-ui")
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
                // Contact sheet contains only disposable icon fixtures, no account data.
                val sheet = Bitmap.createBitmap(size * 4, size, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(sheet); canvas.drawColor(if (dark) Color.BLACK else Color.WHITE)
                for ((index, bitmap) in listOf(idle, first, later, end).withIndex()) canvas.drawBitmap(bitmap, index * size.toFloat(), 0f, null)
                context.contentResolver.openOutputStream(uri)!!.use { sheet.compress(Bitmap.CompressFormat.PNG, 100, it) }
                for (bitmap in listOf(idle, first, later, end, fallback, reset, sheet)) bitmap.recycle()
            }
        }
    }
}
