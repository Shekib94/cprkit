package com.cyberpunk.gmtool.data

import android.content.Context
import android.content.SharedPreferences
import java.io.File

/**
 * یادداشت کلیدهای خرابِ SharedPreferences که پاک/بازنشانی شده‌اند.
 *
 * چرا: اگر یک کلید با نوع اشتباه ذخیره شده باشد (مثلاً نسخه‌ی قبلی برنامه همان
 * کلید را عدد نوشته و حالا رشته خوانده می‌شود)، Android استثنای
 * ClassCastException می‌دهد و برنامه بی‌درنگ بسته می‌شود. اینجا آن کلید پاک
 * می‌شود تا برنامه باز شود، و نامش ثبت می‌شود تا معلوم باشد چه چیزی خراب بود.
 */
object PrefsGuard {
    private const val FILE_NAME = "prefs-repair.txt"
    private const val MAX_LINES = 40

    @Volatile
    var lastResetKey: String? = null
        private set

    fun note(context: Context, key: String) {
        lastResetKey = key
        runCatching {
            val f = File(context.filesDir, FILE_NAME)
            val lines = (if (f.exists()) f.readLines() else emptyList()) + key
            f.writeText(lines.takeLast(MAX_LINES).joinToString("\n"))
        }
    }

    fun read(context: Context): String? =
        runCatching { File(context.filesDir, FILE_NAME).takeIf { it.exists() }?.readText() }.getOrNull()
}

/**
 * خواندن مقاوم از SharedPreferences.
 *
 * مسئله: `SharedPreferences.getX` اگر مقدار ذخیره‌شده با نوع درخواستی نخواند،
 * `ClassCastException` پرت می‌کند — یک استثنای زمانِ اجرا که هیچ `?:` و
 * `let` جلویش را نمی‌گیرد. اگر این خواندن داخل ترکیبِ یک صفحه (composition)
 * باشد، نتیجه فقط یک چیز است: بسته‌شدن فوری برنامه با کلیک روی آن صفحه.
 *
 * این توابع همان خواندن‌ها را با مقدار پیش‌فرض امن انجام می‌دهند و کلید خراب را
 * یک‌بار پاک می‌کنند تا دفعه‌ی بعد سالم برگردد. برای همین هم همه‌ی خواندن‌های
 * مبارزه/تنظیمات از اینجا رد می‌شوند.
 */
private fun SharedPreferences.dropBadKey(context: Context?, key: String) {
    if (context != null) PrefsGuard.note(context, key)
    runCatching { edit().remove(key).apply() }
}

/** نیاز به context برای ثبت کلید خراب؛ نسخه‌ی ساده هم موجود است. */
fun SharedPreferences.safeString(key: String, def: String? = null, context: Context? = null): String? =
    runCatching { getString(key, def) }.getOrElse { dropBadKey(context, key); def }

fun SharedPreferences.safeStringSet(key: String, def: Set<String> = emptySet(), context: Context? = null): Set<String> =
    runCatching { getStringSet(key, def) ?: def }.getOrElse { dropBadKey(context, key); def }

fun SharedPreferences.safeInt(key: String, def: Int = 0, context: Context? = null): Int =
    runCatching { getInt(key, def) }.getOrElse { dropBadKey(context, key); def }

fun SharedPreferences.safeLong(key: String, def: Long = 0L, context: Context? = null): Long =
    runCatching { getLong(key, def) }.getOrElse { dropBadKey(context, key); def }

fun SharedPreferences.safeBoolean(key: String, def: Boolean = false, context: Context? = null): Boolean =
    runCatching { getBoolean(key, def) }.getOrElse { dropBadKey(context, key); def }

fun SharedPreferences.safeFloat(key: String, def: Float = 0f, context: Context? = null): Float =
    runCatching { getFloat(key, def) }.getOrElse { dropBadKey(context, key); def }
