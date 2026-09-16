package com.cyberpunk.gmtool.data

import kotlin.math.ceil

/**
 * قواعد سایبرسایکوزیس — طبق Cyberpunk RED Core.
 *
 * قاعده‌ی پایه (CRB ص۸۰):
 *  • EMP فعلی = رقم دهگانِ Humanity فعلی. یعنی 44 → EMP 4، و 39 → EMP 3.
 *  • وقتی Humanity و EMP هر دو به صفر برسند، شخصیت سایبرسایکو می‌شود و
 *    از دست بازیکن خارج می‌شود؛ GM آن را «طبق بدترین تمایلاتش» می‌گرداند.
 *
 * قاعده‌ی درمان (CRB ص۲۲۹–۲۳۰):
 *  • تراپی استاندارد: هفته‌ای 2d6 بازیابی — 500eb، یا 100eb + یک Medical Tech با DV15.
 *  • تراپی شدید:     هفته‌ای 4d6 بازیابی — 1000eb، یا 500eb + یک Medical Tech با DV17.
 *  • سقف بازیابی: هر قطعه سایبرویر معمولیِ نصب‌شده ۲ و هر Borgware ۴ واحد از
 *    سقف کم می‌کند. برای عبور از این سقف باید قطعه واقعاً برداشته شود.
 *
 * این فایل فقط محاسبه می‌کند؛ هیچ تصمیمی به‌جای GM نمی‌گیرد.
 */
object CyberpsychosisRules {

    /** آستانه‌ای که از آن به بعد باید هشدار داد (EMP کمتر از ۲). */
    const val WARN_EMP = 2

    /** آیا این شخصیت همین حالا سایبرسایکو است؟ */
    fun isCyberpsycho(character: Character): Boolean =
        character.currentHumanity <= 0

    /** آیا در منطقه‌ی خطر است ولی هنوز سایبرسایکو نشده؟ */
    fun isAtRisk(character: Character): Boolean {
        if (isCyberpsycho(character)) return false
        return GameRules.currentEmpathy(character.currentHumanity) < WARN_EMP
    }

    /**
     * پیش‌بینی نتیجه‌ی نصب، پیش از انجام آن.
     * چون HL بعد از ساخت شخصیت با تاس تعیین می‌شود، بدترین حالت را هم برمی‌گرداند
     * تا GM بداند ریسک واقعی چقدر است.
     */
    data class InstallForecast(
        val minLoss: Int,
        val maxLoss: Int,
        val humanityNow: Int,
        val worstCaseHumanity: Int,
        /** با بدترین تاس، آیا به صفر می‌رسد؟ */
        val canGoCyberpsycho: Boolean,
        /** حتی با بهترین تاس هم به صفر می‌رسد؟ */
        val willGoCyberpsycho: Boolean
    )

    fun forecast(itemName: String, character: Character): InstallForecast {
        val dice = CyberwareCatalog.humanityDiceFor(itemName).lowercase().replace(" ", "")
        val flat = CyberwareCatalog.humanityLossFor(itemName)
        val m = Regex("(\\d+)d6(?:/(\\d+))?").matchEntire(dice)

        val (lo, hi) = if (flat == 0) {
            0 to 0
        } else if (m != null) {
            val count = m.groupValues[1].toInt()
            val divisor = m.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }?.toInt() ?: 1
            ceil(count.toDouble() / divisor).toInt() to ceil((count * 6).toDouble() / divisor).toInt()
        } else {
            flat to flat
        }

        val now = character.currentHumanity
        return InstallForecast(
            minLoss = lo,
            maxLoss = hi,
            humanityNow = now,
            worstCaseHumanity = (now - hi).coerceAtLeast(0),
            canGoCyberpsycho = now - hi <= 0 && hi > 0,
            willGoCyberpsycho = now - lo <= 0 && lo > 0
        )
    }

    /** متن هشدار پیش از نصب. اگر خطری نیست، null. */
    fun installWarning(itemName: String, character: Character): String? {
        val f = forecast(itemName, character)
        if (f.maxLoss == 0) return null
        return when {
            f.willGoCyberpsycho ->
                "این نصب قطعاً انسانیت را به صفر می‌رساند.\n\n" +
                "انسانیت فعلی ${f.humanityNow} و کمترین کاهش ممکن ${f.minLoss} است. " +
                "طبق قواعد، شخصیت سایبرسایکو می‌شود و از دست بازیکن خارج می‌شود؛ " +
                "از این پس GM او را طبق بدترین تمایلاتش می‌گرداند.\n\n" +
                "اگر مطمئنی، ادامه بده."
            f.canGoCyberpsycho ->
                "این نصب می‌تواند انسانیت را به صفر برساند.\n\n" +
                "انسانیت فعلی ${f.humanityNow} و کاهش بین ${f.minLoss} تا ${f.maxLoss} است. " +
                "با بدترین تاس، شخصیت سایبرسایکو می‌شود.\n\n" +
                "می‌توانی اول تراپی بروی یا قطعه‌ای را برداری."
            GameRules.currentEmpathy((f.humanityNow - f.maxLoss).coerceAtLeast(0)) < WARN_EMP ->
                "بعد از این نصب، EMP زیر $WARN_EMP می‌رود.\n\n" +
                "انسانیت به حدود ${f.worstCaseHumanity} می‌رسد. در این محدوده شخصیت " +
                "نشانه‌های جدی بروز می‌دهد و طبق Core، اطرافیان و نیروهای شهر متوجه می‌شوند."
            else -> null
        }
    }

    // ─────────────────────── مسیر درمان ───────────────────────

    /**
     * سقف بازیابی با تراپی: تا وقتی سایبرویر روی بدن است،
     * تراپی نمی‌تواند بیشتر از این عدد برگرداند.
     */
    fun therapyCeiling(character: Character): Int {
        val baseMax = character.baseEmp.coerceAtLeast(character.stats.emp) * 10
        val reduction = character.inventory
            .filter { it.equipped && it.category.equals("Cyberware", true) }
            .sumOf { CyberwareCatalog.maxReductionFor(it.name) }
        return (baseMax - reduction).coerceAtLeast(0)
    }

    data class TherapyPlan(
        val target: Int,
        val ceiling: Int,
        val needed: Int,
        val standardWeeks: Int,
        val standardCost: Int,
        val extremeWeeks: Int,
        val extremeCost: Int,
        /** آیا حتی با تراپی کامل هم به هدف نمی‌رسد؟ */
        val blockedByCyberware: Boolean
    )

    /**
     * چند هفته و چقدر پول لازم است تا از سایبرسایکوزیس بیرون بیاید.
     * هدف پیش‌فرض ۳۰ است: جایی که شخصیت دوباره قابل بازی می‌شود (EMP 3).
     */
    fun therapyPlan(character: Character, target: Int = 30): TherapyPlan {
        val ceiling = therapyCeiling(character)
        val reachable = minOf(target, ceiling)
        val needed = (reachable - character.currentHumanity).coerceAtLeast(0)

        // میانگین تاس: 2d6 = 7 و 4d6 = 14.
        // این «تخمین خوش‌بینانه» است نه شبیه‌سازی: کتاب (ص۲۳۰) هر هفته یک
        // Medical Tech Check می‌خواهد (DV15/DV17) که می‌تواند شکست بخورد.
        val stdWeeks = if (needed <= 0) 0 else ceil(needed / 7.0).toInt()
        val extWeeks = if (needed <= 0) 0 else ceil(needed / 14.0).toInt()

        return TherapyPlan(
            target = reachable,
            ceiling = ceiling,
            needed = needed,
            standardWeeks = stdWeeks,
            standardCost = stdWeeks * 500,
            extremeWeeks = extWeeks,
            extremeCost = extWeeks * 1000,
            blockedByCyberware = ceiling < target
        )
    }

    /** خلاصه‌ی وضعیت برای نمایش روی برگه. */
    fun statusLine(character: Character): String {
        val emp = GameRules.currentEmpathy(character.currentHumanity)
        return when {
            isCyberpsycho(character) ->
                "سایبرسایکو — انسانیت صفر. شخصیت از دست بازیکن خارج است و GM او را می‌گرداند."
            emp < WARN_EMP ->
                "نشانه‌های جدی — انسانیت ${character.currentHumanity} (EMP $emp). تراپی لازم است."
            emp < 3 ->
                "در آستانه‌ی خطر — انسانیت ${character.currentHumanity} (EMP $emp)."
            else -> ""
        }
    }
}
