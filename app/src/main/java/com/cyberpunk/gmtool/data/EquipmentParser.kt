package com.cyberpunk.gmtool.data

import java.util.UUID
import kotlin.math.absoluteValue

/** Converts starter/store strings into typed equipment without re-inventing weapon stats. */
object EquipmentParser {
    private fun canonicalCyberwareName(raw: String): String = when {
        raw.equals("Audio Recorder", true) -> "Audio Recorder (Cyberaudio)"
        else -> raw
    }
    data class ParsedGear(
        val weapons: List<Weapon>,
        val inventory: List<InventoryItem>,
        val headArmorSp: Int,
        val bodyArmorSp: Int
    )

    private fun uniqueWeaponId(): Int = UUID.randomUUID().hashCode().absoluteValue.coerceAtLeast(1)

    private fun normalizedForMatch(text: String): String = text.lowercase()
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")

    private fun catalogNameMatches(raw: String, catalogName: String): Boolean {
        val hay = " ${normalizedForMatch(raw)} "
        val needle = normalizedForMatch(catalogName)
        if (needle.isBlank()) return false
        return hay.contains(" $needle ")
    }

    fun parse(rawItems: List<String>): ParsedGear {
        val weapons = mutableListOf<Weapon>()
        val inventory = mutableListOf<InventoryItem>()
        var headSp = 0
        var bodySp = 0

        rawItems.forEach { raw ->
            val item = raw.trim()
            val lower = item.lowercase()
            val looksLikeAmmo = lower.contains("ammo") || lower.contains("ammunition") || lower.contains("shell") || lower.contains("slug")
            val weaponDef = if (looksLikeAmmo) null else StoreCatalog.items.filter { it.category == "Weapons" }
                .sortedByDescending { it.name.length }
                .firstOrNull { catalogNameMatches(item, it.name) }

            when {
                weaponDef != null -> {
                    val count = if (weaponDef.weaponSkill.equals("Athletics", true)) {
                        Regex("""x\s*(\d+)""", RegexOption.IGNORE_CASE).find(item)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1
                    } else 1
                    repeat(count.coerceIn(1, 20)) { weapons += weaponFromStore(weaponDef.name, weaponDef) }
                }
                lower.contains("head armor") || lower.contains("head armour") -> {
                    headSp = extractSp(item)
                    inventory += InventoryItem(item, "Armor", sp = headSp, currentHeadSp = headSp, equipped = true)
                }
                lower.contains("body armor") || lower.contains("body armour") -> {
                    bodySp = extractSp(item)
                    inventory += InventoryItem(item, "Armor", sp = bodySp, currentBodySp = bodySp, equipped = true)
                }
                lower.contains("armorjack") || lower.contains("kevlar") || lower.contains("leather") || lower.contains("flak") ||
                    lower.contains("metalgear") || lower.contains("bodyweight") -> {
                    val sp = ArmorCatalog.spFor(item)
                    val both = lower.contains("bodyweight")
                    val isHead = lower.contains("head")
                    if (both || isHead) headSp = maxOf(headSp, sp ?: 0)
                    if (both || !isHead) bodySp = maxOf(bodySp, sp ?: 0)
                    inventory += InventoryItem(
                        item, "Armor", sp = sp,
                        currentHeadSp = if (both || isHead) sp else null,
                        currentBodySp = if (both || !isHead) sp else null,
                        equipped = true
                    )
                }
                lower.contains("ammo") || lower.contains("ammunition") || lower.contains("shells") || lower.contains("slugs") -> {
                    val qty = Regex("""x\s*(\d+)""", RegexOption.IGNORE_CASE).find(item)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 10
                    inventory += InventoryItem(item, "Ammo", quantity = qty)
                }
                lower.startsWith("program") || listOf("armor", "sword", "worm", "eraser", "see ya", "vrizzbolt", "banhammer", "deckkrash", "hellbolt").any { lower == it } -> inventory += InventoryItem(item, "Programs")
                CyberwareCatalog.ruleFor(item) != null ->
                    inventory += InventoryItem(item, "Cyberware", equipped = true)
                lower.contains("chic") || lower.contains("wear") || lower.contains("jacket") || lower.contains("footwear") || lower.contains("bottoms") ||
                    lower.contains("outfit") || lower.contains("businesswear") || lower.contains("mirrorshades") || lower.contains("jewelry") || lower.contains("urbanflash") ||
                    lower.contains("urban flash") || lower.contains("leisurewear") -> inventory += InventoryItem(item, "Clothing")
                lower.contains("vehicle") || lower.contains("car") || lower.contains("bike") -> inventory += InventoryItem(item, "Vehicles")
                // گزارش تست ۵.۱: «Cyberdeck (7 Slots)» به else می‌افتاد و Utilities می‌شد؛
                // در نتیجه NET Architecture (که فقط category == "Gear" با نام شامل
                // "Cyberdeck" را می‌شناسد) دکِ نت‌رانر را اصلاً نشان نمی‌داد.
                lower.contains("cyberdeck") -> inventory += InventoryItem(item, "Gear")
                lower.contains("grenade") -> {
                    val count = Regex("""x\s*(\d+)""", RegexOption.IGNORE_CASE).find(item)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1
                    val clean = item.replace(Regex("""\s*x\s*\d+""", RegexOption.IGNORE_CASE), "").trim()
                    val lowerClean = clean.lowercase()
                    val damage = if (listOf("flashbang","teargas","smoke","emp","biotoxin","poison","sleep").any { lowerClean.contains(it) }) "0d6" else "6d6"
                    val modes = buildList { add("Explosive"); if(lowerClean.contains("armor piercing")||lowerClean.contains("armor-piercing")) add("Armor Piercing"); add(clean) }.joinToString(" • ")
                    repeat(count.coerceIn(1,20)) { weapons += Weapon(id=uniqueWeaponId(), name=clean, category="Grenade", type="Athletics", damage=damage, rof=1, handsRequired=1, concealable=true, modes=modes, rangeSingle=listOf("0-6" to 16, "7-12" to 15, "13-25" to 15, "26-50" to 17, "51-100" to 20, "101-200" to 22, "201-400" to 25)) }
                }
                else -> inventory += InventoryItem(item, "Utilities")
            }
        }

        val firstRanged = weapons.indexOfFirst { it.magazineSize > 0 }
        if (firstRanged >= 0) weapons[firstRanged] = weapons[firstRanged].copy(isEquipped = true)
        addDefaultReloads(weapons, inventory)
        return ParsedGear(weapons, inventory, headSp, bodySp)
    }

    /**
     * مهمات یدکی برای NPCها.
     *
     * تا پیش از این NPC فقط با خشابِ پرِ داخل سلاح ساخته می‌شد و هیچ ذخیره‌ای
     * نداشت؛ یعنی آدام اسمشر دقیقاً **یک** راکت داشت و بعد از شلیک اول
     * راکت‌اندازش بی‌مصرف می‌شد. مقدارها عمداً محافظه‌کارانه‌اند و GM
     * هر وقت خواست کم و زیادشان می‌کند.
     */
    private fun addDefaultReloads(weapons: List<Weapon>, inventory: MutableList<InventoryItem>) {
        val needed = LinkedHashMap<String, Int>()
        weapons.filter { it.magazineSize > 0 && it.ammoType.isNotBlank() }.forEach { w ->
            val type = w.ammoType.trim()
            val spare = when {
                type.equals("Rocket", true) -> 3
                type.equals("Grenade", true) -> 4
                w.magazineSize <= 4 -> w.magazineSize * 4   // Sniper و مشابه
                else -> w.magazineSize * 2                   // دو خشاب یدکی
            }
            needed[type] = (needed[type] ?: 0) + spare
        }
        needed.forEach { (type, qty) ->
            val already = inventory.any {
                it.category.equals("Ammo", true) && it.name.contains(type, true)
            }
            if (!already) inventory += InventoryItem("$type Ammo", "Ammo", quantity = qty)
        }
    }


    /** Parse a role starter package with category context so ambiguous names (for example Audio Recorder)
     * keep their Core meaning instead of being guessed from strings. */
    fun parseStarting(equipment: StartingEquipment, selectedChoices: List<String>): ParsedGear {
        val weapons = mutableListOf<Weapon>()
        val inventory = mutableListOf<InventoryItem>()
        var headSp = 0
        var bodySp = 0

        fun merge(parsed: ParsedGear) {
            weapons += parsed.weapons
            inventory += parsed.inventory
            headSp = maxOf(headSp, parsed.headArmorSp)
            bodySp = maxOf(bodySp, parsed.bodyArmorSp)
        }

        // Weapon list also contains armor/ammunition in the Core starter tables, so normal typed parsing is correct here.
        merge(parse(equipment.weapons))
        // Gear must remain gear/program/clothing and must never be accidentally converted to same-named cyberware.
        equipment.gear.forEach { raw ->
            val lower = raw.lowercase()
            when {
                lower.startsWith("program:") -> inventory += InventoryItem(raw, "Programs")
                lower.contains("chic") || lower.contains("wear") || lower.contains("jacket") || lower.contains("footwear") ||
                    lower.contains("bottom") || lower.contains("urbanflash") || lower.contains("urban flash") || lower.contains("leisurewear") ||
                    lower.contains("businesswear") || lower.contains("mirrorshades") || lower.contains("jewelry") || lower.contains("nomad leathers") ->
                    inventory += InventoryItem(raw, "Clothing")
                else -> {
                    val qty = Regex("""x\s*(\d+)""", RegexOption.IGNORE_CASE).find(raw)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1
                    inventory += InventoryItem(raw, "Utilities", quantity = qty)
                }
            }
        }
        // Cyberware list is category-authoritative, even when a mundane item has the same display name.
        equipment.cyberware.forEach { raw ->
            val cyberName = canonicalCyberwareName(raw)
            val sp = when {
                cyberName.contains("Subdermal Armor", true) -> 11
                cyberName.contains("Skin Weave", true) -> 7
                else -> null
            }
            inventory += InventoryItem(cyberName, "Cyberware", sp = sp, currentHeadSp = sp, currentBodySp = sp, equipped = true)
        }

        // Preserve the category of each pending choice. Composite choices are expanded after category lookup.
        selectedChoices.forEach { selected ->
            val group = equipment.pendingChoices.firstOrNull { selected in it.options }
            val parts = selected.split("||").map(String::trim).filter(String::isNotBlank)
            when {
                group?.category?.contains("Cyber", true) == true || group?.category?.contains("Internal", true) == true || group?.category?.contains("Fashionware", true) == true ->
                    parts.forEach { inventory += InventoryItem(canonicalCyberwareName(it), "Cyberware", equipped = true) }
                group?.category?.contains("Program", true) == true -> parts.forEach { inventory += InventoryItem(it, "Programs") }
                group?.category?.contains("Gear", true) == true -> parts.forEach { inventory += InventoryItem(it, "Utilities") }
                else -> parts.forEach { merge(parse(listOf(it))) }
            }
        }

        val firstRanged = weapons.indexOfFirst { it.magazineSize > 0 }
        if (firstRanged >= 0) weapons[firstRanged] = weapons[firstRanged].copy(isEquipped = true)
        return ParsedGear(weapons, inventory, headSp, bodySp)
    }

    fun cyberwareNames(equipment: StartingEquipment, selectedChoices: List<String>): List<String> = buildList {
        addAll(equipment.cyberware.map(::canonicalCyberwareName))
        selectedChoices.forEach { selected ->
            val group = equipment.pendingChoices.firstOrNull { selected in it.options }
            if (group?.category?.contains("Cyber", true) == true || group?.category?.contains("Internal", true) == true || group?.category?.contains("Fashionware", true) == true)
                addAll(selected.split("||").map(String::trim).filter(String::isNotBlank).map(::canonicalCyberwareName))
        }
    }

    private fun weaponCategory(item: StoreItem): String {
        val n = item.name.lowercase()
        val modes = item.modes.lowercase()
        val ammo = item.ammoType.lowercase()
        return when {
            "melee" in modes -> when (item.damage.lowercase()) {
                "1d6" -> "Light Melee Weapon"
                "2d6" -> "Medium Melee Weapon"
                "3d6" -> "Heavy Melee Weapon"
                "4d6" -> "Very Heavy Melee Weapon"
                else -> "Melee Weapon"
            }
            "shotgun" in ammo || "shotgun" in n || "shotgun shell" in modes -> "Shotgun"
            "rocket" in ammo || "rocket launcher" in n -> "Rocket Launcher"
            "grenade" in ammo || "grenade launcher" in n -> "Grenade Launcher"
            "arrow" in ammo || "bow" in n || "crossbow" in n -> "Bows / Crossbow"
            "rifle" in ammo && (item.autofire.toIntOrNull() ?: 0) > 0 -> "Assault Rifle"
            "rifle" in ammo && ("sniper" in n) -> "Sniper Rifle"
            "rifle" in ammo -> "Assault Rifle"
            "pistol" in ammo && (item.autofire.toIntOrNull() ?: 0) > 0 -> if (item.damage.equals("3d6", true)) "Heavy SMG" else "SMG"
            "pistol" in ammo -> when (item.damage.lowercase()) {
                "2d6" -> "Medium Pistol"
                "3d6" -> "Heavy Pistol"
                else -> "Very Heavy Pistol"
            }
            else -> item.name
        }
    }

    fun weaponFromStore(displayName: String, item: StoreItem, quality: String = "Standard"): Weapon = Weapon(
        id = uniqueWeaponId(), name = displayName, category = weaponCategory(item),
        type = item.weaponSkill.ifBlank { "Melee Weapon" }, damage = item.damage,
        rof = item.rof.toIntOrNull() ?: if (item.modes.contains("Melee") && item.damage == "4d6") 1 else if (item.modes.contains("Melee")) 2 else 1,
        handsRequired = item.hands.toIntOrNull() ?: 1,
        concealable = item.concealed.equals("Pocket", true) || item.concealed.equals("Jacket", true) || item.concealed.equals("Longcoat", true),
        cost = StoreCatalog.qualityPrice(item.basePrice, quality), magazineSize = item.magStd ?: 0, currentAmmo = item.magStd ?: 0,
        ammoType = item.ammoType, modes = item.modes, autofireMultiplier = item.autofire.toIntOrNull() ?: 0,
        rangeSingle = item.rangeSingle, rangeAuto = item.rangeAuto, quality = quality
    )

    private fun extractSp(text: String): Int = Regex("SP\\s*(\\d+)", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)?.toIntOrNull() ?: 0
}
