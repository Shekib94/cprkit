package com.cyberpunk.gmtool.data

/**
 * Resolves item names shown outside Store/Inventory (starter choices, Night Markets, etc.)
 * back to the same StoreItem record used by the canonical Item Details UI.
 */
object CoreItemReference {
    private val categoryOrder = listOf(
        "Weapons", "Weapon Mods", "Ammo", "Armor", "Cyberware", "Clothing", "Gear", "Drugs", "Hardware", "Programs", "Vehicles"
    )

    private val aliases = mapOf(
        "cyberdeck (7 slots)" to "Cyberdeck (Standard Quality)",
        "cyberdeck (5 slots)" to "Cyberdeck (Poor Quality)",
        "cyberdeck (9 slots)" to "Cyberdeck (Excellent Quality)",
        "program: armor" to "Armor",
        "program: sword" to "Sword",
        "program: see ya" to "See Ya",
        "program: eraser" to "Eraser",
        "program: vrizzbolt" to "Vrizzbolt",
        "program: worm" to "Worm",
        "flashbang grenade" to "Flashbang Grenade",
        "smoke grenade" to "Smoke Grenade",
        "teargas grenade" to "Teargas Grenade",
        "armor piercing grenade" to "Armor Piercing Grenade",
        "electric guitar" to "Electric Guitar or Other Instrument",
        "disposable cellphone" to "Disposable Cell Phone",
        "disposable phone" to "Disposable Cell Phone",
        "rope" to "Rope (60m/yd)",
        "sandevistan" to "Sandevistan Speedware"
    )

    private fun clean(raw: String): String = raw
        .substringBefore(" — ")
        .substringBefore(" • GM choice")
        .replace(Regex("\\s+x\\d+\\b", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\s*\\(SP\\d+\\)", RegexOption.IGNORE_CASE), "")
        .trim()

    private fun key(raw: String): String = clean(raw)
        .lowercase()
        .replace("®", "").replace("™", "")
        .replace(Regex("[^a-z0-9]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    fun splitComposite(label: String): List<String> = label.split("||")
        .map { it.trim() }
        .filter { it.isNotBlank() }

    fun resolve(raw: String, categoryHint: String? = null): StoreItem? {
        val cleaned = clean(raw)
        val alias = aliases[cleaned.lowercase()] ?: cleaned.removePrefix("Program:").trim()
        val candidates = StoreCatalog.allItems

        categoryHint?.let { hint ->
            StoreCatalog.findForInventory(alias, hint)?.let { return it }
        }

        candidates.firstOrNull { it.name.equals(alias, true) }?.let { return it }

        val wanted = key(alias)
        candidates.firstOrNull { key(it.name) == wanted }?.let { return it }

        // Starter equipment often prefixes ammo with Basic H/VH/Rifle/Slug naming.
        val normalizedAmmo = alias
            .replace("Basic H Pistol Ammunition", "Basic Ammunition", true)
            .replace("Basic VH Pistol Ammunition", "Basic Ammunition", true)
            .replace("Basic Rifle Ammunition", "Basic Ammunition", true)
            .replace("Basic Shotgun Shell Ammunition", "Basic Ammunition", true)
            .replace("Basic Slug Ammunition", "Basic Ammunition", true)
        if (!normalizedAmmo.equals(alias, true)) {
            StoreCatalog.findForInventory(normalizedAmmo, "Ammo")?.let { return it }
            candidates.firstOrNull { it.category == "Ammo" && it.name.equals("Basic Ammunition", true) }?.let { return it }
        }

        // Incendiary starter-package names encode the compatible ammo family but share one Core record.
        if (alias.contains("Incendiary", true) && alias.contains("Ammunition", true)) {
            candidates.firstOrNull { it.category == "Ammo" && it.name.equals("Incendiary Ammunition", true) }?.let { return it }
        }

        // Named grenade convenience records live in Weapons; if absent, show the underlying ammo rule.
        if (alias.endsWith("Grenade", true)) {
            val ammoName = when {
                alias.contains("Flashbang", true) -> "Flashbang Ammunition"
                alias.contains("Smoke", true) -> "Smoke Ammunition"
                alias.contains("Teargas", true) -> "Teargas Ammunition"
                alias.contains("Armor Piercing", true) -> "Armor-Piercing Ammunition"
                else -> null
            }
            if (ammoName != null) candidates.firstOrNull { it.category == "Ammo" && it.name.equals(ammoName, true) }?.let { return it }
        }

        // Longest safe match. This is only for display references and is still deterministic.
        return candidates.filter {
            val k = key(it.name)
            k.length >= 5 && (wanted.contains(k) || k.contains(wanted))
        }.sortedWith(compareBy<StoreItem> { categoryOrder.indexOf(it.category).let { n -> if (n < 0) 999 else n } }.thenByDescending { key(it.name).length })
            .firstOrNull()
    }

    fun resolveComposite(label: String, categoryHint: String? = null): List<StoreItem> =
        splitComposite(label).mapNotNull { resolve(it, categoryHint) }.distinctBy { it.category.lowercase() + "|" + it.name.lowercase() }
}
