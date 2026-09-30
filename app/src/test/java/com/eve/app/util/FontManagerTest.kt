package com.eve.app.util

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FontManagerTest {

    private class FakeEditor(private val data: MutableMap<String, Any?>) : SharedPreferences.Editor {
        private val temp = mutableMapOf<String, Any?>()

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = this
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor = this
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor = this
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = this
        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = this
        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) temp[key] = null
            return this
        }
        override fun clear(): SharedPreferences.Editor = this

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            for ((k, v) in temp) {
                if (v == null) data.remove(k) else data[k] = v
            }
            temp.clear()
        }
    }

    private class FakeSharedPreferences : SharedPreferences {
        val map = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = map
        override fun getString(key: String?, defValue: String?): String? = (map[key] as? String) ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = null
        override fun getInt(key: String?, defValue: Int): Int = (map[key] as? Int) ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = (map[key] as? Long) ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = (map[key] as? Float) ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = (map[key] as? Boolean) ?: defValue
        override fun contains(key: String?): Boolean = map.containsKey(key)
        override fun edit(): SharedPreferences.Editor = FakeEditor(map)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    }

    @Before
    fun setup() {
        FontManager.resetForTesting()
    }

    @Test
    fun testDefaultFontChoice() {
        val prefs = FakeSharedPreferences()
        val choice = FontManager.getFontChoice(prefs)
        assertEquals(FontManager.FONT_EVE_DEFAULT, choice)
        assertEquals(0, FontManager.fontVersion)
    }

    @Test
    fun testSetFontChoiceUpdatesPrefsAndIncrementsVersion() {
        val prefs = FakeSharedPreferences()
        assertEquals(0, FontManager.fontVersion)

        FontManager.setFontChoice(prefs, FontManager.FONT_DEVICE)
        assertEquals(FontManager.FONT_DEVICE, FontManager.getFontChoice(prefs))
        assertEquals(1, FontManager.fontVersion)

        // Setting same value again should not increment version
        FontManager.setFontChoice(prefs, FontManager.FONT_DEVICE)
        assertEquals(1, FontManager.fontVersion)

        // Setting back to default
        FontManager.setFontChoice(prefs, FontManager.FONT_EVE_DEFAULT)
        assertEquals(FontManager.FONT_EVE_DEFAULT, FontManager.getFontChoice(prefs))
        assertEquals(2, FontManager.fontVersion)
    }
}
