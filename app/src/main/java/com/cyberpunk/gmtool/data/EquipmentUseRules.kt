package com.cyberpunk.gmtool.data

/** Mechanical install/load rules shared by Inventory UI and combat-facing code. */
object EquipmentUseRules {
    data class AttachmentRule(val slots: Int = 1, val requiresShoulderArms: Boolean = false, val excludesBow: Boolean = false, val magazine: Boolean = false)
    private val attachments = mapOf(
        "Bayonet" to AttachmentRule(requiresShoulderArms = true),
        "Drum Magazine" to AttachmentRule(excludesBow = true, magazine = true),
        "Extended Magazine" to AttachmentRule(excludesBow = true, magazine = true),
        "Grenade Launcher Underbarrel" to AttachmentRule(slots = 2, requiresShoulderArms = true),
        "Infrared Nightvision Scope" to AttachmentRule(),
        "Shotgun Underbarrel" to AttachmentRule(slots = 2, requiresShoulderArms = true),
        "Smartgun Link" to AttachmentRule(slots = 2),
        "Sniping Scope" to AttachmentRule()
    )

    fun attachmentRule(name: String) = attachments.entries.firstOrNull { it.key.equals(name, true) }?.value
    fun attachmentSlotsUsed(weapon: Weapon): Int = weapon.attachments.sumOf { attachmentRule(it)?.slots ?: 0 }

    fun canInstallAttachment(weapon: Weapon, attachment: String): String? {
        val rule = attachmentRule(attachment) ?: return "این Attachment در قواعد Core ثبت نشده است."
        val core = StoreCatalog.items.firstOrNull { it.category == "Weapons" && it.name.equals(weapon.name, true) }
        val exotic = core?.subtitle?.contains("Exotic", true) == true || weapon.name in setOf("Air Pistol","Battleglove","Constitution Arms Hurricane","Dartgun","Kendachi Mono-Three","Malorian Arms 3516","Microwaver","Militech Cowboy U-56","Rhinemetall EMG-86 Railgun","Shrieker","Stun Baton","Stun Gun","Tsunami Arms Helix")
        val ranged = weapon.magazineSize > 0 || weapon.type in setOf("Handgun","Shoulder Arms","Archery","Heavy Weapons","Autofire")
        if (exotic) return "Exotic Weapon به‌طور پیش‌فرض Weapon Attachment نمی‌پذیرد."
        if (!ranged) return "Weapon Attachment فقط روی Non-Exotic Ranged Weapon نصب می‌شود."
        if (weapon.attachments.any { it.equals(attachment, true) }) return "نصب دوباره‌ی همان Attachment اثر اضافه‌ای ندارد."
        if (rule.requiresShoulderArms && !weapon.type.equals("Shoulder Arms", true)) return "این Attachment فقط برای سلاحی است که با Shoulder Arms شلیک می‌شود."
        if (rule.excludesBow && (weapon.name.contains("Bow", true) || weapon.name.contains("Crossbow", true) || weapon.type.equals("Archery", true))) return "این خشاب روی Bow/Crossbow نصب نمی‌شود."
        if (rule.magazine && weapon.attachments.any { it.contains("Magazine", true) }) return "هم‌زمان فقط یک Extended/Drum Magazine می‌تواند روی سلاح باشد."
        if (attachmentSlotsUsed(weapon) + rule.slots > 3) return "Attachment Slot کافی نیست (${attachmentSlotsUsed(weapon)}/3 استفاده شده)."
        return null
    }

    fun battlegloveSlotsUsed(weapon: Weapon): Int = if (!weapon.name.equals("Battleglove", true)) 0 else weapon.cyberOptions.sumOf { name ->
        val r = CyberwareCatalog.ruleFor(name)
        if (r?.noSlot == true) 0 else (r?.slotsUsed ?: 1).coerceAtLeast(1)
    }

    fun canInstallInBattleglove(weapon: Weapon, option: InventoryItem): String? {
        if (!weapon.name.equals("Battleglove", true)) return "مقصد Battleglove نیست."
        if (!option.category.equals("Cyberware", true)) return "فقط Cyberarm/Cyberlimb Option قابل نصب است."
        val r = CyberwareCatalog.ruleFor(option.name) ?: return "Rule این Cyberware ثبت نشده است."
        if (r.group !in setOf("Cyberarm", "Cyberlimb")) return "Battleglove فقط Cyberarm/Cyberlimb Option می‌پذیرد."
        if (option.name.equals("Cyberarm", true) || option.name.equals("Cyberleg", true)) return "Foundational Cyberlimb داخل Battleglove نصب نمی‌شود."
        if (option.equipped || option.installedIn != null) return "این Option از قبل نصب شده است."
        if (weapon.cyberOptions.any { it.equals(option.name, true) }) return "نصب دوباره‌ی همان Option مزیت اضافه‌ای ایجاد نمی‌کند."
        val need = if (r.noSlot) 0 else r.slotsUsed.coerceAtLeast(1)
        if (battlegloveSlotsUsed(weapon) + need > 3) return "Battleglove فقط 3 Option Slot دارد (${battlegloveSlotsUsed(weapon)}/3 استفاده شده؛ این Option $need Slot می‌خواهد)."
        return null
    }

    fun deckCapacity(deckName: String): Int = when {
        deckName.contains("Poor", true) -> 5
        deckName.contains("Excellent", true) -> 9
        deckName.contains("Cyberdeck", true) -> 7
        else -> 0
    }

    /** Extra Hardware-only slot granted by a worn Bodyweight Suit. */
    fun deckCapacityFor(character: Character, deckName: String, item: InventoryItem? = null): Int {
        val base = deckCapacity(deckName)
        val bodyweightHardwareSlot = item?.category.equals("Hardware", true) && character.inventory.any {
            it.category.equals("Armor", true) && it.equipped && it.name.contains("Bodyweight Suit", true)
        }
        return base + if (bodyweightHardwareSlot) 1 else 0
    }

    /** A Cyberware option can be active either in the body or in worn Smart Glasses. */
    fun cyberOptionActive(character: Character, optionName: String): Boolean {
        if (character.inventory.any { it.category.equals("Cyberware", true) && it.equipped && it.name.contains(optionName, true) }) return true
        val wornGlassesIds = character.inventory.filter {
            it.category.equals("Gear", true) && it.equipped && it.name.equals("Smart Glasses", true)
        }.map { it.instanceId }.toSet()
        return wornGlassesIds.isNotEmpty() && character.inventory.any {
            it.category.equals("Cyberware", true) && it.installedIn in wornGlassesIds && it.name.contains(optionName, true)
        }
    }

    fun smartGlassesSlotsUsed(character: Character, glassesId: String): Int = character.inventory
        .filter { it.category.equals("Cyberware", true) && it.installedIn == glassesId }
        .sumOf { CyberwareCatalog.slotsUsed(it.name).coerceAtLeast(1) }

    fun canInstallInSmartGlasses(character: Character, option: InventoryItem, glasses: InventoryItem): String? {
        if (!glasses.category.equals("Gear", true) || !glasses.name.equals("Smart Glasses", true)) return "مقصد Smart Glasses نیست."
        if (!option.category.equals("Cyberware", true)) return "فقط Cybereye Option را می‌توان داخل Smart Glasses نصب کرد."
        val rule = CyberwareCatalog.ruleFor(option.name) ?: return "Rule این Cyberware ثبت نشده است."
        if (rule.group != "Cyberoptics" || option.name.contains("Cybereye", true)) return "Smart Glasses فقط Cybereye Option می‌پذیرد، نه Cybereye foundational."
        if (option.equipped || option.installedIn != null) return "این Option از قبل نصب شده است."
        val need = rule.slotsUsed.coerceAtLeast(1)
        val used = smartGlassesSlotsUsed(character, glasses.instanceId)
        if (used + need > 2) return "Smart Glasses فقط 2 Option Slot دارد ($used/2 استفاده شده؛ این Option به $need Slot نیاز دارد)."
        return null
    }

    fun deckSlotCost(item: InventoryItem): Int = when {
        item.category.equals("Programs", true) -> {
            val core = StoreCatalog.findForInventory(item.name, "Programs")
            if (core?.programClass?.contains("Black ICE", true) == true) 2 else 1
        }
        item.category.equals("Hardware", true) -> if (item.name in setOf("Backup Drive", "DNA Lock", "KRASH Barrier")) 2 else 1
        else -> 0
    }

    fun deckSlotsUsed(inventory: List<InventoryItem>, deckId: String): Int = inventory.filter { it.installedIn == deckId && (it.category.equals("Programs", true) || it.category.equals("Hardware", true)) }.sumOf(::deckSlotCost)

    fun effectiveStat(character: Character, stat: String, base: Int): Int {
        val drug = StreetDrugRules.statModifier(character, stat)
        val tempPenalty = if (character.temporaryStatPenaltyHours > 0)
            character.temporaryStatPenalties.entries.firstOrNull { it.key.equals(stat, true) }?.value ?: 0 else 0
        return (base + drug - tempPenalty).coerceAtLeast(1)
    }

    /** Contextual bonuses from Core gear. Multiple copies never stack. */
    fun gearSkillBonus(character: Character, skillName: String): Int {
        fun owns(name: String): Boolean = character.inventory.any {
            it.name.equals(name, true) && (!it.category.equals("Cyberware", true) || it.equipped)
        } || character.weapons.any { w -> w.name.equals("Battleglove", true) && w.isEquipped && w.cyberOptions.any { it.contains(name, true) } }
        val techSkills = setOf("Basic Tech", "Cybertech", "Land Vehicle Tech", "Sea Vehicle Tech", "Air Vehicle Tech", "Electronics/Security Tech", "Weaponstech")
        return when {
            skillName.equals("Library Search", true) && owns("Agent") -> 2
            (skillName.equals("First Aid", true) || skillName.equals("Paramedic", true)) && owns("Medscanner") -> 2
            techSkills.any { it.equals(skillName, true) } && owns("Techscanner") -> 2
            else -> 0
        }
    }
}

/** Core Street Drug mechanics kept separate from UI so Combat/Stats can query the same state. */
object StreetDrugRules {
    data class Rule(val durationHours: Int, val secondaryDv: Int)
    private val rules = mapOf(
        "Black Lace" to Rule(24, 17),
        "Blue Glass" to Rule(4, 15),
        "Boost" to Rule(24, 17),
        "Smash" to Rule(4, 15),
        "Synthcoke" to Rule(4, 15)
    )

    fun rule(name: String): Rule? = rules.entries.firstOrNull { it.key.equals(name, true) }?.value
    fun isActive(c: Character, name: String): Boolean = (c.activeDrugEffects.entries.firstOrNull { it.key.equals(name, true) }?.value ?: 0) > 0
    fun isAddicted(c: Character, name: String): Boolean = c.addictions.any { it.equals(name, true) }

    fun statModifier(c: Character, stat: String): Int = when (stat.uppercase()) {
        "INT" -> (if (isActive(c, "Boost")) 2 else 0) + (if (isAddicted(c, "Boost")) -2 else 0)
        "REF" -> (if (isActive(c, "Synthcoke")) 1 else 0) +
            (if (isAddicted(c, "Synthcoke") && !isActive(c, "Synthcoke")) -2 else 0) +
            (if (isAddicted(c, "Black Lace") && !isActive(c, "Black Lace")) -2 else 0)
        else -> 0
    }

    fun skillModifier(c: Character, skill: String): Int {
        val party = setOf("Dance", "Contortionist", "Conversation", "Human Perception", "Persuasion", "Acting")
        if (party.none { it.equals(skill, true) }) return 0
        return (if (isActive(c, "Smash")) 2 else 0) + (if (isAddicted(c, "Smash") && !isActive(c, "Smash")) -2 else 0)
    }

    fun ignoresSeriouslyWounded(c: Character): Boolean = isActive(c, "Black Lace")
}
