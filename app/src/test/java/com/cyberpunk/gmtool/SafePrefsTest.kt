package com.cyberpunk.gmtool

import android.content.SharedPreferences
import com.cyberpunk.gmtool.data.safeBoolean
import com.cyberpunk.gmtool.data.safeInt
import com.cyberpunk.gmtool.data.safeLong
import com.cyberpunk.gmtool.data.safeString
import com.cyberpunk.gmtool.data.safeStringSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * `SharedPreferences.getX` اگر مقدار ذخیره‌شده با نوع درخواستی نخواند،
 * `ClassCastException` پرت می‌کند — نه `null`. هر خواندنِ بی‌محافظ در مسیر
 * ترکیبِ یک صفحه یعنی «کلیک روی آن صفحه و بسته‌شدن فوری برنامه». این تست
 * همان سناریو را می‌سازد و مطمئن می‌شود توابع safe به‌جای استثنا، مقدار
 * پیش‌فرض برمی‌گردانند.
 *
 * اجرا:  ./gradlew test
 */
class SafePrefsTest {

    /** SharedPreferences تقلبی: فقط نوعِ «درست» را برمی‌گرداند، وگرنه CCE. */
    private class FakePrefs(private val values: MutableMap<String, Any?> = mutableMapOf()) : SharedPreferences {

        override fun getString(key: String?, defValue: String?): String? =
            values[key]?.let { it as String } ?: defValue

        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            values[key]?.let { @Suppress("UNCHECKED_CAST") (it as MutableSet<String>) } ?: defValues

        override fun getInt(key: String?, defValue: Int): Int = values[key]?.let { it as Int } ?: defValue

        override fun getLong(key: String?, defValue: Long): Long = values[key]?.let { it as Long } ?: defValue

        override fun getFloat(key: String?, defValue: Float): Float = values[key]?.let { it as Float } ?: defValue

        override fun getBoolean(key: String?, defValue: Boolean): Boolean = values[key]?.let { it as Boolean } ?: defValue

        override fun contains(key: String?): Boolean = values.containsKey(key)

        override fun getAll(): MutableMap<String, *> = values

        override fun registerOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?
        ) {}

        override fun unregisterOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?
        ) {}

        override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
            override fun putString(key: String?, value: String?): SharedPreferences.Editor = this
            override fun putStringSet(key: String?, values2: MutableSet<String>?): SharedPreferences.Editor = this
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor = this
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor = this
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = this
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = this
            override fun remove(key: String?): SharedPreferences.Editor { values.remove(key); return this }
            override fun clear(): SharedPreferences.Editor { values.clear(); return this }
            override fun commit(): Boolean = true
            override fun apply() {}
        }
    }

    @Test
    fun wrongTypeReadsFallBackToDefaultInsteadOfThrowing() {
        // کلیدهایی که «اشتباه» ذخیره شده‌اند: رشته‌ای که باید عدد باشد و برعکس.
        val prefs = FakePrefs(mutableMapOf<String, Any?>("round" to "1", "runtime_7" to 42, "haptics" to "yes"))

        assertEquals("int از کلید رشته‌ای باید مقدار پیش‌فرض بدهد", 1, prefs.safeInt("round", 1))
        assertNull("string از کلید عددی باید null بدهد", prefs.safeString("runtime_7", null))
        assertEquals("boolean از کلید رشته‌ای باید مقدار پیش‌فرض بدهد", true, prefs.safeBoolean("haptics", true))
    }

    @Test
    fun healthyValuesStillReadNormally() {
        val prefs = FakePrefs(mutableMapOf<String, Any?>(
            "round" to 3, "runtime_7" to "{\"targetId\":2}", "haptics" to false,
            "linked_campaign" to 9L, "participants" to mutableSetOf("1", "2")
        ))

        assertEquals(3, prefs.safeInt("round", 1))
        assertEquals("{\"targetId\":2}", prefs.safeString("runtime_7", null))
        assertEquals(false, prefs.safeBoolean("haptics", true))
        assertEquals(9L, prefs.safeLong("linked_campaign", -1L))
        assertEquals(setOf("1", "2"), prefs.safeStringSet("participants", emptySet()))
    }

    @Test
    fun missingKeysReturnDefaults() {
        val prefs = FakePrefs()
        assertEquals(1, prefs.safeInt("nothing", 1))
        assertEquals("10", prefs.safeString("distanceText", "10"))
        assertEquals(-1L, prefs.safeLong("linked_session", -1L))
        assertEquals(emptySet<String>(), prefs.safeStringSet("participants", emptySet()))
        assertEquals(false, prefs.safeBoolean("flags", false))
    }
}
