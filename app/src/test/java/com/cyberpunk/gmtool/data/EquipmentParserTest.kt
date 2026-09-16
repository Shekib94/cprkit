package com.cyberpunk.gmtool.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * گزارش تست ۶.۶: EquipmentParser آیتم‌های ناشناخته را بی‌صدا به «Utilities» می‌انداخت
 * و آیتم‌های حیاتی (مثل Cyberdeck) از UIهایی که بر اساس category فیلتر می‌کنند
 * (NET Architecture فقط category == "Gear" با نام شامل "Cyberdeck" را می‌بیند)
 * بیرون می‌ماندند. این تست‌ها دسته‌های حیاتی را قفل می‌کنند تا تغییرات بعدی
 * بی‌صدا آن‌ها را نشکنند.
 */
class EquipmentParserTest {

    private fun firstInventory(raw: String): InventoryItem {
        val parsed = EquipmentParser.parse(listOf(raw))
        assertTrue("«$raw» باید یک آیتم inventory بسازد", parsed.inventory.isNotEmpty())
        return parsed.inventory.first()
    }

    @Test
    fun cyberdeckParsesAsGearSoNetArchitectureCanSeeIt() {
        // باگ واقعی گزارش‌شده: «Cyberdeck (7 Slots)» به else می‌افتاد و Utilities می‌شد.
        for (raw in listOf("Cyberdeck (7 Slots)", "Cyberdeck (Poor Quality)", "Cyberdeck (Excellent Quality)", "cyberdeck")) {
            val item = firstInventory(raw)
            assertEquals("«$raw» باید Gear شود", "Gear", item.category)
            // قرارداد NET Architecture در GearTab/GmToolsScreen:
            assertTrue(item.category.equals("Gear", true) && item.name.contains("Cyberdeck", true))
        }
    }

    @Test
    fun bodyArmorParsesAsEquippedArmorWithSp() {
        val parsed = EquipmentParser.parse(listOf("Body Armor SP11"))
        val item = parsed.inventory.first()
        assertEquals("Armor", item.category)
        assertTrue("زره باید پوشیده باشد", item.equipped)
        assertEquals(11, item.sp)
        assertEquals(11, parsed.bodyArmorSp)
    }

    @Test
    fun headArmorParsesAsEquippedArmorWithSp() {
        val parsed = EquipmentParser.parse(listOf("Head Armor SP10"))
        val item = parsed.inventory.first()
        assertEquals("Armor", item.category)
        assertTrue(item.equipped)
        assertEquals(10, parsed.headArmorSp)
    }

    @Test
    fun ammoParsesAsAmmoWithQuantity() {
        val item = firstInventory("Basic Ammo x30")
        assertEquals("Ammo", item.category)
        assertEquals(30, item.quantity)
    }

    @Test
    fun programsParseAsPrograms() {
        assertEquals("Programs", firstInventory("Program: Armor").category)
        assertEquals("Programs", firstInventory("Sword").category)
        assertEquals("Programs", firstInventory("Worm").category)
    }

    @Test
    fun knownCyberwareParsesAsCyberware() {
        assertNotNull(CyberwareCatalog.ruleFor("Interface Plugs"))
        val item = firstInventory("Interface Plugs")
        assertEquals("Cyberware", item.category)
        assertTrue(item.equipped)
    }

    @Test
    fun clothingParsesAsClothing() {
        assertEquals("Clothing", firstInventory("Mirrorshades").category)
        assertEquals("Clothing", firstInventory("Businesswear Jacket").category)
    }

    @Test
    fun storeWeaponsBecomeWeaponsNotInventory() {
        val parsed = EquipmentParser.parse(listOf("Medium Pistol"))
        assertTrue("اسلحه‌ی کاتالوگ باید Weapon بسازد", parsed.weapons.any { it.name.contains("Medium Pistol", true) })
    }

    @Test
    fun grenadesBecomeWeapons() {
        val parsed = EquipmentParser.parse(listOf("Flashbang Grenade x2"))
        assertTrue(parsed.weapons.any { it.name.contains("Flashbang", true) })
    }

    @Test
    fun unknownItemsFallBackToUtilities() {
        // fallback عمدی است؛ فقط نباید آیتم‌های شناخته‌شده‌ی بالا را ببلعد.
        assertEquals("Utilities", firstInventory("Bag of Holding").category)
    }
}
