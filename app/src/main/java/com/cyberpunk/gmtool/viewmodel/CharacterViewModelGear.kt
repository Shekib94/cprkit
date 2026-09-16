package com.cyberpunk.gmtool.viewmodel

// ─────────────────────────────────────────────────────────────
// دامنه‌ی تجهیزات: موجودی و سلاح‌ها، خرید از فروشگاه،
// Weapon Attachments، Cyberdeck، Battleglove، Smart Glasses،
// نصب/خارج‌کردن Cyberware و دکمه‌ی پوشیدن/درآوردن آیتم‌ها.
// توابع توسعه روی CharacterViewModel — API عمومی بدون تغییر.
// (هویت آیتم‌ها ایندکس/instanceId است، نه نام نمایشی.)
// ─────────────────────────────────────────────────────────────

import com.cyberpunk.gmtool.data.gtr


import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cyberpunk.gmtool.data.Character
import com.cyberpunk.gmtool.data.CharacterRepository
import com.cyberpunk.gmtool.data.GameSaveManager
import com.cyberpunk.gmtool.data.GameSaveSlot
import com.cyberpunk.gmtool.data.InventoryItem
import com.cyberpunk.gmtool.data.LifepathData
import com.cyberpunk.gmtool.data.SettingsRepository
import com.cyberpunk.gmtool.data.SkillData
import com.cyberpunk.gmtool.data.StreetratData
import com.cyberpunk.gmtool.data.Stats
import com.cyberpunk.gmtool.data.Weapon
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong

// ---------- موجودی و سلاح ----------
fun CharacterViewModel.addInventoryItem(id: Int, name: String, category: String) =
    updateCharacter(id) { c ->
        c.copy(inventory = c.inventory + InventoryItem(name, category, instanceId = "inv_${java.util.UUID.randomUUID()}"))
    }

fun CharacterViewModel.removeInventoryItem(id: Int, index: Int) =
    updateCharacter(id) { c ->
        if (index !in c.inventory.indices) return@updateCharacter c
        val removed = c.inventory[index]
        if (removed.category.equals("Cyberware", true) && removed.equipped) return@updateCharacter c
        var next = c.copy(inventory = c.inventory.filterIndexed { i, _ -> i != index })
        if (removed.category.equals("Cyberware", true)) {
            // Humanity از دست‌رفته با برداشتن سایبرویر خودکار برنمی‌گردد؛ فقط سقف Humanity باز می‌شود.
            next = next.copy(maxHumanity = com.cyberpunk.gmtool.data.GameRules.maxHumanity(next.baseEmp, next.inventory))
        }
        com.cyberpunk.gmtool.data.GameRules.syncDerived(next)
    }

fun CharacterViewModel.setWeaponEquipped(id: Int, weaponId: Int, equipped: Boolean) =
    updateCharacter(id) { c ->
        c.copy(weapons = c.weapons.map {
            if (it.id == weaponId) it.copy(isEquipped = equipped) else it
        })
    }

fun CharacterViewModel.dropHeldWeapon(id: Int, weaponId: Int): Boolean {
    var dropped = false
    updateCharacter(id) { c ->
        c.copy(weapons = c.weapons.map { w ->
            if (w.id == weaponId && w.isEquipped) { dropped = true; w.copy(isEquipped = false) } else w
        }, combatEffects = if (dropped) c.combatEffects + ("dropped_weapon_$weaponId" to -1) else c.combatEffects)
    }
    return dropped
}

fun CharacterViewModel.transferHeldWeapon(attackerId: Int, defenderId: Int, weaponId: Int): Boolean {
    val attacker = getCharacter(attackerId) ?: return false
    val defender = getCharacter(defenderId) ?: return false
    val weapon = defender.weapons.firstOrNull { it.id == weaponId && it.isEquipped } ?: return false
    val newId = (attacker.weapons.maxOfOrNull { it.id } ?: 0) + 1
    updateCharacter(defenderId) { d -> d.copy(weapons = d.weapons.filterNot { it.id == weaponId }) }
    updateCharacter(attackerId) { a -> a.copy(weapons = a.weapons + weapon.copy(id = newId, isEquipped = true)) }
    return true
}

/** Mark the current Move Action as fully spent (used by Flying Kick). */
fun CharacterViewModel.spendRemainingMovement(id: Int) {
    val c = getCharacter(id) ?: return
    val total = com.cyberpunk.gmtool.data.CombatRules.effectiveMove(c) * 2
    updateCharacter(id) { ch -> ch.copy(combatEffects = ch.combatEffects.filterKeys { !it.startsWith("moved_this_turn_") } + ("moved_this_turn_$total" to -1)) }
}

/** فروش سلاح: سلاح حذف و مبلغ به یورودلار اضافه می‌شود. */
fun CharacterViewModel.sellWeapon(id: Int, weaponId: Int, salePrice: Int): Boolean {
    val c = getCharacter(id) ?: return false
    if (salePrice < 0) return false
    if (c.weapons.none { it.id == weaponId }) return false
    updateCharacter(id) { ch ->
        com.cyberpunk.gmtool.data.GameRules.syncDerived(
            ch.copy(
                eurodollars = ch.eurodollars + salePrice,
                weapons = ch.weapons.filterNot { it.id == weaponId }
            )
        )
    }
    return true
}

fun CharacterViewModel.removeWeapon(id: Int, weaponId: Int) =
    updateCharacter(id) { c -> c.copy(weapons = c.weapons.filterNot { it.id == weaponId }) }

fun CharacterViewModel.adjustWeaponAmmo(id: Int, weaponId: Int, delta: Int) =
    updateCharacter(id) { c ->
        c.copy(weapons = c.weapons.map { w ->
            if (w.id == weaponId) w.copy(currentAmmo = (w.currentAmmo + delta).coerceIn(0, w.magazineSize))
            else w
        })
    }

/** شلیک: مهمات سلاح کم می‌شود (برای اتصال تب Combat) */
fun CharacterViewModel.fireWeapon(id: Int, weaponId: Int, shots: Int = 1) =
    updateCharacter(id) { c ->
        c.copy(weapons = c.weapons.map { w ->
            if (w.id == weaponId) w.copy(currentAmmo = (w.currentAmmo - shots).coerceIn(0, w.magazineSize))
            else w
        })
    }

internal fun CharacterViewModel.isCompatibleAmmo(weapon: Weapon, ammoName: String): Boolean {
    val n = ammoName.lowercase()
    val t = weapon.ammoType.lowercase()
    if (t.isBlank()) return false
    return when {
        t.contains("pistol") -> n.contains("pistol") || n.contains("bullet") || n.contains("ammo")
        t.contains("rifle") -> n.contains("rifle") || n.contains("bullet") || n.contains("ammo")
        t.contains("shotgun") -> n.contains("shotgun") || n.contains("shell") || n.contains("slug")
        t.contains("arrow") -> n.contains("arrow")
        t.contains("grenade") -> n.contains("grenade")
        t.contains("rocket") -> n.contains("rocket")
        else -> n.contains(t)
    }
}

/** پرکردن خشاب */
fun CharacterViewModel.reloadWeapon(id: Int, weaponId: Int) =
    updateCharacter(id) { c ->
        val weapon = c.weapons.firstOrNull { it.id == weaponId } ?: return@updateCharacter c
        if (weapon.magazineSize <= 0) return@updateCharacter c
        val ammoIndex = c.inventory.indexOfFirst { it.category.equals("Ammo", true) && it.quantity > 0 && isCompatibleAmmo(weapon, it.name) }
        if (ammoIndex < 0) return@updateCharacter c
        val need = (weapon.magazineSize - weapon.currentAmmo).coerceAtLeast(0)
        if (need == 0) return@updateCharacter c
        val ammo = c.inventory[ammoIndex]
        val loaded = minOf(need, ammo.quantity)
        val newInv = c.inventory.mapIndexedNotNull { i, it ->
            if (i != ammoIndex) it else {
                val remain = it.quantity - loaded
                if (remain > 0) it.copy(quantity = remain) else null
            }
        }
        c.copy(
            weapons = c.weapons.map { if (it.id == weaponId) it.copy(currentAmmo = it.currentAmmo + loaded, loadedAmmoName = ammo.name) else it },
            inventory = newInv
        )
    }

// ---------- خرید از فروشگاه (پول + موجودی/سلاح) ----------
/** خروجی: پیام نتیجه. موفقیت با "OK" */
fun CharacterViewModel.buyItem(id: Int, name: String, category: String, price: Int, quality: String = "Standard"): String {
    val c = getCharacter(id) ?: return "کاراکتر یافت نشد"
    val mayUseFashionBudget = c.creationMethod.contains("Complete", true) &&
        (category.equals("Clothing", true) || (category.equals("Cyberware", true) && com.cyberpunk.gmtool.data.CyberwareCatalog.isFashionware(name)))
    val fashionSpent = if (mayUseFashionBudget) minOf(price, c.fashionBudget) else 0
    val cashSpent = price - fashionSpent
    if (c.eurodollars < cashSpent) return "پول کافی نیست"
    // Buying cyberware is intentionally unrestricted here. Core prerequisites, pairing, slots and HL
    // are checked only when the player actually installs/uses the purchased piece.
    if (category.equals("Vehicles", true)) {
        val spec = com.cyberpunk.gmtool.data.VehicleCatalog.byName(name) ?: return "تعریف وسیله پیدا نشد"
        val instance = com.cyberpunk.gmtool.data.OwnedVehicle(
            id = "veh_${System.currentTimeMillis()}_${(1000..9999).random()}", // allow-raw-random: یکتا کردن شناسه‌ی وسیله‌نقلیه — تاس قواعد نیست
            specName = spec.name, currentSdp = spec.sdp, source = "PURCHASED"
        )
        updateCharacter(id) { ch -> ch.copy(eurodollars = ch.eurodollars - cashSpent, ownedVehicles = ch.ownedVehicles + instance) }
    } else if (category == "Weapons") {
        val def = com.cyberpunk.gmtool.data.StoreCatalog.items.firstOrNull { it.category == "Weapons" && it.name == name }
            ?: return "تعریف سلاح پیدا نشد"
        val weapon = com.cyberpunk.gmtool.data.EquipmentParser.weaponFromStore(name, def, quality)
        updateCharacter(id) { ch -> ch.copy(eurodollars = ch.eurodollars - cashSpent, fashionBudget = ch.fashionBudget - fashionSpent, weapons = ch.weapons + weapon) }
    } else {
        val autoEquip = category.equals("Armor", true)
        updateCharacter(id) { ch ->
            val sp = when {
                category.equals("Armor", true) -> com.cyberpunk.gmtool.data.ArmorCatalog.spFor(name)
                category.equals("Cyberware", true) && name.contains("Subdermal Armor", true) -> 11
                category.equals("Cyberware", true) && name.contains("Skin Weave", true) -> 7
                else -> null
            }
            val purchased = com.cyberpunk.gmtool.data.InventoryItem(
                name = name, category = category, sp = sp,
                currentHeadSp = if (category.equals("Cyberware", true) && sp != null) sp else if (category.equals("Armor", true) && name.contains("Head", true)) sp else if (category.equals("Armor", true) && name.contains("Bodyweight", true)) sp else null,
                currentBodySp = if (category.equals("Cyberware", true) && sp != null) sp else if (category.equals("Armor", true) && !name.contains("Head", true)) sp else null,
                equipped = autoEquip,
                instanceId = "inv_${java.util.UUID.randomUUID()}"
            )
            com.cyberpunk.gmtool.data.GameRules.syncDerived(ch.copy(
                eurodollars = ch.eurodollars - cashSpent,
                fashionBudget = ch.fashionBudget - fashionSpent,
                inventory = ch.inventory + purchased
            ))
        }
    }
    return "OK"
}

/** SP فعال از روی همه‌ی منابع زره دوباره محاسبه می‌شود. */
/** هنگام عبور Damage از زره، تمام منابع SP در همان location هم‌زمان ablate می‌شوند. */
fun CharacterViewModel.ablateArmor(id: Int, head: Boolean, amount: Int = 1) =
    updateCharacter(id) { c -> com.cyberpunk.gmtool.data.GameRules.ablateArmor(c, head, amount) }

fun CharacterViewModel.repairArmor(id: Int, head: Boolean, amount: Int = 1) =
    updateCharacter(id) { c -> com.cyberpunk.gmtool.data.GameRules.repairArmor(c, head, amount) }


// ---------- Weapon Attachments / Cyberdeck loading ----------
fun CharacterViewModel.installWeaponAttachment(id: Int, inventoryIndex: Int, weaponId: Int): String {
    var c = getCharacter(id) ?: return "کاراکتر یافت نشد"
    if (inventoryIndex !in c.inventory.indices) return "Attachment پیدا نشد"
    c = ensureInventoryInstanceIds(c)
    val item = c.inventory[inventoryIndex]
    if (!item.category.equals("Weapon Mods", true)) return "این آیتم Weapon Attachment نیست"
    val weapon = c.weapons.firstOrNull { it.id == weaponId } ?: return "سلاح پیدا نشد"
    com.cyberpunk.gmtool.data.EquipmentUseRules.canInstallAttachment(weapon, item.name)?.let { return it }
    val core = com.cyberpunk.gmtool.data.StoreCatalog.items.firstOrNull { it.category == "Weapons" && it.name.equals(weapon.name, true) }
    val newAttachments = weapon.attachments + item.name
    var newMag = weapon.magazineSize
    if (item.name.equals("Extended Magazine", true)) newMag = core?.magExt ?: newMag
    if (item.name.equals("Drum Magazine", true)) newMag = core?.magDrum ?: newMag
    val updatedWeapon = weapon.copy(
        attachments = newAttachments,
        magazineSize = newMag,
        currentAmmo = weapon.currentAmmo.coerceAtMost(newMag),
        concealable = if (item.name in listOf("Bayonet","Drum Magazine","Extended Magazine","Grenade Launcher Underbarrel","Shotgun Underbarrel")) false else weapon.concealable
    )
    val newInv = c.inventory.toMutableList().also { it.removeAt(inventoryIndex) }
    val next = c.copy(weapons = c.weapons.map { if (it.id == weaponId) updatedWeapon else it }, inventory = newInv)
    persist(_characters.value.map { if (it.id == id) next else it })
    return "OK|${item.name}|${com.cyberpunk.gmtool.data.EquipmentUseRules.attachmentSlotsUsed(updatedWeapon)}/3"
}

fun CharacterViewModel.uninstallWeaponAttachment(id: Int, weaponId: Int, attachment: String): String {
    val c = getCharacter(id) ?: return "کاراکتر یافت نشد"
    val weapon = c.weapons.firstOrNull { it.id == weaponId } ?: return "سلاح پیدا نشد"
    if (weapon.attachments.none { it.equals(attachment, true) }) return "Attachment روی این سلاح نصب نیست"
    val core = com.cyberpunk.gmtool.data.StoreCatalog.items.firstOrNull { it.category == "Weapons" && it.name.equals(weapon.name, true) }
    val remaining = weapon.attachments.filterNot { it.equals(attachment, true) }
    val mag = when {
        remaining.any { it.equals("Drum Magazine", true) } -> core?.magDrum ?: weapon.magazineSize
        remaining.any { it.equals("Extended Magazine", true) } -> core?.magExt ?: weapon.magazineSize
        else -> core?.magStd ?: weapon.magazineSize
    }
    val concealable = (core?.concealed?.let { !it.equals("No", true) } ?: weapon.concealable) && remaining.none { it in listOf("Bayonet","Drum Magazine","Extended Magazine","Grenade Launcher Underbarrel","Shotgun Underbarrel") }
    val updated = weapon.copy(attachments = remaining, magazineSize = mag, currentAmmo = weapon.currentAmmo.coerceAtMost(mag), concealable = concealable)
    val returned = com.cyberpunk.gmtool.data.InventoryItem(attachment, "Weapon Mods", instanceId = "inv_${java.util.UUID.randomUUID()}")
    val next = c.copy(weapons = c.weapons.map { if (it.id == weaponId) updated else it }, inventory = c.inventory + returned)
    persist(_characters.value.map { if (it.id == id) next else it })
    return "OK"
}

fun CharacterViewModel.loadCyberdeckItem(id: Int, inventoryIndex: Int, deckInventoryIndex: Int): String {
    var c = getCharacter(id) ?: return "کاراکتر یافت نشد"
    if (inventoryIndex !in c.inventory.indices || deckInventoryIndex !in c.inventory.indices) return "آیتم یا Cyberdeck پیدا نشد"
    c = ensureInventoryInstanceIds(c)
    val item = c.inventory[inventoryIndex]
    val deck = c.inventory[deckInventoryIndex]
    if (!(item.category.equals("Programs", true) || item.category.equals("Hardware", true))) return "فقط Program یا Hardware روی Cyberdeck نصب می‌شود"
    if (!deck.category.equals("Gear", true) || !deck.name.contains("Cyberdeck", true)) return "مقصد Cyberdeck نیست"
    if (item.installedIn != null) return "این آیتم از قبل روی یک Cyberdeck نصب شده است"
    val capacity = com.cyberpunk.gmtool.data.EquipmentUseRules.deckCapacityFor(c, deck.name, item)
    val used = com.cyberpunk.gmtool.data.EquipmentUseRules.deckSlotsUsed(c.inventory, deck.instanceId)
    val cost = com.cyberpunk.gmtool.data.EquipmentUseRules.deckSlotCost(item)
    if (used + cost > capacity) return "Cyberdeck Slot کافی نیست: $used/$capacity استفاده شده و این آیتم $cost Slot می‌خواهد"
    val inv = c.inventory.mapIndexed { idx, it -> if (idx == inventoryIndex) it.copy(equipped = true, installedIn = deck.instanceId) else it }
    val next = c.copy(inventory = inv)
    persist(_characters.value.map { if (it.id == id) next else it })
    return "OK|$cost|${used + cost}/$capacity"
}

fun CharacterViewModel.unloadCyberdeckItem(id: Int, inventoryIndex: Int): String {
    val c = getCharacter(id) ?: return "کاراکتر یافت نشد"
    if (inventoryIndex !in c.inventory.indices) return "آیتم پیدا نشد"
    val item = c.inventory[inventoryIndex]
    if (!(item.category.equals("Programs", true) || item.category.equals("Hardware", true)) || item.installedIn == null) return "این آیتم روی Cyberdeck نصب نیست"
    val next = c.copy(inventory = c.inventory.mapIndexed { idx, it -> if (idx == inventoryIndex) it.copy(equipped = false, installedIn = null) else it })
    persist(_characters.value.map { if (it.id == id) next else it })
    return "OK"
}


// ---------- Battleglove external cyberlimb options ----------
fun CharacterViewModel.installBattlegloveOption(id: Int, optionInventoryIndex: Int, battlegloveWeaponId: Int): String {
    var c = getCharacter(id) ?: return "کاراکتر یافت نشد"
    if (optionInventoryIndex !in c.inventory.indices) return "Option پیدا نشد"
    c = ensureInventoryInstanceIds(c)
    val option = c.inventory[optionInventoryIndex]
    val glove = c.weapons.firstOrNull { it.id == battlegloveWeaponId } ?: return "Battleglove پیدا نشد"
    com.cyberpunk.gmtool.data.EquipmentUseRules.canInstallInBattleglove(glove, option)?.let { return it }
    val rule = com.cyberpunk.gmtool.data.CyberwareCatalog.ruleFor(option.name) ?: return "Rule پیدا نشد"
    val need = if (rule.noSlot) 0 else rule.slotsUsed.coerceAtLeast(1)
    val updatedGlove = glove.copy(cyberOptions = glove.cyberOptions + option.name)
    val inv = c.inventory.mapIndexed { i, it -> if (i == optionInventoryIndex) it.copy(equipped = false, installedIn = "battleglove:${glove.id}") else it }
    val next = c.copy(inventory = inv, weapons = c.weapons.map { if (it.id == glove.id) updatedGlove else it })
    persist(_characters.value.map { if (it.id == id) next else it })
    return "OK|$need|${com.cyberpunk.gmtool.data.EquipmentUseRules.battlegloveSlotsUsed(updatedGlove)}/3"
}

fun CharacterViewModel.uninstallBattlegloveOption(id: Int, optionInventoryIndex: Int): String {
    val c = getCharacter(id) ?: return "کاراکتر یافت نشد"
    if (optionInventoryIndex !in c.inventory.indices) return "Option پیدا نشد"
    val option = c.inventory[optionInventoryIndex]
    val gloveId = option.installedIn?.removePrefix("battleglove:")?.toIntOrNull() ?: return "این Option داخل Battleglove نصب نیست"
    val glove = c.weapons.firstOrNull { it.id == gloveId } ?: return "Battleglove پیدا نشد"
    val updatedGlove = glove.copy(cyberOptions = glove.cyberOptions.toMutableList().also { list ->
        val i = list.indexOfFirst { it.equals(option.name, true) }; if (i >= 0) list.removeAt(i)
    })
    val next = c.copy(
        inventory = c.inventory.mapIndexed { i, it -> if (i == optionInventoryIndex) it.copy(installedIn = null, equipped = false) else it },
        weapons = c.weapons.map { if (it.id == gloveId) updatedGlove else it }
    )
    persist(_characters.value.map { if (it.id == id) next else it })
    return "OK"
}

// ---------- Smart Glasses / external cyberoptic options ----------
fun CharacterViewModel.installSmartGlassesOption(id: Int, optionInventoryIndex: Int, glassesInventoryIndex: Int): String {
    var c = getCharacter(id) ?: return "کاراکتر یافت نشد"
    if (optionInventoryIndex !in c.inventory.indices || glassesInventoryIndex !in c.inventory.indices) return "Option یا Smart Glasses پیدا نشد"
    c = ensureInventoryInstanceIds(c)
    val option = c.inventory[optionInventoryIndex]
    val glasses = c.inventory[glassesInventoryIndex]
    com.cyberpunk.gmtool.data.EquipmentUseRules.canInstallInSmartGlasses(c, option, glasses)?.let { return it }
    val need = com.cyberpunk.gmtool.data.CyberwareCatalog.slotsUsed(option.name).coerceAtLeast(1)
    val inv = c.inventory.mapIndexed { idx, it ->
        when (idx) {
            optionInventoryIndex -> it.copy(equipped = false, installedIn = glasses.instanceId)
            else -> it
        }
    }
    val next = c.copy(inventory = inv)
    persist(_characters.value.map { if (it.id == id) next else it })
    return "OK|$need|${com.cyberpunk.gmtool.data.EquipmentUseRules.smartGlassesSlotsUsed(next, glasses.instanceId)}/2"
}

fun CharacterViewModel.uninstallSmartGlassesOption(id: Int, optionInventoryIndex: Int): String {
    val c = getCharacter(id) ?: return "کاراکتر یافت نشد"
    if (optionInventoryIndex !in c.inventory.indices) return "Option پیدا نشد"
    val option = c.inventory[optionInventoryIndex]
    val parent = option.installedIn ?: return "این Option داخل Smart Glasses نصب نیست"
    val glasses = c.inventory.firstOrNull { it.instanceId == parent && it.name.equals("Smart Glasses", true) } ?: return "Smart Glasses مقصد پیدا نشد"
    updateCharacter(id) { ch -> ch.copy(inventory = ch.inventory.mapIndexed { i, it -> if (i == optionInventoryIndex) it.copy(installedIn = null, equipped = false) else it }) }
    return "OK|${glasses.name}"
}

fun CharacterViewModel.toggleSmartGlasses(id: Int, glassesInventoryIndex: Int): String {
    val c = getCharacter(id) ?: return "کاراکتر یافت نشد"
    if (glassesInventoryIndex !in c.inventory.indices) return "Smart Glasses پیدا نشد"
    val glasses = c.inventory[glassesInventoryIndex]
    if (!glasses.category.equals("Gear", true) || !glasses.name.equals("Smart Glasses", true)) return "این آیتم Smart Glasses نیست"
    val nextWorn = !glasses.equipped
    updateCharacter(id) { ch -> ch.copy(inventory = ch.inventory.mapIndexed { i, it ->
        when {
            i == glassesInventoryIndex -> it.copy(equipped = nextWorn)
            nextWorn && it.category.equals("Gear", true) && it.name.equals("Smart Glasses", true) -> it.copy(equipped = false)
            else -> it
        }
    }) }
    return "OK|${if (nextWorn) "WORN" else "REMOVED"}"
}

/**
 * Install a purchased cyberware instance. Buying never installs it.
 * Returns OK|<HL paid>|<count> on success, otherwise a Persian validation error.
 */
fun CharacterViewModel.installCyberware(id: Int, inventoryIndex: Int): String {
    var c = getCharacter(id) ?: return "کاراکتر یافت نشد"
    if (inventoryIndex !in c.inventory.indices) return "آیتم پیدا نشد"
    c = ensureInventoryInstanceIds(c)
    val item = c.inventory[inventoryIndex]
    if (!item.category.equals("Cyberware", true)) return "این آیتم Cyberware نیست"
    if (item.equipped) return "این Cyberware از قبل نصب شده است"
    com.cyberpunk.gmtool.data.CyberwareCatalog.validationError(item.name, c, inventoryIndex)?.let { return it }

    val targetIndices = if (com.cyberpunk.gmtool.data.CyberwareCatalog.isPaired(item.name)) {
        c.inventory.mapIndexedNotNull { i, it ->
            if (it.category.equals("Cyberware", true) && !it.equipped && it.name.equals(item.name, true)) i else null
        }.take(2)
    } else listOf(inventoryIndex)
    if (targetIndices.isEmpty()) return "آیتم قابل نصب پیدا نشد"

    val pairedFoundations = if (targetIndices.size == 2)
        com.cyberpunk.gmtool.data.CyberwareCatalog.pairedFoundationIds(item.name, c) else emptyList()

    var working = c
    var totalLoss = 0
    targetIndices.forEachIndexed { pos, idx ->
        val currentItem = working.inventory[idx]
        val parent = pairedFoundations.getOrNull(pos)
            ?: com.cyberpunk.gmtool.data.CyberwareCatalog.foundationForInstall(currentItem.name, working)
        val payHl = com.cyberpunk.gmtool.data.CyberwareCatalog.shouldPayHumanityOnInstall(currentItem, working)
        val loss = if (payHl) com.cyberpunk.gmtool.data.CyberwareCatalog.rollHumanityLoss(currentItem.name) else 0
        totalLoss += loss
        val updated = currentItem.copy(
            equipped = true,
            installedIn = parent,
            cyberwareInstallCount = currentItem.cyberwareInstallCount + 1,
            humanityLossPaid = currentItem.humanityLossPaid + loss
        )
        working = working.copy(inventory = working.inventory.mapIndexed { i, it -> if (i == idx) updated else it })
    }

    // Reattaching a previously-used Quick Change Cyberarm brings its already-mounted options back with it.
    val mainAfter = working.inventory[targetIndices.first()]
    if (mainAfter.name.equals("Cyberarm", true) && mainAfter.cyberwareInstallCount > 1) {
        val hasUsedQuickChange = working.inventory.any {
            it.name.contains("Quick Change Mount", true) && it.installedIn == mainAfter.instanceId && it.cyberwareInstallCount > 0
        }
        if (hasUsedQuickChange) {
            working = working.copy(inventory = working.inventory.map { child ->
                if (child.installedIn == mainAfter.instanceId && child.cyberwareInstallCount > 0) child.copy(equipped = true) else child
            })
        }
    }

    val maxHum = com.cyberpunk.gmtool.data.GameRules.maxHumanity(working.baseEmp.coerceAtLeast(working.stats.emp), working.inventory)
    working = working.copy(currentHumanity = (working.currentHumanity - totalLoss).coerceIn(0, maxHum), maxHumanity = maxHum)
    persist(_characters.value.map { if (it.id == id) working else it })
    return "OK|$totalLoss|${targetIndices.size}"
}


/**
 * Remove/uninstall cyberware. Humanity already lost is never restored here; only Maximum Humanity can rise.
 * Quick Change Mount removes the arm together with its mounted options without "uninstalling" those options.
 */
fun CharacterViewModel.uninstallCyberware(id: Int, inventoryIndex: Int): String {
    val c = getCharacter(id) ?: return "کاراکتر یافت نشد"
    if (inventoryIndex !in c.inventory.indices) return "آیتم پیدا نشد"
    val item = c.inventory[inventoryIndex]
    if (!item.category.equals("Cyberware", true) || !item.equipped) return "این Cyberware نصب نیست"

    val children = c.inventory.filter { it.equipped && it.installedIn == item.instanceId }
    val isQuickArm = item.name.equals("Cyberarm", true) && c.inventory.any {
        it.name.contains("Quick Change Mount", true) && it.installedIn == item.instanceId && it.cyberwareInstallCount > 0
    }
    if (children.isNotEmpty() && !isQuickArm) {
        return "ابتدا Optionهای نصب‌شده روی ${item.name} را خارج کن (${children.joinToString { it.name }})."
    }

    // Paired option: remove both copies together, but do not refund Humanity.
    val paired = com.cyberpunk.gmtool.data.CyberwareCatalog.isPaired(item.name)
    val pairIds = if (paired) c.inventory.filter { it.equipped && it.name.equals(item.name, true) }.take(2).map { it.instanceId }.toSet() else emptySet()
    val inv = c.inventory.map { it2 ->
        when {
            isQuickArm && (it2.instanceId == item.instanceId || it2.installedIn == item.instanceId) -> it2.copy(equipped = false)
            paired && it2.instanceId in pairIds -> it2.copy(equipped = false)
            it2.instanceId == item.instanceId -> it2.copy(equipped = false, installedIn = null)
            else -> it2
        }
    }
    // Deliberately preserve currentHumanity. syncDerived only recalculates the now-higher Humanity ceiling.
    updateCharacter(id) { it.copy(inventory = inv) }
    return "OK"
}

/** دکمه‌ی پوشیدن/نصب و درآوردن/حذف. خروجی برای نمایش خطای prerequisite/slot در UI است. */
fun CharacterViewModel.toggleEquipItem(id: Int, index: Int): String {
    val c = getCharacter(id) ?: return "کاراکتر یافت نشد"
    if (index !in c.inventory.indices) return "آیتم پیدا نشد"
    val item = c.inventory[index]
    return when {
        item.category.equals("Armor", true) -> {
            val nowEquipped = !item.equipped
            // هویتِ آیتم، ایندکس/instanceId است نه نام نمایشی؛
            // syncDerived هم داخل خودِ persist انجام می‌شود.
            updateCharacter(id) { ch -> ch.copy(inventory = ch.inventory.mapIndexed { i, it -> if (i == index) it.copy(equipped = nowEquipped) else it }) }
            "OK"
        }
        item.category.equals("Cyberware", true) -> {
            if (item.equipped) uninstallCyberware(id, index) else installCyberware(id, index)
        }
        item.category.equals("Gear", true) && com.cyberpunk.gmtool.data.GameRules.isExternalLinearFrame(item) -> {
            val plugsInstalled = c.inventory.count { it.equipped && it.category.equals("Cyberware", true) && it.name.equals("Interface Plugs", true) }
            val required = if (item.name.contains("Beta", true)) 2 else 1
            if (!item.equipped && plugsInstalled < required) return "این Linear Frame برای کارکرد به $required نصب Interface Plugs نیاز دارد."
            val nowEquipped = !item.equipped
            updateCharacter(id) { ch ->
                val inv = ch.inventory.mapIndexed { i, it ->
                    when {
                        i == index -> it.copy(equipped = nowEquipped)
                        nowEquipped && com.cyberpunk.gmtool.data.GameRules.isExternalLinearFrame(it) -> it.copy(equipped = false)
                        else -> it
                    }
                }
                com.cyberpunk.gmtool.data.GameRules.syncDerived(ch.copy(inventory = inv))
            }
            "OK"
        }
        else -> "این آیتم حالت Equip ندارد"
    }
}
