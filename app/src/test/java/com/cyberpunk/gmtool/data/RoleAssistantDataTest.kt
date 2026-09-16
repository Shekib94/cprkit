package com.cyberpunk.gmtool.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoleAssistantDataTest {
    @Test fun corePharmaceuticals_areExactlyFiveAndUnique() {
        assertEquals(5, RoleAssistantData.corePharmaceuticals.size)
        assertEquals(5, RoleAssistantData.corePharmaceuticals.map { it.name }.toSet().size)
        assertEquals(setOf("Antibiotic", "Rapidetox", "Speedheal", "Stim", "Surge"), RoleAssistantData.corePharmaceuticals.map { it.name }.toSet())
    }

    @Test fun rumorDvs_matchCoreProgression() {
        assertEquals(listOf(7, 9, 11, 13), RoleAssistantData.rumorLevels.map { it.passiveDv })
        assertEquals(listOf(13, 15, 17, 21), RoleAssistantData.rumorLevels.map { it.activeDv })
    }

    @Test fun credibilityBelievability_progressionMatchesCore() {
        val expected = listOf(2,2,3,3,4,4,5,5,6,7)
        assertEquals(expected, (1..10).map { RoleAssistantData.mediaProfile(it).believability })
    }

    @Test fun cryoProgression_hasAllFiveLevels() {
        assertEquals(listOf(1,2,3,4,5), RoleAssistantData.cryoBenefits.map { it.level })
        assertTrue(RoleAssistantData.cryoBenefits.last().textFa.contains("۳ نفر"))
    }
    @Test fun nomadAssistant_hasCoreRelevantSituationsAndHooks() {
        assertTrue(RoleAssistantData.nomadInteractionSituations.contains("وسیله یا تعویض وسیله"))
        assertTrue(RoleAssistantData.nomadInteractionSituations.contains("تعمیر یا بازیابی Family Vehicle"))
        assertTrue(RoleAssistantData.nomadHooks.size >= 6)
        val advice = RoleAssistantData.nomadInteractionAdvice("وسیله یا تعویض وسیله", familyNearby = true, urgent = false)
        assertEquals("RECOMMENDED", advice.level)
        assertTrue(advice.adviceFa.contains("صبح روز بعد"))
    }

}
