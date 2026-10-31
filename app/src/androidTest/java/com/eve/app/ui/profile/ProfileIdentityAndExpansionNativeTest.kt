package com.eve.app.ui.profile

import android.content.ContentValues
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.provider.MediaStore
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eve.app.R
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.databinding.ActivityProfileBinding
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class ProfileIdentityAndExpansionNativeTest {
    @Test fun realLayoutKeepsUsernameCategoryActionsAndExpandedHeaderUsable() {
        val inst = InstrumentationRegistry.getInstrumentation()
        inst.runOnMainSync {
            val base = inst.targetContext
            TelegramThemeEngine.resolveCurrentPalette(base)
            val preview = TelegramThemeEngine.isPreviewActive()
            val day = TelegramThemeEngine.getDayThemeId(); val dayAccent = TelegramThemeEngine.getDayAccentId()
            val night = TelegramThemeEngine.getNightThemeId(); val nightAccent = TelegramThemeEngine.getNightAccentId()
            val mode = TelegramThemeEngine.getThemeMode()
            TelegramThemeEngine.restorePreviewState("day", 0, "night", 0, TelegramThemeEngine.ThemeMode.SYSTEM, base)
            try {
            for (dark in listOf(false, true)) for (font in listOf(1f, 2f)) {
                val config = Configuration(base.resources.configuration).apply {
                    fontScale = font
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                        if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                }
                val context = ContextThemeWrapper(base.createConfigurationContext(config), R.style.Theme_Eve)
                val b = ActivityProfileBinding.inflate(LayoutInflater.from(context))
                val density = context.resources.displayMetrics.density
                val w = ((if (font > 1) 320 else 360) * density).roundToInt(); val h = (640 * density).toInt()
                fun layout() {
                    b.root.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
                    b.root.layout(0, 0, w, h)
                }
                b.root.layoutDirection = if (font > 1) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
                b.tvName.text = if (font > 1) "आरती शर्मा / A long student name" else "Ananya Sharma"; b.tvStatus.text = ProfilePresenceStatus.label(null)
                b.tvUsernameValue.text = "@ananya_eve"; b.tvMobileValue.text = "Add phone number"
                b.tvBirthdayValue.text = "Add birthday"; b.tvCategoryValue.text = "OBC"
                ProfileActionAccessibility.install(listOf(b.btnSetPhoto, b.btnEditInfo, b.btnSettings))
                layout()
                ProfileHeaderExpansion.apply(b, 0f, 0)
                ProfileActionIcons.applyTo(listOf(b.btnSetPhoto, b.btnEditInfo, b.btnSettings), context.getColor(R.color.category_pill_unselected_bg))
                // Disposable raster photo; no live account, remote image or Firebase access.
                val photo = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
                Canvas(photo).drawColor(0xFF527CA8.toInt())
                b.ivProfilePhoto.setImageBitmap(photo)
                ProfileBioCapability.apply(b.cellBio, b.tvBioValue, null)
                layout()
                val tint = (b.btnSetPhoto.getChildAt(0) as android.widget.ImageView).imageTintList!!.defaultColor
                val brightness = androidx.core.graphics.ColorUtils.calculateLuminance(tint)
                assertTrue("Source action ink must contrast in both themes", if (dark) brightness > .8 else brightness < .2)
                val column = b.cardPersonalInfo.parent as ViewGroup
                assertEquals(column.indexOfChild(b.cardPersonalInfo) + 1, column.indexOfChild(b.cardCategory))
                assertEquals("Personal QR code", b.btnBack.contentDescription.toString())
                assertTrue(b.btnProfileMenu.width >= (48 * density).toInt())
                assertTrue(b.btnBack.width >= (48 * density).toInt())
                fun save(name: String) {
                    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    b.root.draw(Canvas(bitmap))
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, "$name-${if (dark) "dark" else "light"}.png")
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/eve-approved-ui")
                    }
                    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
                    context.contentResolver.openOutputStream(uri)!!.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    bitmap.recycle()
                }
                for (button in listOf(b.btnSetPhoto, b.btnEditInfo, b.btnSettings)) {
                    val label = button.getChildAt(1) as android.widget.TextView
                    assertTrue("Action label must not clip at $font", label.layout.getLineBottom(label.lineCount - 1) <= label.height)
                }
                if (font == 1f) save("profile-mission-initial")
                b.profileScrollView.canExpand = { true }
                b.profileScrollView.maximumPull = w - 200 * density
                b.profileScrollView.onExpansionChanged = { ProfileHeaderExpansion.apply(b, it, 0) }
                b.profileScrollView.setExpanded(true, animated = false)
                layout(); ProfileHeaderExpansion.apply(b, 1f, 0); layout()
                assertEquals(w, b.avatarContainer.width)
                assertTrue(b.tvStatus.bottom + b.tvStatus.translationY <= w - 8 * density)
                assertTrue(b.tvName.bottom + b.tvName.translationY <= b.tvStatus.top + b.tvStatus.translationY)
                assertEquals(w.toFloat(), b.layoutActions.top + b.layoutActions.translationY, 1f)
                if (font == 1f) save("profile-mission-expanded")
                b.profileScrollView.setExpanded(false, animated = false)
                layout(); ProfileHeaderExpansion.apply(b, 0f, 0); layout()
                assertEquals((100 * density).roundToInt(), b.avatarContainer.width)
                assertEquals(View.GONE, b.profilePhotoScrim.visibility)
            }
            } finally {
                if (preview) TelegramThemeEngine.restorePreviewState(day, dayAccent, night, nightAccent, mode, base)
                else TelegramThemeEngine.cancelPreview(base)
            }
        }
    }

    @Test fun qrOpensOnlyTheRegisteredEveUsernameDestination() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val uri = Uri.parse(ProfileQrPayload.encode("alice_eve")!!)
        val intent = Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE).setPackage(context.packageName)
        val target = context.packageManager.resolveActivity(intent, 0)
        assertNotNull(target)
        assertEquals(UsernameCardActivity::class.java.name, target!!.activityInfo.name)
        assertEquals("alice_eve", ProfileQrPayload.parse(uri.toString()))
    }
}
