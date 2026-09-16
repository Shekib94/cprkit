package com.cyberpunk.gmtool.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * تست جدول Backup لاومن.
 *
 * چرا این تست: گزارش کاربر این بود که «سیستم Backup» معلوم نیست چطور کار
 * می‌کند و آیا همه‌ی NPCها برایش ساخته می‌شوند. اینجا همان چیزهایی که در
 * Core آمده تست می‌شود: شرایط موفقیت تماس (1d10 مساوی یا کمتر از Rank)،
 * جدول رده‌ها برای Rank ۱ تا ۱۰، 1d6 برای رسیدن و ارتقای رده با تاس ۶، و
 * اینکه هر رده دقیقاً به یک تمپلیت واقعی در [NpcData] وصل باشد تا دکمه‌ی
 * «ساخت NPC» هیچ‌وقت به تمپلیت ناموجود نخورد.
 */
class BackupRulesTest {

    @Test
    fun callSucceedsWhenDieIsEqualOrUnderRank() {
        assertTrue(BackupRules.callSucceeds(4, 1))
        assertTrue(BackupRules.callSucceeds(4, 4))
        assertFalse(BackupRules.callSucceeds(4, 5))
        assertFalse(BackupRules.callSucceeds(1, 10))
        assertTrue(BackupRules.callSucceeds(10, 10))
    }

    @Test
    fun tierTableCoversRankOneToTen() {
        assertEquals(10, BackupRules.tiers.size)
        assertEquals("Corporate Security", BackupRules.unitForRank(1).tierName)
        assertEquals("Local Beat Cops", BackupRules.unitForRank(3).tierName)
        assertEquals("Sheriff's Department", BackupRules.unitForRank(5).tierName)
        assertEquals("Recovery Zone Marshal", BackupRules.unitForRank(8).tierName)
        assertEquals("C-SWAT (Psycho Squad)", BackupRules.unitForRank(9).tierName)
        assertEquals("National Law Enforcement", BackupRules.unitForRank(10).tierName)
        // ورودی خارج از بازه نباید کرش کند (Rank صفر یا منفی از برگه‌ی ناقص می‌آید).
        assertEquals("Corporate Security", BackupRules.unitForRank(0).tierName)
        assertEquals("National Law Enforcement", BackupRules.unitForRank(99).tierName)
    }

    @Test
    fun naturalSixOnArrivalUpgradesTheTier() {
        assertTrue(BackupRules.upgradesTier(6))
        for (d in 1..5) assertFalse(BackupRules.upgradesTier(d))
        assertEquals("Local Beat Cops", BackupRules.upgradedUnit(2).tierName)
        // در Rank ۱۰ رده‌ی بالاتری وجود ندارد؛ همان رده می‌ماند ولی دو گروه می‌رسد.
        assertEquals(BackupRules.unitForRank(10).tierName, BackupRules.upgradedUnit(10).tierName)
        assertTrue(BackupRules.upgradedCountIsDoubled(10))
        assertFalse(BackupRules.upgradedCountIsDoubled(9))
    }

    @Test
    fun everyTierPointsAtARealNpcTemplate() {
        BackupRules.tiers.forEach { unit ->
            val found = NpcData.allTemplates.firstOrNull { it.name.equals(unit.npcTemplateName, true) }
            assertNotNull("تمپلیت «${unit.npcTemplateName}» در NpcData نیست (رده ${unit.tierName})", found)
        }
    }

    @Test
    fun pendingEffectRoundTripsThroughCharacterState() {
        val key = BackupRules.pendingEffectKey(3)
        val c = Character(name = "Test", role = "Lawman", roleRank = 6, combatEffects = mapOf(key to 1))
        assertTrue(BackupRules.isPending(c))
        assertEquals(3, BackupRules.pendingRounds(c) ?: -1)
        val clean = c.copy(combatEffects = emptyMap())
        assertFalse(BackupRules.isPending(clean))
        assertNull(BackupRules.pendingRounds(clean))
    }

    @Test
    fun quickGuideExplainsTheWholeFlow() {
        assertTrue(BackupRules.quickGuide.size >= 5)
        val text = BackupRules.quickGuide.joinToString(" ") { (t, b) -> "$t $b" }
        assertTrue(text.contains("d10"))
        assertTrue(text.contains("d6"))
        assertTrue(text.contains("Rank"))
        assertTrue(text.contains("NPC"))
    }
}
