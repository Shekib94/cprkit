package com.cyberpunk.gmtool

import com.cyberpunk.gmtool.data.Character
import com.cyberpunk.gmtool.data.CombatRules
import com.cyberpunk.gmtool.data.CriticalInjuries
import com.cyberpunk.gmtool.data.DiceSource
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.ArrayDeque

/**
 * تست «هیچ تاسی بی‌صدا ساخته نمی‌شود».
 *
 * مسئله‌ای که داشتیم: در حالت **تاس دستی**، همه‌ی تاس‌های قواعد باید از
 * [DiceSource] رد شوند تا GM عدد تاس فیزیکی را وارد کند. اما انتخاب جراحت
 * بحرانی در حالتِ نادری که رول به گزینه‌های آزاد نمی‌خورد به
 * `available.random()` می‌افتاد — یعنی برنامه بدون پرسیدن از کسی، یک جراحت
 * بحرانی از خودش درمی‌آورد. حالا آن انتخاب آخر هم یک درخواست تاس صریح است.
 *
 * اجرا:  ./gradlew test
 */
class CriticalInjuryDiceTest {

    /** همه‌ی درخواست‌های تاسی که در طول تست از DiceSource گذشته‌اند. */
    private val diceRequests = mutableListOf<DiceSource.Request>()

    /**
     * DiceSource را روی یک سناریوی از پیش نوشته‌شده قفل می‌کند.
     * هر درخواست، صف را یک قدم جلو می‌برد و همان تعداد تاس را برمی‌گرداند.
     */
    private fun scriptDice(vararg rolls: List<Int>) {
        val queue = ArrayDeque(rolls.toList())
        diceRequests.clear()
        DiceSource.manual = { req ->
            diceRequests += req
            val next = queue.poll()
            if (next != null) next.take(req.count) else emptyList()
        }
    }

    @After
    fun restoreAutomaticDice() {
        DiceSource.manual = null
        diceRequests.clear()
    }

    private fun target(criticalInjuries: List<String> = emptyList()) = Character(
        id = 1, name = "Test", hp = 40, maxHp = 40, criticalInjuries = criticalInjuries
    )

    /** رول ۱۲ روی جدول بدن = Dismembered Leg. */
    @Test
    fun criticalRollTwelve_picksDismemberedLegThroughDiceSource() {
        scriptDice(listOf(6, 6))

        val (after, resolution) = CombatRules.resolveDamage(
            target = target(), rawDamage = 12, head = false, melee = false, critical = true
        )

        assertEquals(listOf("dismembered_leg"), after.criticalInjuries)
        assertEquals("dismembered_leg", resolution.injuryKey)
        assertEquals("تاس باید از DiceSource رد شود", 1, diceRequests.size)
        assertTrue(diceRequests.first().label.contains("Critical Injury"))
    }

    /** قاعده‌ی Core: اگر جراحت تکراری باشد، آن‌قدر دوباره می‌ریزی تا یکی آزاد بیاید. */
    @Test
    fun rerollsUntilTheInjuryIsNotAlreadySuffered() {
        // رول اول ۱۲ → dismembered_leg که هدف همین حالا دارد؛ رول دوم ۱۰ → spinal_injury.
        scriptDice(listOf(6, 6), listOf(5, 5))
        val before = target(criticalInjuries = listOf("dismembered_leg"))

        val (after, _) = CombatRules.resolveDamage(
            target = before, rawDamage = 12, head = false, melee = false, critical = true
        )

        assertEquals(listOf("dismembered_leg", "spinal_injury"), after.criticalInjuries)
        assertEquals("باید دقیقاً دو بار رول شده باشد", 2, diceRequests.size)
    }

    /**
     * حالت سخت: تقریباً کل جدول روی یک نفر ریخته شده. انتخابِ آخرین گزینه‌ی
     * آزاد باید هم از تاس بیاید و هم در فهرست درخواست‌های DiceSource دیده شود —
     * نه از `Random` داخلی.
     */
    @Test
    fun lastResortPickIsAlsoDiceDriven_neverSilentRandom() {
        val held = CriticalInjuries.bodyInjuries.map { it.key }.filter { it != "torn_muscle" }
        // سی رولِ ۲ (dismembered_arm — قبلاً داشته) و بعد یک dN برای انتخاب آخر.
        // نوع صریح لازم است: بدون آن، `+ listOf(1)` به overload الحاقِ
        // عناصر با T=Any حل می‌شد (Array<Any> به‌جای Array<List<Int>>).
        val rolls: List<List<Int>> = (1..30).map { listOf(1, 1) } + listOf(listOf(1))
        scriptDice(*rolls.toTypedArray())

        val (after, resolution) = CombatRules.resolveDamage(
            target = target(criticalInjuries = held),
            rawDamage = 12, head = false, melee = false, critical = true
        )

        assertEquals("torn_muscle", resolution.injuryKey)
        assertTrue(after.criticalInjuries.contains("torn_muscle"))
        assertEquals(
            "۳۰ رول جدول + ۱ انتخاب نهایی؛ همه از DiceSource",
            31, diceRequests.size
        )
        assertTrue(
            "آخرین درخواست باید انتخاب dN باشد، نه تاس تصادفی",
            diceRequests.last().label.contains("d1")
        )
    }

    /** جراحت بحرانی هیچ‌وقت نباید تکراری روی یک برگه بنشیند. */
    @Test
    fun neverAssignsAnInjuryTheTargetAlreadyHas() {
        val held = listOf("dismembered_arm", "dismembered_leg", "spinal_injury")
        scriptDice(*(1..40).map { listOf(6, 6) }.toTypedArray())

        val (after, _) = CombatRules.resolveDamage(
            target = target(criticalInjuries = held),
            rawDamage = 15, head = false, melee = false, critical = true
        )

        val duplicated = after.criticalInjuries.groupingBy { it }.eachCount().filter { it.value > 1 }
        assertTrue("جراحت تکراری ثبت شده: $duplicated", duplicated.isEmpty())
        assertFalse(after.criticalInjuries.size == held.size)
    }

    /** ضربه به سر باید از جدول سر انتخاب کند، نه جدول بدن. */
    @Test
    fun headshotRollsOnTheHeadTable() {
        scriptDice(listOf(6, 6)) // رول ۱۲ در جدول سر = lost_ear

        val (after, resolution) = CombatRules.resolveDamage(
            target = target(), rawDamage = 10, head = true, melee = false, critical = true
        )

        assertEquals("lost_ear", resolution.injuryKey)
        val headKey = CriticalInjuries.headInjuries.first { it.roll == 12 }.key
        assertEquals(headKey, after.criticalInjuries.single())
    }
}
