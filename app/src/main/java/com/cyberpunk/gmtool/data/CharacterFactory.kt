package com.cyberpunk.gmtool.data



object CharacterFactory {

    private fun expandChoices(choices: List<String>): List<String> =
        choices.flatMap { it.split("||").map(String::trim).filter(String::isNotBlank) }

    private fun nativeLanguageSkill(lifepath: LifepathData): String? {
        val raw = lifepath.culturalOrigins
        val lang = Regex("زبان مادری:\\s*([^\\)]+)").find(raw)?.groupValues?.getOrNull(1)?.trim()
        return lang?.takeIf { it.isNotBlank() }?.let { gtr("Language (%1s)", it) }
    }

    private fun withFreeNativeLanguage(levels: Map<String, Int>, lifepath: LifepathData): Map<String, Int> {
        val language = nativeLanguageSkill(lifepath) ?: return levels
        val out = levels.toMutableMap()
        out[language] = maxOf(out[language] ?: 0, 4)
        return out
    }

    private fun humanityFromInstalled(emp: Int, items: List<String>): Pair<Int, Int> {
        val installLoss = items.sumOf { CyberwareCatalog.humanityLossFor(it) }
        val maxReduction = items.sumOf { CyberwareCatalog.maxReductionFor(it) }
        val naturalMax = emp * 10
        return (naturalMax - maxReduction).coerceAtLeast(0) to (naturalMax - installLoss).coerceAtLeast(0)
    }

    private fun housingFor(roleName: String): Triple<String, String, Int> =
        if (roleName.equals("Exec", true)) Triple("Good Prepak", "Corporate Conapt", 0)
        else Triple("Kibble", "Cargo Container", 1000)

    fun create(
        legalName: String,
        handle: String,
        roleName: String,
        creationMethod: String,
        stats: Stats,
        skillLevels: Map<String, Int>,
        equipment: StartingEquipment,
        selectedChoices: List<String>,
        lifepath: LifepathData = LifepathData()
    ): Character {
        val displayName = if (legalName.isNotBlank()) legalName else handle
        val parsed = EquipmentParser.parseStarting(equipment, selectedChoices)
        val skills = SkillCatalog.buildSkills(withFreeNativeLanguage(skillLevels, lifepath))
        val maxHp = GameRules.maxHp(stats.body, stats.will)
        val (maxHumanity, currentHumanity) = humanityFromInstalled(stats.emp, EquipmentParser.cyberwareNames(equipment, selectedChoices))
        val (lifestyle, housing, rent) = housingFor(roleName)

        return Character(
            name = displayName, handle = handle, role = roleName, creationMethod = creationMethod,
            improvementPoints = 0, roleRank = 4, reputation = 0,
            hp = maxHp, maxHp = maxHp,
            headArmorSp = parsed.headArmorSp, bodyArmorSp = parsed.bodyArmorSp,
            currentHumanity = currentHumanity.coerceAtMost(maxHumanity), maxHumanity = maxHumanity, baseEmp = stats.emp, baseBody = stats.body,
            currentLuck = stats.luck, maxLuck = stats.luck, eurodollars = 500, fashionBudget = 0,
            isAlly = true, stats = stats, skills = skills, weapons = parsed.weapons,
            inventory = parsed.inventory, lifepath = lifepath, roleAbilityPoints = emptyMap(),
            description = "", lifestyle = lifestyle, housing = housing, monthlyRent = rent
        )
    }

    /** Edgerunner receives the role package plus 500eb. Complete Package receives no free role package and starts with 2,550eb to shop. */
    fun createCustom(
        legalName: String,
        handle: String,
        roleName: String,
        creationMethod: String,
        stats: Stats,
        skillLevels: Map<String, Int>,
        startingEurodollars: Int? = null,
        selectedChoices: List<String> = emptyList(),
        lifepath: LifepathData = LifepathData()
    ): Character {
        val displayName = if (legalName.isNotBlank()) legalName else handle
        val isComplete = creationMethod.contains("Complete", true)
        val equipment = if (isComplete) null else StreetratData.getEquipmentForRole(roleName)
        val parsed = if (equipment == null) EquipmentParser.parse(emptyList()) else EquipmentParser.parseStarting(equipment, selectedChoices)
        val skills = SkillCatalog.buildSkills(withFreeNativeLanguage(skillLevels, lifepath))
        val maxHp = GameRules.maxHp(stats.body, stats.will)
        val (maxHumanity, currentHumanity) = humanityFromInstalled(stats.emp, equipment?.let { EquipmentParser.cyberwareNames(it, selectedChoices) } ?: emptyList())
        val (lifestyle, housing, rent) = housingFor(roleName)
        val cash = startingEurodollars ?: if (isComplete) 2550 else 500

        return Character(
            name = displayName, handle = handle, role = roleName, creationMethod = creationMethod,
            improvementPoints = 0, roleRank = 4, reputation = 0,
            hp = maxHp, maxHp = maxHp, headArmorSp = parsed.headArmorSp, bodyArmorSp = parsed.bodyArmorSp,
            currentHumanity = currentHumanity.coerceAtMost(maxHumanity), maxHumanity = maxHumanity, baseEmp = stats.emp, baseBody = stats.body,
            currentLuck = stats.luck, maxLuck = stats.luck, eurodollars = cash, fashionBudget = if (isComplete) 800 else 0,
            isAlly = true, stats = stats, skills = skills, weapons = parsed.weapons,
            inventory = parsed.inventory, lifepath = lifepath, roleAbilityPoints = emptyMap(), description = "",
            lifestyle = lifestyle, housing = housing, monthlyRent = rent
        )
    }
}
