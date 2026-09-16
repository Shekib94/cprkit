package com.cyberpunk.gmtool.data

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import org.json.JSONObject

/**
 * لایه‌ی زبان برنامه (i18n)
 * ============================================================
 * کلیدها همان رشته‌ی انگلیسی داخل سورس هستند.
 *
 * ⚠️ قانون طلایی:
 *   هرگز رشته‌ی انگلیسی را در سورس Kotlin عوض نکنید. آن‌ها به‌عنوان
 *   شناسه (ID) در منطق برنامه و در فایل‌های سیو (Gson) استفاده می‌شوند.
 *   ترجمه فقط هنگام *نمایش* اعمال می‌شود.
 *
 * اگر ترجمه‌ای موجود نباشد، خودِ متن انگلیسی برگردانده می‌شود (fallback).
 * به همین دلیل می‌توان ترجمه را مرحله‌به‌مرحله جلو برد بدون اینکه
 * برنامه بشکند.
 */

/** حالت‌های زبان برنامه. */
enum class AppLanguage(val code: String, val label: String) {
    /** همه‌چیز انگلیسی (بدون ترجمه). */
    ENGLISH("en", "English"),

    /** حالت فعلی و دست‌نخورده‌ی برنامه: سرخط‌ها انگلیسی، توضیحات فارسی.
     *  در این حالت هیچ ترجمه‌ای اعمال نمی‌شود. */
    BILINGUAL("bi", "دوزبانه"),

    /** تمام‌فارسی. */
    PERSIAN("fa", "فارسی");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code == code } ?: BILINGUAL
    }
}

/**
 * جدول ترجمه‌ی یک زبان.
 * @param map نگاشت «انگلیسی → ترجمه»
 */
class Strings(
    val language: AppLanguage,
    private val map: Map<String, String>
) {
    /** ترجمه‌ی [key]؛ اگر نبود خودِ [key] برمی‌گردد. */
    fun t(key: String): String =
        if (language != AppLanguage.PERSIAN) key
        else map[key]?.takeIf { it.isNotBlank() } ?: key

    /**
     * ترجمه‌ی پارامتری.
     * مثال:  t("Damage: %1s to %2s", dmg, target)
     */
    fun t(key: String, vararg args: Any?): String {
        val template = t(key)
        var out = template
        args.forEachIndexed { i, a ->
            out = out.replace("%${i + 1}s", a?.toString() ?: "")
        }
        return out
    }

    /** آیا این کلید ترجمه‌ی واقعی دارد؟ (برای ابزار توسعه) */
    fun has(key: String): Boolean = map[key]?.isNotBlank() == true

    val size: Int get() = map.size

    companion object {
        /** جدول خالی — همه‌چیز انگلیسی می‌ماند. */
        val EMPTY = Strings(AppLanguage.ENGLISH, emptyMap())
    }
}

/**
 * بارگذاری فایل‌های ترجمه از assets.
 *
 * ساختار مورد انتظار در پوشه‌ی assets:
 *   assets/i18n/fa.1-core-ui.json
 *   assets/i18n/fa.2-rules.json
 *   ...
 *
 * همه‌ی فایل‌های فاز روی هم merge می‌شوند، پس می‌توانید فازها را
 * یکی‌یکی اضافه کنید بدون تغییر در کد.
 */
object LocalizationLoader {

    /** فازهایی که فایل ترجمه‌شان در assets قرار دارد. */
    private val PHASE_FILES = listOf(
        "fa.1-core-ui.json",
        "fa.2-rules.json",
        "fa.3-combat.json",
        "fa.4-gmtools.json",
        "fa.5-lifepath.json",
        "fa.6-catalog.json",
        "fa.7-battlemap.json",
        "fa.8-interp.json",
        "fa.10-leftovers.json",
        "fa.11-extra.json",
        "fa.12-interp2.json",
        "fa.13-words.json",
        "fa.14-combatlog.json",
        "fa.15-full.json",
        "fa.16-gm.json",
        "fa.17-sell.json",
        "fa.18-newui.json",
        "fa.19-lan.json",
        "fa.20-gigs.json"
    )

    private const val DIR = "i18n"

    /** کش تا هر بار از دیسک خوانده نشود. */
    private val cache = mutableMapOf<AppLanguage, Strings>()

    fun load(context: Context, language: AppLanguage): Strings {
        cache[language]?.let { return it }

        val result = when (language) {
            // ENGLISH و BILINGUAL هر دو دقیقاً همان چیزی را نشان می‌دهند
            // که در سورس نوشته شده؛ هیچ ترجمه‌ای بار نمی‌شود.
            AppLanguage.PERSIAN -> Strings(language, readMerged(context))
            else -> Strings(language, emptyMap())
        }
        cache[language] = result
        return result
    }

    fun clearCache() = cache.clear()

    private fun readMerged(context: Context): Map<String, String> {
        val merged = HashMap<String, String>(4096)
        val assets = context.applicationContext.assets

        // فقط فایل‌هایی که واقعاً وجود دارند خوانده می‌شوند
        val present = try {
            assets.list(DIR)?.toSet() ?: emptySet()
        } catch (e: Exception) {
            emptySet()
        }

        for (fileName in PHASE_FILES) {
            if (fileName !in present) continue
            try {
                val text = assets.open("$DIR/$fileName")
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }
                val obj = JSONObject(text)
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    if (k == "_meta") continue
                    val v = obj.optString(k, "")
                    if (v.isNotBlank()) merged[k] = v
                }
            } catch (e: Exception) {
                // یک فایل خراب نباید کل برنامه را بشکند
                android.util.Log.w("i18n", "خطا در خواندن $fileName: ${e.message}")
            }
        }
        return merged
    }
}

/**
 * دسترسی به جدول ترجمه در هر Composable.
 * مقدار پیش‌فرض EMPTY است تا Preview ها بدون Provider هم کار کنند.
 */
val LocalStrings: ProvidableCompositionLocal<Strings> =
    compositionLocalOf { Strings.EMPTY }

/** میان‌بر: در Composable بنویسید `tr("Save")` */
@Composable
fun tr(key: String): String = LocalStrings.current.t(key)

@Composable
fun tr(key: String, vararg args: Any?): String = LocalStrings.current.t(key, *args)

/**
 * نسخه‌ی غیر-Composable ترجمه.
 * ------------------------------------------------------------
 * `tr()` یک تابع @Composable است و فقط داخل بدنه‌ی Composable
 * فراخوانی می‌شود. اما خیلی از متن‌های برنامه در جاهایی ساخته
 * می‌شوند که Composable نیستند:
 *
 *   • داخل لامبدای رویداد   →  onClick = { actionResult = "GRAB SUCCESS…" }
 *   • داخل توابع کمکی عادی  →  private fun buildLog(): String
 *   • داخل ViewModel
 *
 * برای آن موارد از `gtr()` استفاده کنید. این تابع از یک snapshot
 * سراسری از جدول ترجمه می‌خواند که `LocalizedApp` هنگام تغییر زبان
 * به‌روزش می‌کند.
 *
 * رفتار fallback دقیقاً مثل `tr()` است: اگر ترجمه نبود، خودِ متن
 * انگلیسی برمی‌گردد.
 */
object GlobalStrings {
    @Volatile
    var current: Strings = Strings.EMPTY
        internal set
}

/** ترجمه‌ی سراسری — همه‌جا قابل استفاده (لامبدا، ViewModel، تابع عادی). */
fun gtr(key: String): String = GlobalStrings.current.t(key)

/** ترجمه‌ی سراسری پارامتری. */
fun gtr(key: String, vararg args: Any?): String = GlobalStrings.current.t(key, *args)
