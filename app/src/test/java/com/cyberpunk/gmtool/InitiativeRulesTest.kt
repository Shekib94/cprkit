package com.cyberpunk.gmtool

import com.cyberpunk.gmtool.data.Character
import com.cyberpunk.gmtool.data.CombatRules
import com.cyberpunk.gmtool.data.DiceSource
import com.cyberpunk.gmtool.data.InventoryItem
import com.cyberpunk.gmtool.data.Stats
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * تست‌های Initiative — قفلِ «یک فرمول، دو دکمه».
 *
 * چرا این فایل وجود دارد:
 * فرمول Initiative قبلاً **دو نسخه** داشت: یکی در `CombatTab` برای دکمه‌ی
 * «ROLL SELF» که Sandevistan فعال را حساب می‌کرد و یکی در ViewModel برای
 * «ROLL ALL» که نمی‌کرد. یعنی یک کاراکتر با Sandevistan فعال، بسته به اینکه
 * دکمه‌ی خودش زده شود یا دکمه‌ی گروهی، دو عدد مختلف Initiative می‌گرفت —
 * و این دقیقاً همان نوع ناهم‌راستایی است که سر میز بدون تست دیده نمی‌شود.
 *
 * حالا هر دو مسیر `CombatRules.rollInitiative` را صدا می‌زنند و این تست همان
 * فرمول را قفل می‌کند: REF + Kerenzikov (+۲) + Sandevistan (+۳ فقط وقتی فعال
 * است) + Solo `initiativeReaction` − جریمه‌ی زره.
 */
class InitiativeRulesTest {

    @After
    fun tearDown() {
        DiceSource.manual = null
        DiceSource.onManualFallback = null
    }

    private fun character(
        ref: Int = 6,
        inventory: List<InventoryItem> = emptyList(),
        soloRank: Int = 0
    ) = Character(
        role = "Solo",
        stats = Stats(ref = ref),
        inventory = inventory,
        roleAbilityPoints = if (soloRank == 0) emptyMap() else mapOf("initiativeReaction" to soloRank)
    )

    private val kerenzikov = InventoryItem(name = "Kerenzikov", category = "Cyberware", equipped = true)

    /** کاراکتر ساده: فقط REF. */
    @Test
    fun bonus_withNoGear_isJustRef() {
        val b = CombatRules.initiativeBonus(character(ref = 7))
        assertEquals(7, b.ref)
        assertEquals(0, b.kerenzikov)
        assertEquals(0, b.sandevistan)
        assertEquals(0, b.solo)
        assertEquals(0, b.armorPenalty)
        assertEquals(0, b.speedware)
    }

    /** Kerenzikov همیشه +۲ می‌دهد (نه فقط وقتی فعال باشد). */
    @Test
    fun kerenzikov_isAlwaysPlusTwo() {
        val b = CombatRules.initiativeBonus(character(inventory = listOf(kerenzikov)))
        assertEquals(2, b.kerenzikov)
        assertEquals(2, b.speedware)
    }

    /** Sandevistan فقط در بازه‌ی فعال بودن +۳ می‌دهد و بعد از تمام‌شدن اثر صفر است. */
    @Test
    fun sandevistan_isPlusThreeOnlyWhileActive() {
        val now = 1_000_000L
        val active = CombatRules.initiativeBonus(
            character(inventory = listOf(kerenzikov)), sandevistanActiveUntil = now + 60_000, now = now
        )
        assertEquals(3, active.sandevistan)
        assertEquals("Kerenzikov + Sandevistan", 5, active.speedware)

        val expired = CombatRules.initiativeBonus(
            character(inventory = listOf(kerenzikov)), sandevistanActiveUntil = now - 1, now = now
        )
        assertEquals(0, expired.sandevistan)
        assertEquals(2, expired.speedware)
    }

    /** قابلیت Solo روی Initiative اضافه می‌شود. */
    @Test
    fun soloInitiativeReaction_addsToTotal() {
        val b = CombatRules.initiativeBonus(character(soloRank = 4))
        assertEquals(4, b.solo)
    }

    /**
     * مسیر کامل: تاس از `DiceSource` می‌آید و مجموع = تاس + REF + سرعت‌افزار
     * + Solo − جریمه‌ی زره. برای قطعی‌بودن تست، خروجی GM را ثابت می‌کنیم.
     */
    @Test
    fun fullRoll_isDiePlusEveryBonus() {
        DiceSource.manual = { listOf(7) }        // GM می‌گوید تاس ۷

        val c = character(ref = 6, inventory = listOf(kerenzikov), soloRank = 3)
        val r = CombatRules.rollInitiative(c, sandevistanActiveUntil = System.currentTimeMillis() + 60_000)

        assertEquals(7, r.dice.first)
        assertEquals("7", r.die)
        assertEquals(7 + 6 + 2 + 3 + 3, r.total)
    }

    /** Critical روی تاس ۱۰ برچسب `10+extra` می‌سازد و مقدار تاس اضافه هم در مجموع می‌آید. */
    @Test
    fun criticalTen_isLabeledAndCounted() {
        var calls = 0
        DiceSource.manual = { calls++; if (calls == 1) listOf(10) else listOf(4) }

        val r = CombatRules.rollInitiative(character(ref = 6))
        assertEquals("10+4", r.die)
        assertEquals(14 + 6, r.total)
    }
}
