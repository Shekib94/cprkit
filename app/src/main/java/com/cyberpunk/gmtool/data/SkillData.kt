package com.cyberpunk.gmtool.data

import java.io.Serializable

// یک مهارت کاراکتر
data class SkillData(
    val name: String,
    val stat: String,   // نام کامل استات: Intelligence/Reflexes/Dexterity/Technique/Cool/Willpower/Movement/Body/Empathy
    val level: Int = 0,
    val totalMod: Int = 0,
    val description: String = ""
) : Serializable

// ============================================================
// کاتالوگ رسمی مهارت‌های Cyberpunk RED (کتاب Core / برگه‌ی رسمی)
// نگاشت مهارت ← استات بر اساس برگه‌ی رسمی RTG.
// ============================================================
object SkillCatalog {

    private val statBySkill: Map<String, String> = mapOf(
        // --- Awareness ---
        "Concentration" to "Willpower",
        "Conceal/Reveal Object" to "Intelligence",
        "Lip Reading" to "Intelligence",
        "Perception" to "Intelligence",
        "Tracking" to "Intelligence",

        // --- Body ---
        "Athletics" to "Dexterity",
        "Contortionist" to "Dexterity",
        "Dance" to "Dexterity",
        "Endurance" to "Willpower",
        "Resist Torture/Drugs" to "Willpower",
        "Stealth" to "Dexterity",

        // --- Control ---
        "Drive Land Vehicle" to "Reflexes",
        "Pilot Air Vehicle" to "Reflexes",
        "Pilot Sea Vehicle" to "Reflexes",
        "Riding" to "Reflexes",

        // --- Education ---
        "Accounting" to "Intelligence",
        "Animal Handling" to "Intelligence",
        "Bureaucracy" to "Intelligence",
        "Business" to "Intelligence",
        "Composition" to "Intelligence",
        "Criminology" to "Intelligence",
        "Cryptography" to "Intelligence",
        "Deduction" to "Intelligence",
        "Education" to "Intelligence",
        "Gamble" to "Intelligence",
        "Library Search" to "Intelligence",
        "Tactics" to "Intelligence",
        "Wilderness Survival" to "Intelligence",

        // --- Fighting ---
        "Brawling" to "Dexterity",
        "Evasion" to "Dexterity",
        "Martial Arts" to "Dexterity",
        "Melee Weapon" to "Dexterity",

        // --- Performance ---
        "Acting" to "Cool",
        "Play Instrument" to "Technique",

        // --- Ranged Weapons ---
        "Archery" to "Reflexes",
        "Autofire" to "Reflexes",
        "Handgun" to "Reflexes",
        "Heavy Weapons" to "Reflexes",
        "Shoulder Arms" to "Reflexes",

        // --- Social ---
        "Bribery" to "Cool",
        "Conversation" to "Empathy",
        "Human Perception" to "Empathy",
        "Interrogation" to "Cool",
        "Persuasion" to "Cool",
        "Personal Grooming" to "Cool",
        "Streetwise" to "Cool",
        "Trading" to "Cool",
        "Wardrobe & Style" to "Cool",

        // --- Technique ---
        "Air Vehicle Tech" to "Technique",
        "Basic Tech" to "Technique",
        "Cybertech" to "Technique",
        "Demolitions" to "Technique",
        "Electronics/Security Tech" to "Technique",
        "First Aid" to "Technique",
        "Forgery" to "Technique",
        "Land Vehicle Tech" to "Technique",
        "Paint/Draw/Sculpt" to "Technique",
        "Paramedic" to "Technique",
        "Photography/Film" to "Technique",
        "Pick Lock" to "Technique",
        "Pick Pocket" to "Technique",
        "Sea Vehicle Tech" to "Technique",
        "Weaponstech" to "Technique"
    )

    // نام‌های پارامتری (Language/Local Expert/Science/Play Instrument) را به استات درست نگاشت می‌کند
    private fun canonicalName(skillName: String): String =
        skillName.replace(Regex("\\s*\\(x2\\)\\s*$", RegexOption.IGNORE_CASE), "").trim()

    fun isKnownSkill(skillName: String): Boolean {
        val name = canonicalName(skillName)
        return statBySkill.containsKey(name) || name.startsWith("Language") ||
            name.startsWith("Local Expert") || name.startsWith("Science") || name.startsWith("Play Instrument")
    }

    fun statFor(skillName: String): String {
        val name = canonicalName(skillName)
        statBySkill[name]?.let { return it }
        return when {
            name.startsWith("Language") -> "Intelligence"
            name.startsWith("Local Expert") -> "Intelligence"
            name.startsWith("Science") -> "Intelligence"
            name.startsWith("Play Instrument") -> "Technique"
            // Explicit non-Core/homebrew aliases used by imported NPCs.
            name.equals("Hacking", true) -> "Intelligence"
            name.equals("Intimidation", true) -> "Cool"
            name.equals("Maker", true) -> "Technique"
            name.equals("Surgery", true) -> "Technique"
            name.equals("Medical Tech", true) -> "Technique"
            // Legacy compatibility only: unknown imported skills previously defaulted to INT.
            else -> "Intelligence"
        }
    }

    // ساخت فهرست مهارت‌های کاراکتر از روی سطح‌های رول/انتخاب‌شده
    fun buildSkills(levels: Map<String, Int>): List<SkillData> {
        return levels.entries
            .map { (name, lvl) -> SkillData(name = name, stat = statFor(name), level = lvl) }
            .sortedBy { it.name }
    }

    // هفت مهارت Difficult (x2) طبق Core ص۸۲ — هزینه‌ی ساخت و IP آن‌ها دوبرابر است.
    private val difficultSkills = setOf(
        "Autofire", "Heavy Weapons", "Martial Arts", "Demolitions",
        "Electronics/Security Tech", "Paramedic", "Pilot Air Vehicle"
    )

    fun isDifficult(skillName: String): Boolean =
        difficultSkills.any { skillName.equals(it, ignoreCase = true) }

    fun creationPointCost(skillName: String, level: Int): Int =
        level.coerceAtLeast(0) * if (isDifficult(skillName)) 2 else 1

    fun improvementCost(skillName: String, targetLevel: Int): Int =
        targetLevel.coerceAtLeast(1) * if (isDifficult(skillName)) 40 else 20

    // مهارت‌های ثابت + مهارت‌های پایه. Language/Local Expert/Science/Instrumentهای سفارشی از UI اضافه می‌شوند.
    fun allSkillNames(): List<String> = (statBySkill.keys + StreetratData.baseLifeSkills).distinct().sorted()

    // مهارت‌های مجاز برای متد Edgerunners (محدود به نقش)
    // = ۱۳ مهارت پایه + مهارت‌های نقش + زبان بومی رول‌شده
    fun allowedSkillsForRole(role: String, nativeLanguage: String? = null): List<String> {
        val roleMap = StreetratData.getSkillsForRole(role, 1)
        val names = (baseLifeNames() + roleMap.keys).toMutableList()
        if (nativeLanguage != null) names.add(nativeLanguage)
        return names.distinct().sorted()
    }

    // فقط ۱۳ مهارت پایه (برای تضمین حداقل ۲)
    fun baseLifeNames(): List<String> = StreetratData.baseLifeSkills
}
