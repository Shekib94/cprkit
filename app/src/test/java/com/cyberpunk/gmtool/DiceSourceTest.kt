package com.cyberpunk.gmtool

import com.cyberpunk.gmtool.data.CombatRules
import com.cyberpunk.gmtool.data.DiceSource
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * تست‌های «تنها منبع تاس» (DiceSource).
 *
 * چرا این فایل وجود دارد:
 * وعده‌ی برنامه این است که وقتی «تاس خودکار» خاموش شود، **هیچ** عددی از خودِ
 * برنامه نیاید؛ همه‌ی تاس‌های قواعدی از GM پرسیده شوند. این وعده فقط وقتی ارزش
 * دارد که واقعاً قابل تست باشد. اینجا دو چیز قفل می‌شود:
 *   ۱. درخواست دستی واقعاً برای هر تاس (d10 چک، آسیب، جدول) استفاده می‌شود.
 *   ۲. اگر پاسخ GM به هر دلیلی نرسد (انصراف/پایان مهلت/فراخوان روی نخ اصلی)،
 *      برنامه **بی‌صدا** عدد نمی‌سازد؛ خبر می‌دهد.
 *
 * تستِ ساختاریِ «هیچ تاس دست‌سازی در سورس نیست» در DiceSourceEnforcementTest است.
 */
class DiceSourceTest {

    @After
    fun tearDown() {
        DiceSource.manual = null
        DiceSource.onManualFallback = null
        DiceSource.onAutoRoll = null
    }

    /** در حالت دستی، تک‌تک تاس‌ها از GM می‌آید و برچسب درست هم پرسیده می‌شود. */
    @Test
    fun manualHandler_isAskedForEveryDie() {
        val seen = mutableListOf<DiceSource.Request>()
        DiceSource.manual = { req ->
            seen += req
            List(req.count) { req.sides }   // GM «همه‌ی تاس‌ها را حداکثر» وارد می‌کند
        }

        val roll = DiceSource.roll(2, 6, "جدول Critical Injury (2d6)")
        assertEquals(listOf(6, 6), roll)
        assertEquals(1, seen.size)
        assertEquals(2, seen[0].count)
        assertEquals(6, seen[0].sides)
        assertEquals("جدول Critical Injury (2d6)", seen[0].label)
        assertEquals("2d6", seen[0].expression)
    }

    /** چک d10 قواعد (با Critical Success) هم از همان مسیر رد می‌شود. */
    @Test
    fun rulesChecks_goThroughDiceSource() {
        var rolls = 0
        DiceSource.manual = { req ->
            rolls++
            List(req.count) { if (rolls == 1) 10 else 4 }   // ۱۰ → Critical Success + تاس اضافه
        }

        val check = CombatRules.rollD10(label = "تست")
        assertTrue("۱۰ باید Critical Success باشد", check.criticalSuccess)
        assertEquals(10, check.first)
        assertNotNull(check.extra)
        assertEquals(14, check.totalDie)
        assertEquals("تاس اضافه هم باید از GM پرسیده شود", 2, rolls)
    }

    /** آسیب هم از همان منبع می‌آید؛ دو تا ۶ روی تاس‌ها یعنی Critical Injury. */
    @Test
    fun damageRolls_goThroughDiceSource() {
        DiceSource.manual = { req -> List(req.count) { 6 } }
        val dmg = CombatRules.rollDamage("3d6")
        assertEquals(listOf(6, 6, 6), dmg.dice)
        assertEquals(18, dmg.total)
        assertTrue("سه تا ۶ باید Critical بدهد", dmg.critical)
    }

    /**
     * اگر پاسخ GM نرسد (انصراف یا پایان مهلت) برنامه تصادفی می‌ریزد تا بازی نخوابد،
     * ولی این کار **بی‌صدا** نیست: onManualFallback صدا زده می‌شود تا UI هشدار بدهد.
     */
    @Test
    fun unavailableManualRequest_reportsFallback() {
        var reported: DiceSource.Request? = null
        DiceSource.manual = { emptyList() }          // مثل انصراف GM / تایم‌اوت
        DiceSource.onManualFallback = { reported = it }

        val roll = DiceSource.roll(1, 10, "تست مرگ")

        assertEquals(1, roll.size)
        assertTrue("نتیجه باید داخل محدوده باشد", roll[0] in 1..10)
        assertNotNull("افتادن به تصادفی باید گزارش شود", reported)
        assertEquals("تست مرگ", reported?.label)
    }

    /** حالت خودکار (هیچ میزبانی نصب نیست) هم باید عدد معتبر بدهد و گزارش هشدار نکند. */
    @Test
    fun autoMode_returnsValidDiceWithoutFallbackReport() {
        var reported = false
        DiceSource.manual = null
        DiceSource.onManualFallback = { reported = true }

        repeat(50) {
            assertTrue(DiceSource.rollOne(6) in 1..6)
        }
        assertTrue(DiceSource.roll(4, 10).all { it in 1..10 })
        assertFalse("حالت خودکار نباید هشدار تصادفی بدهد", reported)
    }

    /** تاس صفر یا منفی نباید هیچ درخواستی بسازد. */
    @Test
    fun zeroDice_meansNoRequest() {
        var asked = false
        DiceSource.manual = { asked = true; emptyList() }
        assertEquals(emptyList<Int>(), DiceSource.roll(0, 6))
        assertFalse(asked)
    }
}
