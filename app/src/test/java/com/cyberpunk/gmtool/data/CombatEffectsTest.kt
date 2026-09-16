package com.cyberpunk.gmtool.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * تست طبقه‌بندی اثرهای رزمی.
 *
 * چرا: گزارش کاربر این بود که کارت ACTIVE COMBAT EFFECTS همه‌چیز را نشان
 * می‌داد (حتی شمارنده‌های داخلی مثل «MOVED THIS TURN 7») و معلوم نبود کدام
 * اثر واقعاً روی نبرد اثر دارد. این تست همان قرارداد را قفل می‌کند:
 * فقط وضعیت‌های رزمی نمایش داده شوند، هرکدام برچسب و توضیح مکانیکی داشته
 * باشند، و اثرهای دارویی/درمانی/پشتیبان در این کارت نیایند.
 */
class CombatEffectsTest {

    @Test
    fun internalBookkeepingIsNeverShown() {
        listOf(
            "moved_this_turn_7", "run_used_this_turn", "turn_hit_brawling_3",
            "turn_melee_count_2", "used_ma_iron_grip", "judo_melee_hit_since_turn",
            "death_save_done", "critical_effect_removed_broken_leg", "combat_shield_hp_5",
            "in_cover", "in_vehicle_2", "dropped_weapon_9"
        ).forEach { key ->
            assertEquals("کلید $key نباید به GM نشان داده شود", CombatEffects.Kind.INTERNAL, CombatEffects.kind(key))
        }
    }

    @Test
    fun nonCombatEffectsAreCountedNotListed() {
        listOf("quickfix_broken_leg", "backup_en_route_3", "drug_active", "antibiotic", "stim", "surge")
            .forEach { assertEquals("کلید $it نباید در کارت نبرد بیاید", CombatEffects.Kind.NON_COMBAT, CombatEffects.kind(it)) }

        val effects = mapOf(
            "prone" to 0,
            "quickfix_broken_leg" to -1,
            "backup_en_route_2" to -1,
            "moved_this_turn_4" to 1
        )
        val combat = CombatEffects.combatEntries(effects)
        assertEquals(1, combat.size)
        assertEquals("prone", combat.first().first)
        assertEquals(2, CombatEffects.hiddenNonCombatCount(effects))
    }

    @Test
    fun everyCombatStateHasPersianLabelAndMechanicalDetail() {
        listOf(
            "prone", "unconscious", "on_fire_2", "on_fire_4", "suppressed",
            "must_seek_cover_this_turn", "no_action_this_turn", "no_move_this_turn",
            "no_action_next_turn", "no_move_next_turn", "stabilized",
            "damaged_eye", "damaged_ear", "grappled_by_3", "grappling_3", "iron_grip_by_3",
            "human_shield_3"
        ).forEach { key ->
            val e = CombatEffects.entry(key)
            assertEquals("کلید $key باید اثر رزمی باشد", CombatEffects.Kind.COMBAT, e.kind)
            assertTrue("برچسب فارسی برای $key خالی است", e.labelFa.isNotBlank())
            assertTrue("توضیح مکانیکی برای $key خالی است", e.detailFa.isNotBlank())
        }
    }

    @Test
    fun grappleEffectsNameTheOtherSide() {
        val grappled = CombatEffects.entry("grappled_by_12")
        assertTrue(grappled.labelFa.contains("12"))
        val grappling = CombatEffects.entry("grappling_7")
        assertTrue(grappling.labelFa.contains("7"))
    }

    @Test
    fun onlySoftStatesCanBeClearedByHand() {
        assertTrue(CombatEffects.canClearManually("prone"))
        assertTrue(CombatEffects.canClearManually("must_seek_cover_this_turn"))
        assertTrue(CombatEffects.canClearManually("no_move_next_turn"))
        // گرپل و سپر و آتش مسیر خودشان را دارند؛ پاک‌کردن دستی state را خراب می‌کند.
        assertFalse(CombatEffects.canClearManually("grappled_by_3"))
        assertFalse(CombatEffects.canClearManually("grappling_3"))
        assertFalse(CombatEffects.canClearManually("on_fire_2"))
        assertFalse(CombatEffects.canClearManually("human_shield_3"))
    }
}
