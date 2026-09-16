package com.cyberpunk.gmtool

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * تست ساختاری: «هیچ تاس دست‌سازی بیرون از DiceSource وجود ندارد.»
 *
 * چرا این تست لازم است:
 * وعده‌ی برنامه — «با خاموش‌کردن تاس خودکار، همه‌ی تاس‌های قواعدی از GM پرسیده
 * می‌شوند» — تا وقتی فقط یک کامنت باشد، با اولین اصلاح عادی نقض می‌شود. در همین
 * بازبینی، ده‌ها جای سورس مستقیم `Random` می‌زد (جدول‌های Lifepath، ابزارهای
 * Netrunning، تراپی، داروهای خیابانی، تاس تساوی Initiative و …) و آن وعده را
 * بی‌صدا باطل می‌کرد. این تست همان دسته اشتباه را در چند ثانیه می‌گیرد.
 *
 * ── چطور کار می‌کند ──────────────────────────────────────────────
 * ۱. کامنت‌ها نادیده گرفته می‌شوند (خط `//` و بدنه‌ی کامنت بلوکی) تا توضیحِ خودِ
 *    کد — مثل همین فایل یا توضیح SecureRandom در `LanHost` — باعث خطای الکی نشود.
 * ۲. اگر عمداً جایی تاسِ **غیرقواعدی** می‌ریزی (مثل پسوند عددیِ نام NPC تکراری)،
 *    همان خط را با نشانه‌ی `allow-raw-random: دلیل` علامت بزن. شکستن فهرست
 *    «فایل‌های معاف» عمداً ممکن نیست؛ معافیت خط‌به‌خط است تا بقیه‌ی همان فایل
 *    تحت پوشش بماند.
 * ۳. تعداد معافیت‌ها هم چک می‌شود؛ اگر کسی یکی‌یکی معافیت اضافه کند، سقف
 *    [MAX_EXEMPTIONS] می‌شکند و باید در بازبینی توضیح بدهد.
 */
class DiceSourceEnforcementTest {

    private data class Violation(val file: String, val line: Int, val text: String, val reason: String)

    /** الگوهایی که یعنی «یک تاس قواعدی بیرون از DiceSource ساخته شده». */
    private val forbidden = listOf(
        Regex("""kotlin\.random\.Random""") to "استفاده‌ی مستقیم از kotlin.random.Random",
        Regex("""\bRandom\.next\w*\(""") to "ساخت تاس با Random",
        Regex("""java\.util\.Random""") to "java.util.Random",
        Regex("""Math\.random""") to "Math.random",
        Regex("""\([0-9]+\.\.[0-9]+\)\.random\(\)""") to "تاس دست‌ساز با (a..b).random() — باید DiceSource.rollOne باشد"
    )

    /** تنها فایلی که مجاز است مستقیم تاس بسازد: خودِ منبع تاس. */
    private val allowedFiles = mapOf(
        "com/cyberpunk/gmtool/data/DiceSource.kt" to "خودِ منبع تاس"
    )

    /** نشانه‌ی معافیت خط‌به‌خط؛ باید دلیل داشته باشد. */
    private val exemptionMarker = "allow-raw-random"

    /** سقف معافیت‌ها — تا «موقت» به قاعده‌ی دائمی تبدیل نشود. */
    private val MAX_EXEMPTIONS = 5

    @Test
    fun noRulesDiceOutsideDiceSource() {
        val root = sourceRoot()
        val violations = mutableListOf<Violation>()
        val exemptions = mutableListOf<String>()

        root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .sortedBy { it.path }
            .forEach { file ->
                val relative = file.relativeTo(root).invariantSeparatorsPath
                if (allowedFiles.containsKey(relative)) return@forEach
                file.readLines().forEachIndexed { index, raw ->
                    if (exemptionMarker in raw) {
                        exemptions += "$relative:${index + 1}  →  ${raw.substringAfter("$exemptionMarker:").trim()}"
                        return@forEachIndexed
                    }
                    val code = codePartOf(raw)
                    if (code.isBlank()) return@forEachIndexed
                    forbidden.forEach { (pattern, reason) ->
                        if (pattern.containsMatchIn(code)) {
                            violations += Violation(relative, index + 1, raw.trim(), reason)
                        }
                    }
                }
            }

        assertTrue(
            buildString {
                append("تاس قواعدی بیرون از DiceSource پیدا شد:\n")
                violations.forEach { append("  ${it.file}:${it.line}  →  ${it.reason}\n      ${it.text}\n") }
                append("\nراه‌حل: DiceSource.roll/rollOne را صدا بزن (و اگر عملیات از UI می‌آید، آن را داخل rules { } بگذار).")
            },
            violations.isEmpty()
        )
    }

    /** معافیت‌ها باید دلیل داشته باشند و از سقف کمتر بمانند. */
    @Test
    fun exemptionsAreFewAndJustified() {
        val root = sourceRoot()
        val exemptions = mutableListOf<String>()
        root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                file.readLines().forEachIndexed { index, raw ->
                    if (exemptionMarker in raw) {
                        val reason = raw.substringAfter("$exemptionMarker:").trim()
                        val where = "${file.relativeTo(root).invariantSeparatorsPath}:${index + 1}"
                        assertTrue("معافیت بدون دلیل در $where — دلیل را بعد از «$exemptionMarker:» بنویس", reason.isNotBlank())
                        exemptions += where
                    }
                }
            }
        assertTrue(
            "تعداد معافیت‌ها از سقف ($MAX_EXEMPTIONS) گذشت؛ هر معافیت باید در بازبینی توضیح داده شود:\n" +
                exemptions.joinToString("\n"),
            exemptions.size <= MAX_EXEMPTIONS
        )
    }

    /**
     * قسمت کدِ یک خط را برمی‌گرداند: کامنت `//` و بدنه‌ی کامنت بلوکی حذف می‌شوند.
     * دلیل: توضیحِ داخل کد (مثل «از SecureRandom می‌آید نه kotlin.random.Random»)
     * نباید خودش نقض قاعده حساب شود.
     */
    private fun codePartOf(raw: String): String {
        val trimmed = raw.trimStart()
        if (trimmed.startsWith("*") || trimmed.startsWith("/*")) return ""
        return raw.substringBefore("//")
    }

    /** فهرست فایل‌های مجاز خودش بیات نشود: هر مسیری که وجود ندارد، خطا است. */
    @Test
    fun allowedFilesExist() {
        val root = sourceRoot()
        allowedFiles.keys.forEach { path ->
            assertTrue("فایل مجاز «$path» وجود ندارد (فهرست DiceSourceEnforcementTest بیات شده)", File(root, path).isFile)
        }
    }

    /**
     * ریشه‌ی سورس را از دایرکتوری اجرا پیدا می‌کند.
     * در Gradle (تست واحد JVM) دایرکتوری کاری همان ماژول `app/` است، ولی این تابع
     * هم چند سطح بالاتر و هم ریشه‌ی پروژه را هم امتحان می‌کند تا در IDE هم کار کند.
     */
    private fun sourceRoot(): File {
        var dir: File? = File(System.getProperty("user.dir") ?: ".").absoluteFile
        while (dir != null) {
            File(dir, "src/main/java").takeIf { it.isDirectory }?.let { return it }
            File(dir, "app/src/main/java").takeIf { it.isDirectory }?.let { return it }
            dir = dir.parentFile
        }
        error("ریشه‌ی سورس پیدا نشد (از ${System.getProperty("user.dir")} شروع شد)")
    }
}
