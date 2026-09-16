package com.cyberpunk.gmtool.data

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * آخرین خطای کشنده‌ی برنامه را روی حافظه‌ی داخلی نگه می‌دارد.
 *
 * چرا: بدون این، «برنامه کرش کرد» هیچ سرنخی باقی نمی‌گذارد و باید با adb/logcat
 * دنبال خطا گشت. حالا آخرین stack trace در `filesDir/last-crash.txt` می‌ماند و
 * داشبورد آن را نشان می‌دهد، پس می‌شود متن دقیق خطا را دید/فرستاد.
 */
object CrashLog {
    private const val FILE_NAME = "last-crash.txt"
    private const val MAX_CHARS = 20000

    private fun file(context: Context) = File(context.filesDir, FILE_NAME)

    fun record(context: Context, error: Throwable) {
        runCatching {
            val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            val head = "زمان: $stamp\n${error.javaClass.name}: ${error.message ?: ""}\n\n"
            val trace = error.stackTraceToString()
            val cause = error.cause?.let { "\n\nCaused by: ${it.javaClass.name}: ${it.message ?: ""}\n${it.stackTraceToString()}" } ?: ""
            val text = (head + trace + cause).take(MAX_CHARS)
            file(context).writeText(text)
        }
    }

    fun read(context: Context): String? =
        runCatching { file(context).takeIf { it.exists() && it.length() > 0 }?.readText() }.getOrNull()

    fun clear(context: Context) {
        runCatching { file(context).delete() }
        runCatching { File(context.filesDir, "prefs-repair.txt").delete() }
    }
}
