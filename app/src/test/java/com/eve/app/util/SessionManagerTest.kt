package com.eve.app.util

import android.content.SharedPreferences
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class SessionManagerTest {

    private class FakeEditor(private val data: MutableMap<String, Any?>) : SharedPreferences.Editor {
        private val temp = mutableMapOf<String, Any?>()
        private var clearRequested = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = this

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) temp.remove(key)
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            clearRequested = true
            return this
        }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            if (clearRequested) {
                data.clear()
            }
            data.putAll(temp)
        }
    }

    private class FakeSharedPreferences : SharedPreferences {
        val data = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = data
        override fun getString(key: String?, defValue: String?): String? = (data[key] as? String) ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = defValues
        override fun getInt(key: String?, defValue: Int): Int = (data[key] as? Int) ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = (data[key] as? Long) ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = (data[key] as? Float) ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = (data[key] as? Boolean) ?: defValue
        override fun contains(key: String?): Boolean = data.containsKey(key)
        override fun edit(): SharedPreferences.Editor = FakeEditor(data)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    }

    private lateinit var fakePrefs: FakeSharedPreferences

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
        SessionManager.prefsOverride = fakePrefs
    }

    @After
    fun tearDown() {
        SessionManager.prefsOverride = null
    }

    @Test
    fun testCacheSetAndGet_fresh() {
        SessionManager.setCachedAdminStatus("admin@example.com", true)
        val status = SessionManager.getCachedAdminStatus("admin@example.com", allowStale = false)
        assertEquals(true, status)
    }

    @Test
    fun testCacheSetAndGet_caseInsensitive() {
        SessionManager.setCachedAdminStatus("Admin@Example.COM", true)
        val status = SessionManager.getCachedAdminStatus("admin@example.com", allowStale = false)
        assertEquals(true, status)
    }

    @Test
    fun testCacheMismatchEmail_returnsNull() {
        SessionManager.setCachedAdminStatus("admin@example.com", true)
        val status = SessionManager.getCachedAdminStatus("student@example.com", allowStale = false)
        assertNull(status)
    }

    @Test
    fun testCacheExpired_withoutAllowStale_returnsNull() {
        SessionManager.setCachedAdminStatus("admin@example.com", true)
        // Manually age the timestamp past TTL (5 minutes)
        fakePrefs.data["session_admin_timestamp"] = System.currentTimeMillis() - (SessionManager.CACHE_TTL_MS + 1000L)

        val freshStatus = SessionManager.getCachedAdminStatus("admin@example.com", allowStale = false)
        assertNull(freshStatus)

        val staleStatus = SessionManager.getCachedAdminStatus("admin@example.com", allowStale = true)
        assertEquals(true, staleStatus)
    }

    @Test
    fun testClear_emptiesCache() {
        SessionManager.setCachedAdminStatus("admin@example.com", true)
        SessionManager.clear()
        assertNull(SessionManager.getCachedAdminStatus("admin@example.com", allowStale = true))
    }
}
