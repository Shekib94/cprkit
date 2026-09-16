package com.cyberpunk.gmtool

import com.cyberpunk.gmtool.data.Character
import com.cyberpunk.gmtool.data.DiceSource
import com.cyberpunk.gmtool.data.MedtechCareRules
import com.cyberpunk.gmtool.data.SkillData
import com.cyberpunk.gmtool.data.Stats
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * قواعد درمان Medtech که حالا در GM Tools هم استفاده می‌شود.
 *
 * چرا تست لازم است: این اعداد مستقیم روی برگه‌ی بازیکن‌ها اثر می‌گذارند
 * (HP، جراحت، Death Save). اگر روزی کسی DV یا فرمول را جابه‌جا کند، باید
 * همین‌جا سرخ شود، نه سر میز.
 *
 * اجرا:  ./gradlew test
 */
class MedtechCareTest {

    private val requested = mutableListOf<DiceSource.Request>()

    private fun scriptDice(vararg rolls: Int) {
        val queue = ArrayDeque(rolls.toList())
        requested.clear()
        DiceSource.manual = { req -> requested += req; queue.removeFirstOrNull()?.let { listOf(it) } ?: emptyList() }
    }

    @After
    fun restore() {
        DiceSource.manual = null
        requested.clear()
    }

    private fun healer(tech: Int = 6, firstAid: Int = 4, paramedic: Int = 0, ip: Int = 0, body: Int = 6) = Character(
        id = 10, name = "Ripper", handle = "Ripper", role = "Medtech", roleRank = 4,
        hp = 40, maxHp = 40, improvementPoints = ip,
        stats = Stats(tech = tech, body = body, will = 5),
        skills = buildList {
            if (firstAid > 0) add(SkillData("First Aid", "Technique", firstAid))
            if (paramedic > 0) add(SkillData("Paramedic", "Technique", paramedic))
        },
        roleAbilityPoints = mapOf("surgery" to 2, "pharma" to 3, "cryo" to 2)
    )

    private fun patient(hp: Int, maxHp: Int = 40, injured: List<String> = emptyList()) = Character(
        id = 20, name = "V", handle = "V", hp = hp, maxHp = maxHp, criticalInjuries = injured,
        stats = Stats(body = 7, will = 6)
    )

    // ── DV پایدارسازی از وضعیت بیمار می‌آید ──
    @Test
    fun stabilizeDvFollowsPatientWoundState() {
        assertEquals(15, MedtechCareRules.stabilizeTarget(hp = 0, maxHp = 40, isDead = false).dv)
        assertEquals(13, MedtechCareRules.stabilizeTarget(hp = 20, maxHp = 40, isDead = false).dv)
        assertEquals(10, MedtechCareRules.stabilizeTarget(hp = 30, maxHp = 40, isDead = false).dv)
        // مرده با قواعد Stabilize برنمی‌گردد؛ GM باید صریح تصمیم بگیرد.
        assertTrue(MedtechCareRules.stabilizeTarget(hp = 0, maxHp = 40, isDead = true).dv >= 17)
    }

    @Test
    fun paramedicIsAnExplicitSkillAndTrainingIsRequired() {
        val untrained = healer(paramedic = 0)
        assertTrue(MedtechCareRules.needsParamedicTraining(untrained))
        assertEquals("First Aid" to 4, MedtechCareRules.bestStabilizeSkill(untrained))
        assertTrue(MedtechCareRules.canStabilize(untrained))

        val trained = healer(firstAid = 4, paramedic = 6)
        assertFalse(MedtechCareRules.needsParamedicTraining(trained))
        assertEquals("Paramedic" to 6, MedtechCareRules.bestStabilizeSkill(trained))

        // کسی که هیچ مهارت پزشکی ندارد، پزشک صحنه نیست.
        val civilian = Character(id = 30, name = "Civ", hp = 20, maxHp = 20)
        assertFalse(MedtechCareRules.canStabilize(civilian))
    }

    @Test
    fun stabilizeRollIsTECHPlusSkillVersusDV() {
        // TECH 6 + First Aid 4 + تاس 5 = 15 > DV13 (Seriously Wounded)
        scriptDice(5)
        val result = MedtechCareRules.rollStabilize(patient(hp = 20), healer())
        assertEquals("TECH + Medical skill", 15, result.total)
        assertEquals(13, result.dv)
        assertTrue(result.success)
        assertTrue("تاس باید از DiceSource بیاید", requested.isNotEmpty())
    }

    @Test
    fun mortallyWoundedStabilizationReturnsOneHp() {
        scriptDice(9)  // 6 + 4 + 9 = 19 → DV15 موفق
        val result = MedtechCareRules.rollStabilize(patient(hp = 0), healer())
        assertTrue(result.success)
        assertEquals(1, result.hpAfter)
    }

    @Test
    fun failedStabilizationLeavesWoundStateAlone() {
        scriptDice(1)  // 6 + 4 + 1 = 11 در برابر DV15
        val result = MedtechCareRules.rollStabilize(patient(hp = 0), healer())
        assertFalse(result.success)
        assertTrue(result.detailFa.contains("DV15"))
    }

    // ── داروها ──
    @Test
    fun medicalTechIsPharmaPlusCryo() {
        assertEquals(5, MedtechCareRules.medicalTechLevel(healer()))
        assertEquals(10, MedtechCareRules.medicalTechLevel(healer().copy(roleAbilityPoints = mapOf("pharma" to 5, "cryo" to 5))))
    }

    @Test
    fun speedhealGivesBodyPlusWillAndIsBlockedWhenMortallyWounded() {
        val medtech = healer()
        scriptDice(9)  // DV13 ساخت دارو
        val healed = MedtechCareRules.rollPharmaceutical(patient(hp = 20), medtech, "Speedheal")
        assertTrue(healed.success)
        assertEquals("BODY + WILL", 13, healed.hpDelta)
        assertNull(healed.blockedFa)
        assertFalse("داروی یادنگرفته باید علامت بخورد", healed.learned)

        scriptDice(9)
        val blocked = MedtechCareRules.rollPharmaceutical(patient(hp = 0), medtech, "Speedheal")
        assertEquals(0, blocked.hpDelta)
        assertNotNull(blocked.blockedFa)
    }

    @Test
    fun failedPharmaceuticalWastesOnlyTime() {
        scriptDice(2)  // 6 + 5 + 2 = 13 → DV13 رد (باید بزرگ‌تر باشد)
        val use = MedtechCareRules.rollPharmaceutical(patient(hp = 20), healer(), "Stim")
        assertFalse(use.success)
        assertNull(use.addEffect)
        assertTrue(use.detailFa.contains("ناموفق"))
    }

    @Test
    fun learnedDrugsComeFromTheSheetAndDoseCountFollowsMedicalTech() {
        val medtech = healer().copy(medtechPharmaceuticals = listOf("Stim", "speedheal"))
        assertEquals(setOf("stim", "speedheal"), MedtechCareRules.learnedPharmaceuticals(medtech).map { it.lowercase() }.toSet())
        scriptDice(9)
        val use = MedtechCareRules.rollPharmaceutical(patient(hp = 20), medtech, "Speedheal")
        assertTrue(use.learned)
        assertEquals("تعداد dose = Medical Tech", MedtechCareRules.medicalTechLevel(medtech), use.doses)
    }

    // ── بازیابی ──
    @Test
    fun naturalRecoveryIsBodyPerDayAndActivityInjuriesAreFlagged() {
        assertEquals(7, MedtechCareRules.naturalRecoveryPerDay(patient(hp = 10)))
        val hurt = patient(hp = 10, injured = listOf("broken_ribs", "torn_muscle"))
        assertEquals(listOf("broken_ribs"), MedtechCareRules.activityDamageInjuries(hurt))
        // جراحتی که Quick Fix شده، دیگر آسیب حرکتی نمی‌دهد.
        val fixed = hurt.copy(combatEffects = mapOf("quickfix_broken_ribs" to -1))
        assertTrue(MedtechCareRules.activityDamageInjuries(fixed).isEmpty())
    }
}
